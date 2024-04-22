package fr.vergne.stanos.core.refactorer;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.github.javaparser.JavaToken;
import com.github.javaparser.JavaToken.Category;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import fr.vergne.stanos.core.refactorer.Refactorer.CodeRange;

class JavaParserUtils {

	public static CodeRange tokenRangeToCodeRange(String code, Range range) {
		int start = positionToIndex(code, range.begin);
		int end = positionToIndex(code, range.end);
		return new CodeRange(start, end);
	}

	private static int positionToIndex(String code, Position position) {
		int startOfLine = 0;
		for (int i = 1; i < position.line; i++) {
			startOfLine = code.indexOf("\n", startOfLine) + 1;
		}
		return startOfLine + position.column - 1;
	}

	public static Predicate<JavaToken> is(Category category) {
		return token -> token.getCategory().equals(category);
	}

	public static Predicate<JavaToken> textEquals(String text) {
		return token -> token.getText().equals(text);
	}

	public static Stream<JavaToken> stream(TokenRange tokenRange) {
		return StreamSupport.stream(tokenRange.spliterator(), false);
	}

	public static TokenRange tokensOf(Node locatedClass) {
		return locatedClass.getTokenRange().orElseThrow();
	}

	public static class SearchContext {
		Collection<JavaToken> nameTokens = new LinkedList<>();
	}

	public static SearchContext searchClass(CompilationUnit compilationUnit, String classPath) {
		SearchContext context = new SearchContext();
		compilationUnit.accept(new VoidVisitorAdapter<SearchContext>() {
			@Override
			public void visit(ClassOrInterfaceDeclaration decl, SearchContext ctx) {
				if (classPath.equals(decl.getFullyQualifiedName().orElseThrow())) {
					TokenRange tokenRange = decl.getTokenRange().orElseThrow();
					boolean isClass = stream(tokenRange)//
							.filter(is(Category.KEYWORD).and(textEquals("class")))//
							.findFirst().isPresent();
					if (isClass) {
						JavaToken nameToken = stream(tokensOf(decl))//
								.filter(is(Category.IDENTIFIER))//
								.findFirst().orElseThrow();
						ctx.nameTokens.add(nameToken);
					} else {
						super.visit(decl, ctx);
					}
				} else {
					super.visit(decl, ctx);
				}
			}
		}, context);

		if (context.nameTokens.isEmpty()) {
			throw new NoSuchElementException("No class " + classPath);
		} else {
			return context;
		}
	}

	public static SearchContext searchInterface(CompilationUnit compilationUnit, String interfacePath) {
		SearchContext context = new SearchContext();
		compilationUnit.accept(new GenericVisitorAdapter<ClassOrInterfaceDeclaration, SearchContext>() {
			@Override
			public ClassOrInterfaceDeclaration visit(ClassOrInterfaceDeclaration decl, SearchContext ctx) {
				if (interfacePath.equals(decl.getFullyQualifiedName().orElseThrow())) {
					TokenRange tokenRange = decl.getTokenRange().orElseThrow();
					boolean isInterface = stream(tokenRange)//
							.filter(is(Category.KEYWORD).and(textEquals("interface")))//
							.findFirst().isPresent();
					if (isInterface) {
						JavaToken nameToken = stream(tokensOf(decl))//
								.filter(is(Category.IDENTIFIER))//
								.findFirst().orElseThrow();
						ctx.nameTokens.add(nameToken);
						return decl;
					} else {
						return super.visit(decl, ctx);
					}
				} else {
					return super.visit(decl, ctx);
				}
			}
		}, context);

		if (context.nameTokens.isEmpty()) {
			throw new NoSuchElementException("No interface " + interfacePath);
		} else {
			return context;
		}
	}

