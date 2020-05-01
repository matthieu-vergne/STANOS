package fr.vergne.stanos;

import static fr.vergne.stanos.codeitem.Constructor.*;
import static fr.vergne.stanos.codeitem.Lambda.*;
import static fr.vergne.stanos.codeitem.Method.*;
import static fr.vergne.stanos.codeitem.Type.*;
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

import fr.vergne.stanos.DependencyTestCasesBuilder.DependencyTestCase;
import fr.vergne.stanos.codeitem.Constructor;
import fr.vergne.stanos.codeitem.Lambda;
import fr.vergne.stanos.codeitem.Method;

// TODO declare type in package (class, abstract class, interface, enum)
public interface DependencyAnalyserTest {

	DependencyAnalyser createDependencyAnalyzer();

	interface EmptyInterface {
		// Not even a constructor
	}

	@Test
	default void testEmptyInterfaceHasNoDependency() {
		DependencyAnalyser analyser = createDependencyAnalyzer();

		Collection<Dependency> dependencies = analyser.analyse(EmptyInterface.class);

		assertEquals(Collections.emptyList(), dependencies);
	}

	static Stream<DependencyTestCase> testDependenciesAreGenerated() {
		DependencyTestCasesBuilder cases = new DependencyTestCasesBuilder();
		Supplier<Class<?>> sameClass = () -> cases.getLastAnalysedClass();

		cases.testType(Declare.Type.AsInnerClass.class).declares(1, type(Declare.Type.AsInnerClass.Declared.class));
		cases.testType(Declare.Type.AsInterface.class).declares(1, type(Declare.Type.AsInterface.Declared.class));
		cases.testType(Declare.Type.AsStaticClass.class).declares(1, type(Declare.Type.AsStaticClass.Declared.class));

		cases.testType(Declare.Method.Simple.class).declares(1, method(sameClass.get(), void.class, "method"));
		cases.testType(Declare.Method.Complex.class).declares(1,
				method(sameClass.get(), int.class, "method", String.class, List.class));
		cases.testType(Declare.Method.InInterface.class).declares(1, method(sameClass.get(), void.class, "method"));
		cases.testType(Declare.Method.WithDefaultImpl.class).declares(1, method(sameClass.get(), void.class, "method"));

		Method lambdaMethod = method(Declare.Lambda.MyLambda.class, void.class, "x");
		Supplier<Lambda> lambda = () -> lambda(type(sameClass.get()), "lambda$0", lambdaMethod);
		cases.testType(Declare.Lambda.InConstructor.class).declares(0, lambda.get());// hide byte code duplicates
		cases.testConstructor(Declare.Lambda.InConstructor.class).declares(1, lambda.get());
		cases.testConstructor(Declare.Lambda.OnField.class).declares(1, lambda.get());
		cases.testStaticBlock(Declare.Lambda.InStaticBlock.class).declares(1, lambda.get());
		cases.testStaticBlock(Declare.Lambda.OnStaticField.class).declares(1, lambda.get());
		cases.testMethod(Declare.Lambda.InMethod.class, "method").declares(1, lambda.get());
		cases.testMethod(Declare.Lambda.InStaticMethod.class, "method").declares(1, lambda.get());
		cases.testMethod(Declare.Lambda.InDefaultImpl.class, "method").declares(1, lambda.get());
		Lambda lambdaParent = lambda(type(Declare.Lambda.InLambda.class), "lambda$0", lambdaMethod);
		Lambda lambdaChild = lambda(type(Declare.Lambda.InLambda.class), "lambda$1", lambdaMethod);
		cases.testMethod(Declare.Lambda.InLambda.class, "method").declares(1, lambdaParent).and(0, lambdaChild);
		cases.testLambda(Declare.Lambda.InLambda.class, lambdaParent).declares(0, lambdaParent).and(1, lambdaChild);

		Constructor cons = constructor(Call.Constructor.Called.class);
		Constructor consNes = nestedConstructor(Call.Constructor.Called.Nested.class);
		cases.testConstructor(Call.Constructor.InConstructor.class).calls(1, cons).and(0, consNes);
		cases.testConstructor(Call.Constructor.OnField.class).calls(1, cons).and(0, consNes);
		cases.testStaticBlock(Call.Constructor.InStaticBlock.class).calls(1, cons).and(0, consNes);
		cases.testStaticBlock(Call.Constructor.OnStaticField.class).calls(1, cons).and(0, consNes);
		cases.testMethod(Call.Constructor.InMethod.class, "method").calls(1, cons).and(0, consNes);
		cases.testMethod(Call.Constructor.InStaticMethod.class, "method").calls(1, cons).and(0, consNes);
		cases.testMethod(Call.Constructor.MultipleTimes.class, "method").calls(2, cons).and(0, consNes);
		cases.testMethod(Call.Constructor.OfNestedClass.class, "method").calls(1, cons).and(1, consNes);
		cases.testMethod(Call.Constructor.InDefaultImpl.class, "method").calls(1, cons).and(0, consNes);
		Lambda constructorLambda = lambda(type(Call.Constructor.InLambda.class), "lambda$0",
				method(Runnable.class, void.class, "run"));
		cases.testLambda(Call.Constructor.InLambda.class, constructorLambda).calls(1, cons);

		Method methodA = method(Call.Method.Called.class, Object.class, "a");
		Method methodB = method(Call.Method.Called.class, void.class, "b");
		cases.testConstructor(Call.Method.InConstructor.class).calls(1, methodA).and(0, methodB);
		cases.testConstructor(Call.Method.OnField.class).calls(1, methodA).and(0, methodB);
		cases.testStaticBlock(Call.Method.InStaticBlock.class).calls(1, methodA).and(0, methodB);
		cases.testStaticBlock(Call.Method.OnStaticField.class).calls(1, methodA).and(0, methodB);
		cases.testMethod(Call.Method.InMethod.class, "method").calls(1, methodA).and(0, methodB);
		cases.testMethod(Call.Method.InStaticMethod.class, "method").calls(1, methodA).and(0, methodB);
		cases.testMethod(Call.Method.MultipleTimes.class, "method").calls(2, methodA).and(0, methodB);
		cases.testMethod(Call.Method.WithOthers.class, "method").calls(1, methodA).and(1, methodB);
		cases.testMethod(Call.Method.InDefaultImpl.class, "method").calls(1, methodA).and(0, methodB);
		Lambda methodLambda = lambda(type(Call.Method.InLambda.class), "lambda$0",
				method(Runnable.class, void.class, "run"));
		cases.testLambda(Call.Method.InLambda.class, methodLambda).calls(1, methodA).and(0, methodB);

		return cases.build();
	}

	@ParameterizedTest
	@MethodSource
	default void testDependenciesAreGenerated(DependencyTestCase args) {
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
}
