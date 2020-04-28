package fr.vergne.stanos.bytecode.asm;

import static fr.vergne.stanos.Action.*;
import static java.util.Spliterators.*;
import static java.util.stream.StreamSupport.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.ASMifier;

import fr.vergne.stanos.Dependency;
import fr.vergne.stanos.DependencyAnalyser;
import fr.vergne.stanos.node.Method;
import fr.vergne.stanos.node.Type;

@SuppressWarnings("unused")
public class ASMByteCodeAnalyser implements DependencyAnalyser {

	private static final int ASM_VERSION = Opcodes.ASM8;

	public ASMByteCodeAnalyser() {
		// TODO Auto-generated constructor stub
	}

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

			Type classType;

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
				Method method = createMethod(classType, name, descriptor);
				dependencies.add(new Dependency(classType, DECLARES, method));

				return new MethodVisitor(ASM_VERSION) {

					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
							boolean isInterface) {
						Method calledMethod = createMethod(Type.fromClassPath(owner), name, descriptor);
						dependencies.add(new Dependency(method, CALLS, calledMethod));
					}

					@Override
					public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
							Object... bootstrapMethodArguments) {
						System.err.println(String.format("InvokeDynamic: %s %s %s %s", name, descriptor, bootstrapMethodHandle,
								bootstrapMethodArguments));
						// TODO Auto-generated method stub
						super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
					}
				};
			}

			private Method createMethod(Type classType, String methodName, String methodDescriptor) {
				int argsStart = methodDescriptor.indexOf('(') + 1;
				int argsEnd = methodDescriptor.indexOf(')');
				Iterator<String> argsIterator = new ClassNameIterator(methodDescriptor.substring(argsStart, argsEnd));
				Spliterator<String> argsSpliterator = spliteratorUnknownSize(argsIterator, Spliterator.ORDERED);
				List<Type> argsTypes = stream(argsSpliterator, false).map(Type::fromClassName)
						.collect(Collectors.toList());

				int returnStart = argsEnd + 1;
				Type returnType = Type
						.fromClassName(new ClassNameIterator(methodDescriptor.substring(returnStart)).next());

				Method method = Method.method(classType, returnType, methodName, argsTypes);
				return method;
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
//		new ASMByteCodeAnalyser().analyse(new File(
//				"/home/matthieu/Programing/Java/Pester/pester-core/target/classes/fr/vergne/pester/util/cache/Cache.class"))
//				.stream().forEach(System.out::println);
//		System.out.println();
		new ASMByteCodeAnalyser().analyse(A.class).stream().forEach(System.out::println);
		System.out.println();
		new ASMByteCodeAnalyser().analyse(B.class).stream().forEach(System.out::println);
	}

	private static void asmifier(String classPath) throws IOException {
		String[] strs = { classPath };
		ASMifier.main(strs);
	}
}
