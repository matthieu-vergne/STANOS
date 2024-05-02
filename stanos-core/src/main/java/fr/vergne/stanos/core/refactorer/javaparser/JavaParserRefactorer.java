package fr.vergne.stanos.core.refactorer.javaparser;

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaToken;
import com.github.javaparser.JavaToken.Kind;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.Node.ObserverRegistrationMode;
import com.github.javaparser.ast.Node.TreeTraversal;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithTokenRange;
import com.github.javaparser.ast.observer.AstObserver;
import com.github.javaparser.ast.observer.ObservableProperty;
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
import fr.vergne.stanos.core.refactorer.Refactorer;
import fr.vergne.stanos.core.refactorer.javaparser.TokenSerializer.Align;

public interface JavaParserRefactorer extends Refactorer {

	static Refactorer.ForCode forCode(String code) {
		TokenSerializer tokenSerializer = TokenSerializer.textOnly().withMinLength(7, Align.CENTER);
		TokensTreeRenderer tokensTreeRenderer = new TokensTreeRenderer(tokenSerializer);

		// TODO Expose language version
		CompilationUnit compilationUnit = parseWithJavaParser(code, LanguageLevel.JAVA_17);
//		LexicalPreservingPrinter.setup(compilationUnit);
		CodePrinter codePrinter = createCodePrinter(compilationUnit, tokensTreeRenderer, tokenSerializer);
		compilationUnit.register(codePrinter.observer(), ObserverRegistrationMode.SELF_PROPAGATING);
		Code.Source source = abstractFromJavaParser(compilationUnit);
		return new Refactorer.ForCode() {

			@Override
			public String code() {
				System.out.println(">>>>>");
				tokensTreeRenderer.render(compilationUnit, System.out::println);
				System.out.println(">>>>>");
				return codePrinter.print();
//				return LexicalPreservingPrinter.print(compilationUnit);
			}

			@Override
			public Code.Source source() {
				System.out.println("<<<<<");
				tokensTreeRenderer.render(compilationUnit, System.out::println);
				System.out.println("<<<<<");
				return source;
			}
		};
	}

	interface CodePrinter {
		String print();

		AstObserver observer();
	}

