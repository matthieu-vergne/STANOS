package fr.vergne.stanos.dependency.bytecode.asm;

import static fr.vergne.stanos.dependency.Action.*;
import static fr.vergne.stanos.dependency.codeitem.Constructor.*;
import static fr.vergne.stanos.dependency.codeitem.Lambda.*;
import static fr.vergne.stanos.dependency.codeitem.Method.*;
import static fr.vergne.stanos.dependency.codeitem.StaticBlock.*;
import static java.util.Spliterators.*;
import static java.util.stream.StreamSupport.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import fr.vergne.stanos.code.Code;
import fr.vergne.stanos.code.CodeSelector;
import fr.vergne.stanos.dependency.Dependency;
import fr.vergne.stanos.dependency.DependencyAnalyser;
import fr.vergne.stanos.dependency.codeitem.CodeItem;
import fr.vergne.stanos.dependency.codeitem.Constructor;
import fr.vergne.stanos.dependency.codeitem.Callable;
import fr.vergne.stanos.dependency.codeitem.Lambda;
import fr.vergne.stanos.dependency.codeitem.Method;
import fr.vergne.stanos.dependency.codeitem.Package;
import fr.vergne.stanos.dependency.codeitem.StaticBlock;
import fr.vergne.stanos.dependency.codeitem.Type;

@SuppressWarnings("unused")
public class ASMByteCodeAnalyser implements DependencyAnalyser {

	private static final int ASM_VERSION = Opcodes.ASM8;

	@Override
	public Collection<Dependency> analyze(CodeSelector codes) {
		return codes.getCodes().map(Code::open).map(this::analyze).flatMap(Collection<Dependency>::stream)//
				.filter(expectedDuplicates())// Filter duplicates built by this implementation
				.collect(Collectors.toList());
	}

	/**
	 * We expect duplications on several kinds of dependencies:
	 * <ul>
	 * <li>Package declarations: they are inferred from analyzed types. A package
	 * hierarchy will be declared as many times as there is types in it, including
	 * inner types.
	 * <li>Parent type declarations: inner classes infer their own parent classes. A
	 * class hierarchy will be declared as many times as there is inner types in it.
	 * </ul>
	 */
	private Predicate<Dependency> expectedDuplicates() {
		Set<Dependency> expectedDuplicates = new HashSet<>();
		return dep -> {
			if (!(dep.getAction().equals(DECLARES)
					&& (dep.getTarget() instanceof Type || dep.getTarget() instanceof Package))) {
				return true;
			}
			if (expectedDuplicates.contains(dep)) {
				return false;
			}
			expectedDuplicates.add(dep);
			return true;
		};
	}

	@Override
	public Collection<Dependency> analyze(InputStream inputStream) {
		List<Dependency> dependencies = new LinkedList<>();
		ClassVisitor visitor = createClassVisitor(dependencies);
		int options = 0;// TODO ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG |
						// ClassReader.SKIP_FRAMES;
		ClassReader classReader;
		try {
			classReader = new ClassReader(inputStream);
		} catch (IOException | ArrayIndexOutOfBoundsException cause) {
			throw new IllegalArgumentException(cause);
		}
		classReader.accept(visitor, options);
		return dependencies;
	}

	private ClassVisitor createClassVisitor(Collection<Dependency> dependencies) {
		return new ClassVisitor(ASM_VERSION) {

			private Type classType;
			private final Map<String, Lambda> lambdas = new HashMap<>();

			@Override
			public void visit(int version, int access, String classPath, String signature, String superName,
					String[] interfaces) {
				classType = Type.fromClassPath(classPath);
				Type rootType = declareClassHierarchy(classType);
				declarePackageHierarchy(rootType);
			}

			private Type declareClassHierarchy(Type type) {
				String typePath = type.getId();
				int lastDollarIndex = typePath.lastIndexOf('$');
				boolean isInnerType = lastDollarIndex != -1;
				if (isInnerType) {
					String parentTypeName = typePath.substring(0, lastDollarIndex);
					Type parentType = Type.fromClassName(parentTypeName);
					dependencies.add(new Dependency(parentType, DECLARES, type));
					return declareClassHierarchy(parentType);
				} else {
					return type;
				}
			}

			private void declarePackageHierarchy(CodeItem item) {
				String itemPath = item.getId();
				int lastDotIndex = itemPath.lastIndexOf('.');
				boolean isInPackage = lastDotIndex != -1;
				if (isInPackage) {
					String packageName = itemPath.substring(0, lastDotIndex);
					Package itemPackage = Package.fromPackageName(packageName);
					dependencies.add(new Dependency(itemPackage, DECLARES, item));
					declarePackageHierarchy(itemPackage);
				}
			}

			@Override
			public void visitInnerClass(String name, String outerName, String innerName, int access) {
				if (outerName != null && Type.fromClassPath(outerName).equals(classType)) {
					Type nestedType = Type.fromClassPath(name);
					dependencies.add(new Dependency(classType, DECLARES, nestedType));
				}
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
					String[] exceptions) {
				Callable caller;
				if (name.startsWith(Lambda.NAME_PREFIX)) {
					caller = lambdas.get(lambdaId(classType, name));
					// Do not declare here, already done when it was found
				} else {
					caller = createExecutable(classType, name, descriptor);
					dependencies.add(new Dependency(classType, DECLARES, caller));
				}

				return new MethodVisitor(ASM_VERSION) {

					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
							boolean isInterface) {
						Callable called = createExecutable(Type.fromClassPath(owner), name, descriptor);
						dependencies.add(new Dependency(caller, CALLS, called));
					}

					@Override
					public void visitInvokeDynamicInsn(String methodName, String descriptor,
							Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
						org.objectweb.asm.Handle handle = (Handle) bootstrapMethodArguments[1];
						String handleName = handle.getName();
						if (handleName.startsWith(Lambda.NAME_PREFIX)) {
							String lambdaId = lambdaId(Type.fromClassPath(handle.getOwner()), handleName);
							Method lambdaMethod = (Method) createExecutable(extractReturnType(descriptor), methodName,
									handle.getDesc());
							Lambda lambda = lambda(lambdaId, lambdaMethod);
							lambdas.put(lambdaId, lambda);
							dependencies.add(new Dependency(caller, DECLARES, lambda));
						} else {
							// TODO Manage cast: java/lang/Class.cast(Ljava/lang/Object;)Ljava/lang/Object;
							// (5)
							System.err.println("[WARN] Unmanaged handle: " + handle);
						}
					}
				};
			}

			private Callable createExecutable(Type ownerType, String name, String descriptor) {
				List<Type> argsTypes = extractArgsTypes(descriptor);
				if (Constructor.NAME.equals(name)) {
					return constructor(ownerType, argsTypes);
				} else if (StaticBlock.NAME.equals(name)) {
					return staticBlock(ownerType, argsTypes);
				} else {
					Type returnType = extractReturnType(descriptor);
					return method(ownerType, returnType, name, argsTypes);
				}
			}

			private List<Type> extractArgsTypes(String descriptor) {
				return Stream.of(org.objectweb.asm.Type.getArgumentTypes(descriptor))
						.map(org.objectweb.asm.Type::getClassName).map(Type::fromClassName)
						.collect(Collectors.toList());
			}

			private Type extractReturnType(String descriptor) {
				org.objectweb.asm.Type asmType = org.objectweb.asm.Type.getReturnType(descriptor);
				return Type.fromClassName(asmType.getClassName());
			}
		};
	}
}
