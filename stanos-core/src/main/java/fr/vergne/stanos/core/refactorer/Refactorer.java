package fr.vergne.stanos.core.refactorer;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.Node.TreeTraversal;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;

import fr.vergne.stanos.core.refactorer.Y.Class;
import fr.vergne.stanos.core.refactorer.Y.Field;
import fr.vergne.stanos.core.refactorer.Y.Interface;
import fr.vergne.stanos.core.refactorer.Y.Method;
import fr.vergne.stanos.core.refactorer.Y.Package;
import fr.vergne.stanos.core.refactorer.Y.Record;

public interface Refactorer {

	static Refactorer.ForCode forCode(String code) {
		// TODO Expose language version
		CompilationUnit compilationUnit = parseWithJavaParser(code, LanguageLevel.JAVA_17);
		LexicalPreservingPrinter.setup(compilationUnit);
		Code.Source source = abstractFromJavaParser(compilationUnit);
		return new Refactorer.ForCode() {

			@Override
			public String code() {
				return LexicalPreservingPrinter.print(compilationUnit);
			}

			@Override
			public Code.Source source() {
				return source;
			}
		};
	}

	static Code.Source abstractFromJavaParser(CompilationUnit compilationUnit) {
		return new Code.Source() {
			@Override
			public Package defaultPackage() {
				return createDefaultPackage(compilationUnit);
			}
		};
	}

