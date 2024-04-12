package fr.vergne.stanos.core.refactorer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RefactorerTest {

	static Stream<Arguments> testCodeRefactorer() {
		return Stream.of(//
				refactorUseCase(//
						"class MyClass{}", //
						refactorer -> refactorer.locateClass("MyClass").rename("Foo"), //
						"class Foo{}"//
				), //
				refactorUseCase(//
						"class MyClass{String myField;}", //
						refactorer -> refactorer.locateField("MyClass.myField").rename("foo"), //
						"class MyClass{String foo;}"//
				)//
		);
	}

	private static Arguments refactorUseCase(String code, Consumer<Refactorer.ForCode> refactorerExecutor, String expectedCode) {
		return Arguments.arguments(code, refactorerExecutor, expectedCode);
	}

	@ParameterizedTest
	@MethodSource
	void testCodeRefactorer(String code, Consumer<Refactorer.ForCode> refactorerExecutor, String expectedCode) {
		// GIVEN
		Refactorer.ForCode refactorer = Refactorer.forCode(code);

		// WHEN
		refactorerExecutor.accept(refactorer);

		// THEN
		assertThat(refactorer.code(), is(expectedCode));
	}

}
