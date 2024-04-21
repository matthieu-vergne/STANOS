package fr.vergne.stanos.core.refactorer;

import static fr.vergne.stanos.core.refactorer.JavaParserUtils.searchClass;
import static fr.vergne.stanos.core.refactorer.JavaParserUtils.searchField;
import static fr.vergne.stanos.core.refactorer.JavaParserUtils.searchInterface;
import static fr.vergne.stanos.core.refactorer.JavaParserUtils.searchMethod;
import static fr.vergne.stanos.core.refactorer.JavaParserUtils.searchRecord;
import static fr.vergne.stanos.core.refactorer.JavaParserUtils.searchVariable;
import static fr.vergne.stanos.core.refactorer.JavaParserUtils.tokenRangeToCodeRange;

import java.util.Comparator;
import java.util.Optional;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaToken;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;

import fr.vergne.stanos.core.refactorer.JavaParserUtils.SearchContext;
import fr.vergne.stanos.core.refactorer.Y.Variable;

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
			public ForClass locateClass(String classPath) {
				SearchContext locatedClass = searchClass(compilationUnit, classPath);
				return new Refactorer.ForClass() {
					@Override
					public void rename(String newName) {
						applyRenaming(code, locatedClass, newName);
					}
				};
			}

			@Override
			public Refactorer.ForInterface locateInterface(String interfacePath) {
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
				SearchContext locatedField = searchField(compilationUnit, fieldPath);
				return new Refactorer.ForField() {
					@Override
					public void rename(String newName) {
						applyRenaming(code, locatedField, newName);
					}
				};
			}

			@Override
			public ForMethod locateMethod(String methodPath) {
				SearchContext locatedMethod = searchMethod(compilationUnit, methodPath);
				return new Refactorer.ForMethod() {
					@Override
					public void rename(String newName) {
						applyRenaming(code, locatedMethod, newName);
					}
				};
			}

			@Override
			public ForVariable locateVariable(String variablePath) {
				Optional<Y.Variable> variableOpt = searchVariable(code, variablePath, refactoringCode, compilationUnit);
				return new Refactorer.ForVariable() {
					@Override
					public void rename(String newName) {
						variableOpt.get().rename(newName);
					}

				};
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

		Refactorer.ForClass locateClass(String classPath);

		Refactorer.ForInterface locateInterface(String interfacePath);

		Refactorer.ForRecord locateRecord(String recordPath);

		Refactorer.ForField locateField(String fieldPath);

		Refactorer.ForMethod locateMethod(String methodPath);

		Refactorer.ForVariable locateVariable(String variablePath);
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

	interface ForVariable extends Refactorer, Renamable {
	}

	interface Renamable {
		void rename(String newName);
	}

	record CodeRange(int start, int end) {
	}
}
