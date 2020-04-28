package fr.vergne.stanos;

import static fr.vergne.stanos.node.Constructor.*;
import static fr.vergne.stanos.node.Method.*;
import static fr.vergne.stanos.node.StaticBlock.*;
import static fr.vergne.stanos.node.Type.*;
import static fr.vergne.stanos.util.Formatter.*;
import static java.util.function.Predicate.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
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
import fr.vergne.stanos.DependencyAnalyserTest.MethodCall.Interface;
import fr.vergne.stanos.node.Constructor;
import fr.vergne.stanos.node.Method;
import fr.vergne.stanos.node.Node;
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

	static class Parent {
		interface Interface {
			interface Nested {
				interface SubNested {
				}
			}
		}

		static class StaticNested {
			static class StaticSubNested {
			}
		}

		class Nested {
			class SubNested {
			}
		}
	}

	static Stream<Arguments<?, ?>> testTypeDeclaresInnerType() {
		Reducer<Class<?>, Targeter<Type>> test = clazz -> cases.analyse(clazz).test(type(clazz));

		test.x(Parent.Interface.class).declares(1, type(Parent.Interface.Nested.class)).and(0,
				type(Parent.Interface.Nested.SubNested.class));
		test.x(Parent.Interface.Nested.class).declares(1, type(Parent.Interface.Nested.SubNested.class));

		test.x(Parent.class).declares(1, type(Parent.StaticNested.class)).and(0,
				type(Parent.StaticNested.StaticSubNested.class));
		test.x(Parent.StaticNested.class).declares(1, type(Parent.StaticNested.StaticSubNested.class));

		test.x(Parent.class).declares(1, type(Parent.Nested.class)).and(0, type(Parent.Nested.SubNested.class));
		test.x(Parent.Nested.class).declares(1, type(Parent.Nested.SubNested.class));

		return cases.buildAndClean();
	}

	@ParameterizedTest
	@MethodSource
	default void testTypeDeclaresInnerType(Arguments<?, ?> args) {
		testTemplate(args);
	}

	static class MethodDeclare {
		void method() {
		}

		int complexMethod(boolean b, String s, List<Integer> l) {
			return 0;
		}

		class Nested {
			void nestedMethod() {
			}
		}

		interface Interface {
			void method();

			int complexMethod(boolean b, String s, List<Integer> l);

			interface Nested {
				void nestedMethod();
			}
		}
	}

	static Stream<Arguments<?, ?>> testTypeDeclaresMethod() {
		List<Class<?>> complexArgs = Arrays.asList(boolean.class, String.class, List.class);

		cases.analyse(MethodDeclare.Interface.class).test(type(MethodDeclare.Interface.class))
				.declares(1, method(MethodDeclare.Interface.class, void.class, "method"))
				.and(1, method(MethodDeclare.Interface.class, int.class, "complexMethod", complexArgs))
				.and(0, method(MethodDeclare.Interface.Nested.class, void.class, "nestedMethod"));
		cases.analyse(MethodDeclare.Interface.Nested.class).test(type(MethodDeclare.Interface.Nested.class))
				.declares(0, method(MethodDeclare.Interface.class, void.class, "method"))
				.and(0, method(MethodDeclare.Interface.class, int.class, "complexMethod", complexArgs))
				.and(1, method(MethodDeclare.Interface.Nested.class, void.class, "nestedMethod"));

		cases.analyse(MethodDeclare.class).test(type(MethodDeclare.class))
				.declares(1, method(MethodDeclare.class, void.class, "method"))
				.and(1, method(MethodDeclare.class, int.class, "complexMethod", complexArgs))
				.and(0, method(MethodDeclare.Nested.class, void.class, "nestedMethod"));
		cases.analyse(MethodDeclare.Nested.class).test(type(MethodDeclare.Nested.class))
				.declares(0, method(MethodDeclare.class, void.class, "method"))
				.and(0, method(MethodDeclare.class, int.class, "complexMethod", complexArgs))
				.and(1, method(MethodDeclare.Nested.class, void.class, "nestedMethod"));

		return cases.buildAndClean();
	}

	@ParameterizedTest
	@MethodSource
	default void testTypeDeclaresMethod(Arguments<?, ?> args) {
		testTemplate(args);
	}

	class ConstructorCall {
		static class Callee {
			class Nested {
			}
		}

		public ConstructorCall() {
			new ConstructorCall.Callee().new Nested();
		}

		void monoCall() {
			new ConstructorCall.Callee();
		}

		void duoCall() {
			new ConstructorCall.Callee();
			new ConstructorCall.Callee();
		}

		void nestedCall() {
			new ConstructorCall.Callee().new Nested();
		}

		interface Interface {
			default void monoCall() {
				new ConstructorCall.Callee();
			}

			default void duoCall() {
				new ConstructorCall.Callee();
				new ConstructorCall.Callee();
			}

			default void nestedCall() {
				new ConstructorCall.Callee().new Nested();
			}
		}

		static class Field {
			ConstructorCall.Callee.Nested field = new ConstructorCall.Callee().new Nested();
		}

		static class StaticDeclaration {
			static ConstructorCall.Callee.Nested FIELD = new ConstructorCall.Callee().new Nested();
		}

		static class StaticBlock {
			static ConstructorCall.Callee.Nested FIELD;
			static {
				FIELD = new ConstructorCall.Callee().new Nested();
			}
		}

		static class StaticMethod {
			static void call() {
				new ConstructorCall.Callee().new Nested();
			}
		}
	}

	static Stream<Arguments<?, ?>> testConstructorOrMethodCallsConstructor() {
		Constructor calleeConstructor = constructor(ConstructorCall.Callee.class);
		Constructor nestedConstructor = constructor(ConstructorCall.Callee.Nested.class, ConstructorCall.Callee.class);

		Class<?> clazz;
		Reducer<String, Node> caller;

		clazz = ConstructorCall.class;
		caller = noArgCallerFactory(clazz);
		cases.analyse(clazz).test(caller.x(Constructor.NAME)).calls(1, calleeConstructor).and(1, nestedConstructor);
		cases.analyse(clazz).test(caller.x("monoCall")).calls(1, calleeConstructor).and(0, nestedConstructor);
		cases.analyse(clazz).test(caller.x("duoCall")).calls(2, calleeConstructor).and(0, nestedConstructor);
		cases.analyse(clazz).test(caller.x("nestedCall")).calls(1, calleeConstructor).and(1, nestedConstructor);

		clazz = ConstructorCall.Interface.class;
		caller = noArgCallerFactory(clazz);
		cases.analyse(clazz).test(caller.x("monoCall")).calls(1, calleeConstructor).and(0, nestedConstructor);
		cases.analyse(clazz).test(caller.x("duoCall")).calls(2, calleeConstructor).and(0, nestedConstructor);
		cases.analyse(clazz).test(caller.x("nestedCall")).calls(1, calleeConstructor).and(1, nestedConstructor);

		clazz = ConstructorCall.Field.class;
		caller = noArgCallerFactory(clazz);
		cases.analyse(clazz).test(caller.x(Constructor.NAME)).calls(1, calleeConstructor).and(1, nestedConstructor);

		clazz = ConstructorCall.StaticDeclaration.class;
		caller = noArgCallerFactory(clazz);
		cases.analyse(clazz).test(caller.x(StaticBlock.NAME)).calls(1, calleeConstructor).and(1, nestedConstructor);

		clazz = ConstructorCall.StaticBlock.class;
		caller = noArgCallerFactory(clazz);
		cases.analyse(clazz).test(caller.x(StaticBlock.NAME)).calls(1, calleeConstructor).and(1, nestedConstructor);

		clazz = ConstructorCall.StaticMethod.class;
		caller = noArgCallerFactory(clazz);
		cases.analyse(clazz).test(caller.x("call")).calls(1, calleeConstructor).and(1, nestedConstructor);

		return cases.buildAndClean();
	}

	@ParameterizedTest
	@MethodSource
	default void testConstructorOrMethodCallsConstructor(Arguments<?, ?> args) {
		testTemplate(args);
	}

	class MethodCall {
		static class Called {
			Object a() {
				return null;
			}

			void b() {
			}
		}

		MethodCall.Called a = new MethodCall.Called();

		public MethodCall() {
			a.a();
			a.b();
		}

		void noCall() {
		}

		void monoCall() {
			a.a();
		}

		void duoCall() {
			a.a();
			a.a();
		}

		void biCall() {
			a.a();
			a.b();
		}

		interface Interface {
			static MethodCall.Called a = new MethodCall.Called();

			default void noCall() {
			}

			default void monoCall() {
				a.a();
			}

			default void duoCall() {
				a.a();
				a.a();
			}

			default void biCall() {
				a.a();
				a.b();
			}
		}

		static class Field {
			Object field = new MethodCall.Called().a();
		}

		static class StaticDeclaration {
			static Object FIELD = new MethodCall.Called().a();
		}

		static class StaticBlock {
			static Object FIELD;
			static {
				FIELD = new MethodCall.Called().a();
			}
		}

		static class StaticMethod {
			static void call() {
				new MethodCall.Called().a();
			}
		}
	}

	static Stream<Arguments<?, ?>> testConstructorOrMethodCallsMethod() {
		Method aMethod = method(MethodCall.Called.class, Object.class, "a");
		Method bMethod = method(MethodCall.Called.class, void.class, "b");

		Class<?> clazz;
		Reducer<String, Node> caller;

		clazz = MethodCall.class;
		caller = noArgCallerFactory(clazz);
		cases.analyse(clazz).test(caller.x(Constructor.NAME)).calls(1, aMethod).and(1, bMethod);
		cases.analyse(clazz).test(caller.x("noCall")).calls(0, aMethod).and(0, bMethod);
		cases.analyse(clazz).test(caller.x("monoCall")).calls(1, aMethod).and(0, bMethod);
		cases.analyse(clazz).test(caller.x("duoCall")).calls(2, aMethod).and(0, bMethod);
		cases.analyse(clazz).test(caller.x("biCall")).calls(1, aMethod).and(1, bMethod);

		clazz = MethodCall.Interface.class;
		caller = noArgCallerFactory(clazz);
		cases.analyse(clazz).test(caller.x("noCall")).calls(0, aMethod).and(0, bMethod);
		cases.analyse(clazz).test(caller.x("monoCall")).calls(1, aMethod).and(0, bMethod);
		cases.analyse(clazz).test(caller.x("duoCall")).calls(2, aMethod).and(0, bMethod);
		cases.analyse(clazz).test(caller.x("biCall")).calls(1, aMethod).and(1, bMethod);

		clazz = MethodCall.Field.class;
		caller = noArgCallerFactory(clazz);
		cases.analyse(clazz).test(caller.x(Constructor.NAME)).calls(1, aMethod).and(0, bMethod);

		clazz = MethodCall.StaticDeclaration.class;
		caller = noArgCallerFactory(clazz);
		cases.analyse(clazz).test(caller.x(StaticBlock.NAME)).calls(1, aMethod).and(0, bMethod);

		clazz = MethodCall.StaticBlock.class;
		caller = noArgCallerFactory(clazz);
		cases.analyse(clazz).test(caller.x(StaticBlock.NAME)).calls(1, aMethod).and(0, bMethod);

		clazz = MethodCall.StaticMethod.class;
		caller = noArgCallerFactory(clazz);
		cases.analyse(clazz).test(caller.x("call")).calls(1, aMethod).and(0, bMethod);

		return cases.buildAndClean();
	}

	@ParameterizedTest
	@MethodSource
	default void testConstructorOrMethodCallsMethod(Arguments<?, ?> args) {
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

	static Reducer<String, Node> noArgCallerFactory(Class<?> callerClass) {
		return name -> Constructor.NAME.equals(name) ? constructor(callerClass)
				: StaticBlock.NAME.equals(name) ? staticBlock(callerClass) : method(callerClass, void.class, name);
	}
}
