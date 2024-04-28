package fr.vergne.stanos.core.refactorer;

import static java.util.Collections.emptyList;
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
import fr.vergne.stanos.core.refactorer.Y.Field;
import fr.vergne.stanos.core.refactorer.Y.Interface;
import fr.vergne.stanos.core.refactorer.Y.Method;
import fr.vergne.stanos.core.refactorer.Y.Package;
import fr.vergne.stanos.core.refactorer.Y.Parameter;
import fr.vergne.stanos.core.refactorer.Y.Record;
import fr.vergne.stanos.core.refactorer.Y.Variable;

class RefactorerTest {

	record SuccessCase(String code, Consumer<Code.Source> refactoring, String expectedCode) {
		@Override
		public String toString() {
			return reduce(code) + " > " + stringOf(refactoring) + " > " + reduce(expectedCode);
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
		refactorerExecutor.accept(refactorer.source());

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
				testCodeRefactoringSuccess_Parameter(), //
				testCodeRefactoringSuccess_Variable(), //
				testCodeRefactoringSuccess_SplitJoin()//
		).flatMap(stream -> stream);
	}

	private static Stream<SuccessCase> testCodeRefactoringSuccess_SplitJoin() {
		return Stream.of(//
				new SuccessCase(//
						"class MyClass{void myMethod(){String myParam = null;}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList()).variable("myParam", 0).splitDeclaration(), //
						"class MyClass{void myMethod(){String myParam;myParam = null;}}"//
				), //
				new SuccessCase(//
						"class MyClass{void myMethod(){String myParam;myParam = null;}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList()).variable("myParam", 0).joinDeclaration(), //
						"class MyClass{void myMethod(){String myParam = null;}}"//
				), //
				new SuccessCase(//
						"""
								class MyClass {
									void myMethod() {
										String myParam = null;
									}
								}
								""", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList()).variable("myParam", 0).splitDeclaration(), //
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList()).variable("myParam", 0).joinDeclaration(), //
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

	private static Stream<SuccessCase> testCodeRefactoringSuccess_Parameter() {
		return Stream.of(//
				new SuccessCase(//
						"class MyClass{void myMethod(String myParam){myParam = null;}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", List.of("String")).parameter("myParam").rename("foo"), //
						"class MyClass{void myMethod(String foo){foo = null;}}"//
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", List.of("myParam")).parameter("myParam").rename("foo"), //
						"""
								class MyClass{
									class myParam {}
									void myMethod(myParam foo){
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
						source -> source.defaultPackage().clazz("MyClass").clazz("MyChildClass").method("myMethod", emptyList()).clazz("MyInnerClass").method("myMethod", List.of("String")).parameter("myParam").rename("foo"), //
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", List.of("String")).parameter("myParam").rename("foo"), //
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", List.of("boolean")).parameter("myParam").rename("foo"), //
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", List.of("MyInt")).parameter("myParam").rename("foo"), //
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

	private static Stream<SuccessCase> testCodeRefactoringSuccess_Variable() {
		return Stream.of(//
				new SuccessCase(//
						"class MyClass{void myMethod(){String myVar = null;}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList()).variable("myVar", 0).rename("foo"), //
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList()).variable("myVar", 0).rename("foo"), //
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
						source -> source.defaultPackage().clazz("MyClass").clazz("MyChildClass").method("myMethod", emptyList()).clazz("MyInnerClass").method("myMethod", emptyList()).variable("myVar", 0).rename("foo"), //
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList()).variable("myVar", 0).rename("foo"), //
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", List.of("boolean")).variable("myVar", 0).rename("foo"), //
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", List.of("boolean")).variable("myVar", 1).rename("foo"), //
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList()).variable("myVar", 0).rename("foo"), //
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList()).variable("myVar", 0).method("myMethod", emptyList()).variable("myVar", 0).rename("foo"), //
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList()).variable("myVar", 0).rename("foo"), //
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList()).variable("myVar", 0).rename("foo"), //
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", List.of("boolean")).variable("myVar", 0).rename("foo"), //
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
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList()).rename("foo"), //
						"class MyClass{void foo(){}}"//
				)//
		);
	}

	private static Stream<SuccessCase> testCodeRefactoringSuccess_Field() {
		return Stream.of(//
				new SuccessCase(//
						"class MyClass{String myField;}", //
						source -> source.defaultPackage().clazz("MyClass").field("myField").rename("foo"), //
						"class MyClass{String foo;}"//
				)//
		);
	}

	private static Stream<SuccessCase> testCodeRefactoringSuccess_Record() {
		return Stream.of(//
				new SuccessCase(//
						"record MyRecord(){}", //
						source -> source.defaultPackage().record("MyRecord").rename("Foo"), //
						"record Foo(){}"//
				)//
		);
	}

	private static Stream<SuccessCase> testCodeRefactoringSuccess_Interface() {
		return Stream.of(//
				new SuccessCase(//
						"interface MyInt{}", //
						source -> source.defaultPackage().interf("MyInt").rename("Foo"), //
						"interface Foo{}"//
				)//
		);
	}

	static Stream<SuccessCase> testCodeRefactoringSuccess_Class() {
		return Stream.of(//
				new SuccessCase(//
						"class MyClass{}", //
						source -> source.defaultPackage().clazz("MyClass").rename("Foo"), //
						"class Foo{}"//
				)//
		);
	}

	record FailureCase(String code, Consumer<Code.Source> refactoring, Exception expectedException) {
		@Override
		public String toString() {
			return reduce(code) + " > " + stringOf(refactoring) + " > " + expectedException;
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
		Executable action = () -> refactorerExecutor.accept(refactorer.source());

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
				testCodeRefactoringFailure_Parameter(), //
				testCodeRefactoringFailure_Variable(), //
				testCodeRefactoringFailure_SplitJoin()//
		).flatMap(stream -> stream);
	}
	
	private static Stream<FailureCase> testCodeRefactoringFailure_SplitJoin() {
		return Stream.of(//
				new FailureCase(//
						"class MyClass{void myMethod(){String myParam;myParam = null;}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList()).variable("myParam", 0).splitDeclaration(), //
						new IllegalStateException("No assignment to split on myParam declaration")//
				), //
				new FailureCase(//
						"class MyClass{void myMethod(){String myParam = null;}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList()).variable("myParam", 0).joinDeclaration(), //
						new IllegalStateException("myParam declaration already assigns a value")//
				), //
				new FailureCase(//
						"class MyClass{void myMethod(){String myParam;String foo = null;myParam = null;}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList()).variable("myParam", 0).joinDeclaration(), //
						new IllegalStateException("No myParam assignment just after its declaration")//
				), //
				new FailureCase(//
						"class MyClass{void myMethod(String foo){String myParam;foo = null;myParam = null;}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", List.of("String")).variable("myParam", 0).joinDeclaration(), //
						new IllegalStateException("No myParam assignment just after its declaration")//
				) //
		);
	}

	private static Stream<FailureCase> testCodeRefactoringFailure_Parameter() {
		return Stream.of(//
				new FailureCase(//
						"interface MyInt{void myMethod();}", //
						source -> source.defaultPackage().interf("MyInt").method("myMethod", emptyList()).parameter("x"), //
						new NoSuchElementException("No parameter x")//
				), //
				new FailureCase(//
						"interface MyInt{void myMethod(boolean myParam);}", //
						source -> source.defaultPackage().interf("MyInt").method("myMethod", List.of("boolean")).parameter("x"), //
						new NoSuchElementException("No parameter x")//
				), //
				new FailureCase(//
						"class MyClass{void myMethod(){boolean myVar = true;}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList()).parameter("x"), //
						new NoSuchElementException("No parameter x")//
				), //
				new FailureCase(//
						"class MyClass{void myMethod(boolean myParam){myParam = true;}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", List.of("boolean")).parameter("x"), //
						new NoSuchElementException("No parameter x")//
				)//
					// TODO Lambda parameter
		);
	}

	private static Stream<FailureCase> testCodeRefactoringFailure_Variable() {
		return Stream.of(//
				new FailureCase(//
						"class MyClass{void myMethod(){String myVar = null;}}", //
						source -> source.defaultPackage().clazz("MyClass").method("myMethod", emptyList()).variable("x", 0), //
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
		Runnable firstDisplayer = () -> builder.append(currentMethodName() + "()");
		Consumer<Object> nextDisplayer = arg -> builder.append("." + currentMethodName() + "(" + arg + ")");
		try {
			refactoring.accept(new UnimplementedSource() {
				@Override
				public Package defaultPackage() {
					firstDisplayer.run();
					return new UnimplementedPackage() {
						@Override
						public void rename(String newName) {
							nextDisplayer.accept(newName);
						}

						@Override
						public Record record(String name) {
							nextDisplayer.accept(name);
							return new UnimplementedRecord() {
								@Override
								public void rename(String newName) {
									nextDisplayer.accept(name);
								}
							};
						};

						@Override
						public Interface interf(String name) {
							nextDisplayer.accept(name);
							return new UnimplementedInterface() {
								@Override
								public void rename(String newName) {
									nextDisplayer.accept(newName);
								}

								@Override
								public Method method(String name, List<String> parameterTypes) {
									nextDisplayer.accept(name + ", " + parameterTypes);
									return new UnimplementedMethod() {
										@Override
										public Y.Parameter parameter(String name) {
											nextDisplayer.accept(name);
											return new UnimplementedParameter() {
												@Override
												public void rename(String newName) {
													nextDisplayer.accept(newName);
												}
											};
										};
									};
								};
							};
						};

						@Override
						public Class clazz(String name) {
							nextDisplayer.accept(name);
							return new UnimplementedClass() {
								@Override
								public void rename(String newName) {
									nextDisplayer.accept(newName);
								}

								@Override
								public Field field(String name) {
									nextDisplayer.accept(name);
									return new UnimplementedField() {

										@Override
										public void rename(String newName) {
											nextDisplayer.accept(newName);
										}
									};
								}

								@Override
								public Method method(String name, List<String> parameterTypes) {
									nextDisplayer.accept(name + ", " + parameterTypes);
									return new UnimplementedMethod() {

										@Override
										public void rename(String newName) {
											nextDisplayer.accept(name);
										}

										@Override
										public Y.Parameter parameter(String name) {
											nextDisplayer.accept(name);
											return new UnimplementedParameter() {
												@Override
												public void rename(String newName) {
													nextDisplayer.accept(newName);
												}
											};
										};

										@Override
										public Variable variable(String name, int index) {
											nextDisplayer.accept(name + ", " + index);
											return new UnimplementedVariable() {
												@Override
												public void rename(String newName) {
													nextDisplayer.accept(newName);
												}

												@Override
												public void splitDeclaration() {
													nextDisplayer.accept("");
												}

												@Override
												public void joinDeclaration() {
													nextDisplayer.accept("");
												}

												@Override
												public Method method(String name, List<String> parameterTypes) {
													nextDisplayer.accept(name + ", " + parameterTypes);
													return new UnimplementedMethod() {

														@Override
														public void rename(String newName) {
															nextDisplayer.accept(name);
														}

														@Override
														public Y.Parameter parameter(String name) {
															nextDisplayer.accept(name);
															return new UnimplementedParameter() {
																@Override
																public void rename(String newName) {
																	nextDisplayer.accept(newName);
																}
															};
														};

														@Override
														public Variable variable(String name, int index) {
															nextDisplayer.accept(name + ", " + index);
															return new UnimplementedVariable() {
																@Override
																public void rename(String newName) {
																	nextDisplayer.accept(newName);
																}

																@Override
																public void splitDeclaration() {
																	nextDisplayer.accept("");
																};
															};
														};
													};
												}
											};
										};
									};
								}

								@Override
								public Class clazz(String name) {
									nextDisplayer.accept(name);
									return new UnimplementedClass() {
										@Override
										public void rename(String newName) {
											nextDisplayer.accept(name);
										}

										@Override
										public Method method(String name, List<String> parameterTypes) {
											nextDisplayer.accept(name + ", " + parameterTypes);
											return new UnimplementedMethod() {
												@Override
												public Class clazz(String name) {
													nextDisplayer.accept(name);
													return new UnimplementedClass() {
														@Override
														public Method method(String name, List<String> parameterTypes) {
															nextDisplayer.accept(name + ", " + parameterTypes);
															return new UnimplementedMethod() {
																@Override
																public Y.Parameter parameter(String name) {
																	nextDisplayer.accept(name);
																	return new UnimplementedParameter() {
																		@Override
																		public void rename(String newName) {
																			nextDisplayer.accept(newName);
																		}
																	};
																};

																@Override
																public Variable variable(String name, int index) {
																	nextDisplayer.accept(name + ", " + index);
																	return new UnimplementedVariable() {
																		@Override
																		public void rename(String newName) {
																			nextDisplayer.accept(name);
																		}

																		@Override
																		public void splitDeclaration() {
																			nextDisplayer.accept("");
																		}

																		@Override
																		public void joinDeclaration() {
																			nextDisplayer.accept("");
																		}
																	};
																}
															};
														}
													};
												}
											};
										}
									};
								}
							};
						}
					};
				}
			});
		} catch (Exception cause) {
			cause.printStackTrace();
			builder.append(".???");
		}
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

	private static String reduce(String code) {
		return code.length() < 100 ? code : code.substring(0, 100) + "[...]";
	}

	private static class UnimplementedSource implements Code.Source {
		@Override
		public Package defaultPackage() {
			throw new UnsupportedOperationException("Not implemented yet");
		}
	}

	private static class UnimplementedClass implements Y.Class {
		@Override
		public String name() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public void rename(String newName) {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public Stream<Field> fields() {
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
	}

	private static class UnimplementedInterface implements Y.Interface {
		@Override
		public String name() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public void rename(String newName) {
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
	}

	private static class UnimplementedMethod implements Y.Method {
		@Override
		public String name() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public List<String> parameterTypes() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public void rename(String newName) {
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

		@Override
		public Stream<Parameter> parameters() {
			throw new UnsupportedOperationException("Not implemented yet");
		}
	}

	private static class UnimplementedField implements Y.Field {
		@Override
		public String name() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public void rename(String newName) {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public Stream<Method> methods() {
			throw new UnsupportedOperationException("Not implemented yet");
		}
	}

	private static class UnimplementedVariable implements Y.Variable {
		@Override
		public String name() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public void rename(String newName) {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public Stream<Method> methods() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public void splitDeclaration() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public void joinDeclaration() {
			throw new UnsupportedOperationException("Not implemented yet");
		}
	}

	private static class UnimplementedParameter implements Y.Parameter {
		@Override
		public String name() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public void rename(String newName) {
			throw new UnsupportedOperationException("Not implemented yet");
		}
	}

	private static class UnimplementedPackage implements Y.Package {
		@Override
		public String name() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public void rename(String newName) {
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

		@Override
		public Stream<Record> records() {
			throw new UnsupportedOperationException("Not implemented yet");
		}
	}

	private static class UnimplementedRecord implements Y.Record {
		@Override
		public String name() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public void rename(String newName) {
			throw new UnsupportedOperationException("Not implemented yet");
		}
	}
}
