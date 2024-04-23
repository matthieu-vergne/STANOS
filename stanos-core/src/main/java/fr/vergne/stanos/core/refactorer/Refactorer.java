package fr.vergne.stanos.core.refactorer;

import java.util.LinkedList;
import java.util.List;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;

import fr.vergne.stanos.core.refactorer.Code.Source;

public interface Refactorer {

	static Refactorer.ForCode forCode(String code) {
		return new Refactorer.ForCode() {
			// TODO Expose language version
			private final StringBuilder refactoringCode = new StringBuilder(code);
			private final Code.Source source = parse(code, refactoringCode, LanguageLevel.JAVA_17);

			@Override
			public String code() {
				return refactoringCode.toString();
			}

			@Override
			public Code.Source source() {
				return source;
			}
		};
	}

	private static Source parse(String code, StringBuilder refactoringCode, LanguageLevel languageLevel) {
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
		CompilationUnit compilationUnit = parseResult.getResult().orElseThrow();

		List<Y.Class> defaultPackageClasses = new LinkedList<>();
		List<Y.Interface> defaultPackageInterfaces = new LinkedList<>();
		List<Y.Record> defaultPackageRecords = new LinkedList<>();
		Y.Package defaultPackage = DefaultSource.createDefaultPackage(defaultPackageClasses, defaultPackageInterfaces, defaultPackageRecords);
		Code.Source source = new DefaultSource(defaultPackage);
		Scope root = Scope.root(defaultPackageClasses::add, defaultPackageInterfaces::add, defaultPackageRecords::add);
		X x = new X(new Scope.Context(root), source);
		compilationUnit.accept(new Visitor(code, refactoringCode), x);

		return source;
	}

	interface ForCode extends Refactorer {
		String code();

		Code.Source source();
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
