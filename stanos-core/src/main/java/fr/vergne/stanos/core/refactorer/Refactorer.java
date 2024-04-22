package fr.vergne.stanos.core.refactorer;

import static fr.vergne.stanos.core.refactorer.JavaParserUtils.searchField;
import static fr.vergne.stanos.core.refactorer.JavaParserUtils.searchInterface;
import static fr.vergne.stanos.core.refactorer.JavaParserUtils.searchRecord;
import static fr.vergne.stanos.core.refactorer.JavaParserUtils.tokenRangeToCodeRange;
import static java.util.Collections.emptyList;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaToken;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;

import fr.vergne.stanos.core.refactorer.JavaParserUtils.SearchContext;

public interface Refactorer {

	static Refactorer.ForCode forCode(String code) {
		return new Refactorer.ForCode() {
			// TODO Expose language version
			CompilationUnit compilationUnit = parse(LanguageLevel.JAVA_17, code);

			private final StringBuilder refactoringCode = new StringBuilder(code);

			@Override
			public String code() {
				return refactoringCode.toString();
			}

			@Override
			public Y.Class locateClass(String classPath) {
				Code.Source source = JavaParserUtils.parse(code, refactoringCode, compilationUnit);
				if (classPath.equals("MyClass")) {
					try {
						return source.defaultPackage().clazz("MyClass");
					} catch (Exception cause) {
						throw new NoSuchElementException("No class " + classPath, cause);
					}
				} else if (classPath.equals("X")) {
					try {
						return source.defaultPackage().clazz("X");
					} catch (Exception cause) {
						throw new NoSuchElementException("No class " + classPath, cause);
					}
				} else if (classPath.equals("Foo")) {
					try {
						return source.defaultPackage().clazz("Foo");
					} catch (Exception cause) {
						throw new NoSuchElementException("No class " + classPath, cause);
					}
				} else {
					throw new UnsupportedOperationException("Not implemented: " + classPath);
				}
			}

			@Override
			public Refactorer.ForInterface locateInterface(String interfacePath) {
				// FIXME Replace by Code.Source
				SearchContext locatedInterface = searchInterface(compilationUnit, interfacePath);
				return new Refactorer.ForInterface() {
					@Override
					public void rename(String newName) {
						applyRenaming(code, locatedInterface, newName);
					}
				};
			}

			@Override
			public Refactorer.ForRecord locateRecord(String recordPath) {
				// FIXME Replace by Code.Source
				SearchContext locatedRecord = searchRecord(compilationUnit, recordPath);
				return new Refactorer.ForRecord() {
					@Override
					public void rename(String newName) {
						applyRenaming(code, locatedRecord, newName);
					}
				};
			}

			@Override
			public ForField locateField(String fieldPath) {
				// FIXME Replace by Code.Source
				SearchContext locatedField = searchField(compilationUnit, fieldPath);
				return new Refactorer.ForField() {
					@Override
					public void rename(String newName) {
						applyRenaming(code, locatedField, newName);
					}
				};
			}

			@Override
			public Y.Method locateMethod(String methodPath) {
				Code.Source source = JavaParserUtils.parse(code, refactoringCode, compilationUnit);
				if (methodPath.equals("MyClass.myMethod()")) {
					try {
						return source.defaultPackage().clazz("MyClass").method("myMethod", emptyList());
					} catch (Exception cause) {
						throw new NoSuchElementException("No method " + methodPath, cause);
					}
				} else if (methodPath.equals("MyClass.x()")) {
					try {
						return source.defaultPackage().clazz("MyClass").method("x", emptyList());
					} catch (Exception cause) {
						throw new NoSuchElementException("No method " + methodPath, cause);
					}
				} else {
					throw new UnsupportedOperationException("Not implemented: " + methodPath);
				}
			}

			@Override
			public Y.Variable locateVariable(String variablePath) {
				Code.Source source = JavaParserUtils.parse(code, refactoringCode, compilationUnit);
				if (variablePath.equals("MyClass.myMethod(boolean).myVar%0")) {
					try {
						return source.defaultPackage().clazz("MyClass").method("myMethod", List.of("boolean")).variable("myVar", 0);
					} catch (Exception cause) {
						throw new NoSuchElementException("No variable " + variablePath, cause);
					}
				} else if (variablePath.equals("MyClass.myMethod(boolean).myVar%1")) {
					try {
						return source.defaultPackage().clazz("MyClass").method("myMethod", List.of("boolean")).variable("myVar", 1);
					} catch (Exception cause) {
						throw new NoSuchElementException("No variable " + variablePath, cause);
					}
				} else if (variablePath.equals("MyClass.myMethod().myVar%")) {
					try {
						return source.defaultPackage().clazz("MyClass").method("myMethod", emptyList()).variable("myVar", 0);
					} catch (Exception cause) {
						throw new NoSuchElementException("No variable " + variablePath, cause);
					}
				} else if (variablePath.equals("MyClass.MyChildClass.myMethod().MyInnerClass.myMethod().myVar%")) {
					try {
						return source.defaultPackage().clazz("MyClass").clazz("MyChildClass").method("myMethod", emptyList()).clazz("MyInnerClass").method("myMethod", emptyList()).variable("myVar", 0);
					} catch (Exception cause) {
						throw new NoSuchElementException("No variable " + variablePath, cause);
					}
				} else if (variablePath.equals("MyClass.myMethod(boolean).myVar%")) {
					try {
						return source.defaultPackage().clazz("MyClass").method("myMethod", List.of("boolean")).variable("myVar", 0);
					} catch (Exception cause) {
						throw new NoSuchElementException("No variable " + variablePath, cause);
					}
				} else if (variablePath.equals("MyClass.myMethod().myVar%.myMethod().myVar%")) {
					try {
						return source.defaultPackage().clazz("MyClass").method("myMethod", emptyList()).variable("myVar", 0).method("myMethod", emptyList()).variable("myVar", 0);
					} catch (Exception cause) {
						throw new NoSuchElementException("No variable " + variablePath, cause);
					}
				} else if (variablePath.equals("MyClass.myMethod().x%")) {
					try {
						return source.defaultPackage().clazz("MyClass").method("myMethod", emptyList()).variable("x", 0);
					} catch (Exception cause) {
						throw new NoSuchElementException("No variable " + variablePath, cause);
					}
				} else {
					throw new UnsupportedOperationException("Not implemented yet: " + variablePath);
				}
			}

			private void applyRenaming(String code, SearchContext context, String newName) {
				context.nameTokens.stream()//
						.map(JavaToken::getRange)//
						.map(Optional<Range>::orElseThrow)//
						.map(range -> tokenRangeToCodeRange(code, range))//
						// Process from last to first, so the ranges are not shifted
						.sorted(Comparator.comparing(CodeRange::start).reversed())//
						.collect(() -> refactoringCode, (builder, nameRange) -> {
							builder.replace(nameRange.start(), nameRange.end() + 1, newName);
						}, (b1, b2) -> {
							throw new UnsupportedOperationException("Combiner not supported");
						});
			}
		};
	}

