package fr.vergne.stanos.test.refactorer;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import fr.vergne.stanos.core.refactorer.Code;
import fr.vergne.stanos.core.refactorer.Component;
import fr.vergne.stanos.core.refactorer.Refactorer;

// TODO Fix formatting of expected codes in success cases
public abstract class RefactorerTest {
	protected abstract Refactorer.ForCode parseCode(String code);

	record SuccessCase(String code, Consumer<Code.Source> refactoring, String expectedCode,
			RuntimeException instanciationException) {
		SuccessCase(String code, Consumer<Code.Source> refactoring, String expectedCode) {
			this(code, refactoring, expectedCode, new RuntimeException("Success case expectations"));
		}

		SuccessCase {
			StackTraceElement[] originalStack = instanciationException.getStackTrace();
			StackTraceElement[] relevantStack = Arrays.copyOfRange(originalStack, 1, originalStack.length);
			instanciationException.setStackTrace(relevantStack);
		}

		@Override
		public String toString() {
			return reduce(code) + " > " + stringOf(refactoring) + " > " + reduce(expectedCode);
		}
	}

	@ParameterizedTest
	@MethodSource
	public void testCodeRefactoringSuccess(SuccessCase successCase) {
		try {
			var code = successCase.code();
			var refactorerExecutor = successCase.refactoring();
			var expectedCode = successCase.expectedCode();

			// GIVEN
			Refactorer.ForCode refactorer = parseCode(code);

			// WHEN
			refactorerExecutor.accept(refactorer.source());

			// THEN
			assertThat(refactorer.code(), is(expectedCode));
		} catch (RuntimeException | AssertionError cause) {
			cause.addSuppressed(successCase.instanciationException());
			throw cause;
		}
	}

	public static Stream<SuccessCase> testCodeRefactoringSuccess() {
		return Stream.of(//
				testCodeRefactoringSuccess_ClassRename(), //
				testCodeRefactoringSuccess_InterfaceRename(), //
				testCodeRefactoringSuccess_RecordRename(), //
				testCodeRefactoringSuccess_FieldRename(), //
				testCodeRefactoringSuccess_MethodRename(), //
				testCodeRefactoringSuccess_ParameterRename(), //
				testCodeRefactoringSuccess_VariableRename(), //
				testCodeRefactoringSuccess_SplitJoin()//
		).flatMap(stream -> stream);
	}

