package fr.vergne.stanos.dependency;

import static fr.vergne.stanos.dependency.codeitem.Constructor.*;
import static fr.vergne.stanos.dependency.codeitem.Field.*;
import static fr.vergne.stanos.dependency.codeitem.Lambda.*;
import static fr.vergne.stanos.dependency.codeitem.Method.*;
import static fr.vergne.stanos.dependency.codeitem.Type.*;
import static fr.vergne.stanos.dependency.util.Formatter.*;
import static java.nio.file.attribute.PosixFilePermissions.*;
import static java.util.function.Predicate.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import fr.vergne.stanos.code.CodeSelector;
import fr.vergne.stanos.dependency.DependencyTestCasesBuilder.DependencyTestCase;
import fr.vergne.stanos.dependency.codeitem.CodeItem;
import fr.vergne.stanos.dependency.codeitem.Constructor;
import fr.vergne.stanos.dependency.codeitem.Field;
import fr.vergne.stanos.dependency.codeitem.Lambda;
import fr.vergne.stanos.dependency.codeitem.Method;
import fr.vergne.stanos.dependency.codeitem.Package;

// TODO declare type in package (class, abstract class, interface, enum)
public interface DependencyAnalyserTest {

	DependencyAnalyser createDependencyAnalyzer();

	static Stream<Path> testAnalyseOfNonClassPathFails() throws IOException {
		return Stream.of(Paths.get("my/inexistent/path"), Files.createTempDirectory("directory"),
				Files.createTempFile("unreadableFile", "", asFileAttribute(fromString("-wx-wx-wx"))));
	}

	@ParameterizedTest
	@MethodSource
	default void testAnalyseOfNonClassPathFails(Path classFile) {
		DependencyAnalyser analyser = createDependencyAnalyzer();

		Executable analyse = () -> analyser.analyze(classFile);

		assertThrows(IllegalArgumentException.class, analyse);
	}

	static Stream<Path> testAnalyseOfNonClassInputStreamFails() throws IOException {
		Path directory = Files.createTempDirectory("directory");
		Path emptyFile = Files.createTempFile("emptyFile", "");
		Path nonClassFile = Files.createTempFile("nonClassFile", "");
		Files.write(nonClassFile, "non class content".getBytes());
		return Stream.of(directory, emptyFile, nonClassFile);
	}

	@ParameterizedTest
	@MethodSource
	default void testAnalyseOfNonClassInputStreamFails(Path classFile) throws IOException {
		DependencyAnalyser analyser = createDependencyAnalyzer();

		InputStream inputStream = Files.newInputStream(classFile);
		Executable analyse = () -> analyser.analyze(inputStream);

		assertThrows(IllegalArgumentException.class, analyse);
	}

	interface Analyse {
		Collection<Dependency> execute(DependencyAnalyser analyser, Class<?> analysedClass);
	}

	// token indicating we analyse the parent directory of the analysed class
	static final String PARENT_DIR_TOKEN = "parentDir";

	/** @return {@link Analyse}s on the analysed class only */
	static Stream<Analyse> focusedAnalyses() {
		return Stream.of(name("analyze(class)", (analyser, analysedClass) -> {
			return analyser.analyze(analysedClass);
		}), name("analyze(is)", (analyser, analysedClass) -> {
			String classPath = toClassPath(analysedClass);
			InputStream is = analysedClass.getResourceAsStream(classPath);
			return analyser.analyze(is);
		}), name("analyze(path)", (analyser, analysedClass) -> {
			try {
				String classPath = toClassPath(analysedClass);
				Path path = Paths.get(analysedClass.getResource(classPath).toURI());
				return analyser.analyze(path);
			} catch (URISyntaxException cause) {
				throw new RuntimeException(cause);
			}
		}), name("analyze(file(path))", (analyser, analysedClass) -> {
			try {
				String classPath = toClassPath(analysedClass);
				Path path = Paths.get(analysedClass.getResource(classPath).toURI());
				CodeSelector selector = CodeSelector.onFile(path);
				return analyser.analyze(selector);
			} catch (URISyntaxException cause) {
				throw new RuntimeException(cause);
			}
		}), name("analyze(paths(path, path))", (analyser, analysedClass) -> {
			try {
				String classPath = toClassPath(analysedClass);
				Path path = Paths.get(analysedClass.getResource(classPath).toURI());
				CodeSelector selector = CodeSelector.onPaths(Arrays.asList(path, path));
				return analyser.analyze(selector);
			} catch (URISyntaxException cause) {
				throw new RuntimeException(cause);
			}
		}));
	}