	static CompilationUnit parseWithJavaParser(String code, LanguageLevel languageLevel) {
		ParserConfiguration parserConf = new ParserConfiguration();
		parserConf.setLanguageLevel(languageLevel);
		parserConf.setSymbolResolver(new JavaSymbolSolver(new CombinedTypeSolver()));
		JavaParser parser = new JavaParser(parserConf);
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

	private static Y.Variable createVariable(MethodDeclaration methodDeclaration, VariableDeclarator variableDeclaration) {
		return new Y.Variable() {
			@Override
			public String name() {
				return variableDeclaration.getNameAsString();
			}

			@Override
			public void rename(String newName) {
				String currentName = variableDeclaration.getNameAsString();
				methodDeclaration.getBody().orElseThrow().stream()//
						.flatMap(filterOnClass(com.github.javaparser.ast.expr.SimpleName.class))//
						.filter(nameNode -> nameNode.getIdentifier().equals(currentName))//
						.forEach(nameNode -> {
							Node parentNode = nameNode.getParentNode().orElseThrow();
							if (parentNode instanceof NameExpr exp) {
								ResolvedValueDeclaration resolved = exp.resolve();
								if (resolved.isVariable()) {
									VariableDeclarationExpr declarations = resolved.toAst(com.github.javaparser.ast.expr.VariableDeclarationExpr.class).orElseThrow();
									if (declarations.getVariables().contains(variableDeclaration)) {
										nameNode.setIdentifier(newName);
									} else {
										// Relate to another parameter with the same name
									}
								} else {
									// Relate to something else with the same name
								}
							} else {
								// Relate to something else with the same name
							}
						});
				variableDeclaration.getName().setIdentifier(newName);
			}

			@Override
			public Stream<Y.Method> methods() {
				return variableDeclaration.getInitializer().map(expr -> {
					if (expr instanceof ObjectCreationExpr ocExpr) {
						return ocExpr.getAnonymousClassBody().map(body -> {
							if (body instanceof NodeList<?> list) {
								return list.stream().map(node -> {
									if (node instanceof MethodDeclaration decl) {
										return createMethod(decl);
									} else {
										throw new UnsupportedOperationException("Not supported: " + node.getClass().getSimpleName());
									}
								});
							} else {
								throw new UnsupportedOperationException("Not supported: " + body.getClass().getSimpleName());
							}
						}).orElse(Stream.empty());
					} else {
						throw new UnsupportedOperationException("Not supported: " + expr.getClass().getSimpleName());
					}
				}).orElse(Stream.empty()).peek(method -> System.out.println("M= " + method.name()));
			}
		};
	}

	private static Y.Parameter createParameter(com.github.javaparser.ast.body.MethodDeclaration methodDeclaration, com.github.javaparser.ast.body.Parameter parameterDeclaration) {
		return new Y.Parameter() {
			@Override
			public String name() {
				return parameterDeclaration.getNameAsString();
			}

			@Override
			public void rename(String newName) {
				String currentName = parameterDeclaration.getNameAsString();
				methodDeclaration.getBody().ifPresent(body -> {
					body.stream()//
							.flatMap(filterOnClass(com.github.javaparser.ast.expr.SimpleName.class))//
							.filter(nameNode -> !(nameNode.getParentNode().orElseThrow().getParentNode().orElseThrow() instanceof ObjectCreationExpr))//
							.filter(nameNode -> nameNode.getIdentifier().equals(currentName))//
							.forEach(nameNode -> {
								Node parentNode = nameNode.getParentNode().orElseThrow();
								if (parentNode instanceof NameExpr exp) {
									com.github.javaparser.ast.body.Parameter declaration = exp.resolve().asParameter().toAst(com.github.javaparser.ast.body.Parameter.class).orElseThrow();
									if (declaration.equals(parameterDeclaration)) {
										nameNode.setIdentifier(newName);
									} else {
										// Relate to another parameter with the same name
									}
								} else {
									// Relate to something else with the same name
								}
							});
				});
				parameterDeclaration.getName().setIdentifier(newName);
			}
		};
	}

	private static Method createMethod(MethodDeclaration methodDeclaration) {
		return new Y.Method() {
			@Override
			public String name() {
				return methodDeclaration.getNameAsString();
			}

			@Override
			public List<String> parameterTypes() {
				return methodDeclaration.getParameters().stream().map(x -> x.getTypeAsString()).toList();
			}

			@Override
			public void rename(String newName) {
				methodDeclaration.setName(newName);
			}

			@Override
			public Stream<Y.Parameter> parameters() {
				return methodDeclaration.stream(TreeTraversal.DIRECT_CHILDREN)//
						.flatMap(filterOnClass(com.github.javaparser.ast.body.Parameter.class))//
						.map(parameterDeclaration -> createParameter(methodDeclaration, parameterDeclaration));
			}

			@Override
			public Stream<Y.Variable> variables() {
				return methodDeclaration.getBody().orElseThrow().stream(TreeTraversal.DIRECT_CHILDREN)//
						.flatMap(node -> {
							if (node instanceof Statement stmt) {
								return resolveStatementToExpressions(stmt);
							} else {
								throw new UnsupportedOperationException("Not supported: " + node.getClass().getSimpleName());
							}
						})//
						.flatMap(node -> {
							if (node instanceof VariableDeclarationExpr exp) {
								return exp.getVariables().stream();
							} else if (node instanceof NameExpr exp) {
								return Stream.empty();
							} else {
								throw new UnsupportedOperationException("Not supported: " + node.getClass().getSimpleName());
							}
						})//
						.flatMap(filterOnClass(com.github.javaparser.ast.body.VariableDeclarator.class))//
						.map(variableDeclaration -> {
							return createVariable(methodDeclaration, variableDeclaration);
						});
			}

			private static Stream<? extends Expression> resolveStatementToExpressions(Statement statement) {
				if (statement instanceof ExpressionStmt stmt) {
					return Stream.of(stmt.getExpression());
				} else if (statement instanceof BlockStmt stmt) {
					return stmt.getStatements().stream().flatMap(stmt2 -> resolveStatementToExpressions(stmt2));
				} else if (statement instanceof ReturnStmt stmt) {
					return Stream.of(stmt.getExpression().orElseThrow());
				} else if (statement instanceof IfStmt stmt) {
					return Stream.concat(//
							resolveStatementToExpressions(stmt.getThenStmt()), //
							stmt.getElseStmt().map(elseStmt -> resolveStatementToExpressions(elseStmt)).orElse(Stream.empty())//
					);
				} else if (statement instanceof LocalClassDeclarationStmt stmt) {
					return Stream.empty();
				} else {
					throw new UnsupportedOperationException("Not supported: " + statement.getClass().getSimpleName());
				}
			}

			@Override
			public Stream<Y.Interface> interfaces() {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("Not implemented yet");
			}

			@Override
			public Stream<Y.Class> classes() {
				return methodDeclaration.getBody()//
						.map(body -> body.stream(TreeTraversal.DIRECT_CHILDREN)//
								.flatMap(filterOnClass(LocalClassDeclarationStmt.class))//
								.flatMap(localClassStatement -> {
									return localClassStatement.stream(TreeTraversal.DIRECT_CHILDREN)//
											.flatMap(filterOnClass(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class))//
											.filter(decl -> !decl.isInterface())//
											.map(localClassDeclaration -> {
												return createClass(localClassDeclaration);
											});
								}))//
						.orElseGet(Stream::empty);
			}

		};
	}

	private static Field createField(VariableDeclarator variableDeclarator) {
		return new Y.Field() {
			@Override
			public String name() {
				return variableDeclarator.getNameAsString();
			}

			@Override
			public void rename(String newName) {
				variableDeclarator.setName(newName);
				// TODO Rename uses
			}

			@Override
			public Stream<Method> methods() {
				// TODO
				throw new UnsupportedOperationException("Not implemented yet");
			}
		};
	}

	private static Class createClass(ClassOrInterfaceDeclaration classDeclaration) {
		return new Y.Class() {
			@Override
			public String name() {
				return classDeclaration.getNameAsString();
			}

			@Override
			public void rename(String newName) {
				classDeclaration.setName(newName);
			}

			@Override
			public Stream<Y.Method> methods() {
				return classDeclaration.stream(TreeTraversal.DIRECT_CHILDREN)//
						.flatMap(filterOnClass(com.github.javaparser.ast.body.MethodDeclaration.class))//
						.map(methodDeclaration -> {
							return createMethod(methodDeclaration);
						});
			}

			@Override
			// method
			public Stream<Y.Class> classes() {
				return classDeclaration.stream(TreeTraversal.DIRECT_CHILDREN)//
						.flatMap(filterOnClass(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class))//
						.filter(decl -> !decl.isInterface())//
						.map(innerClassDeclaration -> {
							return createClass(innerClassDeclaration);
						});
			}

			@Override
			public Stream<Y.Interface> interfaces() {
				// TODO
				throw new UnsupportedOperationException("Not implemented yet");
			}

			@Override
			public Stream<Field> fields() {
				return classDeclaration.getFields().stream().flatMap(fieldDeclaration -> {
					return fieldDeclaration.getVariables().stream().map(variableDeclarator -> {
						return createField(variableDeclarator);
					});
				});
			}

		};
	}

	private static Interface createInterface(ClassOrInterfaceDeclaration interfaceDeclaration) {
		return new Y.Interface() {
			@Override
			public String name() {
				return interfaceDeclaration.getFullyQualifiedName().orElseThrow();
			}

			@Override
			public void rename(String newName) {
				interfaceDeclaration.setName(newName);
				// TODO rename uses
			}

			@Override
			public Stream<Y.Method> methods() {
				return interfaceDeclaration.stream(TreeTraversal.DIRECT_CHILDREN)//
						.flatMap(filterOnClass(com.github.javaparser.ast.body.MethodDeclaration.class))//
						.map(methodDeclaration -> {
							return createMethod(methodDeclaration);
						});
			}

			@Override
			public Stream<Y.Class> classes() {
				// TODO
				throw new UnsupportedOperationException("Not implemented yet");
			}

			@Override
			public Stream<Y.Interface> interfaces() {
				// TODO
				throw new UnsupportedOperationException("Not implemented yet");
			}
		};
	}

	private static Record createRecord(com.github.javaparser.ast.body.RecordDeclaration recordDeclaration) {
		return new Y.Record() {
			@Override
			public String name() {
				return recordDeclaration.getFullyQualifiedName().orElseThrow();
			}

			@Override
			public void rename(String newName) {
				recordDeclaration.setName(newName);
			}
		};
	}

	private static Package createDefaultPackage(CompilationUnit compilationUnit) {
		return new Y.Package() {

			@Override
			public Stream<Y.Record> records() {
				return compilationUnit.stream(TreeTraversal.DIRECT_CHILDREN)//
						.flatMap(filterOnClass(com.github.javaparser.ast.body.RecordDeclaration.class))//
						.map(decl -> {
							return createRecord(decl);
						});
			}

			@Override
			public Stream<Y.Interface> interfaces() {
				return compilationUnit.stream(TreeTraversal.DIRECT_CHILDREN)//
						.flatMap(filterOnClass(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class))//
						.filter(decl -> decl.isInterface())//
						.map(interfaceDeclaration -> {
							return createInterface(interfaceDeclaration);
						});
			}

			@Override
			public Stream<Y.Class> classes() {
				return compilationUnit.stream(TreeTraversal.DIRECT_CHILDREN)//
						.flatMap(filterOnClass(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class))//
						.filter(decl -> !decl.isInterface())//
						.map(classDeclaration -> {
							return createClass(classDeclaration);
						});
			}

			@Override
			public void rename(String newName) {
				// TODO
				throw new UnsupportedOperationException("Not implemented yet");
			}

			@Override
			public String name() {
				// TODO
				throw new UnsupportedOperationException("Not implemented yet");
			}
		};
	}

	private static <T> Function<? super Node, Stream<T>> filterOnClass(java.lang.Class<T> clazz) {
		return node -> clazz.isInstance(node) ? Stream.of(clazz.cast(node)) : Stream.empty();
	}
}