	private static Stream<SuccessCase> testCodeRefactoringSuccess_SplitJoin() {
		return Stream.of(//
				new SuccessCase(//
						"class MyClass{void myMethod(){String myParam = null;}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList())
								.variable("myParam", 0).splitDeclaration(), //
						"class MyClass {\r\n\r\n    void myMethod() {\r\n        String myParam;\r\n        myParam = null;\r\n    }\r\n}\r\n"//
				), //
				new SuccessCase(//
						"class MyClass{void myMethod(){String myParam;myParam = null;}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList())
								.variable("myParam", 0).joinDeclaration(), //
						"class MyClass {\r\n\r\n    void myMethod() {\r\n        String myParam = null;\r\n    }\r\n}\r\n"//
				), //
				new SuccessCase(//
						"""
								class MyClass {
									void myMethod() {
										String myParam = null;
									}
								}
								""", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList())
								.variable("myParam", 0).splitDeclaration(), //
						"""
								class MyClass {

								    void myMethod() {
								        String myParam;
								        myParam = null;
								    }
								}
								"""//
				), //
				new SuccessCase(//
						"""
								class MyClass {
									void myMethod() {
										String myParam;
										myParam = null;
									}
								}
								""", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList())
								.variable("myParam", 0).joinDeclaration(), //
						"""
								class MyClass {

								    void myMethod() {
								        String myParam = null;
								    }
								}
								"""//
				)//
		);
	}

	private static Stream<SuccessCase> testCodeRefactoringSuccess_ParameterRename() {
		return Stream.of(//
				new SuccessCase(//
						"class MyClass{void myMethod(String myParam){myParam = null;}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", List.of("String"))
								.parameter("myParam").rename("foo"), //
						"class MyClass {\r\n\r\n    void myMethod(String foo) {\r\n        foo = null;\r\n    }\r\n}\r\n"//
				), //
				new SuccessCase(//
						"""
								class MyClass{
									class myParam {}
									void myMethod(myParam myParam){
										myParam = new myParam();
									}
								}
								""", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", List.of("myParam"))
								.parameter("myParam").rename("foo"), //
						"""
								class MyClass {

								    class myParam {
								    }

								    void myMethod(myParam foo) {
								        foo = new myParam();
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
												void myMethod(String myParam) {
													myParam = null;
												}
											}
										}
									}
								}
								""", //
						source -> source.defaultPackage().clazz("MyClass").clazz("MyChildClass")
								.method("myMethod", emptyList()).clazz("MyInnerClass")
								.method("myMethod", List.of("String")).parameter("myParam").rename("foo"), //
						"""
								class MyClass {

								    class MyChildClass {

								        void myMethod() {
								            class MyInnerClass {

								                void myMethod(String foo) {
								                    foo = null;
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
									void myMethod(String myParam) {
										class MyInnerClass {
											String myMethod() {
												return myParam;
											}
										}
									}
								}
								""", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", List.of("String"))
								.parameter("myParam").rename("foo"), //
						"""
								class MyClass {

								    void myMethod(String foo) {
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
									boolean myMethod(boolean myParam) {
										return myParam;
									}
									String myMethod(String myParam) {
										return myParam;
									}
								}
								""", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", List.of("boolean"))
								.parameter("myParam").rename("foo"), //
						"""
								class MyClass {

								    boolean myMethod(boolean foo) {
								        return foo;
								    }

								    String myMethod(String myParam) {
								        return myParam;
								    }
								}
								"""//
				), //
				new SuccessCase(//
						"""
								class MyClass {
									interface MyInt {
										String myMethod(String myParam);
									}
									MyInt myMethod(MyInt myParam) {
										myParam = new MyInt() {
											@Override
											public String myMethod(String myParam) {
												return myParam;
											}
										};
										return myParam;
									}
								}
								""", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", List.of("MyInt"))
								.parameter("myParam").rename("foo"), //
						"""
								class MyClass {

								    interface MyInt {

								        String myMethod(String myParam);
								    }

								    MyInt myMethod(MyInt foo) {
								        foo = new MyInt() {

								            @Override
								            public String myMethod(String myParam) {
								                return myParam;
								            }
								        };
								        return foo;
								    }
								}
								"""//
				// TODO Test anonymous class
//				), //
//				new SuccessCase(//
//						"""
//								class MyClass {
//									interface MyInt {
//										String myMethod();
//									}
//									MyInt myMethod(MyInt myParam) {
//										myParam = new MyInt() {
//											@Override
//											public String myMethod(String myParam) {
//												return myParam;
//											}
//										};
//										return myParam;
//									}
//								}
//								""", //
//						source -> source.defaultPackage().clazz("MyClass").method("myMethod", List.of("MyInt")).anonymousClass("MyInt", 0).method("myMethod", List.of("String")).parameter("myParam").rename("foo"), //
//						"""
//								class MyClass {
//									interface MyInt {
//										String myMethod();
//									}
//									MyInt myMethod(MyInt myParam) {
//										myParam = new MyInt() {
//											@Override
//											public String myMethod(String foo) {
//												return foo;
//											}
//										};
//										return myParam;
//									}
//								}
//								"""//
				)//
					// TODO Test lambda parameter
		);
	}

	private static Stream<SuccessCase> testCodeRefactoringSuccess_VariableRename() {
		return Stream.of(//
				new SuccessCase(//
						"class MyClass{void myMethod(){String myVar = null;}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList())
								.variable("myVar", 0).rename("foo"), //
						"class MyClass {\r\n\r\n    void myMethod() {\r\n        String foo = null;\r\n    }\r\n}\r\n"//
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList())
								.variable("myVar", 0).rename("foo"), //
						"""
								class MyClass {

								    void myMethod() {
								        class myVar {
								        }
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
						source -> source.defaultPackage().clazz("MyClass").clazz("MyChildClass")
								.method("myMethod", emptyList()).clazz("MyInnerClass").method("myMethod", emptyList())
								.variable("myVar", 0).rename("foo"), //
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList())
								.variable("myVar", 0).rename("foo"), //
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", List.of("boolean"))
								.variable("myVar", 0).rename("foo"), //
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", List.of("boolean"))
								.variable("myVar", 1).rename("foo"), //
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList())
								.variable("myVar", 0).rename("foo"), //
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList())
								.variable("myVar", 0).method("myMethod", emptyList()).variable("myVar", 0)
								.rename("foo"), //
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList())
								.variable("myVar", 0).rename("foo"), //
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList())
								.variable("myVar", 0).rename("foo"), //
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", List.of("boolean"))
								.variable("myVar", 0).rename("foo"), //
						"""
								class MyClass {

								    void myMethod(boolean b) {
								        String foo = null;
								    }

								    void myMethod(String s) {
								        String myVar = null;
								    }

								    void myMethod(boolean b, String s) {
								        String myVar = null;
								    }

								    void myMethod() {
								        String myVar = null;
								    }
								}
								"""//
				)//
		);
	}

	private static Stream<SuccessCase> testCodeRefactoringSuccess_MethodRename() {
		return Stream.of(//
				new SuccessCase(//
						"class MyClass{void myMethod(){}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList())
								.rename("foo"), //
						"class MyClass {\r\n\r\n    void foo() {\r\n    }\r\n}\r\n"//
				)//
		);
	}

	private static Stream<SuccessCase> testCodeRefactoringSuccess_FieldRename() {
		return Stream.of(//
				new SuccessCase(//
						"class MyClass{String myField;}", //
						source -> source.defaultPackage().clazz("MyClass").field("myField").rename("foo"), //
						"class MyClass {\r\n\r\n    String foo;\r\n}\r\n"//
				)//
		);
	}

	private static Stream<SuccessCase> testCodeRefactoringSuccess_RecordRename() {
		return Stream.of(//
				new SuccessCase(//
						"record MyRecord(){}", //
						source -> source.defaultPackage().record("MyRecord").rename("Foo"), //
						"record Foo() {\r\n}\r\n"//
				)//
		);
	}

	private static Stream<SuccessCase> testCodeRefactoringSuccess_InterfaceRename() {
		return Stream.of(//
				new SuccessCase(//
						"interface MyInt{}", //
						source -> source.defaultPackage().interf("MyInt").rename("Foo"), //
						"interface Foo {\r\n}\r\n"//
				)//
		);
	}

	static Stream<SuccessCase> testCodeRefactoringSuccess_ClassRename() {
		return Stream.of(//
				new SuccessCase(//
						"class MyClass{}", //
						source -> source.defaultPackage().clazz("MyClass").rename("Foo"), //
						"class Foo {\r\n}\r\n"//
				)//
		);
	}

	record FailureCase(String code, Consumer<Code.Source> refactoring, Exception expectedException,
			RuntimeException instanciationException) {
		FailureCase(String code, Consumer<Code.Source> refactoring, Exception expectedException) {
			this(code, refactoring, expectedException, new RuntimeException("Success case expectations"));
		}

		FailureCase {
			StackTraceElement[] originalStack = instanciationException.getStackTrace();
			StackTraceElement[] relevantStack = Arrays.copyOfRange(originalStack, 1, originalStack.length);
			instanciationException.setStackTrace(relevantStack);
		}

		@Override
		public String toString() {
			return reduce(code) + " > " + stringOf(refactoring) + " > " + expectedException;
		}
	}

	@ParameterizedTest
	@MethodSource
	public void testCodeRefactoringFailure(FailureCase failureCase) {
		try {
			var code = failureCase.code();
			var refactorerExecutor = failureCase.refactoring();
			var expectedException = failureCase.expectedException();

			// GIVEN
			Refactorer.ForCode refactorer = parseCode(code);

			// WHEN
			Executable action = () -> refactorerExecutor.accept(refactorer.source());

			// THEN
			Exception except = assertThrows(expectedException.getClass(), action);
			assertThat(except.getMessage(), is(expectedException.getMessage()));
		} catch (RuntimeException | AssertionError cause) {
			cause.addSuppressed(failureCase.instanciationException());
			throw cause;
		}
	}

	public static Stream<FailureCase> testCodeRefactoringFailure() {
		return Stream.of(//
				testCodeRefactoringFailure_Class(), //
				testCodeRefactoringFailure_Interface(), //
				testCodeRefactoringFailure_Record(), //
				testCodeRefactoringFailure_Field(), //
				testCodeRefactoringFailure_Method(), //
				testCodeRefactoringFailure_Parameter(), //
				testCodeRefactoringFailure_Variable(), //
				testCodeRefactoringFailure_SplitJoin()//
		).flatMap(stream -> stream);
	}

	private static Stream<FailureCase> testCodeRefactoringFailure_SplitJoin() {
		return Stream.of(//
				new FailureCase(//
						"class MyClass{void myMethod(){String myParam;myParam = null;}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList())
								.variable("myParam", 0).splitDeclaration(), //
						new IllegalStateException("No assignment to split on myParam declaration")//
				), //
				new FailureCase(//
						"class MyClass{void myMethod(){String myParam = null;}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList())
								.variable("myParam", 0).joinDeclaration(), //
						new IllegalStateException("myParam declaration already assigns a value")//
				), //
				new FailureCase(//
						"class MyClass{void myMethod(){String myParam;String foo = null;myParam = null;}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList())
								.variable("myParam", 0).joinDeclaration(), //
						new IllegalStateException("No myParam assignment just after its declaration")//
				), //
				new FailureCase(//
						"class MyClass{void myMethod(String foo){String myParam;foo = null;myParam = null;}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", List.of("String"))
								.variable("myParam", 0).joinDeclaration(), //
						new IllegalStateException("No myParam assignment just after its declaration")//
				) //
		);
	}

	private static Stream<FailureCase> testCodeRefactoringFailure_Parameter() {
		return Stream.of(//
				new FailureCase(//
						"interface MyInt{void myMethod();}", //
						source -> source.defaultPackage().interf("MyInt").method("myMethod", emptyList())
								.parameter("x"), //
						new NoSuchElementException("No parameter x")//
				), //
				new FailureCase(//
						"interface MyInt{void myMethod(boolean myParam);}", //
						source -> source.defaultPackage().interf("MyInt").method("myMethod", List.of("boolean"))
								.parameter("x"), //
						new NoSuchElementException("No parameter x")//
				), //
				new FailureCase(//
						"class MyClass{void myMethod(){boolean myVar = true;}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList())
								.parameter("x"), //
						new NoSuchElementException("No parameter x")//
				), //
				new FailureCase(//
						"class MyClass{void myMethod(boolean myParam){myParam = true;}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", List.of("boolean"))
								.parameter("x"), //
						new NoSuchElementException("No parameter x")//
				)//
					// TODO Lambda parameter
		);
	}

	private static Stream<FailureCase> testCodeRefactoringFailure_Variable() {
		return Stream.of(//
				new FailureCase(//
						"class MyClass{void myMethod(){String myVar = null;}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList()).variable("x",
								0), //
						new NoSuchElementException("No variable x #0")//
				)//
		);
	}

	private static Stream<FailureCase> testCodeRefactoringFailure_Method() {
		return Stream.of(//
				new FailureCase(//
						"class MyClass{void myMethod(){}}", //
						source -> source.defaultPackage().clazz("MyClass").method("x", emptyList()), //
						new NoSuchElementException("No method x()")//
				) //
		);
	}

	private static Stream<FailureCase> testCodeRefactoringFailure_Field() {
		return Stream.of(//
				new FailureCase(//
						"class MyClass{String myField;}", //
						source -> source.defaultPackage().clazz("MyClass").field("x"), //
						new NoSuchElementException("No field x")//
				) //
		);
	}

	private static Stream<FailureCase> testCodeRefactoringFailure_Record() {
		return Stream.of(//
				new FailureCase(//
						"class Foo{}", //
						source -> source.defaultPackage().record("Foo"), //
						new NoSuchElementException("No record Foo")//
				), //
				new FailureCase(//
						"interface Foo{}", //
						source -> source.defaultPackage().record("Foo"), //
						new NoSuchElementException("No record Foo")//
				), //
				new FailureCase(//
						"record Foo(){}", //
						source -> source.defaultPackage().record("X"), //
						new NoSuchElementException("No record X")//
				)//
		);
	}

	private static Stream<FailureCase> testCodeRefactoringFailure_Interface() {
		return Stream.of(//
				new FailureCase(//
						"class Foo{}", //
						source -> source.defaultPackage().interf("Foo"), //
						new NoSuchElementException("No interface Foo")//
				), //
				new FailureCase(//
						"interface Foo{}", //
						source -> source.defaultPackage().interf("X"), //
						new NoSuchElementException("No interface X")//
				), //
				new FailureCase(//
						"record Foo(){}", //
						source -> source.defaultPackage().interf("Foo"), //
						new NoSuchElementException("No interface Foo")//
				)//
		);
	}

	private static Stream<FailureCase> testCodeRefactoringFailure_Class() {
		return Stream.of(//
				new FailureCase(//
						"class Foo{}", //
						source -> source.defaultPackage().clazz("X"), //
						new NoSuchElementException("No class X")//
				), //
				new FailureCase(//
						"interface Foo{}", //
						source -> source.defaultPackage().clazz("Foo"), //
						new NoSuchElementException("No class Foo")//
				), //
				new FailureCase(//
						"record Foo(){}", //
						source -> source.defaultPackage().clazz("Foo"), //
						new NoSuchElementException("No class Foo")//
				)//
		);
	}

	private static String stringOf(Consumer<Code.Source> refactoring) {
		StringBuilder builder = new StringBuilder();
		Consumable.ArgsConsumer displayer = new Consumable.ArgsConsumer() {
			@Override
			public void consumeArgs(Object... args) {
				// Get element at index 2 in stack trace:
				// 0 - getStackTrace()
				// 1 - the consumer we are in
				// 2 - the method we are interested in
				StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[2];
				String methodName = stackTraceElement.getMethodName();
				String argsInParentheses = Stream.of(args).map(Objects::toString).collect(joining(", ", "(", ")"));

				if (!builder.isEmpty()) {
					builder.append(".");
				}
				builder.append(methodName);
				builder.append(argsInParentheses);
			}
		};
		Code.Source source = new Code.Source() {
			@Override
			public Component.Package defaultPackage() {
				displayer.consumeArgs("");
				return new Consumable.Package(displayer);
			}
		};
		try {
			refactoring.accept(source);
		} catch (RuntimeException cause) {
			/*
			 * This method is used to build the string that will name a test. When such an
			 * operation throws an exception, it is ignored and the fallback strategy
			 * consists in using a default toString() implementation. Thus, no exception
			 * appears anywhere, but the report is corrupted with unusable test names.
			 * 
			 * We avoid that by forcing the exception display on the console (with
			 * additional info) before to throw it.
			 */
			builder.append(".???");
			var exception = new UnsupportedOperationException(builder.toString(), cause);
			exception.printStackTrace();
			throw exception;
		}
		return builder.toString();
	}

	private static String reduce(String code) {
		return code.length() < 100 ? code : code.substring(0, 100) + "[...]";
	}
}