	@ParameterizedTest
	@MethodSource("focusedAnalyses")
	default void testEmptyInterfaceInDefaultPackageHasNoDependency(Analyse analyse) throws ClassNotFoundException {
		DependencyAnalyser analyser = createDependencyAnalyzer();

		Collection<Dependency> dependencies = analyse.execute(analyser,
				Class.forName("EmptyInterfaceInDefaultPackage"));

		assertEquals(Collections.emptyList(), dependencies);
	}

	static Stream<DependencyTestCase> testCases() {
		DependencyTestCasesBuilder cases = new DependencyTestCasesBuilder();
		Supplier<Class<?>> sameClass = () -> cases.getLastAnalysedClass();
		Supplier<CodeItem> testedItem = () -> cases.getLastTestedItem();

		/***********
		 * DECLARE *
		 ***********/

		// Declare package

		Package p1 = Package.fromPackageName("fr");
		Package p2 = Package.fromPackageName("fr.vergne");
		Package p3 = Package.fromPackageName("fr.vergne.stanos");
		Package p4 = Package.fromPackageName("fr.vergne.stanos.dependency");
		cases.analyse(DependencyAnalyserTest.class).test(p1).declares(0, p1).and(1, p2).and(0, p3).and(0, p4);
		cases.analyse(DependencyAnalyserTest.class).test(p2).declares(0, p1).and(0, p2).and(1, p3).and(0, p4);
		cases.analyse(DependencyAnalyserTest.class).test(p3).declares(0, p1).and(0, p2).and(0, p3).and(1, p4);
		cases.analyse(DependencyAnalyserTest.class).test(p4).declares(0, p1).and(0, p2).and(0, p3).and(0, p4);

		// Declare type

		cases.analyse(DependencyAnalyserTest.class).test(p1).declares(0, type(DependencyAnalyserTest.class));
		cases.analyse(DependencyAnalyserTest.class).test(p2).declares(0, type(DependencyAnalyserTest.class));
		cases.analyse(DependencyAnalyserTest.class).test(p3).declares(0, type(DependencyAnalyserTest.class));
		cases.analyse(DependencyAnalyserTest.class).test(p4).declares(1, type(DependencyAnalyserTest.class));

		cases.analyse(DependencyAnalyserTest.class).test(p1).declares(0, type(Declare.class));
		cases.analyse(DependencyAnalyserTest.class).test(p2).declares(0, type(Declare.class));
		cases.analyse(DependencyAnalyserTest.class).test(p3).declares(0, type(Declare.class));
		cases.analyse(DependencyAnalyserTest.class).test(p4).declares(0, type(Declare.class));

		cases.testType(Declare.Type.AsInnerClass.class).declares(1, type(Declare.Type.AsInnerClass.Declared.class));
		cases.testType(Declare.Type.AsInterface.class).declares(1, type(Declare.Type.AsInterface.Declared.class));
		cases.testType(Declare.Type.AsStaticClass.class).declares(1, type(Declare.Type.AsStaticClass.Declared.class));

		// Declare method

		cases.testType(Declare.Method.Simple.class).declares(1, method(sameClass.get(), void.class, "method"));
		cases.testType(Declare.Method.Complex.class).declares(1,
				method(sameClass.get(), int.class, "method", String.class, List.class));
		cases.testType(Declare.Method.InInterface.class).declares(1, method(sameClass.get(), void.class, "method"));
		cases.testType(Declare.Method.WithDefaultImpl.class).declares(1, method(sameClass.get(), void.class, "method"));

		// Declare lambda

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

		// Declare field

		Supplier<Field> field = () -> field(testedItem.get(), type(int.class), "field");

		cases.testType(Declare.Field.AsMember.class).declares(1, field.get());

		cases.testConstructor(Declare.Field.InConstructor.class).declares(1, field.get());
		cases.testConstructor(Declare.Field.AsConstructorArgument.class, int.class).declares(1, field.get());

		cases.testStaticBlock(Declare.Field.InStaticBlock.class).declares(1, field.get());
		cases.testStaticBlock(Declare.Field.AsStaticMember.class).declares(1, field.get());

		cases.testMethod(Declare.Field.InMethod.class, "method").declares(1, field.get());
		cases.testMethod(Declare.Field.InMethodBloc.class, "method").declares(1, field.get());
		cases.testMethod(Declare.Field.InMultipleScopes.class, "method").declares(2, field.get());
		cases.testMethod(Declare.Field.InStaticMethod.class, "method").declares(1, field.get());
		cases.testMethod(Declare.Field.InDefaultMethod.class, "method").declares(1, field.get());
		cases.testMethod(Declare.Field.AsMethodArgument.class, "method", int.class).declares(1, field.get());
		cases.testMethod(Declare.Field.AsStaticMethodArgument.class, "method", int.class).declares(1, field.get());
		cases.testMethod(Declare.Field.AsDefaultMethodArgument.class, "method", int.class).declares(1, field.get());

		Method lambdaMethod2 = method(Declare.Field.MyLambda.class, void.class, "x");
		Supplier<Lambda> lambda2 = () -> lambda(type(sameClass.get()), "lambda$0", lambdaMethod2);
		Method lambdaMethod3 = method(Declare.Field.MyLambdaInt.class, void.class, "x");
		Supplier<Lambda> lambda3 = () -> lambda(type(sameClass.get()), "lambda$0", lambdaMethod3);

		cases.testLambda(Declare.Field.InLambda.class, lambda2).declares(1, field.get());
		cases.testLambda(Declare.Field.AsLambdaArgument.class, lambda3).declares(1, field.get());

		/********
		 * CALL *
		 ********/

		// Call constructor

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

		// Call method
		// Note: call lambda => call method

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

		// Call field

		Field fieldMember = field(Call.Field.class, int.class, "called");
		Field fieldStatic = field(Call.Field.class, int.class, "staticCalled");

		cases.testConstructor(Call.Field.InConstructor.class).calls(1, fieldMember).and(1, fieldStatic);
		cases.testConstructor(Call.Field.OnField.class).calls(1, fieldMember).and(1, fieldStatic);

		cases.testStaticBlock(Call.Field.InStaticBlock.class).calls(1, fieldMember).and(1, fieldStatic);
		cases.testStaticBlock(Call.Field.OnStaticField.class).calls(1, fieldMember).and(1, fieldStatic);

		cases.testMethod(Call.Field.InMethod.class, "method").calls(1, fieldMember).and(1, fieldStatic);
		cases.testMethod(Call.Field.InStaticMethod.class, "method").calls(1, fieldMember).and(1, fieldStatic);
		cases.testMethod(Call.Field.MultipleTimes.class, "method").calls(2, fieldMember).and(2, fieldStatic);
		cases.testMethod(Call.Field.InDefaultImpl.class, "method").calls(1, fieldMember).and(1, fieldStatic);

		Lambda fieldLambda = lambda(type(Call.Field.InLambda.class), "lambda$0",
				method(Runnable.class, void.class, "run"));

		cases.testLambda(Call.Field.InLambda.class, fieldLambda).calls(1, fieldMember).and(1, fieldStatic);

		return cases.build();
	}

