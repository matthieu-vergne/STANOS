package fr.vergne.stanos.core.refactorer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class RefactorerTest {

	static Stream<SuccessCase> testCodeRefactoringSuccess() {
		return Stream.of(//
				new SuccessCase(//
						"class MyClass{}", //
						name("rename class", refactorer -> refactorer.locateClass("MyClass").rename("Foo")), //
						"class Foo{}"//
				), //
				new SuccessCase(//
						"class MyClass{String myField;}", //
						name("rename field", refactorer -> refactorer.locateField("MyClass.myField").rename("foo")), //
						"class MyClass{String foo;}"//
				), //
				new SuccessCase(//
						"class MyClass{void myMethod(){}}", //
						name("rename method", refactorer -> refactorer.locateMethod("MyClass.myMethod").rename("foo")), //
						"class MyClass{void foo(){}}"//
				), //
				new SuccessCase(//
						"class MyClass{void myMethod(){String myVar = null;}}", //
						name("rename variable", refactorer -> refactorer.locateVariable("MyClass.myMethod.myVar").rename("foo")), //
						"class MyClass{void myMethod(){String foo = null;}}"//
				)//
		);
	}

	record SuccessCase(String code, Consumer<Refactorer.ForCode> refactoring, String expectedCode) {
		@Override
		public String toString() {
			return code + " > " + refactoring.toString() + " > " + expectedCode;
		}
	}

	@ParameterizedTest
	@MethodSource
	void testCodeRefactoringSuccess(SuccessCase successCase) {
		var code = successCase.code();
		var refactorerExecutor = successCase.refactoring();
		var expectedCode = successCase.expectedCode();

		// GIVEN
		Refactorer.ForCode refactorer = Refactorer.forCode(code);

		// WHEN
		refactorerExecutor.accept(refactorer);

		// THEN
		assertThat(refactorer.code(), is(expectedCode));
	}

	static Stream<FailureCase> testCodeRefactoringFailure() {
		return Stream.of(//
				new FailureCase(//
						"class Foo{}", //
						name("locate class X", refactorer -> refactorer.locateClass("X")), //
						new NoSuchElementException("No class X")//
				), //
				new FailureCase(//
						"interface Foo{}", //
						name("locate class", refactorer -> refactorer.locateClass("Foo")), //
						new NoSuchElementException("No class Foo")//
				), //
				new FailureCase(//
						"record Foo(){}", //
						name("locate class", refactorer -> refactorer.locateClass("Foo")), //
						new NoSuchElementException("No class Foo")//
				), //

				new FailureCase(//
						"class MyClass{String myField;}", //
						name("locate field", refactorer -> refactorer.locateField("MyClass.X")), //
						new NoSuchElementException("No field MyClass.X")//
				), //

				new FailureCase(//
						"class MyClass{void myMethod(){}}", //
						name("locate method", refactorer -> refactorer.locateMethod("MyClass.x")), //
						new NoSuchElementException("No method MyClass.x")//
				), //

				new FailureCase(//
						"class MyClass{void myMethod(){String myVar = null;}}", //
						name("locate variable", refactorer -> refactorer.locateVariable("MyClass.myMethod.x")), //
						new NoSuchElementException("No variable MyClass.myMethod.x")//
				)//
		);
	}

	record FailureCase(String code, Consumer<Refactorer.ForCode> refactoring, Exception expectedException) {
		@Override
		public String toString() {
			return code + " > " + refactoring.toString() + " > " + expectedException;
		}
	}

	@ParameterizedTest
	@MethodSource
	void testCodeRefactoringFailure(FailureCase failureCase) {
		var code = failureCase.code();
		var refactorerExecutor = failureCase.refactoring();
		var expectedException = failureCase.expectedException();

		// GIVEN
		Refactorer.ForCode refactorer = Refactorer.forCode(code);

		// WHEN
		Executable action = () -> refactorerExecutor.accept(refactorer);

		// THEN
		Exception except = assertThrows(expectedException.getClass(), action);
		assertThat(except.getMessage(), is(expectedException.getMessage()));
	}

	private static <T> Consumer<T> name(String name, Consumer<T> consumer) {
		return new Consumer<T>() {
			@Override
			public void accept(T input) {
				consumer.accept(input);
			}

			@Override
			public String toString() {
				return name;
			}
		};
	}
}
