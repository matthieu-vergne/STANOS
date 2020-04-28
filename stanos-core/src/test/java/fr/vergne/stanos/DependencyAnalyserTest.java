package fr.vergne.stanos;

import static fr.vergne.stanos.node.Method.*;
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
import fr.vergne.stanos.DependencyAnalyserTest.IParent.INested;
import fr.vergne.stanos.DependencyAnalyserTest.IParent.INested.ISubNested;
import fr.vergne.stanos.DependencyAnalyserTest.Parent.Nested;
import fr.vergne.stanos.DependencyAnalyserTest.Parent.Nested.SubNested;
import fr.vergne.stanos.DependencyAnalyserTest.Parent.StaticNested;
import fr.vergne.stanos.DependencyAnalyserTest.Parent.StaticNested.StaticSubNested;
import fr.vergne.stanos.node.Method;

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

	interface IParent {
		interface INested {
			interface ISubNested {
			}
		}
	}

	static class Parent {
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
		cases.analyse(IParent.class).test(type(IParent.class)).declares(1, type(INested.class)).and(0,
				type(ISubNested.class));
		cases.analyse(INested.class).test(type(INested.class)).declares(1, type(ISubNested.class));

		cases.analyse(Parent.class).test(type(Parent.class)).declares(1, type(StaticNested.class)).and(0,
				type(StaticSubNested.class));
		cases.analyse(StaticNested.class).test(type(StaticNested.class)).declares(1, type(StaticSubNested.class));

		cases.analyse(Parent.class).test(type(Parent.class)).declares(1, type(Nested.class)).and(0,
				type(SubNested.class));
		cases.analyse(Nested.class).test(type(Nested.class)).declares(1, type(SubNested.class));

		return cases.buildAndClean();
	}

	@ParameterizedTest
	@MethodSource
	default void testTypeDeclaresInnerType(Arguments<?, ?> args) {
		testTemplate(args);
	}

	interface IMethodDeclare {
		void method();

		int complexMethod(boolean b, String s, List<Integer> l);

		interface Nested {
			void nestedMethod();
		}
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
	}

	static Stream<Arguments<?, ?>> testTypeDeclaresMethod() {
		List<Class<?>> complexArgs = Arrays.asList(boolean.class, String.class, List.class);

		cases.analyse(IMethodDeclare.class).test(type(IMethodDeclare.class))
				.declares(1, method(IMethodDeclare.class, void.class, "method"))
				.and(1, method(IMethodDeclare.class, int.class, "complexMethod", complexArgs))
				.and(0, method(IMethodDeclare.Nested.class, void.class, "nestedMethod"));
		cases.analyse(IMethodDeclare.Nested.class).test(type(IMethodDeclare.Nested.class))
				.declares(0, method(IMethodDeclare.class, void.class, "method"))
				.and(0, method(IMethodDeclare.class, int.class, "complexMethod", complexArgs))
				.and(1, method(IMethodDeclare.Nested.class, void.class, "nestedMethod"));

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

	class Called {
		void a() {
		}

		void b() {
		}
	}

	class MethodCall {
		Called a = new Called();

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
	}

	static Stream<Arguments<?, ?>> testMethodCallsMethod() {
		Method aMethod = method(Called.class, void.class, "a");
		Method bMethod = method(Called.class, void.class, "b");
		Reducer<String, Method> caller = name -> method(MethodCall.class, void.class, name);

		cases.analyse(MethodCall.class).test(caller.x("noCall")).calls(0, aMethod).and(0, bMethod);
		cases.analyse(MethodCall.class).test(caller.x("monoCall")).calls(1, aMethod).and(0, bMethod);
		cases.analyse(MethodCall.class).test(caller.x("duoCall")).calls(2, aMethod).and(0, bMethod);
		cases.analyse(MethodCall.class).test(caller.x("biCall")).calls(1, aMethod).and(1, bMethod);
		return cases.buildAndClean();
	}

	@ParameterizedTest
	@MethodSource
	default void testMethodCallsMethod(Arguments<?, ?> args) {
		testTemplate(args);
	}

	class Callee {
		class Nested {
		}
	}

	class ConstructorCall {
		void monoCall() {
			new Callee();
		}

		void duoCall() {
			new Callee();
			new Callee();
		}

		void nestedCall() {
			Callee callee = new Callee();
			callee.new Nested();
		}
	}

	interface IConstructorCall {
		default void monoCall() {
			new Callee();
		}

		default void duoCall() {
			new Callee();
			new Callee();
		}

		default void nestedCall() {
			Callee callee = new Callee();
			callee.new Nested();
		}
	}

	static Stream<Arguments<?, ?>> testMethodCallsConstructor() {
		// TODO use dedicated Constructor class
		Method calleeConstructor = method(Callee.class, void.class, "<init>");
		Method nestedConstructor = method(Callee.Nested.class, void.class, "<init>", Callee.class);

		Reducer<String, Method> caller = name -> method(ConstructorCall.class, void.class, name);
		cases.analyse(ConstructorCall.class).test(caller.x("monoCall")).calls(1, calleeConstructor).and(0,
				nestedConstructor);
		cases.analyse(ConstructorCall.class).test(caller.x("duoCall")).calls(2, calleeConstructor).and(0,
				nestedConstructor);
		cases.analyse(ConstructorCall.class).test(caller.x("nestedCall")).calls(1, calleeConstructor).and(1,
				nestedConstructor);

		Reducer<String, Method> icaller = name -> method(IConstructorCall.class, void.class, name);
		cases.analyse(IConstructorCall.class).test(icaller.x("monoCall")).calls(1, calleeConstructor).and(0,
				nestedConstructor);
		cases.analyse(IConstructorCall.class).test(icaller.x("duoCall")).calls(2, calleeConstructor).and(0,
				nestedConstructor);
		cases.analyse(IConstructorCall.class).test(icaller.x("nestedCall")).calls(1, calleeConstructor).and(1,
				nestedConstructor);

		return cases.buildAndClean();
	}

	@ParameterizedTest
	@MethodSource
	default void testMethodCallsConstructor(Arguments<?, ?> args) {
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
}