	public static SearchContext searchRecord(CompilationUnit compilationUnit, String recordPath) {
		SearchContext context = new SearchContext();
		compilationUnit.accept(new VoidVisitorAdapter<SearchContext>() {
			@Override
			public void visit(RecordDeclaration decl, SearchContext ctx) {
				if (recordPath.equals(decl.getFullyQualifiedName().orElseThrow())) {
					JavaToken nameToken = stream(tokensOf(decl))//
							.filter(is(Category.IDENTIFIER))//
							.findFirst().orElseThrow();
					ctx.nameTokens.add(nameToken);
				} else {
					super.visit(decl, ctx);
				}
			}
		}, context);

		if (context.nameTokens.isEmpty()) {
			throw new NoSuchElementException("No record " + recordPath);
		} else {
			return context;
		}
	}

	public static SearchContext searchField(CompilationUnit compilationUnit, String fieldPath) {
		int sep = fieldPath.lastIndexOf(".");
		String classPath = fieldPath.substring(0, sep);
		String fieldName = fieldPath.substring(sep + 1);

		SearchContext context = new SearchContext();
		compilationUnit.accept(new VoidVisitorAdapter<SearchContext>() {
			@Override
			public void visit(ClassOrInterfaceDeclaration decl, SearchContext ctx) {
				if (classPath.equals(decl.getFullyQualifiedName().orElseThrow())) {
					super.visit(decl, ctx);
				}
			}

			@Override
			public void visit(FieldDeclaration decl, SearchContext ctx) {
				// TODO Manage more than one variable
				if (fieldName.equals(decl.getVariable(0).getNameAsString())) {
					JavaToken nameToken = stream(tokensOf(decl))//
							.filter(is(Category.IDENTIFIER))//
							.skip(1)// Skip type
							.findFirst().orElseThrow();
					ctx.nameTokens.add(nameToken);
				}
				super.visit(decl, ctx);
			}
		}, context);

		if (context.nameTokens.isEmpty()) {
			throw new NoSuchElementException("No field " + fieldPath);
		} else {
			return context;
		}
	}

	public static SearchContext searchMethod(CompilationUnit compilationUnit, String methodPath) {
		int sep = methodPath.lastIndexOf(".");
		String classPath = methodPath.substring(0, sep);
		String methodName = methodPath.substring(sep + 1);

		SearchContext context = new SearchContext();
		compilationUnit.accept(new VoidVisitorAdapter<SearchContext>() {
			@Override
			public void visit(ClassOrInterfaceDeclaration decl, SearchContext ctx) {
				if (classPath.equals(decl.getFullyQualifiedName().orElseThrow())) {
					super.visit(decl, ctx);
				}
			}

			@Override
			public void visit(MethodDeclaration decl, SearchContext ctx) {
				if (methodName.equals(decl.getNameAsString())) {
					JavaToken nameToken = stream(tokensOf(decl))//
							.filter(is(Category.IDENTIFIER))//
							.findFirst().orElseThrow();
					ctx.nameTokens.add(nameToken);
				} else {
					super.visit(decl, ctx);
				}
			}
		}, context);

		if (context.nameTokens.isEmpty()) {
			throw new NoSuchElementException("No method " + methodPath);
		} else {
			return context;
		}
	}

	record VarContext(Code.VariableDeclaration declaration, SearchContext context) {
	}

	public static Code.Source parse(String code, StringBuilder refactoringCode, CompilationUnit compilationUnit) {
		List<Y.Class> defaultPackageClasses = new LinkedList<>();
		List<Y.Interface> defaultPackageInterfaces = new LinkedList<>();
		List<Y.Record> defaultPackageRecords = new LinkedList<>();
		Y.Package defaultPackage = DefaultSource.createDefaultPackage(defaultPackageClasses, defaultPackageInterfaces, defaultPackageRecords);
		Code.Source source = new DefaultSource(defaultPackage);
		X x = new X(new Scope.Context(Scope.root(defaultPackageClasses::add, defaultPackageInterfaces::add, defaultPackageRecords::add)), source, null);
		compilationUnit.accept(new Visitor(code, refactoringCode), x);
		return source;
	}
}
