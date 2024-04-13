package fr.vergne.stanos.core.refactorer;

import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaToken;
import com.github.javaparser.JavaToken.Category;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;

public interface Refactorer {

	static Refactorer.ForCode forCode(String code) {
		JavaParser parser = new JavaParser();
		parser.getParserConfiguration().setLanguageLevel(LanguageLevel.JAVA_17);
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
		return new Refactorer.ForCode() {
			private String refactoredCode = code;

			@Override
			public String code() {
				return refactoredCode;
			}

			@Override
			public ForClass locateClass(String classPath) {
				ClassOrInterfaceDeclaration locatedClass = searchClass(compilationUnit, classPath);
				return new Refactorer.ForClass() {
					@Override
					public void rename(String newName) {
						JavaToken nameToken = stream(tokensOf(locatedClass))//
								.filter(is(Category.IDENTIFIER))//
								.findFirst().orElseThrow();
						CodeRange nameRange = CodeRange.fromTokenRange(nameToken.getRange().orElseThrow());

						String before = refactoredCode.substring(0, nameRange.start() - 1);
						String after = refactoredCode.substring(nameRange.end());
						refactoredCode = before + newName + after;
					}
				};
			}

			@Override
			public ForField locateField(String fieldPath) {
				FieldDeclaration locatedField = searchField(compilationUnit, fieldPath);
				return new Refactorer.ForField() {
					@Override
					public void rename(String newName) {
						JavaToken nameToken = stream(tokensOf(locatedField))//
								.filter(is(Category.IDENTIFIER))//
								.skip(1)// Skip type
								.findFirst().orElseThrow();
						CodeRange nameRange = CodeRange.fromTokenRange(nameToken.getRange().orElseThrow());

						String before = refactoredCode.substring(0, nameRange.start() - 1);
						String after = refactoredCode.substring(nameRange.end());
						refactoredCode = before + newName + after;
					}
				};
			}

			@Override
			public ForMethod locateMethod(String methodPath) {
				MethodDeclaration locatedMethod = searchMethod(compilationUnit, methodPath);
				return new Refactorer.ForMethod() {
					@Override
					public void rename(String newName) {
						JavaToken nameToken = stream(tokensOf(locatedMethod))//
								.filter(is(Category.IDENTIFIER))//
								.findFirst().orElseThrow();
						CodeRange nameRange = CodeRange.fromTokenRange(nameToken.getRange().orElseThrow());

						String before = refactoredCode.substring(0, nameRange.start() - 1);
						String after = refactoredCode.substring(nameRange.end());
						refactoredCode = before + newName + after;
					}
				};
			}

			@Override
			public ForVariable locateVariable(String variablePath) {
				VariableDeclarationExpr locatedVariable = searchVariable(compilationUnit, variablePath);
				return new Refactorer.ForVariable() {
					@Override
					public void rename(String newName) {
						JavaToken nameToken = stream(tokensOf(locatedVariable))//
								.filter(is(Category.IDENTIFIER))//
								.skip(1)// Skip type
								.findFirst().orElseThrow();
						CodeRange nameRange = CodeRange.fromTokenRange(nameToken.getRange().orElseThrow());

						String before = refactoredCode.substring(0, nameRange.start() - 1);
						String after = refactoredCode.substring(nameRange.end());
						refactoredCode = before + newName + after;
					}
				};
			}
		};
	}

	interface ForCode extends Refactorer {
		String code();

		Refactorer.ForClass locateClass(String classPath);

		Refactorer.ForField locateField(String fieldPath);

		Refactorer.ForMethod locateMethod(String methodPath);

		Refactorer.ForVariable locateVariable(String variablePath);
	}

	interface ForClass extends Refactorer, Renamable {
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

	private static ClassOrInterfaceDeclaration searchClass(CompilationUnit compilationUnit, String classPath) {
		ClassOrInterfaceDeclaration declaration = compilationUnit.accept(new GenericVisitorAdapter<ClassOrInterfaceDeclaration, Void>() {
			@Override
			public ClassOrInterfaceDeclaration visit(ClassOrInterfaceDeclaration decl, Void arg) {
				if (classPath.equals(decl.getFullyQualifiedName().orElseThrow())) {
					TokenRange tokenRange = decl.getTokenRange().orElseThrow();
					boolean isClass = stream(tokenRange)//
							.filter(is(Category.KEYWORD).and(textEquals("class")))//
							.findFirst().isPresent();
					if (isClass) {
						return decl;
					} else {
						return super.visit(decl, arg);
					}
				} else {
					return super.visit(decl, arg);
				}
			}
		}, null);

		if (declaration == null) {
			throw new NoSuchElementException("No class " + classPath);
		} else {
			return declaration;
		}
	}

