package fr.vergne.stanos.core.refactorer.javaparser;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaToken;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.Node.TreeTraversal;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;

import fr.vergne.stanos.core.refactorer.Code;
import fr.vergne.stanos.core.refactorer.Component;
import fr.vergne.stanos.core.refactorer.Component.IfStatement;
import fr.vergne.stanos.core.refactorer.Refactorer;

public interface JavaParserRefactorer extends Refactorer {

	static Refactorer.ForCode forCode(String code) {
		// TODO Expose language version
		CompilationUnit compilationUnit = parseWithJavaParser(code, LanguageLevel.JAVA_17);
//		LexicalPreservingPrinter.setup(compilationUnit);
		Code.Source source = abstractFromJavaParser(compilationUnit);
		return new Refactorer.ForCode() {

			@Override
			public String code() {
				return compilationUnit.toString();
//				return LexicalPreservingPrinter.print(compilationUnit);
			}

			@Override
			public Code.Source source() {
				return source;
			}
		};
	}

	interface CodePrinter {
		String print();
	}

	static Code.Source abstractFromJavaParser(CompilationUnit compilationUnit) {
		return new Code.Source() {
			@Override
			public Component.Package defaultPackage() {
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
		return parseResult.getResult().orElseThrow();
	}

	private static Component.Variable createVariable(MethodDeclaration methodDeclaration,
			VariableDeclarator variableDeclaration) {
		return new Component.Variable() {
			@Override
			public String name() {
				return variableDeclaration.getNameAsString();
			}

			@Override
			public void rename(String newName) {
				_intern.retrieveVariableOtherOccurrences(methodDeclaration, variableDeclaration)
						.forEach(nameNode -> nameNode.setIdentifier(newName));
				variableDeclaration.getName().setIdentifier(newName);
			}

			@Override
			public Stream<Component.Method> methods() {
				return variableDeclaration.getInitializer().map(expr -> {
					if (expr instanceof ObjectCreationExpr ocExpr) {
						return ocExpr.getAnonymousClassBody().map(list -> {
							return list.stream().map(node -> {
								if (node instanceof MethodDeclaration decl) {
									return createMethod(decl);
								} else {
									throw new UnsupportedOperationException(
											"Not supported: " + node.getClass().getSimpleName());
								}
							});
						}).orElse(Stream.empty());
					} else {
						throw new UnsupportedOperationException("Not supported: " + expr.getClass().getSimpleName());
					}
				}).orElse(Stream.empty());
			}

			@Override
			public void splitDeclaration() {
				Expression initializer = variableDeclaration.getInitializer().orElseThrow(() -> {
					return new IllegalStateException("No assignment to split on " + name() + " declaration");
				});
				variableDeclaration.removeInitializer();

				Node parentNode = variableDeclaration.getParentNode().orElseThrow();
				if (parentNode instanceof VariableDeclarationExpr exp) {
					Node grandParentNode = exp.getParentNode().orElseThrow();
					if (grandParentNode instanceof ExpressionStmt stmt) {
						Node grandGrandParentNode = stmt.getParentNode().orElseThrow();
						if (grandGrandParentNode instanceof BlockStmt block) {
							List<Node> childNodes = block.getChildNodes();
							int index = childNodes.indexOf(stmt) + 1;
							AssignExpr assignExpr = new AssignExpr();
							assignExpr.setTarget(new NameExpr(name()));
							assignExpr.setOperator(Operator.ASSIGN);
							assignExpr.setValue(initializer);// Keep it last to not break parent change notif
							block.addStatement(index, assignExpr);
						} else {
							throw new UnsupportedOperationException(
									"Not implemented yet: " + grandGrandParentNode.getClass().getSimpleName());
						}
					} else {
						throw new UnsupportedOperationException(
								"Not implemented yet: " + grandParentNode.getClass().getSimpleName());
					}
				} else {
					throw new UnsupportedOperationException(
							"Not implemented yet: " + parentNode.getClass().getSimpleName());
				}
			}

			@Override
			public void joinDeclaration() {
				variableDeclaration.getInitializer().ifPresent(init -> {
					throw new IllegalStateException(name() + " declaration already assigns a value");
				});

				Node parentNode = variableDeclaration.getParentNode().orElseThrow();
				if (parentNode instanceof VariableDeclarationExpr exp) {
					Node grandParentNode = exp.getParentNode().orElseThrow();
					if (grandParentNode instanceof ExpressionStmt stmt) {
						Node grandGrandParentNode = stmt.getParentNode().orElseThrow();
						if (grandGrandParentNode instanceof BlockStmt block) {
							List<Node> childNodes = block.getChildNodes();
							int indexOfNextStatement = childNodes.indexOf(stmt) + 1;
							Statement statement = block.getStatement(indexOfNextStatement);
							if (statement instanceof ExpressionStmt nextStmt) {
								Expression nextExpr = nextStmt.getExpression();
								if (nextExpr instanceof AssignExpr nextAssignExpr) {
									Operator operator = nextAssignExpr.getOperator();
									if (operator.equals(Operator.ASSIGN)) {
										Expression target = nextAssignExpr.getTarget();
										if (target instanceof NameExpr nameExpr) {
											String name = nameExpr.getNameAsString();
											if (name.equals(name())) {
												block.getStatements().remove(indexOfNextStatement);
												variableDeclaration.setInitializer(nextAssignExpr.getValue());
											} else {
												throw new IllegalStateException(
														"No " + name() + " assignment just after its declaration");
											}
										} else {
											throw new UnsupportedOperationException(
													"Not implemented yet: " + target.getClass().getSimpleName());
										}
									} else {
										throw new UnsupportedOperationException("Not implemented yet: " + operator);
									}
								} else {
									throw new IllegalStateException(
											"No " + name() + " assignment just after its declaration");
								}
							} else {
								throw new UnsupportedOperationException(
										"Not implemented yet: " + statement.getClass().getSimpleName());
							}
						} else {
							throw new UnsupportedOperationException(
									"Not implemented yet: " + grandGrandParentNode.getClass().getSimpleName());
						}
					} else {
						throw new UnsupportedOperationException(
								"Not implemented yet: " + grandParentNode.getClass().getSimpleName());
					}
				} else {
					throw new UnsupportedOperationException(
							"Not implemented yet: " + parentNode.getClass().getSimpleName());
				}
			}

			@Override
			public void increaseScope(Class clazz) {
				VariableDeclarationExpr variableDeclarationExp = (VariableDeclarationExpr) variableDeclaration
						.getParentNode().orElseThrow();
				ExpressionStmt expressionStmt = (ExpressionStmt) variableDeclarationExp.getParentNode().orElseThrow();
				BlockStmt blockStmt = (BlockStmt) expressionStmt.getParentNode().orElseThrow();
				blockStmt.remove(expressionStmt);

				// FIXME Fail if clazz is not parent clazz
				ClassOrInterfaceDeclaration classOrInterface = (ClassOrInterfaceDeclaration) methodDeclaration
						.getParentNode().orElseThrow();
				classOrInterface.addFieldWithInitializer(variableDeclaration.getType(),
						variableDeclaration.getNameAsString(), variableDeclaration.getInitializer().orElseThrow(),
						Keyword.PRIVATE);
			}

			@Override
			public void decreaseScope() {
				VariableDeclarationExpr variableDeclarationExp = (VariableDeclarationExpr) variableDeclaration
						.getParentNode().orElseThrow();
				ExpressionStmt expressionStmt = (ExpressionStmt) variableDeclarationExp.getParentNode().orElseThrow();
				BlockStmt blockStmt = (BlockStmt) expressionStmt.getParentNode().orElseThrow();
				List<Node> childNodes = blockStmt.getChildNodes();
				int index = childNodes.indexOf(expressionStmt);
				if (index == childNodes.size() - 1) {
					throw new IllegalStateException("Minimum scope reached for " + name());
				}
				blockStmt.remove(expressionStmt);

				// TODO Fail if no next statement
				// TODO Fail if next statement depends on it
				blockStmt.addStatement(index + 1, expressionStmt);
			}

			@Override
			public void increaseScope() {
				VariableDeclarationExpr variableDeclarationExp = (VariableDeclarationExpr) variableDeclaration
						.getParentNode().orElseThrow();
				ExpressionStmt expressionStmt = (ExpressionStmt) variableDeclarationExp.getParentNode().orElseThrow();
				BlockStmt blockStmt = (BlockStmt) expressionStmt.getParentNode().orElseThrow();
				List<Node> childNodes = blockStmt.getChildNodes();
				int index = childNodes.indexOf(expressionStmt);
				blockStmt.remove(expressionStmt);

				// TODO Fail if no previous statement
				// TODO Fail if previous statement depends on it
				if (index > 0) {
					blockStmt.addStatement(index - 1, expressionStmt);
				} else {
					Node parentNode = blockStmt.getParentNode().orElseThrow();
					Node childNode = blockStmt;
					while (parentNode instanceof IfStmt ifStmt) {
						childNode = parentNode;
						parentNode = ifStmt.getParentNode().orElseThrow();
					}
					BlockStmt parentBlockStmt = (BlockStmt) parentNode;
					index = parentBlockStmt.getChildNodes().indexOf(childNode);
					parentBlockStmt.addStatement(index, expressionStmt);
				}
			}

			@Override
			public void decreaseScope(int blockIndex) {
				VariableDeclarationExpr variableDeclarationExp = (VariableDeclarationExpr) variableDeclaration
						.getParentNode().orElseThrow();
				ExpressionStmt expressionStmt = (ExpressionStmt) variableDeclarationExp.getParentNode().orElseThrow();
				BlockStmt blockStmt = (BlockStmt) expressionStmt.getParentNode().orElseThrow();
				List<Node> childNodes = blockStmt.getChildNodes();
				int index = childNodes.indexOf(expressionStmt);
				Node node = blockStmt.getChildNodes().get(index + 1);
				if (node instanceof IfStmt) {
					for (; blockIndex > 0; blockIndex--) {
						node = ((IfStmt) node).getElseStmt().orElseThrow();
					}
					BlockStmt targetStmt;
					if (node instanceof IfStmt ifStmt) {
						// Not last else
						targetStmt = (BlockStmt) ifStmt.getThenStmt();
					} else {
						// Last else
						targetStmt = (BlockStmt) node;
					}
					blockStmt.remove(expressionStmt);
					targetStmt.addStatement(0, expressionStmt);
				} else {
					throw new UnsupportedOperationException("Not implemented yet");
				}
			}

			@Override
			public void removeUnused() {
				Action action = Action.NO_OP;

				{
					VariableDeclarationExpr expr = (VariableDeclarationExpr) variableDeclaration.getParentNode()
							.orElseThrow();
					ExpressionStmt stmt = (ExpressionStmt) expr.getParentNode().orElseThrow();
					BlockStmt blockStmt = (BlockStmt) stmt.getParentNode().orElseThrow();
					action = action.then(() -> blockStmt.remove(stmt));
				}

				{
					Iterator<SimpleName> occurrences = _intern
							.retrieveVariableOtherOccurrences(methodDeclaration, variableDeclaration).iterator();
					while (occurrences.hasNext()) {
						SimpleName nameNode = occurrences.next();
						NameExpr nameExpr = (NameExpr) nameNode.getParentNode().orElseThrow();
						Node parentNode = nameExpr.getParentNode().orElseThrow();
						if (parentNode instanceof MethodCallExpr //
								|| parentNode instanceof VariableDeclarator //
								|| parentNode instanceof BinaryExpr) {
							while (!(parentNode instanceof ExpressionStmt)) {
								parentNode = parentNode.getParentNode().orElseThrow();
							}
							throw new IllegalStateException(name() + " is used in: " + parentNode.toString());
						} else if (parentNode instanceof AssignExpr expr) {
							if (!expr.getTarget().equals(nameExpr)) {
								while (!(parentNode instanceof ExpressionStmt)) {
									parentNode = parentNode.getParentNode().orElseThrow();
								}
								throw new IllegalStateException(name() + " is used in: " + parentNode.toString());
							}
							ExpressionStmt stmt = (ExpressionStmt) expr.getParentNode().orElseThrow();
							Node actualParent = stmt.getParentNode().orElseThrow();
							action = action.then(() -> actualParent.remove(stmt));
						} else {
							throw new UnsupportedOperationException("Not implemented yet: " + parentNode.getClass());
						}
					}
				}

				action.act();
			}
		};
	}

	private static Component.Parameter createParameter(MethodDeclaration methodDeclaration,
			Parameter parameterDeclaration) {
		return new Component.Parameter() {
			@Override
			public String name() {
				return parameterDeclaration.getNameAsString();
			}

			@Override
			public void rename(String newName) {
				String currentName = parameterDeclaration.getNameAsString();
				methodDeclaration.getBody().ifPresent(body -> {
					body.stream()//
							.flatMap(filterOnClass(SimpleName.class))//
							.filter(nameNode -> nameNode.getIdentifier().equals(currentName))//
							.forEach(nameNode -> {
								Node parentNode = nameNode.getParentNode().orElseThrow();
								if (parentNode instanceof NameExpr exp) {
									com.github.javaparser.ast.body.Parameter declaration = exp.resolve()
											.toAst(com.github.javaparser.ast.body.Parameter.class).orElseThrow();
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

	private static Component.Method createMethod(MethodDeclaration methodDeclaration) {
		return new Component.Method() {
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
				methodDeclaration.getName().setIdentifier(newName);
			}

			@Override
			public Stream<Component.Parameter> parameters() {
				return methodDeclaration.stream(TreeTraversal.DIRECT_CHILDREN)//
						.flatMap(filterOnClass(Parameter.class))//
						.map(parameterDeclaration -> createParameter(methodDeclaration, parameterDeclaration));
			}

			@Override
			public Stream<Component.Variable> variables() {
				return methodDeclaration.getBody().orElseThrow().stream(TreeTraversal.DIRECT_CHILDREN)//
						.flatMap(node -> {
							if (node instanceof Statement stmt) {
								return resolveStatementToExpressions(stmt);
							} else {
								throw new UnsupportedOperationException(
										"Not supported: " + node.getClass().getSimpleName());
							}
						})//
						.flatMap(node -> {
							if (node instanceof VariableDeclarationExpr exp) {
								return exp.getVariables().stream();
							} else if (node instanceof NameExpr) {
								return Stream.empty();
							} else if (node instanceof MethodCallExpr) {
								return Stream.empty();
							} else {
								throw new UnsupportedOperationException(
										"Not supported: " + node.getClass().getSimpleName());
							}
						})//
						.flatMap(filterOnClass(VariableDeclarator.class))//
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
							stmt.getElseStmt().map(elseStmt -> resolveStatementToExpressions(elseStmt))
									.orElse(Stream.empty())//
					);
				} else if (statement instanceof LocalClassDeclarationStmt) {
					return Stream.empty();
				} else {
					throw new UnsupportedOperationException("Not supported: " + statement.getClass().getSimpleName());
				}
			}

			@Override
			public Stream<Component.Interface> interfaces() {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("Not implemented yet");
			}

			@Override
			public Stream<Component.Class> classes() {
				return methodDeclaration.getBody()//
						.map(body -> body.stream(TreeTraversal.DIRECT_CHILDREN)//
								.flatMap(filterOnClass(LocalClassDeclarationStmt.class))//
								.flatMap(localClassStatement -> {
									return localClassStatement.stream(TreeTraversal.DIRECT_CHILDREN)//
											.flatMap(filterOnClass(ClassOrInterfaceDeclaration.class))//
											.filter(decl -> !decl.isInterface())//
											.map(localClassDeclaration -> {
												return createClass(localClassDeclaration);
											});
								}))//
						.orElseGet(Stream::empty);
			}

			@Override
			public IfStatement ifStatement(int index) {
				IfStmt ifStmt = (IfStmt) methodDeclaration.getBody().orElseThrow().stream()//
						.filter(node -> node instanceof IfStmt).skip(index)//
						.findFirst().orElseThrow();
				return createIfStatement(methodDeclaration, ifStmt);
			}
		};
	}

	private static Component.IfStatement createIfStatement(MethodDeclaration methodDeclaration, IfStmt ifStmt) {
		return new Component.IfStatement() {

			@Override
			public void distributePrevious() {
				BlockStmt blockStmt = (BlockStmt) ifStmt.getParentNode().orElseThrow();
				List<Node> childNodes = blockStmt.getChildNodes();
				int index = childNodes.indexOf(ifStmt);
				ExpressionStmt previous = (ExpressionStmt) childNodes.get(index - 1);

				VariableDeclarationExpr exp = (VariableDeclarationExpr) previous.getExpression();
				VariableDeclarator variable = exp.getVariable(0);
				if (isVariableUsedOutOfIf(methodDeclaration, ifStmt, variable)) {
					throw new IllegalStateException(variable.getNameAsString() + " is used out of the if");
				}

				blockStmt.remove(previous);

				// TODO Reuse previous or clone it?
				Node currentNode = ifStmt;
				do {
					IfStmt currentIfStmt = (IfStmt) currentNode;
					((BlockStmt) currentIfStmt.getThenStmt()).addStatement(0, previous);
					currentNode = currentIfStmt.getElseStmt().orElseThrow();
				} while (currentNode instanceof IfStmt);
				{
					((BlockStmt) currentNode).addStatement(0, previous);
				}
			}

			private boolean isVariableUsedOutOfIf(MethodDeclaration methodDeclaration, IfStmt ifStmt,
					VariableDeclarator variable) {
				return _intern.retrieveVariableOtherOccurrences(methodDeclaration, variable)//
						.filter(occurrence -> {
							Node current = occurrence;
							do {
								current = current.getParentNode().orElseThrow();
								if (current == ifStmt) {
									return false;
								} else if (current == methodDeclaration) {
									return true;
								} else {
									continue;
								}
							} while (true);
						}).findFirst().isPresent();
			}

			@Override
			public void distributeNext() {
				BlockStmt blockStmt = (BlockStmt) ifStmt.getParentNode().orElseThrow();
				List<Node> childNodes = blockStmt.getChildNodes();
				int index = childNodes.indexOf(ifStmt);
				ExpressionStmt next = (ExpressionStmt) childNodes.get(index + 1);

				VariableDeclarationExpr exp = (VariableDeclarationExpr) next.getExpression();
				VariableDeclarator variable = exp.getVariable(0);
				if (isVariableUsedOutOfIf(methodDeclaration, ifStmt, variable)) {
					throw new IllegalStateException(variable.getNameAsString() + " is used out of the if");
				}

				blockStmt.remove(next);

				// TODO Reuse previous or clone it?
				Node currentNode = ifStmt;
				do {
					IfStmt currentIfStmt = (IfStmt) currentNode;
					BlockStmt blockStmt2 = (BlockStmt) currentIfStmt.getThenStmt();
					int lastIndex = blockStmt2.getChildNodes().size();
					blockStmt2.addStatement(lastIndex, next);
					currentNode = currentIfStmt.getElseStmt().orElseThrow();
				} while (currentNode instanceof IfStmt);
				{
					BlockStmt blockStmt2 = (BlockStmt) currentNode;
					int lastIndex = blockStmt2.getChildNodes().size();
					blockStmt2.addStatement(lastIndex, next);
				}
			}

			@Override
			public void factorFirst() {
				// TODO Fail if 1 first statement missing
				ExpressionStmt ref = null;
				Node currentNode = ifStmt;
				do {
					IfStmt currentIfStmt = (IfStmt) currentNode;
					BlockStmt blockStmt = (BlockStmt) currentIfStmt.getThenStmt();
					ExpressionStmt firstStmt = (ExpressionStmt) blockStmt.getChildNodes().get(0);
					if (ref == null) {
						ref = firstStmt;
					} else if (!firstStmt.toString().equals(ref.toString())) {
						throw new IllegalStateException("Some blocks do not start with: " + ref.toString());
					}
					blockStmt.remove(firstStmt);
					currentNode = currentIfStmt.getElseStmt().orElseThrow();
				} while (currentNode instanceof IfStmt);
				{
					BlockStmt blockStmt = (BlockStmt) currentNode;
					ExpressionStmt firstStmt = (ExpressionStmt) blockStmt.getChildNodes().get(0);
					blockStmt.remove(firstStmt);
				}

				BlockStmt blockStmt = (BlockStmt) ifStmt.getParentNode().orElseThrow();
				List<Node> childNodes = blockStmt.getChildNodes();
				int index = childNodes.indexOf(ifStmt);
				blockStmt.addStatement(index, ref);
			}

			@Override
			public void factorLast() {
				// TODO Fail if 1 last statement missing
				ExpressionStmt ref = null;
				Node currentNode = ifStmt;
				do {
					IfStmt currentIfStmt = (IfStmt) currentNode;
					BlockStmt blockStmt = (BlockStmt) currentIfStmt.getThenStmt();
					List<Node> childNodes = blockStmt.getChildNodes();
					ExpressionStmt lastStmt = (ExpressionStmt) childNodes.get(childNodes.size() - 1);
					if (ref == null) {
						ref = lastStmt;
					} else if (!lastStmt.toString().equals(ref.toString())) {
						throw new IllegalStateException("Some blocks do not finish with: " + ref.toString());
					}
					blockStmt.remove(lastStmt);
					currentNode = currentIfStmt.getElseStmt().orElseThrow();
				} while (currentNode instanceof IfStmt);
				{
					BlockStmt blockStmt = (BlockStmt) currentNode;
					List<Node> childNodes = blockStmt.getChildNodes();
					ExpressionStmt lastStmt = (ExpressionStmt) childNodes.get(childNodes.size() - 1);
					blockStmt.remove(lastStmt);
				}

				BlockStmt blockStmt = (BlockStmt) ifStmt.getParentNode().orElseThrow();
				List<Node> childNodes = blockStmt.getChildNodes();
				int index = childNodes.indexOf(ifStmt);
				blockStmt.addStatement(index + 1, ref);
			}
		};
	}

	private static Component.Field createField(VariableDeclarator variableDeclarator) {
		return new Component.Field() {
			@Override
			public String name() {
				return variableDeclarator.getNameAsString();
			}

			@Override
			public void rename(String newName) {
				variableDeclarator.getName().setIdentifier(newName);
				// TODO Rename uses
			}

			@Override
			public Stream<Method> methods() {
				// TODO
				throw new UnsupportedOperationException("Not implemented yet");
			}

			@Override
			public void decreaseScope() {
				FieldDeclaration fieldDeclaration = (FieldDeclaration) variableDeclarator.getParentNode().orElseThrow();
				if (fieldDeclaration.getAccessSpecifier().equals(AccessSpecifier.PUBLIC)) {
					fieldDeclaration.setModifiers(Keyword.PROTECTED);
				} else if (fieldDeclaration.getAccessSpecifier().equals(AccessSpecifier.PROTECTED)) {
					fieldDeclaration.setModifiers(Keyword.PRIVATE);
				} else {
					System.out.println(fieldDeclaration.getClass() + ": " + fieldDeclaration);
					// TODO Auto-generated method stub
					throw new UnsupportedOperationException("Not implemented yet");
				}
			}

			@Override
			public void decreaseScope(Method method) {
				FieldDeclaration fieldDeclaration = (FieldDeclaration) variableDeclarator.getParentNode().orElseThrow();
				ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) fieldDeclaration.getParentNode()
						.orElseThrow();
				clazz.remove(fieldDeclaration);

				// FIXME Fail if method is not one of ours
				MethodDeclaration methodDeclaration = clazz.getMethods().stream()//
						.filter(meth -> meth.getNameAsString().equals(method.name()))//
						.filter(meth -> meth.hasParametersOfType(method.parameterTypes().toArray(new String[0])))//
						.findFirst().orElseThrow();
				BlockStmt methodBody = methodDeclaration.getBody().orElseThrow();
				methodBody.addStatement(0, new VariableDeclarationExpr(variableDeclarator));
			}

			@Override
			public void increaseScope() {
				FieldDeclaration fieldDeclaration = (FieldDeclaration) variableDeclarator.getParentNode().orElseThrow();
				if (fieldDeclaration.getAccessSpecifier().equals(AccessSpecifier.PRIVATE)) {
					fieldDeclaration.setModifiers(Keyword.PROTECTED);
				} else if (fieldDeclaration.getAccessSpecifier().equals(AccessSpecifier.PROTECTED)) {
					fieldDeclaration.setModifiers(Keyword.PUBLIC);
				} else {
					throw new IllegalStateException("Maximum scope reached for " + name());
				}
			}

			@Override
			public void distributeToMethods() {
				FieldDeclaration fieldDeclaration = (FieldDeclaration) variableDeclarator.getParentNode().orElseThrow();
				if (fieldDeclaration.isStatic()) {
					throw new IllegalStateException(name() + " is static");
				}

				ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) fieldDeclaration.getParentNode()
						.orElseThrow();
				clazz.remove(fieldDeclaration);

				clazz.getMethods().stream()//
						.forEach(methodDeclaration -> {
							BlockStmt methodBody = methodDeclaration.getBody().orElseThrow();
							methodBody.addStatement(0, new VariableDeclarationExpr(variableDeclarator));
						});
			}

			@Override
			public void distributeToInstances() {
				FieldDeclaration fieldDeclaration = (FieldDeclaration) variableDeclarator.getParentNode().orElseThrow();
				if (fieldDeclaration.isStatic()) {
					fieldDeclaration.setStatic(false);
				} else {
					throw new IllegalStateException(name() + " is not static");
				}
			}

			@Override
			public void factorFromInstances() {
				FieldDeclaration fieldDeclaration = (FieldDeclaration) variableDeclarator.getParentNode().orElseThrow();
				if (!fieldDeclaration.isStatic()) {
					fieldDeclaration.setStatic(true);
				} else {
					throw new IllegalStateException(name() + " is static");
				}
			}
		};
	}

	private static Component.Class createClass(ClassOrInterfaceDeclaration classDeclaration) {
		return new Component.Class() {
			@Override
			public String name() {
				return classDeclaration.getNameAsString();
			}

			@Override
			public void rename(String newName) {
				classDeclaration.getName().setIdentifier(newName);
			}

			@Override
			public Stream<Component.Method> methods() {
				return internalMethods().map(JavaParserRefactorer::createMethod);
			}

			private Stream<MethodDeclaration> internalMethods() {
				return classDeclaration.stream(TreeTraversal.DIRECT_CHILDREN)//
						.flatMap(filterOnClass(MethodDeclaration.class));
			}

			@Override
			// method
			public Stream<Component.Class> classes() {
				return classDeclaration.stream(TreeTraversal.DIRECT_CHILDREN)//
						.flatMap(filterOnClass(ClassOrInterfaceDeclaration.class))//
						.filter(decl -> !decl.isInterface())//
						.map(innerClassDeclaration -> {
							return createClass(innerClassDeclaration);
						});
			}

			@Override
			public Stream<Component.Interface> interfaces() {
				// TODO
				throw new UnsupportedOperationException("Not implemented yet");
			}

			@Override
			public Stream<Component.Field> fields() {
				return classDeclaration.getFields().stream().flatMap(fieldDeclaration -> {
					return fieldDeclaration.getVariables().stream().map(variableDeclarator -> {
						return createField(variableDeclarator);
					});
				});
			}

			@Override
			public void factorFromMethods() {
				ExpressionStmt ref = null;
				VariableDeclarator declarator = null;
				Iterator<MethodDeclaration> iterator = internalMethods().iterator();
				while (iterator.hasNext()) {
					MethodDeclaration methodDeclaration = iterator.next();
					BlockStmt body = methodDeclaration.getBody().orElseThrow();
					ExpressionStmt firstNode = (ExpressionStmt) body.getChildNodes().get(0);
					if (declarator == null) {
						ref = firstNode;
						VariableDeclarationExpr childNode = (VariableDeclarationExpr) firstNode.getChildNodes().get(0);
						declarator = (VariableDeclarator) childNode.getChildNodes().get(0);
					} else if (!firstNode.toString().equals(ref.toString())) {
						throw new IllegalStateException("Some methods do not start with: " + ref.toString());
					}
					body.remove(firstNode);
				}

				String type = declarator.getTypeAsString();
				String name = declarator.getNameAsString();
				Expression initializer = declarator.getInitializer().orElseThrow();
				classDeclaration.addFieldWithInitializer(type, name, initializer);
			}
		};
	}

	private static Component.Interface createInterface(ClassOrInterfaceDeclaration interfaceDeclaration) {
		return new Component.Interface() {
			@Override
			public String name() {
				return interfaceDeclaration.getFullyQualifiedName().orElseThrow();
			}

			@Override
			public void rename(String newName) {
				interfaceDeclaration.getName().setIdentifier(newName);
				// TODO rename uses
			}

			@Override
			public Stream<Component.Method> methods() {
				return interfaceDeclaration.stream(TreeTraversal.DIRECT_CHILDREN)//
						.flatMap(filterOnClass(MethodDeclaration.class))//
						.map(methodDeclaration -> {
							return createMethod(methodDeclaration);
						});
			}

			@Override
			public Stream<Component.Class> classes() {
				// TODO
				throw new UnsupportedOperationException("Not implemented yet");
			}

			@Override
			public Stream<Component.Interface> interfaces() {
				// TODO
				throw new UnsupportedOperationException("Not implemented yet");
			}
		};
	}

	private static Component.Record createRecord(RecordDeclaration recordDeclaration) {
		return new Component.Record() {
			@Override
			public String name() {
				return recordDeclaration.getFullyQualifiedName().orElseThrow();
			}

			@Override
			public void rename(String newName) {
				recordDeclaration.getName().setIdentifier(newName);
			}
		};
	}

	private static Component.Package createDefaultPackage(CompilationUnit compilationUnit) {
		return new Component.Package() {

			@Override
			public Stream<Component.Record> records() {
				return compilationUnit.stream(TreeTraversal.DIRECT_CHILDREN)//
						.flatMap(filterOnClass(RecordDeclaration.class))//
						.map(decl -> {
							return createRecord(decl);
						});
			}

			@Override
			public Stream<Component.Interface> interfaces() {
				return compilationUnit.stream(TreeTraversal.DIRECT_CHILDREN)//
						.flatMap(filterOnClass(ClassOrInterfaceDeclaration.class))//
						.filter(decl -> decl.isInterface())//
						.map(interfaceDeclaration -> {
							return createInterface(interfaceDeclaration);
						});
			}

			@Override
			public Stream<Component.Class> classes() {
				return compilationUnit.stream(TreeTraversal.DIRECT_CHILDREN)//
						.flatMap(filterOnClass(ClassOrInterfaceDeclaration.class))//
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

	static boolean sameTokens(JavaToken token1, JavaToken token2) {
		return token1.getKind() == token2.getKind() //
				&& token1.getText().equals(token2.getText())//
				&& token1.getRange().equals(token2.getRange());
	}

	// TODO Make private
	public static interface Action {
		public static Action NO_OP = () -> {
		};

		void act();

		default Action then(Action next) {
			Action previous = this;
			return () -> {
				previous.act();
				next.act();
			};
		}
	}

	public static class _intern {
		private static Stream<SimpleName> retrieveVariableOtherOccurrences(MethodDeclaration methodDeclaration,
				VariableDeclarator variableDeclaration) {
			String currentName = variableDeclaration.getNameAsString();
			return methodDeclaration.getBody().orElseThrow().stream()//
					.flatMap(filterOnClass(SimpleName.class))//
					.filter(nameNode -> nameNode.getIdentifier().equals(currentName))//
					.flatMap(nameNode -> {
						Node parentNode = nameNode.getParentNode().orElseThrow();
						if (parentNode instanceof NameExpr exp) {
							ResolvedValueDeclaration resolved = exp.resolve();
							if (resolved.isVariable()) {
								VariableDeclarationExpr declarations = resolved.toAst(VariableDeclarationExpr.class)
										.orElseThrow();
								if (declarations.getVariables().contains(variableDeclaration)) {
									return Stream.of(nameNode);
								} else {
									// Relate to another parameter with the same name
								}
							} else {
								// Relate to something else with the same name
							}
						} else {
							// Relate to something else with the same name
						}
						return Stream.empty();
					});
		}
	}
}
