package fr.vergne.stanos;

import static fr.vergne.stanos.node.Constructor.*;
import static fr.vergne.stanos.node.Lambda.*;
import static fr.vergne.stanos.node.Method.*;
import static fr.vergne.stanos.node.StaticBlock.*;
import static fr.vergne.stanos.node.Type.*;
import static fr.vergne.stanos.util.Formatter.*;
import static java.util.function.Predicate.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import fr.vergne.stanos.CasesBuilder.Arguments;
import fr.vergne.stanos.CasesBuilder.Targeter;
import fr.vergne.stanos.Reducer.BiReducer;
import fr.vergne.stanos.node.Constructor;
import fr.vergne.stanos.node.Executable;
import fr.vergne.stanos.node.Lambda;
import fr.vergne.stanos.node.Method;
import fr.vergne.stanos.node.StaticBlock;
import fr.vergne.stanos.node.Type;

public interface DependencyAnalyserTest {

	DependencyAnalyser createDependencyAnalyzer();

	CasesBuilder cases = new CasesBuilder();

	interface EmptyInterface {
		// Not even a constructor
	}

	@Test
	default void testEmptyInterfaceHasNoDependency() {
		DependencyAnalyser analyser = createDependencyAnalyzer();

		Collection<Dependency> dependencies = analyser.analyse(EmptyInterface.class);

		assertEquals(Collections.emptyList(), dependencies);
	}

	static Stream<Arguments<?, ?>> testBasicDeclarations() {
		Method lambdaMethod = method(Declare.Lambda.MyLambda.class, void.class, "x");
		Supplier<Lambda> lambda = () -> lambda(type(classCache[0]), "lambda$0", lambdaMethod);

		testType.x(Declare.Type.AsInnerClass.class).declares(1, type(Declare.Type.AsInnerClass.Declared.class));
		testType.x(Declare.Type.AsInterface.class).declares(1, type(Declare.Type.AsInterface.Declared.class));
		testType.x(Declare.Type.AsStaticClass.class).declares(1, type(Declare.Type.AsStaticClass.Declared.class));

		testType.x(Declare.Method.Simple.class).declares(1, method(sameClass.get(), void.class, "method"));
		testType.x(Declare.Method.Complex.class).declares(1,
				method(sameClass.get(), int.class, "method", String.class, List.class));
		testType.x(Declare.Method.InInterface.class).declares(1, method(sameClass.get(), void.class, "method"));
		testType.x(Declare.Method.WithDefaultImpl.class).declares(1, method(sameClass.get(), void.class, "method"));

		testType.x(Declare.Lambda.InConstructor.class).declares(0, lambda.get());// hide byte code duplicates
		testExecutable.x(Declare.Lambda.InConstructor.class, Constructor.NAME).declares(1, lambda.get());
		testExecutable.x(Declare.Lambda.InStaticBlock.class, StaticBlock.NAME).declares(1, lambda.get());
		testExecutable.x(Declare.Lambda.OnField.class, Constructor.NAME).declares(1, lambda.get());
		testExecutable.x(Declare.Lambda.OnStaticField.class, StaticBlock.NAME).declares(1, lambda.get());
		testExecutable.x(Declare.Lambda.InMethod.class, "method").declares(1, lambda.get());
		testExecutable.x(Declare.Lambda.InStaticMethod.class, "method").declares(1, lambda.get());
		testExecutable.x(Declare.Lambda.InDefaultImpl.class, "method").declares(1, lambda.get());
		Lambda lambdaParent = lambda(type(Declare.Lambda.InLambda.class), "lambda$0", lambdaMethod);
		Lambda lambdaChild = lambda(type(Declare.Lambda.InLambda.class), "lambda$1", lambdaMethod);
		testExecutable.x(Declare.Lambda.InLambda.class, "method").declares(1, lambdaParent).and(0, lambdaChild);
		testLambda.x(Declare.Lambda.InLambda.class, lambdaParent).declares(0, lambdaParent).and(1, lambdaChild);

		return cases.buildAndClean();
	}

	@ParameterizedTest
	@MethodSource
	default void testBasicDeclarations(Arguments<?, ?> args) {
		testTemplate(args);
	}