	private static FieldDeclaration searchField(CompilationUnit compilationUnit, String fieldPath) {
		int sep = fieldPath.lastIndexOf(".");
		String classPath = fieldPath.substring(0, sep);
		String fieldName = fieldPath.substring(sep + 1);
		FieldDeclaration declaration = compilationUnit.accept(new GenericVisitorAdapter<FieldDeclaration, Void>() {
			@Override
			public FieldDeclaration visit(ClassOrInterfaceDeclaration decl, Void arg) {
				if (classPath.equals(decl.getFullyQualifiedName().orElseThrow())) {
					return super.visit(decl, arg);
				} else {
					return null;
				}
			}

			@Override
			public FieldDeclaration visit(FieldDeclaration decl, Void arg) {
				// TODO Manage more than one variable
				if (fieldName.equals(decl.getVariable(0).getNameAsString())) {
					return decl;
				}
				return super.visit(decl, arg);
			}
		}, null);

		if (declaration == null) {
			throw new NoSuchElementException("No field " + fieldPath);
		} else {
			return declaration;
		}
	}

	private static MethodDeclaration searchMethod(CompilationUnit compilationUnit, String methodPath) {
		int sep = methodPath.lastIndexOf(".");
		String classPath = methodPath.substring(0, sep);
		String methodName = methodPath.substring(sep + 1);
		MethodDeclaration declaration = compilationUnit.accept(new GenericVisitorAdapter<MethodDeclaration, Void>() {
			@Override
			public MethodDeclaration visit(ClassOrInterfaceDeclaration decl, Void arg) {
				if (classPath.equals(decl.getFullyQualifiedName().orElseThrow())) {
					return super.visit(decl, arg);
				} else {
					return null;
				}
			}

			@Override
			public MethodDeclaration visit(MethodDeclaration decl, Void arg) {
				if (methodName.equals(decl.getNameAsString())) {
					return decl;
				} else {
					return super.visit(decl, arg);
				}
			}
		}, null);

		if (declaration == null) {
			throw new NoSuchElementException("No method " + methodPath);
		} else {
			return declaration;
		}
	}

	private static VariableDeclarationExpr searchVariable(CompilationUnit compilationUnit, String variablePath) {
		int sep = variablePath.lastIndexOf(".");
		String methodPath = variablePath.substring(0, sep);
		String variableName = variablePath.substring(sep + 1);

		int sep2 = methodPath.lastIndexOf(".");
		String classPath = methodPath.substring(0, sep2);
		String methodName = methodPath.substring(sep2 + 1);

		VariableDeclarationExpr declaration = compilationUnit.accept(new GenericVisitorAdapter<VariableDeclarationExpr, Void>() {
			@Override
			public VariableDeclarationExpr visit(ClassOrInterfaceDeclaration decl, Void arg) {
				if (classPath.equals(decl.getFullyQualifiedName().orElseThrow())) {
					return super.visit(decl, arg);
				} else {
					return null;
				}
			}

			@Override
			public VariableDeclarationExpr visit(MethodDeclaration decl, Void arg) {
				if (methodName.equals(decl.getNameAsString())) {
					return super.visit(decl, arg);
				} else {
					return null;
				}
			}

			@Override
			public VariableDeclarationExpr visit(VariableDeclarationExpr decl, Void arg) {
				// TODO Manage more than one variable
				if (variableName.equals(decl.getVariable(0).getNameAsString())) {
					return decl;
				} else {
					return super.visit(decl, arg);
				}
			}
		}, null);

		if (declaration == null) {
			throw new NoSuchElementException("No variable " + variablePath);
		} else {
			return declaration;
		}
	}

	private static Predicate<JavaToken> is(Category category) {
		return token -> token.getCategory().equals(category);
	}

	private static Predicate<JavaToken> textEquals(String text) {
		return token -> token.getText().equals(text);
	}

	private static Stream<JavaToken> stream(TokenRange tokenRange) {
		return StreamSupport.stream(tokenRange.spliterator(), false);
	}

	private static TokenRange tokensOf(Node locatedClass) {
		return locatedClass.getTokenRange().orElseThrow();
	}

	record CodeRange(int start, int end) {
		public static CodeRange fromTokenRange(Range nameRange) {
			Position nameBegin = nameRange.begin;
			Position nameEnd = nameRange.end;
			// TODO Consider lines
			int start = nameBegin.column;
			int end = nameEnd.column;
			return new CodeRange(start, end);
		}
	}
}