	/** @return all variants of {@link Analyse}s which include the analysed class */
	static Stream<Analyse> allAnalyses() {
		return Stream.concat(focusedAnalyses(),
				// Analyse with parent directory
				Stream.of(name("analyze(directory(" + PARENT_DIR_TOKEN + "))", (analyser, analysedClass) -> {
					try {
						String classPath = toClassPath(analysedClass);
						Path path = Paths.get(analysedClass.getResource(classPath).toURI());
						CodeSelector selector = CodeSelector.onDirectory(path.getParent());
						return analyser.analyze(selector);
					} catch (URISyntaxException cause) {
						throw new RuntimeException(cause);
					}
				}), name("analyze(paths(path, " + PARENT_DIR_TOKEN + "))", (analyser, analysedClass) -> {
					try {
						String classPath = toClassPath(analysedClass);
						Path path = Paths.get(analysedClass.getResource(classPath).toURI());
						CodeSelector selector = CodeSelector.onPaths(Arrays.asList(path, path.getParent()));
						return analyser.analyze(selector);
					} catch (URISyntaxException cause) {
						throw new RuntimeException(cause);
					}
				})));
	}

	static Stream<Arguments> testDependenciesAreGenerated() {
		return allAnalyses().flatMap(analyser -> testCases().map(testCase -> arguments(analyser, testCase)));
	}