	static CodePrinter createCodePrinter(CompilationUnit compilationUnit, TokensTreeRenderer tokensTreeRenderer,
			TokenSerializer tokenLogSerializer) {
		return new CodePrinter() {
			@Override
			public String print() {
				TokenRange tokens = compilationUnit.getTokenRange().orElseThrow();
				return StreamSupport.stream(tokens.spliterator(), false)//
						.peek(token -> System.out.println(tokenLogSerializer.serialize(token)))//
						.map(token -> token.asString())//
						.collect(joining());
			}

			private String display(Object obj) {
				return obj == null ? "(null)" : "[" + obj.getClass().getSimpleName() + "]" + obj;
			}

			@Override
			public AstObserver observer() {
				return new AstObserver() {
					@Override
					public void propertyChange(Node observedNode, ObservableProperty property, Object oldValue,
							Object newValue) {
						System.out.println("property " + property + " on " + display(observedNode) + ": "
								+ display(oldValue) + " > " + display(newValue));
						if (observedNode instanceof NodeWithTokenRange<?> nodeWithTokens) {
							if (singleTokenIn(nodeWithTokens)) {
								if (oldValue instanceof String oldString && newValue instanceof String newString) {
									JavaToken token = nodeWithTokens.getTokenRange().orElseThrow().getBegin();
									token.setText(newString);
								} else if (nodeWithTokens instanceof VariableDeclarator varDec
										&& property == ObservableProperty.INITIALIZER && oldValue == null
										&& newValue instanceof NodeWithTokenRange<?> newNode) {
									TokenInserter.from(varDec.getTokenRange().orElseThrow().getEnd())//
											.insertAfter(new JavaToken(Kind.SPACE.getKind()))//
											.insertAfter(new JavaToken(Kind.ASSIGN.getKind()))//
											.insertAfter(new JavaToken(Kind.SPACE.getKind()))//
											.insertAfter(newNode.getTokenRange().orElseThrow().getBegin());
								} else {
									throw new UnsupportedOperationException("Not implemented yet");
								}
							} else {
								throw new UnsupportedOperationException("Not implemented yet");
							}
						} else {
							throw new UnsupportedOperationException("Not implemented yet");
						}
					}

					@Override
					public void parentChange(Node observedNode, Node previousParent, Node newParent) {
						// TODO
						System.out.println("parent of " + display(observedNode) + ": " + display(previousParent) + " > "
								+ display(newParent));
						System.out.println("======");
						tokensTreeRenderer.render(compilationUnit, System.out::println);
						System.out.println("======");
					}

					@Override
					public void listChange(NodeList<?> observedNodeList, ListChangeType type, int index,
							Node nodeAddedOrRemoved) {
						System.out.println("list of " + observedNodeList + "[" + index + "]: " + type + " "
								+ display(nodeAddedOrRemoved));
						switch (type) {
						case REMOVAL -> listRemoval(observedNodeList, index, nodeAddedOrRemoved);
						case ADDITION -> listAddition(observedNodeList, index, nodeAddedOrRemoved);
						default -> throw new UnsupportedOperationException("Not implemented yet");
						}
						System.out.println("======");
						tokensTreeRenderer.render(compilationUnit, System.out::println);
						System.out.println("======");
					}

					private void listAddition(NodeList<?> observedNodeList, int index, Node nodeAddedOrRemoved) {
					}

					private void listRemoval(NodeList<?> observedNodeList, int index, Node nodeAddedOrRemoved) {
						if (nodeAddedOrRemoved instanceof NodeWithTokenRange<?> nodeWithTokens) {
							Node parentNode = observedNodeList.getParentNode().orElseThrow();
							TokenRange parentTokens = parentNode.getTokenRange().orElseThrow();
							TokenRange removedTokens = nodeAddedOrRemoved.getTokenRange().orElseThrow();
							if (index == 0) {
								JavaToken removedBegin;
								removedBegin = removedTokens.getBegin();
								JavaToken removedEnd = removedTokens.getEnd();
								boolean remove = false;
								for (JavaToken token : parentTokens) {
									if (sameTokens(token, removedBegin)) {
										System.out.println("X " + tokenLogSerializer.serialize(token));
										remove = true;
										token.deleteToken();
									} else if (sameTokens(token, removedEnd)) {
										System.out.println("X " + tokenLogSerializer.serialize(token));
										token.deleteToken();
										remove = false;
									} else if (remove) {
										System.out.println("X " + tokenLogSerializer.serialize(token));
										token.deleteToken();
									} else {
										System.out.println("| " + tokenLogSerializer.serialize(token));
									}
								}
								throw new UnsupportedOperationException("Not implemented yet");
							} else {
								JavaToken keepEnd;
								TokenRange previousTokens = observedNodeList.get(index - 1).getTokenRange()
										.orElseThrow();
								keepEnd = previousTokens.getEnd();
								JavaToken removedEnd = removedTokens.getEnd();
								boolean remove = false;
								for (JavaToken token : parentTokens) {
									if (sameTokens(token, keepEnd)) {
										System.out.println("| " + tokenLogSerializer.serialize(token));
										remove = true;
									} else if (sameTokens(token, removedEnd)) {
										System.out.println("X " + tokenLogSerializer.serialize(token));
										token.deleteToken();
										remove = false;
									} else if (remove) {
										System.out.println("X " + tokenLogSerializer.serialize(token));
										token.deleteToken();
									} else {
										System.out.println("| " + tokenLogSerializer.serialize(token));
									}
								}
							}
						} else {
							throw new UnsupportedOperationException("Not implemented yet");
						}
					}

					@Override
					public void listReplacement(NodeList<?> observedNode, int index, Node oldNode, Node newNode) {
						// TODO Auto-generated method stub
						throw new UnsupportedOperationException("Not implemented yet");
					}

					private boolean singleTokenIn(NodeWithTokenRange<?> nodeWithToken) {
						TokenRange tokenRange = nodeWithToken.getTokenRange().orElseThrow();
						return tokenRange.getBegin() == tokenRange.getEnd();
					}
				};
			}
		};
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
				String currentName = variableDeclaration.getNameAsString();
				methodDeclaration.getBody().orElseThrow().stream()//
						.flatMap(filterOnClass(SimpleName.class))//
						.filter(nameNode -> nameNode.getIdentifier().equals(currentName))//
						.forEach(nameNode -> {
							Node parentNode = nameNode.getParentNode().orElseThrow();
							if (parentNode instanceof NameExpr exp) {
								ResolvedValueDeclaration resolved = exp.resolve();
								if (resolved.isVariable()) {
									VariableDeclarationExpr declarations = resolved.toAst(VariableDeclarationExpr.class)
											.orElseThrow();
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
			public Stream<Component.Method> methods() {
				return variableDeclaration.getInitializer().map(expr -> {
					if (expr instanceof ObjectCreationExpr ocExpr) {
						return ocExpr.getAnonymousClassBody().map(body -> {
							if (body instanceof NodeList<?> list) {
								return list.stream().map(node -> {
									if (node instanceof MethodDeclaration decl) {
										return createMethod(decl);
									} else {
										throw new UnsupportedOperationException(
												"Not supported: " + node.getClass().getSimpleName());
									}
								});
							} else {
								throw new UnsupportedOperationException(
										"Not supported: " + body.getClass().getSimpleName());
							}
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
							// TODO Remove added \r\n
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

				variableDeclaration.removeInitializer();
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
							} else if (node instanceof NameExpr exp) {
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
				} else if (statement instanceof LocalClassDeclarationStmt stmt) {
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
				return classDeclaration.stream(TreeTraversal.DIRECT_CHILDREN)//
						.flatMap(filterOnClass(MethodDeclaration.class))//
						.map(methodDeclaration -> {
							return createMethod(methodDeclaration);
						});
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
}
