package fr.vergne.stanos.core.refactorer;

import java.util.LinkedList;
import java.util.List;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node.TreeTraversal;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

public interface Refactorer {

	static Refactorer.ForCode forCode(String code) {
		// TODO Expose language version
		CompilationUnit compilationUnit = parseWithJavaParser(code, LanguageLevel.JAVA_17);
		Code.Source source = abstractFromJavaParser(compilationUnit);
		return new Refactorer.ForCode() {

			@Override
			public String code() {
				LexicalPreservingPrinter.setup(compilationUnit);
				return LexicalPreservingPrinter.print(compilationUnit);
			}

			@Override
			public Code.Source source() {
				return source;
			}
		};
	}

	static Code.Source abstractFromJavaParser(CompilationUnit compilationUnit) {
		List<Y.Class> defaultPackageClasses = new LinkedList<>();
		List<Y.Interface> defaultPackageInterfaces = new LinkedList<>();
		List<Y.Record> defaultPackageRecords = new LinkedList<>();
		Y.Package defaultPackage = DefaultSource.createDefaultPackage(defaultPackageClasses, defaultPackageInterfaces, defaultPackageRecords);
		Code.Source source = new DefaultSource(defaultPackage);
		Scope root = Scope.root(defaultPackageClasses::add, defaultPackageInterfaces::add, defaultPackageRecords::add);
		X x = new X(new Scope.Context(root), source);
		compilationUnit.accept(new Visitor(), x);
		return source;
	}

	static CompilationUnit parseWithJavaParser(String code, LanguageLevel languageLevel) {
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
		System.out.println(":::::::::::::::");
		compilationUnit.stream(TreeTraversal.DIRECT_CHILDREN).forEach(node -> {
			System.out.println(node.getClass().getSimpleName());
			node.stream(TreeTraversal.DIRECT_CHILDREN).forEach(node2 -> {
				System.out.println("  " + node2.getClass().getSimpleName());
				node2.stream(TreeTraversal.DIRECT_CHILDREN).forEach(node3 -> {
					System.out.println("    " + node3.getClass().getSimpleName());
				});
			});
		});
		System.out.println(":::::::::::::::");
		return compilationUnit;
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

	record CodeRange(int start) {
	}
}
