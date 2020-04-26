package fr.vergne.stanos;

import static fr.vergne.stanos.DependencyType.*;
import static fr.vergne.stanos.junit.Assertions.*;
import static fr.vergne.stanos.node.Method.*;
import static fr.vergne.stanos.node.Type.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import fr.vergne.stanos.DependencyAnalyserTest.ParentClass.NestedClass;
import fr.vergne.stanos.DependencyAnalyserTest.ParentClass.NestedClass.SubNestedClass;
import fr.vergne.stanos.DependencyAnalyserTest.ParentClass.StaticNestedClass;
import fr.vergne.stanos.DependencyAnalyserTest.ParentClass.StaticNestedClass.StaticSubNestedClass;
import fr.vergne.stanos.DependencyAnalyserTest.ParentInterface.NestedInterface;
import fr.vergne.stanos.DependencyAnalyserTest.ParentInterface.NestedInterface.SubNestedInterface;
import fr.vergne.stanos.node.Method;
import fr.vergne.stanos.node.Type;

@SuppressWarnings("unused")
public interface DependencyAnalyserTest {

	DependencyAnalyser createDependencyAnalyzer();

	interface Empty {
		// Not even a constructor
	}

	@Test
	default void testEmptyClassHasNoDependency() {
		DependencyAnalyser analyser = createDependencyAnalyzer();

		Collection<Dependency> dependencies = analyser.analyse(Empty.class);

		assertEquals(Collections.emptyList(), dependencies);
	}

	interface ParentInterface {
		void method();

		int complexMethod(boolean b, String s, List<Integer> l);

		interface NestedInterface {
			interface SubNestedInterface {
			}
		}
	}

	static class ParentClass {
		void method() {
		}

		int complexMethod(boolean b, String s, List<Integer> l) {
			return 0;
		}

		static class StaticNestedClass {
			static class StaticSubNestedClass {
			}
		}

		class NestedClass {
			void nestedMethod() {
			}

			class SubNestedClass {
			}
		}
	}

	Method parentMethod = method(ParentClass.class, void.class, "method");
	Method parentComplexMethod = method(ParentClass.class, int.class, "complexMethod",
			Arrays.asList(boolean.class, String.class, List.class));
	Method nestedMethod = method(NestedClass.class, void.class, "nestedMethod");

	static Stream<Arguments> testTypeDeclaresInnerType() {
		return Stream.of(arguments(1, NestedInterface.class, ParentInterface.class),
				arguments(1, SubNestedInterface.class, NestedInterface.class),
				arguments(0, SubNestedInterface.class, ParentInterface.class),

				arguments(1, NestedClass.class, ParentClass.class),
				arguments(1, SubNestedClass.class, NestedClass.class),
				arguments(0, SubNestedClass.class, ParentClass.class),

				arguments(1, StaticNestedClass.class, ParentClass.class),
				arguments(1, StaticSubNestedClass.class, StaticNestedClass.class),
				arguments(0, StaticSubNestedClass.class, ParentClass.class));
	}

	@ParameterizedTest(name = "{0} declaration of {1} in {2}")
	@MethodSource
	default void testTypeDeclaresInnerType(int count, Class<?> declaredClass, Class<?> analyzedClass) {
		// GIVEN
		DependencyAnalyser analyser = createDependencyAnalyzer();

		// WHEN
		Collection<Dependency> dependencies = analyser.analyse(analyzedClass);

		// THEN
		assertFindInCollection(new Dependency(type(analyzedClass), DECLARES, type(declaredClass)), count, dependencies);
	}

	static Stream<Arguments> testTypeDeclaresMethod() {
		return Stream.of(arguments(1, parentMethod, ParentInterface.class),
				arguments(1, parentComplexMethod, ParentInterface.class),
				arguments(0, nestedMethod, ParentInterface.class),

				arguments(1, parentMethod, ParentClass.class), arguments(1, parentComplexMethod, ParentClass.class),
				arguments(0, nestedMethod, ParentClass.class));
	}

	@ParameterizedTest(name = "{0} declaration of {1} in {2}")
	@MethodSource
	default void testTypeDeclaresMethod(int count, Method method, Class<?> analysedClass) {
		// GIVEN
		DependencyAnalyser analyser = createDependencyAnalyzer();

		// WHEN
		Collection<Dependency> dependencies = analyser.analyse(analysedClass);

		// THEN
		assertFindInCollection(new Dependency(type(analysedClass), DECLARES, method), count, dependencies);
	}

	@Test
	default void testFindMethodCallsMethod() {
		class A {
			void a() {
			}
		}
		class B {
			void b() {
				new A().a();
			}
		}
		DependencyAnalyser analyser = createDependencyAnalyzer();

		Collection<Dependency> dependencies = analyser.analyse(B.class);

		Method caller = method(B.class, void.class, "b");
		Method callee = method(A.class, void.class, "a");
		assertFindInCollection(new Dependency(caller, CALLS, callee), 1, dependencies);
	}

	@Test
	default void testFindMethodCallsConstructor() {
		class A {
		}
		class B {
			void b() {
				new A();
			}
		}
		DependencyAnalyser analyser = createDependencyAnalyzer();

		Collection<Dependency> dependencies = analyser.analyse(B.class);

		Method caller = method(B.class, void.class, "b");
		// TODO make the DependencyAnalyserTest disappear
		Method callee = method(A.class, void.class, "<init>", DependencyAnalyserTest.class);
		assertFindInCollection(new Dependency(caller, CALLS, callee), 1, dependencies);
	}
}