	@ParameterizedTest
	@MethodSource
	default void testDependenciesAreGenerated(Analyse analyse, DependencyTestCase args) {
		// GIVEN
		DependencyAnalyser analyser = createDependencyAnalyzer();

		// WHEN
		Collection<Dependency> dependencies = analyse.execute(analyser, args.analysedClass());

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

		@SuppressWarnings("unused")
		static class Field {
			static class InStaticBlock {
				static {
					/*
					 * Declaring it does not suffice. We must initialize it or its declaration is
					 * ignored.
					 */
					int field = 0;
					/*
					 * For the specific case of static blocks, we must also use it in some way.
					 * Otherwise it is ignored as well.
					 */
					field++; // Must be used to not be ignored
				}
			}

			static class AsStaticMember {
				static int field;
			}

			static class AsMember {
				int field;
			}

			static class InConstructor {
				public InConstructor() {
					int field = 0;
				}
			}

			static class AsConstructorArgument {
				public AsConstructorArgument(int field) {
				}
			}

			static class InStaticMethod {
				public static void method() {
					int field = 0;
				}
			}

			static class AsStaticMethodArgument {
				public static void method(int field) {
				}
			}

			static class InMethod {
				public void method() {
					int field = 0;
				}
			}

			static class InMethodBloc {
				public void method() {
					{
						int field = 0;
						/*
						 * For the specific case of local blocs, we must also use it in some way.
						 * Otherwise it is ignored as well.
						 */
						field++;
					}
				}
			}

			static class InMultipleScopes {
				public void method() {
					{
						int field = 0;
						field++;
					}
					{
						int field = 0;
						field++;
					}
				}
			}

			static class AsMethodArgument {
				public void method(int field) {
				}
			}

			interface InDefaultMethod {
				default void method() {
					int field = 0;
				}
			}

			interface AsDefaultMethodArgument {
				default void method(int field) {
				}
			}

			interface MyLambda {
				void x();
			}

			interface MyLambdaInt {
				void x(int field);
			}

			static class InLambda {
				MyLambda r = () -> {
					int field = 0;
					/*
					 * For the specific case of lambdas, we must also use it in some way. Otherwise
					 * it is ignored as well.
					 */
					field++; // Must be used to not be ignored
				};
			}

			static class AsLambdaArgument {
				MyLambdaInt r = (field) -> {
				};
			}

			// TODO with generics
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

		@SuppressWarnings("unused")
		static class Field {
			static int staticCalled;
			int called;

			static class InConstructor {
				public InConstructor() {
					int field = new Field().called;
					int staticField = Field.staticCalled;
				}
			}

			static class InStaticBlock {
				static {
					int field = new Field().called;
					int staticField = Field.staticCalled;
				}
			}

			static class OnField {
				int field = new Field().called;
				int staticField = Field.staticCalled;
			}

			static class OnStaticField {
				static int field = new Field().called;
				static int staticField = Field.staticCalled;
			}

			static class InMethod {
				void method() {
					int field = new Field().called;
					int staticField = Field.staticCalled;
				}
			}

			static class InStaticMethod {
				static void method() {
					int field = new Field().called;
					int staticField = Field.staticCalled;
				}
			}

			interface InLambda {
				default void method() {
					Runnable r = () -> {
						int field = new Field().called;
						int staticField = Field.staticCalled;
					};
				}
			}

			static class MultipleTimes {
				void method() {
					int field = new Field().called;
					field = new Field().called;
					int staticField = Field.staticCalled;
					staticField = Field.staticCalled;
				}
			}

			interface InDefaultImpl {
				default void method() {
					int field = new Field().called;
					int staticField = Field.staticCalled;
				}
			}
		}
	}

	static Analyse name(String name, Analyse analyse) {
		return new Analyse() {

			@Override
			public Collection<Dependency> execute(DependencyAnalyser analyser, Class<?> clazz) {
				return analyse.execute(analyser, clazz);
			}

			@Override
			public String toString() {
				return name;
			}
		};
	}

	static String toClassPath(Class<?> clazz) {
		return "/" + clazz.getName().replace('.', '/') + ".class";
	}
}