	static Stream<Arguments<?, ?>> testBasicCalls() {
		Constructor cons = constructor(Call.Constructor.Called.class);
		Constructor consNes = nestedConstructor(Call.Constructor.Called.Nested.class);
		Method methodA = method(Call.Method.Called.class, Object.class, "a");
		Method methodB = method(Call.Method.Called.class, void.class, "b");

		testExecutable.x(Call.Constructor.InConstructor.class, Constructor.NAME).calls(1, cons).and(0, consNes);
		testExecutable.x(Call.Constructor.InStaticBlock.class, StaticBlock.NAME).calls(1, cons).and(0, consNes);
		testExecutable.x(Call.Constructor.OnField.class, Constructor.NAME).calls(1, cons).and(0, consNes);
		testExecutable.x(Call.Constructor.OnStaticField.class, StaticBlock.NAME).calls(1, cons).and(0, consNes);
		testExecutable.x(Call.Constructor.InMethod.class, "method").calls(1, cons).and(0, consNes);
		testExecutable.x(Call.Constructor.InStaticMethod.class, "method").calls(1, cons).and(0, consNes);
		testExecutable.x(Call.Constructor.MultipleTimes.class, "method").calls(2, cons).and(0, consNes);
		Lambda lambda = lambda(type(Call.Constructor.InLambda.class), "lambda$0",
				method(Runnable.class, void.class, "run"));
		testLambda.x(Call.Constructor.InLambda.class, lambda).calls(1, cons);
		testExecutable.x(Call.Constructor.OfNestedClass.class, "method").calls(1, cons).and(1, consNes);
		testExecutable.x(Call.Constructor.InDefaultImpl.class, "method").calls(1, cons).and(0, consNes);

		testExecutable.x(Call.Method.InConstructor.class, Constructor.NAME).calls(1, methodA).and(0, methodB);
		testExecutable.x(Call.Method.InStaticBlock.class, StaticBlock.NAME).calls(1, methodA).and(0, methodB);
		testExecutable.x(Call.Method.OnField.class, Constructor.NAME).calls(1, methodA).and(0, methodB);
		testExecutable.x(Call.Method.OnStaticField.class, StaticBlock.NAME).calls(1, methodA).and(0, methodB);
		testExecutable.x(Call.Method.InMethod.class, "method").calls(1, methodA).and(0, methodB);
		testExecutable.x(Call.Method.InStaticMethod.class, "method").calls(1, methodA).and(0, methodB);
		lambda = lambda(type(Call.Method.InLambda.class), "lambda$0", method(Runnable.class, void.class, "run"));
		testLambda.x(Call.Method.InLambda.class, lambda).calls(1, methodA).and(0, methodB);
		testExecutable.x(Call.Method.MultipleTimes.class, "method").calls(2, methodA).and(0, methodB);
		testExecutable.x(Call.Method.WithOthers.class, "method").calls(1, methodA).and(1, methodB);
		testExecutable.x(Call.Method.InDefaultImpl.class, "method").calls(1, methodA).and(0, methodB);

		return cases.buildAndClean();
	}

	@ParameterizedTest
	@MethodSource
	default void testBasicCalls(Arguments<?, ?> args) {
		testTemplate(args);
	}

	default void testTemplate(Arguments<?, ?> args) {
		// GIVEN
		DependencyAnalyser analyser = createDependencyAnalyzer();

		// WHEN
		Collection<Dependency> dependencies = analyser.analyse(args.analysedClass());

		// THEN
		List<Dependency> found = dependencies.stream().filter(isEqual(args.dependency())).collect(Collectors.toList());
		Supplier<String> failureMessage = () -> {
			String extendedMessage = String.format("Search for %s in %s", args.dependency(), dependencies.toString());
			return removeClassPrefixes(extendedMessage, DependencyAnalyserTest.class);
		};
		assertEquals(args.count(), found.size(), failureMessage);
	}

	static final Class<?>[] classCache = { null };
	static final Supplier<Class<?>> sameClass = () -> classCache[0];
	static final Reducer<Class<?>, Targeter<Type>> testType = clazz -> {
		classCache[0] = clazz;
		return cases.analyse(clazz).test(type(clazz));
	};
	static final BiReducer<Class<?>, String, Targeter<Executable>> testExecutable = (clazz, name) -> {
		classCache[0] = clazz;
		return cases.analyse(clazz).test(noArgCallerFactory(clazz).x(name));
	};
	static final BiReducer<Class<?>, Lambda, Targeter<Lambda>> testLambda = (clazz, lambda) -> {
		classCache[0] = clazz;
		return cases.analyse(clazz).test(lambda);
	};

