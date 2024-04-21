package fr.vergne.stanos.core.refactorer;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
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
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import fr.vergne.stanos.core.refactorer.Refactorer.CodeRange;
import fr.vergne.stanos.core.refactorer.Y.Variable;

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

	public static Optional<Variable> searchVariable(String code, String variablePath, StringBuilder refactoringCode, CompilationUnit compilationUnit) {
		if (variablePath.equals("MyClass.myMethod(boolean).myVar%0") //
				|| variablePath.equals("MyClass.myMethod(boolean).myVar%1") //
//				|| variablePath.endsWith("%")//
		) {
			List<Y.Class> defaultPackageClasses = new LinkedList<>();
			List<Y.Interface> defaultPackageInterfaces = new LinkedList<>();
			Y.Package defaultPackage = DefaultSource.createDefaultPackage(defaultPackageClasses, defaultPackageInterfaces);
			Code.Source source = new DefaultSource(defaultPackage);
			X x = new X(new Scope.Context(Scope.root(defaultPackageClasses::add, defaultPackageInterfaces::add)), source, null);
			compilationUnit.accept(new Visitor(code, refactoringCode), x);

			try {
				Y.Variable variable;
				if (variablePath.equals("MyClass.myMethod(boolean).myVar%0")) {
					variable = source.defaultPackage().clazz("MyClass").method("myMethod", List.of("boolean")).variable("myVar", 0);
				} else if (variablePath.equals("MyClass.myMethod(boolean).myVar%1")) {
					variable = source.defaultPackage().clazz("MyClass").method("myMethod", List.of("boolean")).variable("myVar", 1);
				} else if (variablePath.equals("MyClass.myMethod().myVar%")) {
					variable = source.defaultPackage().clazz("MyClass").method("myMethod", List.of()).variable("myVar", 0);
				} else if (variablePath.equals("MyClass.MyChildClass.myMethod().MyInnerClass.myMethod().myVar%")) {
					variable = source.defaultPackage().clazz("MyClass").clazz("MyChildClass").method("myMethod", List.of()).clazz("MyInnerClass").method("myMethod", List.of()).variable("myVar", 0);
				} else if (variablePath.equals("MyClass.myMethod(boolean).myVar%")) {
					variable = source.defaultPackage().clazz("MyClass").method("myMethod", List.of("boolean")).variable("myVar", 0);
				} else if (variablePath.equals("MyClass.myMethod().myVar%.myMethod().myVar%")) {
					variable = source.defaultPackage().clazz("MyClass").method("myMethod", List.of()).variable("myVar", 0).method("myMethod", List.of()).variable("myVar", 0);
				} else {
					throw new UnsupportedOperationException("Not implemented yet: " + variablePath);
				}
				return Optional.of(variable);
			} catch (Exception cause) {
				throw new NoSuchElementException("No variable " + variablePath, cause);
			}
		} else {
			List<Signature> signatures = new LinkedList<>();
			feedSignatures(variablePath, signatures);
			String variableName = ((VariableSignature) signatures.get(signatures.size() - 1)).name();

			SearchContext context = new SearchContext();
			compilationUnit.accept(new VoidVisitorAdapter<SearchContext>() {
				@Override
				public void visit(PackageDeclaration decl, SearchContext ctx) {
					super.visit(decl, ctx);
				}

				@Override
				public void visit(ClassOrInterfaceDeclaration decl, SearchContext ctx) {
					if (signatures.isEmpty()) {
						// Already where we target, let's look around
						super.visit(decl, ctx);
					} else if (signatures.get(0).correspondsTo(decl)) {
						// On the path
						Signature signature = signatures.remove(0);
						super.visit(decl, ctx);
						signatures.add(0, signature);
					} else {
						// Not yet where we target
					}
				}

				@Override
				public void visit(MethodDeclaration decl, SearchContext ctx) {
					if (signatures.isEmpty()) {
						// Already where we target, let's look around
						super.visit(decl, ctx);
					} else if (signatures.get(0).correspondsTo(decl)) {
						// On the path
						Signature signature = signatures.remove(0);
						super.visit(decl, ctx);
						signatures.add(0, signature);
					} else {
						// Not yet where we target
					}
				}

				@Override
				public void visit(VariableDeclarationExpr decl, SearchContext ctx) {
					if (signatures.isEmpty()) {
						// Already where we target, let's look around
						super.visit(decl, ctx);
					} else if (signatures.get(0).correspondsTo(decl.getVariable(0))) {
						// TODO Support variables after 0
						// On the path
						Signature signature = signatures.remove(0);
						if (signatures.isEmpty()) {
							// At the end of the path
							JavaToken nameToken = stream(tokensOf(decl))//
									.filter(is(Category.IDENTIFIER))//
									.skip(1)// Skip type
									.findFirst().orElseThrow();
							ctx.nameTokens.add(nameToken);
						} else {
							System.out.println("v " + decl);
							// Continue going down the path
							super.visit(decl, ctx);
							signatures.add(0, signature);
						}
					} else {
						// Not yet where we target
					}
				}

				@Override
				public void visit(NameExpr decl, SearchContext ctx) {
					if (signatures.isEmpty() && decl.getNameAsString().equals(variableName)) {
						JavaToken token = decl.getTokenRange().orElseThrow().getBegin();
						ctx.nameTokens.add(token);
					} else {
						// Not yet where we target
					}
				}
			}, context);
			if (context.nameTokens.isEmpty()) {
				throw new NoSuchElementException("No variable " + variablePath);
			} else {
				Optional<Variable> variableOpt = Optional.of(new Y.Variable() {

					@Override
					public String name() {
						throw new UnsupportedOperationException("Not implemented here");
					}

					@Override
					public void rename(String newName) {
						context.nameTokens.stream()//
								.map(JavaToken::getRange)//
								.map(Optional<Range>::orElseThrow)//
								// FIXME code and refactoringCode uncorrelated after 1 operation
								// TODO Retrieve the ranges at parsing then update them
								.map(range -> tokenRangeToCodeRange(code, range))//
								// Process from last to first, so the ranges are not shifted
								.sorted(Comparator.comparing(CodeRange::start).reversed())//
								.collect(() -> refactoringCode, (builder, nameRange) -> {
									builder.replace(nameRange.start(), nameRange.end() + 1, newName);
								}, (b1, b2) -> {
									throw new UnsupportedOperationException("Combiner not supported");
								});
					}

					@Override
					public Stream<Method> methods() {
						// TODO Auto-generated method stub
						throw new UnsupportedOperationException("Not implemented yet");
					}
				});
				return variableOpt;
			}
		}
	}

	private static void feedSignatures(String path, List<Signature> signatures) {
		int sepIndex = path.indexOf(".");
		if (sepIndex == -1) {
			signatures.add(Signature.parse(path));
		} else {
			String element = path.substring(0, sepIndex);
			String remaining = path.substring(sepIndex + 1);
			signatures.add(Signature.parse(element));

			feedSignatures(remaining, signatures);
		}
	}

	private static interface Signature {
		boolean correspondsTo(Node decl);

		public static Signature parse(String element) {
			Signature signature;
			if (VariableSignature.matches(element)) {
				signature = VariableSignature.parse(element);
			} else if (MethodSignature.matches(element)) {
				signature = MethodSignature.parse(element);
			} else if (ClassSignature.matches(element)) {
				signature = ClassSignature.parse(element);
			} else {
				throw new UnsupportedOperationException("Not supported: " + element);
			}
			return signature;
		}
	}

	private static record ClassSignature(String name) implements Signature {
		public static boolean matches(String element) {
			return element.matches("[a-zA-Z0-9]+");
		}

		@Override
		public boolean correspondsTo(Node node) {
			return node instanceof ClassOrInterfaceDeclaration decl //
					&& name.equals(decl.getNameAsString());
		}

		public static ClassSignature parse(String className) {
			return new ClassSignature(className);
		}
	}

	private static record VariableSignature(String name, int index) implements Signature {
		public static boolean matches(String element) {
			return element.matches("[a-zA-Z0-9]+%([0-9]+)?");
		}

		@Override
		public boolean correspondsTo(Node node) {
			return node instanceof VariableDeclarator decl //
					&& name.equals(decl.getNameAsString());
		}

		public static VariableSignature parse(String variableName) {
			int sepIndex = variableName.indexOf("%");
			String name = variableName.substring(0, sepIndex);
			int index;
			if (sepIndex == variableName.length() - 1) {
				index = 0;
			} else {
				index = Integer.parseInt(variableName.substring(sepIndex + 1));
			}
			return new VariableSignature(name, index);
		}
	}

	private static record MethodSignature(String name, List<String> parameterTypes) implements Signature {
		public static boolean matches(String element) {
			return element.matches("[a-zA-Z0-9]+\\(.*\\)");
		}

		@Override
		public boolean correspondsTo(Node node) {
			return node instanceof MethodDeclaration decl //
					&& name.equals(decl.getNameAsString()) //
					&& parameterTypesFromDecl(decl.getParameters()).equals(parameterTypes);
		}

		public static MethodSignature parse(String methodSignature) {
			int argsStart = methodSignature.indexOf("(") + 1;
			int argsEnd = methodSignature.indexOf(")", argsStart);
			String methodName = methodSignature.substring(0, argsStart - 1);
			List<String> parameterTypes;
			if (argsStart == argsEnd) {
				parameterTypes = Collections.emptyList();
			} else {
				parameterTypes = Stream.of(methodSignature.substring(argsStart, argsEnd).split(",")).toList();
			}
			return new MethodSignature(methodName, parameterTypes);
		}
	}

	private static List<String> parameterTypesFromDecl(NodeList<Parameter> parameters) {
		return StreamSupport.stream(parameters.spliterator(), false)//
				.map(Parameter::getType)//
				.map(Type::asString)//
				.toList();
	}
}
