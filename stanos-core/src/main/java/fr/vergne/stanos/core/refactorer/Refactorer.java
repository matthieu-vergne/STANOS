package fr.vergne.stanos.core.refactorer;

import static java.util.stream.Collectors.toMap;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.Node.TreeTraversal;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;

import fr.vergne.stanos.core.refactorer.Y.Class;
import fr.vergne.stanos.core.refactorer.Y.Field;
import fr.vergne.stanos.core.refactorer.Y.Interface;
import fr.vergne.stanos.core.refactorer.Y.Method;
import fr.vergne.stanos.core.refactorer.Y.Package;
import fr.vergne.stanos.core.refactorer.Y.Record;
import fr.vergne.stanos.core.refactorer.Y.Variable;

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
						return createRecordStream(compilationUnit, defaultRecords);
					}

					@Override
					public Stream<Y.Interface> interfaces() {
						Map<String, Y.Interface> defaultInterfaces = defaultPackage.interfaces().collect(toMap(r -> r.name(), r -> r));
						return createInterfaceStream(compilationUnit, defaultInterfaces);
					}

					@Override
					public Stream<Y.Class> classes() {
						Map<String, Y.Class> defaultClasses = defaultPackage.classes().collect(toMap(r -> r.name(), r -> r));
						return createClassStream(compilationUnit, defaultClasses);
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

	private static Stream<fr.vergne.stanos.core.refactorer.Y.Parameter> createParameterStream(com.github.javaparser.ast.body.MethodDeclaration methodDeclaration) {
		return methodDeclaration.stream(TreeTraversal.DIRECT_CHILDREN)//
				.flatMap(filterOnClass(com.github.javaparser.ast.body.Parameter.class))//
				.map(parameterDeclaration -> {
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
				});
	}

	// FIXME Remove default map
	private static Stream<Method> createMethodStream(ClassOrInterfaceDeclaration classDeclaration, Map<String, Y.Method> defaultMethods) {
		return classDeclaration.stream(TreeTraversal.DIRECT_CHILDREN)//
				.flatMap(filterOnClass(com.github.javaparser.ast.body.MethodDeclaration.class))//
				.map(methodDeclaration -> {
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
							return createParameterStream(methodDeclaration);
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
							Map<String, Class> defaultClasses = defaultMethods.get(name + parameterTypes).classes().collect(toMap(r -> r.name(), r -> r));
							return methodDeclaration.getBody()//
									.map(body -> body.stream(TreeTraversal.DIRECT_CHILDREN)//
											.flatMap(filterOnClass(LocalClassDeclarationStmt.class))//
											.flatMap(localClassStatement -> createClassStream(localClassStatement, defaultClasses)))//
									.orElseGet(Stream::empty);
						}

					};
				});
	}

	// FIXME Remove default map
	private static Stream<Class> createClassStream(Node parentNode, Map<String, Y.Class> defaultClasses) {
		return parentNode.stream(TreeTraversal.DIRECT_CHILDREN)//
				.flatMap(filterOnClass(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class))//
				.filter(decl -> !decl.isInterface())//
				.map(classDeclaration -> {
					String name = classDeclaration.getNameAsString();
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
							return createMethodStream(classDeclaration, defaultMethods);
						}

						@Override
						// method
						public Stream<Y.Class> classes() {
							Map<String, Y.Class> defaultClasses2 = defaultClasses.get(name).classes().collect(toMap(r -> r.name(), r -> r));
							return createClassStream(classDeclaration, defaultClasses2);
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

	// FIXME Remove default map
	private static Stream<Interface> createInterfaceStream(Node parentNode, Map<String, Y.Interface> defaultInterfaces) {
		return parentNode.stream(TreeTraversal.DIRECT_CHILDREN)//
				.flatMap(filterOnClass(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class))//
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
							return createMethodStream(interfaceDeclaration, defaultMethods);
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

	// FIXME Remove default map
	private static Stream<Record> createRecordStream(Node parentNode, Map<String, Y.Record> defaultRecords) {
		return parentNode.stream(TreeTraversal.DIRECT_CHILDREN)//
				.flatMap(filterOnClass(com.github.javaparser.ast.body.RecordDeclaration.class))//
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

	private static <T> Function<? super Node, Stream<T>> filterOnClass(java.lang.Class<T> clazz) {
		return node -> clazz.isInstance(node) ? Stream.of(clazz.cast(node)) : Stream.empty();
	}
}