	static class Declare {
		static class Type {
			static class AsInnerClass {
				class Declared {
				}
			}

			static class AsInterface {
				interface Declared {
				}
			}

			static class AsStaticClass {
				static class Declared {
				}
			}

			// TODO with generics
			// TODO in method
		}

		static class Method {
			static class Simple {
				void method() {
				}
			}

			static class Complex {
				int method(String s, List<Object> l) {
					return 0;
				}
			}

			// TODO with generics

			interface InInterface {
				void method();
			}

			interface WithDefaultImpl {
				default void method() {
				}
			}
		}

		@SuppressWarnings("unused")
		static class Lambda {
			static interface MyLambda {
				void x();
			}

			static class InConstructor {
				public InConstructor() {
					MyLambda r = () -> {
					};
				}
			}

			static class InStaticBlock {
				static MyLambda FIELD;
				static {
					FIELD = () -> {
					};
				}
			}

			static class OnField {
				MyLambda field = () -> {
				};
			}

			static class OnStaticField {
				static MyLambda FIELD = () -> {
				};
			}

			static class InMethod {
				void method() {
					MyLambda r = () -> {
					};
				}
			}

			static class InStaticMethod {
				static void method() {
					MyLambda r = () -> {
					};
				}
			}

			class InLambda {
				void method() {
					MyLambda r1 = () -> {
						MyLambda r2 = () -> {
						};
					};
				}
			}

			interface InDefaultImpl {
				default void method() {
					MyLambda r = () -> {
					};
				}
			}

		}
	}

	static class Call {
		static class Constructor {
			static class Called {
				class Nested {
				}
			}

			static class InConstructor {
				public InConstructor() {
					new Call.Constructor.Called();
				}
			}

			static class InStaticBlock {
				static Call.Constructor.Called FIELD;
				static {
					FIELD = new Call.Constructor.Called();
				}
			}

			static class OnField {
				Call.Constructor.Called field = new Call.Constructor.Called();
			}

			static class OnStaticField {
				static Call.Constructor.Called FIELD = new Call.Constructor.Called();
			}

			static class InMethod {
				void method() {
					new Call.Constructor.Called();
				}
			}

			static class InStaticMethod {
				static void method() {
					new Call.Constructor.Called();
				}
			}

			static class MultipleTimes {
				void method() {
					new Call.Constructor.Called();
					new Call.Constructor.Called();
				}
			}

			@SuppressWarnings("unused")
			static class InLambda {
				static void method() {
					Runnable r1 = () -> {
						new Call.Constructor.Called();
					};
				}
			}

			static class OfNestedClass {
				void method() {
					new Call.Constructor.Called().new Nested();
				}
			}

			interface InDefaultImpl {
				default void method() {
					new Call.Constructor.Called();
				}
			}
		}

		static class Method {
			static class Called {
				Object a() {
					return null;
				}

				void b() {
				}
			}

			static Call.Method.Called called = new Call.Method.Called();

			static class InConstructor {
				public InConstructor() {
					called.a();
				}
			}

			static class InStaticBlock {
				static Object FIELD;
				static {
					FIELD = called.a();
				}
			}

			static class OnField {
				Object field = called.a();
			}

			static class OnStaticField {
				static Object FIELD = called.a();
			}

			static class InMethod {
				void method() {
					called.a();
				}
			}

			static class InStaticMethod {
				static void method() {
					called.a();
				}
			}

			@SuppressWarnings("unused")
			interface InLambda {
				default void method() {
					Runnable r = () -> {
						called.a();
					};
				}
			}

			static class MultipleTimes {
				void method() {
					called.a();
					called.a();
				}
			}

			static class WithOthers {
				void method() {
					called.a();
					called.b();
				}
			}

			interface InDefaultImpl {
				default void method() {
					called.a();
				}
			}
		}
	}

	static Reducer<String, Executable> noArgCallerFactory(Class<?> callerClass) {
		return name -> Constructor.NAME.equals(name) ? constructor(callerClass)
				: StaticBlock.NAME.equals(name) ? staticBlock(callerClass) : method(callerClass, void.class, name);
	}
}
