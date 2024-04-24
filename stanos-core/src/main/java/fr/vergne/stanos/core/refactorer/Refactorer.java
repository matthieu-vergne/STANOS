package fr.vergne.stanos.core.refactorer;

import static java.util.stream.Collectors.toMap;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node.TreeTraversal;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import fr.vergne.stanos.core.refactorer.Y.Field;
import fr.vergne.stanos.core.refactorer.Y.Package;
import fr.vergne.stanos.core.refactorer.Y.Variable;

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
		Code.Source defaultSource = new DefaultSource(defaultPackage);
		Scope root = Scope.root(defaultPackageClasses::add, defaultPackageInterfaces::add, defaultPackageRecords::add);
		X x = new X(new Scope.Context(root), defaultSource);
		try {
			compilationUnit.accept(new Visitor(), x);
		} catch (Exception cause) {
			// TODO Remove visitor
		}
		return new Code.Source() {

			@Override
			public Package defaultPackage() {
				Package defaultPackage = defaultSource.defaultPackage();
				return new Y.Package() {

					@Override
					public Stream<Y.Record> records() {
						Map<String, Y.Record> defaultRecords = defaultPackage.records().collect(toMap(r -> r.name(), r -> r));
						return compilationUnit.stream(TreeTraversal.DIRECT_CHILDREN)//
								.filter(node -> node instanceof com.github.javaparser.ast.body.RecordDeclaration)//
								.map(node -> (com.github.javaparser.ast.body.RecordDeclaration) node)//
								.map(decl -> {
									String name = decl.getFullyQualifiedName().orElseThrow();
									return new Y.Record() {
										@Override
										public String name() {
											return name;
										}

										@Override
										public void rename(String newName) {
											defaultRecords.get(name).rename(newName);
										}
									};
								});
					}

					@Override
					public Stream<Y.Interface> interfaces() {
						Map<String, Y.Interface> defaultInterfaces = defaultPackage.interfaces().collect(toMap(r -> r.name(), r -> r));
						return compilationUnit.stream(TreeTraversal.DIRECT_CHILDREN)//
								.filter(node -> node instanceof com.github.javaparser.ast.body.ClassOrInterfaceDeclaration)//
								.map(node -> (com.github.javaparser.ast.body.ClassOrInterfaceDeclaration) node)//
								.filter(decl -> decl.isInterface())//
								.map(interfaceDeclaration -> {
									String name = interfaceDeclaration.getFullyQualifiedName().orElseThrow();
									return new Y.Interface() {
										@Override
										public String name() {
											return name;
										}

										@Override
										public void rename(String newName) {
											defaultInterfaces.get(name).rename(newName);
										}

										@Override
										public Stream<Y.Method> methods() {
											Map<String, Y.Method> defaultMethods = defaultInterfaces.get(name).methods().collect(toMap(r -> r.name(), r -> r));
											return interfaceDeclaration.stream(TreeTraversal.DIRECT_CHILDREN)//
													.filter(node -> node instanceof com.github.javaparser.ast.body.MethodDeclaration)//
													.map(node -> (com.github.javaparser.ast.body.MethodDeclaration) node)//
													.map(methodDeclaration -> {
														System.out.println("::: " + methodDeclaration.getClass().getSimpleName());
														String name = methodDeclaration.getNameAsString();
														return new Y.Method() {
															@Override
															public String name() {
																return name;
															}

															@Override
															public List<String> parameterTypes() {
																return defaultMethods.get(name).parameterTypes();
															}

															@Override
															public Stream<Y.Parameter> parameters() {
																return defaultMethods.get(name).parameters();
															}

															@Override
															public Stream<Variable> variables() {
																// TODO Auto-generated method stub
																throw new UnsupportedOperationException("Not implemented yet");
															}

															@Override
															public void rename(String newName) {
																// TODO Auto-generated method stub
																throw new UnsupportedOperationException("Not implemented yet");
															}

															@Override
															public Stream<Y.Interface> interfaces() {
																// TODO Auto-generated method stub
																throw new UnsupportedOperationException("Not implemented yet");
															}

															@Override
															public Stream<Y.Class> classes() {
																// TODO Auto-generated method stub
																throw new UnsupportedOperationException("Not implemented yet");
															}
														};
													});
										}

										@Override
										public Stream<Y.Class> classes() {
											throw new UnsupportedOperationException("Not implemented yet");
										}

										@Override
										public Stream<Y.Interface> interfaces() {
											throw new UnsupportedOperationException("Not implemented yet");
										}
									};
								});
					}

					@Override
					public Stream<Y.Class> classes() {
						Map<String, Y.Class> defaultClasses = defaultPackage.classes().collect(toMap(r -> r.name(), r -> r));
						return compilationUnit.stream(TreeTraversal.DIRECT_CHILDREN)//
								.filter(node -> node instanceof com.github.javaparser.ast.body.ClassOrInterfaceDeclaration)//
								.map(node -> (com.github.javaparser.ast.body.ClassOrInterfaceDeclaration) node)//
								.filter(decl -> !decl.isInterface())//
								.map(classDeclaration -> {
									String name = classDeclaration.getFullyQualifiedName().orElseThrow();
									return new Y.Class() {
										@Override
										public String name() {
											return name;
										}

										@Override
										public void rename(String newName) {
											defaultClasses.get(name).rename(newName);
										}

										@Override
										public Stream<Y.Method> methods() {
											Map<String, Y.Method> defaultMethods = defaultClasses.get(name).methods().collect(toMap(r -> r.name() + r.parameterTypes(), r -> r));
											return classDeclaration.stream(TreeTraversal.DIRECT_CHILDREN)//
													.filter(node -> node instanceof com.github.javaparser.ast.body.MethodDeclaration)//
													.map(node -> (com.github.javaparser.ast.body.MethodDeclaration) node)//
													.map(methodDeclaration -> {
														System.out.println("::: " + methodDeclaration.getClass().getSimpleName());
														String name = methodDeclaration.getNameAsString();
														List<String> parameterTypes = methodDeclaration.getParameters().stream().map(x -> x.getTypeAsString()).toList();
														return new Y.Method() {
															@Override
															public String name() {
																return name;
															}

															@Override
															public List<String> parameterTypes() {
																return parameterTypes;
															}

															@Override
															public void rename(String newName) {
																defaultMethods.get(name + parameterTypes).rename(newName);
															}

															@Override
															public Stream<Y.Parameter> parameters() {
																Map<String, Y.Parameter> defaultParameters = defaultMethods.get(name + parameterTypes).parameters().collect(toMap(r -> r.name(), r -> r));
																return methodDeclaration.stream(TreeTraversal.DIRECT_CHILDREN)//
																		.filter(node -> node instanceof com.github.javaparser.ast.body.Parameter)//
																		.map(node -> (com.github.javaparser.ast.body.Parameter) node)//
																		.map(parameterDeclaration -> {
																			String name = parameterDeclaration.getNameAsString();
																			return new Y.Parameter() {
																				@Override
																				public String name() {
																					return name;
																				}

																				@Override
																				public void rename(String newName) {
																					// FIXME
																					defaultParameters.get(name).rename(newName);
																				}
																			};
																		});
															}

															@Override
															public Stream<Variable> variables() {
																return defaultMethods.get(name + parameterTypes).variables();
															}

															@Override
															public Stream<Y.Interface> interfaces() {
																// TODO Auto-generated method stub
																throw new UnsupportedOperationException("Not implemented yet");
															}

															@Override
															public Stream<Y.Class> classes() {
																// TODO Auto-generated method stub
																throw new UnsupportedOperationException("Not implemented yet");
															}
														};
													});
										}

										@Override
										public Stream<Y.Class> classes() {
											return defaultClasses.get(name).classes();
										}

										@Override
										public Stream<Y.Interface> interfaces() {
											throw new UnsupportedOperationException("Not implemented yet");
										}

										@Override
										public Stream<Field> fields() {
											return defaultClasses.get(name).fields();
										}
									};
								});
					}

					@Override
					public void rename(String newName) {
						throw new UnsupportedOperationException("Not implemented yet");
					}

					@Override
					public String name() {
						throw new UnsupportedOperationException("Not implemented yet");
					}
				};
			}

			@Override
			public ClassDeclaration getClassDeclaration(String name) {
				throw new UnsupportedOperationException("Not implemented yet");
			}

			@Override
			public RecordDeclaration getRecordDeclaration(String name) {
				throw new UnsupportedOperationException("Not implemented yet");
			}

			@Override
			public RecordDeclaration createRecordDeclaration() {
				throw new UnsupportedOperationException("Not implemented yet");
			}

			@Override
			public InterfaceDeclaration createInterfaceDeclaration() {
				throw new UnsupportedOperationException("Not implemented yet");
			}

			@Override
			public ClassDeclaration createClassDeclaration() {
				throw new UnsupportedOperationException("Not implemented yet");
			}

			@Override
			public ImportDeclaration createImportDeclaration() {
				throw new UnsupportedOperationException("Not implemented yet");
			}

			@Override
			public PackageDeclaration createPackageDeclaration() {
				throw new UnsupportedOperationException("Not implemented yet");
			}

			@Override
			public List<Code> subCodes() {
				throw new UnsupportedOperationException("Not implemented yet");
			}
		};
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
