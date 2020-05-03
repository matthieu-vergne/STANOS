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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
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

import fr.vergne.stanos.dependency.Dependency;
import fr.vergne.stanos.dependency.DependencyAnalyser;
import fr.vergne.stanos.dependency.codeitem.CodeItem;
import fr.vergne.stanos.dependency.codeitem.Constructor;
import fr.vergne.stanos.dependency.codeitem.Executable;
import fr.vergne.stanos.dependency.codeitem.Lambda;
import fr.vergne.stanos.dependency.codeitem.Method;
import fr.vergne.stanos.dependency.codeitem.StaticBlock;
import fr.vergne.stanos.dependency.codeitem.Type;

@SuppressWarnings("unused")
public class ASMByteCodeAnalyser implements DependencyAnalyser {

	private static final int ASM_VERSION = Opcodes.ASM8;

	@Override
	public Collection<Dependency> analyse(InputStream inputStream) {
		List<Dependency> dependencies = new LinkedList<>();
		ClassVisitor visitor = createVisitor(dependencies);
		int options = 0;// ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES;
		ClassReader classReader;
		try {
			classReader = new ClassReader(inputStream);
		} catch (IOException cause) {
			throw new RuntimeException(cause);
		}
		classReader.accept(visitor, options);
		return dependencies;
	}

	private static ClassVisitor createVisitor(Collection<Dependency> dependencies) {
		return new ClassVisitor(ASM_VERSION) {

			private Type classType;
			private final Map<String, Lambda> lambdas = new HashMap<>();

			@Override
			public void visit(int version, int access, String name, String signature, String superName,
					String[] interfaces) {
				this.classType = Type.fromClassPath(name);
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
				Executable caller;
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
						Executable called = createExecutable(Type.fromClassPath(owner), name, descriptor);
						dependencies.add(new Dependency(caller, CALLS, called));
					}

					@Override
					public void visitInvokeDynamicInsn(String methodName, String descriptor,
							Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
						org.objectweb.asm.Handle handle = (Handle) bootstrapMethodArguments[1];
						String lambdaId = lambdaId(Type.fromClassPath(handle.getOwner()), handle.getName());
						Method lambdaMethod = (Method) createExecutable(extractReturnType(descriptor), methodName,
								handle.getDesc());
						Lambda lambda = lambda(lambdaId, lambdaMethod);
						lambdas.put(lambdaId, lambda);
						dependencies.add(new Dependency(caller, DECLARES, lambda));
					}
				};
			}

			private Executable createExecutable(Type ownerType, String name, String descriptor) {
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
				// TODO use org.objectweb.asm.Type facilities
				int argsStart = descriptor.indexOf('(') + 1;
				int argsEnd = descriptor.indexOf(')');
				Iterator<String> argsIterator = new ClassNameIterator(descriptor.substring(argsStart, argsEnd));
				Spliterator<String> argsSpliterator = spliteratorUnknownSize(argsIterator, Spliterator.ORDERED);
				return stream(argsSpliterator, false).map(Type::fromClassName).collect(Collectors.toList());
			}

			private Type extractReturnType(String descriptor) {
				// TODO use org.objectweb.asm.Type facilities
				int returnStart = descriptor.indexOf(')') + 1;
				String returnDescriptor = descriptor.substring(returnStart);
				String returnClassName = new ClassNameIterator(returnDescriptor).next();
				return Type.fromClassName(returnClassName);
			}
		};
	}

	class A {
		void a() {
		}
	}

	class B {
		A a;

		void b() {
			a = new A();
		}

		void c() {
			a.a();
		}
	}

	public static void main(String[] args) throws Exception {
//		new ASMByteCodeAnalyser().analyse(Paths.get(
//				"/home/matthieu/Programing/Java/Pester/pester-core/target/classes/fr/vergne/pester/util/cache/Cache.class"))
//				.stream().forEach(System.out::println);
//		System.out.println();
		new ASMByteCodeAnalyser().analyse(A.class).stream().forEach(System.out::println);
		System.out.println();
		new ASMByteCodeAnalyser().analyse(B.class).stream().forEach(System.out::println);
	}
}