	private static CompilationUnit parse(LanguageLevel languageLevel, String code) {
		JavaParser parser = new JavaParser();
		parser.getParserConfiguration().setLanguageLevel(languageLevel);
		ParseResult<CompilationUnit> parseResult = parser.parse(code);
		if (!parseResult.isSuccessful()) {
			// TODO Test this part
			var exception = new IllegalArgumentException("Invalid code");
			parseResult.getProblems().forEach(problem -> {
				exception.addSuppressed(problem.getCause().orElseThrow());
			});
			throw exception;
		}
		return parseResult.getResult().orElseThrow();
	}

	interface ForCode extends Refactorer {
		String code();

		Y.Class locateClass(String classPath);

		Refactorer.ForInterface locateInterface(String interfacePath);

		Refactorer.ForRecord locateRecord(String recordPath);

		Refactorer.ForField locateField(String fieldPath);

		Y.Method locateMethod(String methodPath);

		Y.Variable locateVariable(String variablePath);
	}

	interface ForClass extends Refactorer, Renamable {
	}

	interface ForInterface extends Refactorer, Renamable {
	}

	interface ForRecord extends Refactorer, Renamable {
	}

	interface ForField extends Refactorer, Renamable {
	}

	interface ForMethod extends Refactorer, Renamable {
	}

	interface Renamable {
		void rename(String newName);
	}

	record CodeRange(int start, int end) {
	}
}
