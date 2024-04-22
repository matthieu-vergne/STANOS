package fr.vergne.stanos.core.refactorer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import fr.vergne.stanos.core.refactorer.Y.Class;
import fr.vergne.stanos.core.refactorer.Y.Interface;
import fr.vergne.stanos.core.refactorer.Y.Method;
import fr.vergne.stanos.core.refactorer.Y.Variable;

class RefactorerTest {

	record SuccessCase(String code, Consumer<Refactorer.ForCode> refactoring, String expectedCode) {
		@Override
		public String toString() {
			// TODO Reduce code and expected code to parts relevant for diff if too long
			return reduce(code) + " > " + stringOf(refactoring) + " > " + reduce(expectedCode);
		}

		private String reduce(String code) {
			return code.length() < 100 ? code : code.substring(0, 100) + "[...]";
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

	static Stream<SuccessCase> testCodeRefactoringSuccess() {
		return Stream.of(//
				testCodeRefactoringSuccess_Class(), //
				testCodeRefactoringSuccess_Interface(), //
				testCodeRefactoringSuccess_Record(), //
				testCodeRefactoringSuccess_Field(), //
				testCodeRefactoringSuccess_Method(), //
				testCodeRefactoringSuccess_Variable()//
		).flatMap(stream -> stream);
	}

	private static Stream<SuccessCase> testCodeRefactoringSuccess_Variable() {
		return Stream.of(//
				new SuccessCase(//
						"class MyClass{void myMethod(){String myVar = null;}}", //
						refactorer -> refactorer.locateVariable("MyClass.myMethod().myVar%").rename("foo"), //
						"class MyClass{void myMethod(){String foo = null;}}"//
				), //
				new SuccessCase(//
						"""
								class MyClass{
									void myMethod(){
										class myVar {}
										myVar myVar = new myVar();
									}
								}
								""", //
						refactorer -> refactorer.locateVariable("MyClass.myMethod().myVar%").rename("foo"), //
						"""
								class MyClass{
									void myMethod(){
										class myVar {}
										myVar foo = new myVar();
									}
								}
								"""//
				), //
				new SuccessCase(//
						"""
								class MyClass {
									class MyChildClass {
										void myMethod() {
											class MyInnerClass {
												void myMethod() {
													String myVar = null;
												}
											}
										}
									}
								}
								""", //
						refactorer -> refactorer.locateVariable("MyClass.MyChildClass.myMethod().MyInnerClass.myMethod().myVar%").rename("foo"), //
						"""
								class MyClass {
									class MyChildClass {
										void myMethod() {
											class MyInnerClass {
												void myMethod() {
													String foo = null;
												}
											}
										}
									}
								}
								"""//
				), //
				new SuccessCase(//
						"""
								class MyClass {
									void myMethod() {
										String myVar = null;
										class MyInnerClass {
											String myMethod() {
												return myVar;
											}
										}
									}
								}
								""", //
						refactorer -> refactorer.locateVariable("MyClass.myMethod().myVar%").rename("foo"), //
						"""
								class MyClass {
									void myMethod() {
										String foo = null;
										class MyInnerClass {
											String myMethod() {
												return foo;
											}
										}
									}
								}
								"""//
				), //
				new SuccessCase(//
						"""
								class MyClass {
									String myMethod(boolean b) {
										if (b) {
											String myVar = "a";
											return myVar;
										} else {
											String myVar = "b";
											return myVar;
										}
									}
								}
								""", //
						refactorer -> refactorer.locateVariable("MyClass.myMethod(boolean).myVar%0").rename("foo"), //
						"""
								class MyClass {
									String myMethod(boolean b) {
										if (b) {
											String foo = "a";
											return foo;
										} else {
											String myVar = "b";
											return myVar;
										}
									}
								}
								"""//
				), //
				new SuccessCase(//
						"""
								class MyClass {
									String myMethod(boolean b) {
										if (b) {
											String myVar = "a";
											return myVar;
										} else {
											String myVar = "b";
											return myVar;
										}
									}
								}
								""", //
						refactorer -> refactorer.locateVariable("MyClass.myMethod(boolean).myVar%1").rename("foo"), //
						"""
								class MyClass {
									String myMethod(boolean b) {
										if (b) {
											String myVar = "a";
											return myVar;
										} else {
											String foo = "b";
											return foo;
										}
									}
								}
								"""//
				), //
				new SuccessCase(//
						"""
								class MyClass {
									interface MyInt {
										String myMethod();
									}
									MyInt myMethod() {
										MyInt myVar = new MyInt() {
											@Override
											public String myMethod() {
												String myVar = "";
												return myVar;
											}
										};
										return myVar;
									}
								}
								""", //
						refactorer -> refactorer.locateVariable("MyClass.myMethod().myVar%").rename("foo"), //
						"""
								class MyClass {
									interface MyInt {
										String myMethod();
									}
									MyInt myMethod() {
										MyInt foo = new MyInt() {
											@Override
											public String myMethod() {
												String myVar = "";
												return myVar;
											}
										};
										return foo;
									}
								}
								"""//
				), //
				new SuccessCase(//
						"""
								class MyClass {
									interface MyInt {
										String myMethod();
									}
									MyInt myMethod() {
										MyInt myVar = new MyInt() {
											@Override
											public String myMethod() {
												String myVar = "";
												return myVar;
											}
										};
										return myVar;
									}
								}
								""", //
						refactorer -> refactorer.locateVariable("MyClass.myMethod().myVar%.myMethod().myVar%").rename("foo"), //
						"""
								class MyClass {
									interface MyInt {
										String myMethod();
									}
									MyInt myMethod() {
										MyInt myVar = new MyInt() {
											@Override
											public String myMethod() {
												String foo = "";
												return foo;
											}
										};
										return myVar;
									}
								}
								"""//
				), //
				new SuccessCase(//
						"""
								class MyClass {
									String myMethod() {
										String myVar = "abc";
										myVar = "";
										String x = myVar;
										x = "<" + myVar + ">";
										myVar = myMethod(myVar);
										return myVar;
									}

									String myMethod(String s) {
										return null;
									}
								}
								""", //
						refactorer -> refactorer.locateVariable("MyClass.myMethod().myVar%").rename("foo"), //
						"""
								class MyClass {
									String myMethod() {
										String foo = "abc";
										foo = "";
										String x = foo;
										x = "<" + foo + ">";
										foo = myMethod(foo);
										return foo;
									}

									String myMethod(String s) {
										return null;
									}
								}
								"""//
				), //
				new SuccessCase(//
						"""
								import java.util.function.Supplier;

								class MyClass {
									void myMethod() {
										String myVar = "abc";
										Supplier<String> sup = () -> myVar;
										Supplier<Supplier<String>> sup2 = () -> () -> myVar;
									}
								}
								""", //
						refactorer -> refactorer.locateVariable("MyClass.myMethod().myVar%").rename("foo"), //
						"""
								import java.util.function.Supplier;

								class MyClass {
									void myMethod() {
										String foo = "abc";
										Supplier<String> sup = () -> foo;
										Supplier<Supplier<String>> sup2 = () -> () -> foo;
									}
								}
								"""//
				), //
				new SuccessCase(//
						"""
								class MyClass{
									void myMethod(boolean b){
										String myVar = null;
									}
									void myMethod(String s){
										String myVar = null;
									}
									void myMethod(boolean b, String s){
										String myVar = null;
									}
									void myMethod(){
										String myVar = null;
									}
								}
								""", //
						refactorer -> refactorer.locateVariable("MyClass.myMethod(boolean).myVar%").rename("foo"), //
						"""
								class MyClass{
									void myMethod(boolean b){
										String foo = null;
									}
									void myMethod(String s){
										String myVar = null;
									}
									void myMethod(boolean b, String s){
										String myVar = null;
									}
									void myMethod(){
										String myVar = null;
									}
								}
								"""//
				)//
		);
	}

	private static Stream<SuccessCase> testCodeRefactoringSuccess_Method() {
		return Stream.of(//
				new SuccessCase(//
						"class MyClass{void myMethod(){}}", //
						refactorer -> refactorer.locateMethod("MyClass.myMethod()").rename("foo"), //
						"class MyClass{void foo(){}}"//
				)//
		);
	}

	private static Stream<SuccessCase> testCodeRefactoringSuccess_Field() {
		return Stream.of(//
				new SuccessCase(//
						"class MyClass{String myField;}", //
						refactorer -> refactorer.locateField("MyClass.myField").rename("foo"), //
						"class MyClass{String foo;}"//
				)//
		);
	}

	private static Stream<SuccessCase> testCodeRefactoringSuccess_Record() {
		return Stream.of(//
				new SuccessCase(//
						"record MyRecord(){}", //
						refactorer -> refactorer.locateRecord("MyRecord").rename("Foo"), //
						"record Foo(){}"//
				)//
		);
	}

	private static Stream<SuccessCase> testCodeRefactoringSuccess_Interface() {
		return Stream.of(//
				new SuccessCase(//
						"interface MyInt{}", //
						refactorer -> refactorer.locateInterface("MyInt").rename("Foo"), //
						"interface Foo{}"//
				)//
		);
	}

	static Stream<SuccessCase> testCodeRefactoringSuccess_Class() {
		return Stream.of(//
				new SuccessCase(//
						"class MyClass{}", //
						refactorer -> refactorer.locateClass("MyClass").rename("Foo"), //
						"class Foo{}"//
				)//
		);
	}

	record FailureCase(String code, Consumer<Refactorer.ForCode> refactoring, Exception expectedException) {
		@Override
		public String toString() {
			return code + " > " + stringOf(refactoring) + " > " + expectedException;
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

	static Stream<FailureCase> testCodeRefactoringFailure() {
		return Stream.of(//
				testCodeRefactoringFailure_Class(), //
				testCodeRefactoringFailure_Interface(), //
				testCodeRefactoringFailure_Record(), //
				testCodeRefactoringFailure_Field(), //
				testCodeRefactoringFailure_Method(), //
				testCodeRefactoringFailure_Variable()//
		).flatMap(stream -> stream);
	}

	private static Stream<FailureCase> testCodeRefactoringFailure_Variable() {
		return Stream.of(//
				new FailureCase(//
						"class MyClass{void myMethod(){String myVar = null;}}", //
						refactorer -> refactorer.locateVariable("MyClass.myMethod().x%"), //
						new NoSuchElementException("No variable MyClass.myMethod().x%")//
				)//
		);
	}

	private static Stream<FailureCase> testCodeRefactoringFailure_Method() {
		return Stream.of(//
				new FailureCase(//
						"class MyClass{void myMethod(){}}", //
						refactorer -> refactorer.locateMethod("MyClass.x()"), //
						new NoSuchElementException("No method MyClass.x()")//
				) //
		);
	}

	private static Stream<FailureCase> testCodeRefactoringFailure_Field() {
		return Stream.of(//
				new FailureCase(//
						"class MyClass{String myField;}", //
						refactorer -> refactorer.locateField("MyClass.X"), //
						new NoSuchElementException("No field MyClass.X")//
				) //
		);
	}

	private static Stream<FailureCase> testCodeRefactoringFailure_Record() {
		return Stream.of(//
				new FailureCase(//
						"class Foo{}", //
						refactorer -> refactorer.locateRecord("Foo"), //
						new NoSuchElementException("No record Foo")//
				), //
				new FailureCase(//
						"interface Foo{}", //
						refactorer -> refactorer.locateRecord("Foo"), //
						new NoSuchElementException("No record Foo")//
				), //
				new FailureCase(//
						"record Foo(){}", //
						refactorer -> refactorer.locateRecord("X"), //
						new NoSuchElementException("No record X")//
				)//
		);
	}

	private static Stream<FailureCase> testCodeRefactoringFailure_Interface() {
		return Stream.of(//
				new FailureCase(//
						"class Foo{}", //
						refactorer -> refactorer.locateInterface("Foo"), //
						new NoSuchElementException("No interface Foo")//
				), //
				new FailureCase(//
						"interface Foo{}", //
						refactorer -> refactorer.locateInterface("X"), //
						new NoSuchElementException("No interface X")//
				), //
				new FailureCase(//
						"record Foo(){}", //
						refactorer -> refactorer.locateInterface("Foo"), //
						new NoSuchElementException("No interface Foo")//
				)//
		);
	}

	private static Stream<FailureCase> testCodeRefactoringFailure_Class() {
		return Stream.of(//
				new FailureCase(//
						"class Foo{}", //
						refactorer -> refactorer.locateClass("X"), //
						new NoSuchElementException("No class X")//
				), //
				new FailureCase(//
						"interface Foo{}", //
						refactorer -> refactorer.locateClass("Foo"), //
						new NoSuchElementException("No class Foo")//
				), //
				new FailureCase(//
						"record Foo(){}", //
						refactorer -> refactorer.locateClass("Foo"), //
						new NoSuchElementException("No class Foo")//
				)//
		);
	}

	private static String stringOf(Consumer<Refactorer.ForCode> refactoring) {
		StringBuilder builder = new StringBuilder();
		Consumer<Object> locatorDisplayer = arg -> builder.append(currentMethodName() + "(" + arg + ")");
		Consumer<Object> refactorDisplayer = arg -> builder.append("." + currentMethodName() + "(" + arg + ")");

		refactoring.accept(new Refactorer.ForCode() {
			@Override
			public String code() {
				throw new UnsupportedOperationException("Not expected to be called");
			}

			@Override
			public Y.Class locateClass(String classPath) {
				locatorDisplayer.accept(classPath);
				return new Y.Class() {
					@Override
					public void rename(String newName) {
						refactorDisplayer.accept(newName);
					}

					@Override
					public String name() {
						throw new UnsupportedOperationException("Not implemented yet");
					}

					@Override
					public Stream<Method> methods() {
						throw new UnsupportedOperationException("Not implemented yet");
					}

					@Override
					public Stream<Class> classes() {
						throw new UnsupportedOperationException("Not implemented yet");
					}

					@Override
					public Stream<Interface> interfaces() {
						throw new UnsupportedOperationException("Not implemented yet");
					}
				};
			}

			@Override
			public Refactorer.ForInterface locateInterface(String interfacePath) {
				locatorDisplayer.accept(interfacePath);
				return new Refactorer.ForInterface() {
					@Override
					public void rename(String newName) {
						refactorDisplayer.accept(newName);
					}
				};
			}

			@Override
			public Refactorer.ForRecord locateRecord(String recordPath) {
				locatorDisplayer.accept(recordPath);
				return new Refactorer.ForRecord() {
					@Override
					public void rename(String newName) {
						refactorDisplayer.accept(newName);
					}
				};
			}

			@Override
			public Y.Method locateMethod(String methodPath) {
				locatorDisplayer.accept(methodPath);
				return new Y.Method() {
					@Override
					public void rename(String newName) {
						refactorDisplayer.accept(newName);
					}

					@Override
					public String name() {
						throw new UnsupportedOperationException("Not implemented yet");
					}

					@Override
					public List<String> parameterTypes() {
						throw new UnsupportedOperationException("Not implemented yet");
					}

					@Override
					public Stream<Variable> variables() {
						throw new UnsupportedOperationException("Not implemented yet");
					}

					@Override
					public Stream<Class> classes() {
						throw new UnsupportedOperationException("Not implemented yet");
					}

					@Override
					public Stream<Interface> interfaces() {
						throw new UnsupportedOperationException("Not implemented yet");
					}
				};
			}

			@Override
			public Refactorer.ForField locateField(String fieldPath) {
				locatorDisplayer.accept(fieldPath);
				return new Refactorer.ForField() {
					@Override
					public void rename(String newName) {
						refactorDisplayer.accept(newName);
					}
				};
			}

			@Override
			public Y.Variable locateVariable(String variablePath) {
				locatorDisplayer.accept(variablePath);
				return new Y.Variable() {
					@Override
					public void rename(String newName) {
						refactorDisplayer.accept(newName);
					}

					@Override
					public String name() {
						throw new UnsupportedOperationException("Not implemented yet");
					}

					@Override
					public Stream<Method> methods() {
						throw new UnsupportedOperationException("Not implemented yet");
					}
				};
			}
		});

		return builder.toString();
	}

	private static String currentMethodName() {
		// Get element at index 3 in stack trace:
		// 0 - getStackTrace()
		// 1 - the method we are in
		// 2 - the lambda calling this method
		// 3 - the method we are interested in
		return Thread.currentThread().getStackTrace()[3].getMethodName();
	}
}
