package fr.vergne.stanos.core.refactorer;

import static java.util.Collections.emptyList;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import fr.vergne.stanos.core.refactorer.Y.Package;

class DefaultSource implements Code.Source {
	Supplier<Code.PackageDeclaration> packageFactory = () -> new Code.PackageDeclaration() {
		List<Code> subcodes = new LinkedList<>();

		@Override
		public List<Code> subCodes() {
			return subcodes;
		}

		@Override
		public String toString() {
			return stringOf(this);
		}

		@Override
		public ClassDeclaration createClassDeclaration(int codeIndex) {
			ClassDeclaration subcode = classFactory.apply(codeIndex);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public Code.ClassDeclaration getClassDeclaration(String name) {
			for (Code subcode : subcodes) {
				if (subcode instanceof Code.ClassDeclaration decl && decl.name().equals(name)) {
					return decl;
				}
			}
			throw new NoSuchElementException("No class declaration: " + name);
		}

		@Override
		public InterfaceDeclaration createInterfaceDeclaration(int codeIndex) {
			InterfaceDeclaration subcode = interfaceFactory.apply(codeIndex);
			subcodes.add(subcode);
			return subcode;
		}
	};
	Supplier<Code.ImportDeclaration> importDeclarationFactory = () -> new Code.ImportDeclaration() {
		List<Code> subcodes = new LinkedList<>();

		@Override
		public List<Code> subCodes() {
			return subcodes;
		}

		@Override
		public String toString() {
			return stringOf(this);
		}

		@Override
		public Name createName(String identifier) {
			Name subcode = nameFactory.apply(identifier);
			subcodes.add(subcode);
			return subcode;
		}
	};
	Function<Integer, Code.ClassDeclaration> classFactory = (codeIndex) -> new Code.ClassDeclaration() {
		List<Code> subcodes = new LinkedList<>();

		@Override
		public List<Code> subCodes() {
			return subcodes;
		}

		@Override
		public String toString() {
			return stringOf(this, name());
		}

		@Override
		public ClassDeclaration createClassDeclaration(int codeIndex) {
			ClassDeclaration subcode = classFactory.apply(codeIndex);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public Code.ClassDeclaration getClassDeclaration(String name) {
			for (Code subcode : subcodes) {
				if (subcode instanceof Code.ClassDeclaration decl && decl.name().equals(name)) {
					return decl;
				}
			}
			throw new NoSuchElementException("No class declaration: " + name);
		}

		@Override
		public InterfaceDeclaration createInterfaceDeclaration(int codeIndex) {
			InterfaceDeclaration subcode = interfaceFactory.apply(codeIndex);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public MethodDeclaration createMethodDeclaration(int codeIndex) {
			MethodDeclaration subcode = methodFactory.apply(codeIndex);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public Code.MethodDeclaration getMethodDeclaration(String name, String... parameterTypes) {
			for (Code subcode : subcodes) {
				if (subcode instanceof Code.MethodDeclaration decl //
						&& decl.name().equals(name)//
						&& decl.parameters().map(Parameter::type).toList().equals(Arrays.asList(parameterTypes))) {
					return decl;
				}
			}
			throw new NoSuchElementException("No method declaration: " + name + Arrays.toString(parameterTypes));
		}

		@Override
		public SimpleName createSimpleName(String identifier) {
			SimpleName subcode = simpleNameFactory.apply(identifier);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public String name() {
			for (Code subcode : subcodes) {
				if (subcode instanceof SimpleName name) {
					return name.identifier();
				}
			}
			throw new IllegalStateException("No name");
		}

		@Override
		public int codeIndex() {
			return codeIndex;
		}
	};
	Function<Integer, Code.InterfaceDeclaration> interfaceFactory = (codeIndex) -> new Code.InterfaceDeclaration() {
		List<Code> subcodes = new LinkedList<>();

		@Override
		public List<Code> subCodes() {
			return subcodes;
		}

		@Override
		public String toString() {
			return stringOf(this, name());
		}

		@Override
		public ClassDeclaration createClassDeclaration(int codeIndex) {
			ClassDeclaration subcode = classFactory.apply(codeIndex);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public Code.ClassDeclaration getClassDeclaration(String name) {
			for (Code subcode : subcodes) {
				if (subcode instanceof Code.ClassDeclaration decl && decl.name().equals(name)) {
					return decl;
				}
			}
			throw new NoSuchElementException("No class declaration: " + name);
		}

		@Override
		public InterfaceDeclaration createInterfaceDeclaration(int codeIndex) {
			InterfaceDeclaration subcode = interfaceFactory.apply(codeIndex);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public MethodDeclaration createMethodDeclaration(int codeIndex) {
			MethodDeclaration subcode = methodFactory.apply(codeIndex);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public Code.MethodDeclaration getMethodDeclaration(String name, String... parameterTypes) {
			for (Code subcode : subcodes) {
				if (subcode instanceof Code.MethodDeclaration decl //
						&& decl.name().equals(name)//
						&& decl.parameters().map(Parameter::type).toList().equals(Arrays.asList(parameterTypes))) {
					return decl;
				}
			}
			throw new NoSuchElementException("No method declaration: " + name + Arrays.toString(parameterTypes));
		}

		@Override
		public SimpleName createSimpleName(String identifier) {
			SimpleName subcode = simpleNameFactory.apply(identifier);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public String name() {
			for (Code subcode : subcodes) {
				if (subcode instanceof SimpleName name) {
					return name.identifier();
				}
			}
			throw new IllegalStateException("No name");
		}

		@Override
		public int codeIndex() {
			return codeIndex;
		}
	};
	Function<Code.ModifierContainer.Keyword, Code.Modifier> modifierFactory = (keyword) -> new Code.Modifier() {
		@Override
		public List<Code> subCodes() {
			return emptyList();
		}

		@Override
		public String toString() {
			return stringOf(this, keyword);
		}

		@Override
		public Code.ModifierContainer.Keyword keyword() {
			return keyword;
		}
	};
	Supplier<Code.Void> voidFactory = () -> new Code.Void() {
		@Override
		public List<Code> subCodes() {
			return emptyList();
		}

		@Override
		public String toString() {
			return stringOf(this);
		}
	};
	Function<String, Code.PrimitiveType> primitiveTypeFactory = (keyword) -> new Code.PrimitiveType() {
		@Override
		public List<Code> subCodes() {
			return emptyList();
		}

		@Override
		public String toString() {
			return stringOf(this);
		}

		@Override
		public String keyword() {
			return keyword;
		}
	};
	Supplier<Code.Parameter> parameterFactory = () -> new Code.Parameter() {
		List<Code> subcodes = new LinkedList<>();

		@Override
		public List<Code> subCodes() {
			return subcodes;
		}

		@Override
		public String toString() {
			return stringOf(this, type(), name());
		}

		@Override
		public SimpleName createSimpleName(String identifier) {
			SimpleName subcode = simpleNameFactory.apply(identifier);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public PrimitiveType createPrimitiveType(String keyword) {
			PrimitiveType subcode = primitiveTypeFactory.apply(keyword);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public Type createType() {
			Type subcode = typeFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public String name() {
			for (Code subcode : subcodes) {
				if (subcode instanceof SimpleName name) {
					return name.identifier();
				}
			}
			throw new IllegalStateException("No name");
		}

		@Override
		public String type() {
			for (Code subcode : subcodes) {
				if (subcode instanceof Type type) {
					return type.name();
				}
				if (subcode instanceof PrimitiveType type) {
					return type.keyword();
				}
			}
			throw new IllegalStateException("No type");
		}
	};
	Function<String, Code.Name> nameFactory = (identifier) -> new Code.Name() {
		List<Code.Name> names = new LinkedList<>();

		@Override
		public List<Code> subCodes() {
			return names.stream().map(name -> (Code) name).toList();
		}

		@Override
		public String toString() {
			return stringOf(this, identifiers().toList());
		}

		@Override
		public Name createName(String identifier) {
			Name subcode = nameFactory.apply(identifier);
			names.add(subcode);
			return subcode;
		}

		@Override
		public Stream<String> identifiers() {
			return Stream.concat(names.stream().flatMap(Name::identifiers), Stream.of(identifier));
		}
	};
	Supplier<Code.MarkerAnnotation> markerAnnotationFactory = () -> new Code.MarkerAnnotation() {
		List<Code> subcodes = new LinkedList<>();

		@Override
		public List<Code> subCodes() {
			return subcodes;
		}

		@Override
		public String toString() {
			return stringOf(this);
		}

		@Override
		public Name createName(String identifier) {
			Name subcode = nameFactory.apply(identifier);
			subcodes.add(subcode);
			return subcode;
		}
	};
	Function<Integer, Code.MethodDeclaration> methodFactory = (codeIndex) -> new Code.MethodDeclaration() {
		List<Code> subcodes = new LinkedList<>();

		@Override
		public List<Code> subCodes() {
			return subcodes;
		}

		@Override
		public String toString() {
			return stringOf(this, returnType().orElse("void"), name(), parameters().map(Parameter::type).toList(), "vars:" + variables().count());
		}

		@Override
		public ClassDeclaration createClassDeclaration(int codeIndex) {
			ClassDeclaration subcode = classFactory.apply(codeIndex);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public Code.ClassDeclaration getClassDeclaration(String name) {
			for (Code subcode : subcodes) {
				if (subcode instanceof Code.ClassDeclaration decl && decl.name().equals(name)) {
					return decl;
				}
			}
			throw new NoSuchElementException("No class declaration: " + name);
		}

		@Override
		public ObjectCreation createObjectCreation() {
			ObjectCreation subcode = objectCreationFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public Block createBlock() {
			Block subcode = blockFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public Type createType() {
			Type subcode = typeFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public SimpleName createSimpleName(String identifier) {
			SimpleName subcode = simpleNameFactory.apply(identifier);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public Modifier createModifier(Code.ModifierContainer.Keyword keyword) {
			Modifier subcode = modifierFactory.apply(keyword);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public Void createVoid() {
			Void subcode = voidFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public Parameter createParameter() {
			Parameter subcode = parameterFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public MarkerAnnotation createMarkerAnnotation() {
			MarkerAnnotation subcode = markerAnnotationFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public String name() {
			for (Code subcode : subcodes) {
				if (subcode instanceof SimpleName name) {
					return name.identifier();
				}
			}
			throw new IllegalStateException("No name");
		}

		@Override
		public Optional<String> returnType() {
			for (Code subcode : subcodes) {
				if (subcode instanceof Type type) {
					return Optional.of(type.name());
				}
			}
			return Optional.empty();
		}

		@Override
		public Stream<Parameter> parameters() {
			return subcodes.stream()//
					.filter(subcode -> subcode instanceof Parameter)//
					.map(subcode -> (Parameter) subcode);
		}

		@Override
		public Stream<Code.VariableDeclaration> variables() {
			return subcodes.stream()//
					.filter(subcode -> subcode instanceof Block)//
					.flatMap(subcode -> ((Block) subcode).variables());
		}

		@Override
		public int codeIndex() {
			return codeIndex;
		}
	};
	Function<String, Code.SimpleName> simpleNameFactory = (identifier) -> new Code.SimpleName() {
		@Override
		public List<Code> subCodes() {
			return emptyList();
		}

		@Override
		public String toString() {
			return stringOf(this, identifier);
		}

		@Override
		public String identifier() {
			return identifier;
		}
	};
	Supplier<Code.Type> typeFactory = () -> new Code.Type() {
		List<Code> subcodes = new LinkedList<>();

		@Override
		public List<Code> subCodes() {
			return subcodes;
		}

		@Override
		public String toString() {
			List<String> genericTypes = genericTypes().toList();
			if (genericTypes.isEmpty()) {
				return stringOf(this, name());
			} else {
				return stringOf(this, name(), genericTypes);
			}
		}

		@Override
		public SimpleName createSimpleName(String identifier) {
			SimpleName subcode = simpleNameFactory.apply(identifier);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public Type createType() {
			Type subcode = typeFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public String name() {
			for (Code subcode : subcodes) {
				if (subcode instanceof SimpleName name) {
					return name.identifier();
				}
			}
			throw new IllegalStateException("No name");
		}

		@Override
		public Stream<String> genericTypes() {
			return subcodes.stream()//
					.filter(subcode -> subcode instanceof Type)//
					.map(subcode -> ((Type) subcode).name());
		}
	};
	Supplier<Code.NameExpr> nameExpFactory = () -> new Code.NameExpr() {
		List<Code> subcodes = new LinkedList<>();

		@Override
		public List<Code> subCodes() {
			return subcodes;
		}

		@Override
		public String toString() {
			return stringOf(this, name());
		}

		@Override
		public SimpleName createSimpleName(String identifier) {
			SimpleName subcode = simpleNameFactory.apply(identifier);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public String name() {
			for (Code subcode : subcodes) {
				if (subcode instanceof SimpleName name) {
					return name.identifier();
				}
			}
			throw new IllegalStateException("No name");
		}
	};
	Supplier<Code.Return> returnFactory = () -> new Code.Return() {
		List<Code> subcodes = new LinkedList<>();

		@Override
		public List<Code> subCodes() {
			return subcodes;
		}

		@Override
		public String toString() {
			return stringOf(this);
		}

		@Override
		public NameExpr createNameExpr() {
			NameExpr subcode = nameExpFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public NullLiteral createNullLiteral() {
			NullLiteral subcode = nullLiteralFactory.get();
			subcodes.add(subcode);
			return subcode;
		}
	};
	Supplier<Code.LocalClassDeclaration> localClassFactory = () -> new Code.LocalClassDeclaration() {
		List<Code> subcodes = new LinkedList<>();

		@Override
		public List<Code> subCodes() {
			return subcodes;
		}

		@Override
		public String toString() {
			return stringOf(this);
		}

		@Override
		public ClassDeclaration createClassDeclaration(int codeIndex) {
			ClassDeclaration subcode = classFactory.apply(codeIndex);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public Code.ClassDeclaration getClassDeclaration(String name) {
			for (Code subcode : subcodes) {
				if (subcode instanceof Code.ClassDeclaration decl && decl.name().equals(name)) {
					return decl;
				}
			}
			throw new NoSuchElementException("No class declaration: " + name);
		}

		@Override
		public InterfaceDeclaration createInterfaceDeclaration(int codeIndex) {
			InterfaceDeclaration subcode = interfaceFactory.apply(codeIndex);
			subcodes.add(subcode);
			return subcode;
		}
	};
	Supplier<Code.If> ifFactory = () -> new Code.If() {
		List<Code> subcodes = new LinkedList<>();

		@Override
		public List<Code> subCodes() {
			return subcodes;
		}

		@Override
		public String toString() {
			return stringOf(this, "vars:" + variables().count());
		}

		@Override
		public NameExpr createNameExpr() {
			NameExpr subcode = nameExpFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public Block createBlock() {
			Block subcode = blockFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public Stream<Code.VariableDeclaration> variables() {
			return subcodes.stream()//
					.filter(subcode -> subcode instanceof Block)//
					.flatMap(subcode -> ((Block) subcode).variables());
		}
	};
	Supplier<Code.Block> blockFactory = () -> new Code.Block() {
		List<Code> subcodes = new LinkedList<>();

		@Override
		public List<Code> subCodes() {
			return subcodes;
		}

		@Override
		public String toString() {
			return stringOf(this, "vars:" + variables().count());
		}

		@Override
		public Block createBlock() {
			Block subcode = blockFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public ObjectCreation createObjectCreation() {
			ObjectCreation subcode = objectCreationFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public Expression createExpression() {
			Expression subcode = expressionFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public Return createReturn() {
			Return subcode = returnFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public LocalClassDeclaration createLocalClassDeclaration() {
			LocalClassDeclaration subcode = localClassFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public If createIf() {
			If subcode = ifFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public Stream<Code.VariableDeclaration> variables() {
			return subcodes.stream()//
					.filter(subcode -> subcode instanceof Block //
							|| subcode instanceof If//
							|| subcode instanceof Expression)//
					.flatMap(subcode -> {
						if (subcode instanceof Block block) {
							return block.variables();
						} else if (subcode instanceof If ifCode) {
							return ifCode.variables();
						} else if (subcode instanceof Expression expression) {
							Optional<VariableDeclaration> decl = expression.variable();
							return decl.isPresent() ? Stream.of(decl.get()) : Stream.empty();
						} else {
							throw new UnsupportedOperationException("Not supported: " + subcode);
						}
					});
		}
	};
	Supplier<Code.BinaryExpr> binaryExprFactory = () -> new Code.BinaryExpr() {
		List<Code> subcodes = new LinkedList<>();

		@Override
		public List<Code> subCodes() {
			return subcodes;
		}

		@Override
		public String toString() {
			return stringOf(this);
		}

		@Override
		public BinaryExpr createBinaryExpr() {
			BinaryExpr subcode = binaryExprFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public StringLiteral createStringLiteral(String value) {
			StringLiteral subcode = stringLiteralFactory.apply(value);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public NameExpr createNameExpr() {
			NameExpr subcode = nameExpFactory.get();
			subcodes.add(subcode);
			return subcode;
		}
	};
	Supplier<Code.MethodCall> methodCallFactory = () -> new Code.MethodCall() {
		List<Code> subcodes = new LinkedList<>();

		@Override
		public List<Code> subCodes() {
			return subcodes;
		}

		@Override
		public String toString() {
			return stringOf(this, name());
		}

		@Override
		public NameExpr createNameExpr() {
			NameExpr subcode = nameExpFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public SimpleName createSimpleName(String identifier) {
			SimpleName subcode = simpleNameFactory.apply(identifier);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public String name() {
			for (Code subcode : subcodes) {
				if (subcode instanceof SimpleName name) {
					return name.identifier();
				}
			}
			throw new IllegalStateException("No name");
		}
	};
	Supplier<Code.AssignExpr> assignExprFactory = () -> new Code.AssignExpr() {
		List<Code> subcodes = new LinkedList<>();

		@Override
		public List<Code> subCodes() {
			return subcodes;
		}

		@Override
		public String toString() {
			return stringOf(this);
		}

		@Override
		public NameExpr createNameExpr() {
			NameExpr subcode = nameExpFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public StringLiteral createStringLiteral(String value) {
			StringLiteral subcode = stringLiteralFactory.apply(value);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public BinaryExpr createBinaryExpr() {
			BinaryExpr subcode = binaryExprFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public MethodCall createMethodCall() {
			MethodCall subcode = methodCallFactory.get();
			subcodes.add(subcode);
			return subcode;
		}
	};
	Supplier<Code.Expression> expressionFactory = () -> new Code.Expression() {
		List<Code> subcodes = new LinkedList<>();

		@Override
		public List<Code> subCodes() {
			return subcodes;
		}

		@Override
		public String toString() {
			return stringOf(this, "vars:" + (variable().isPresent() ? 1 : 0));
		}

		@Override
		public VariableDeclaration createVariableDeclaration() {
			VariableDeclaration subcode = variableDeclarationFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public Optional<VariableDeclaration> variable() {
			return subcodes.stream()//
					.filter(subcode -> subcode instanceof VariableDeclaration)//
					.map(subcode -> (VariableDeclaration) subcode)//
					.findFirst();
		}

		@Override
		public AssignExpr createAssignExpr() {
			AssignExpr subcode = assignExprFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public NameExpr createNameExpr() {
			NameExpr subcode = nameExpFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public LambdaExpr createLambdaExpr() {
			LambdaExpr subcode = lambdaExprFactory.get();
			subcodes.add(subcode);
			return subcode;
		}
	};
	Supplier<Code.VariableDeclaration> variableDeclarationFactory = () -> new Code.VariableDeclaration() {
		List<Code> subcodes = new LinkedList<>();

		@Override
		public List<Code> subCodes() {
			return subcodes;
		}

		@Override
		public String toString() {
			return stringOf(this);
		}

		@Override
		public VariableDeclarator createVariableDeclarator(int codeIndex) {
			VariableDeclarator subcode = variableDeclaratorFactory.apply(codeIndex);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public VariableDeclarator declarator() {
			return (VariableDeclarator) subcodes.get(0);
		}
	};
	Function<String, Code.StringLiteral> stringLiteralFactory = (value) -> new Code.StringLiteral() {
		@Override
		public List<Code> subCodes() {
			return emptyList();
		}

		@Override
		public String toString() {
			return stringOf(this, '"' + value + '"');
		}

		@Override
		public String value() {
			return value;
		}
	};
	Supplier<Code.NullLiteral> nullLiteralFactory = () -> new Code.NullLiteral() {
		@Override
		public List<Code> subCodes() {
			return emptyList();
		}

		@Override
		public String toString() {
			return stringOf(this);
		}
	};
	Supplier<Code.LambdaExpr> lambdaExprFactory = () -> new Code.LambdaExpr() {
		List<Code> subcodes = new LinkedList<>();

		@Override
		public List<Code> subCodes() {
			return subcodes;
		}

		@Override
		public String toString() {
			return stringOf(this);
		}

		@Override
		public Expression createExpression() {
			Expression subcode = expressionFactory.get();
			subcodes.add(subcode);
			return subcode;
		}
	};
	Function<Integer, Code.VariableDeclarator> variableDeclaratorFactory = (codeIndex) -> new Code.VariableDeclarator() {
		List<Code> subcodes = new LinkedList<>();

		@Override
		public List<Code> subCodes() {
			return subcodes;
		}

		@Override
		public String toString() {
			return stringOf(this, type(), name(), typeStringOf(value()));
		}

		@Override
		public ObjectCreation createObjectCreation() {
			ObjectCreation subcode = objectCreationFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public StringLiteral createStringLiteral(String value) {
			StringLiteral subcode = stringLiteralFactory.apply(value);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public SimpleName createSimpleName(String identifier) {
			SimpleName subcode = simpleNameFactory.apply(identifier);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public Type createType() {
			Type subcode = typeFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public NullLiteral createNullLiteral() {
			NullLiteral subcode = nullLiteralFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public NameExpr createNameExpr() {
			NameExpr subcode = nameExpFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public LambdaExpr createLambdaExpr() {
			LambdaExpr subcode = lambdaExprFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public String name() {
			for (Code subcode : subcodes) {
				if (subcode instanceof SimpleName name) {
					return name.identifier();
				}
			}
			throw new IllegalStateException("No name");
		}

		@Override
		public String type() {
			for (Code subcode : subcodes) {
				if (subcode instanceof Type type) {
					return type.name();
				}
			}
			throw new IllegalStateException("No type");
		}

		@Override
		public Code value() {
			for (Code subcode : subcodes) {
				if (subcode instanceof SimpleName || subcode instanceof Type) {
					continue;
				} else {
					return subcode;
				}
			}
			throw new IllegalStateException("No value");
		}

		@Override
		public int codeIndex() {
			return codeIndex;
		}
	};
	Supplier<Code.ObjectCreation> objectCreationFactory = () -> new Code.ObjectCreation() {
		List<Code> subcodes = new LinkedList<>();

		@Override
		public List<Code> subCodes() {
			return subcodes;
		}

		@Override
		public String toString() {
			return stringOf(this, type());
		}

		@Override
		public MethodDeclaration createMethodDeclaration(int codeIndex) {
			MethodDeclaration subcode = methodFactory.apply(codeIndex);
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public Code.MethodDeclaration getMethodDeclaration(String name, String... parameterTypes) {
			for (Code subcode : subcodes) {
				if (subcode instanceof Code.MethodDeclaration decl //
						&& decl.name().equals(name)//
						&& decl.parameters().map(Parameter::type).toList().equals(Arrays.asList(parameterTypes))) {
					return decl;
				}
			}
			throw new NoSuchElementException("No method declaration: " + name + Arrays.toString(parameterTypes));
		}

		@Override
		public Type createType() {
			Type subcode = typeFactory.get();
			subcodes.add(subcode);
			return subcode;
		}

		@Override
		public String type() {
			for (Code subcode : subcodes) {
				if (subcode instanceof Type type) {
					return type.name();
				}
			}
			throw new IllegalStateException("No type");
		}
	};

	private final List<Code> subcodes = new LinkedList<>();
	private final Package defaultPackage;

	DefaultSource(Y.Package defaultPackage) {
		this.defaultPackage = defaultPackage;
	}

	@Override
	public List<Code> subCodes() {
		return subcodes;
	}

	@Override
	public String toString() {
		return stringOf(this);
	}

	@Override
	public PackageDeclaration createPackageDeclaration() {
		PackageDeclaration subcode = packageFactory.get();
		subcodes.add(subcode);
		return subcode;
	}

	@Override
	public ClassDeclaration createClassDeclaration(int codeIndex) {
		ClassDeclaration subcode = classFactory.apply(codeIndex);
		subcodes.add(subcode);
		return subcode;
	}

	@Override
	public Code.ClassDeclaration getClassDeclaration(String name) {
		for (Code subcode : subcodes) {
			if (subcode instanceof Code.ClassDeclaration decl && decl.name().equals(name)) {
				return decl;
			}
		}
		throw new NoSuchElementException("No class declaration: " + name);
	}

	@Override
	public InterfaceDeclaration createInterfaceDeclaration(int codeIndex) {
		InterfaceDeclaration subcode = interfaceFactory.apply(codeIndex);
		subcodes.add(subcode);
		return subcode;
	}

	@Override
	public ImportDeclaration createImportDeclaration() {
		ImportDeclaration subcode = importDeclarationFactory.get();
		subcodes.add(subcode);
		return subcode;
	}

	private String stringOf(Code code, Object... values) {
		StringBuilder builder = new StringBuilder();
		builder.append(typeStringOf(code));
		if (values.length > 0) {
			builder.append(":");
			builder.append(Arrays.asList(values));
		} else {
			// Nothing to add
		}
		for (Code subcode : code.subCodes()) {
			String tab = "  ";
			builder.append("\n|" + tab);
			builder.append(subcode.toString().replaceAll("\n", "\n|" + tab));
		}
		return builder.toString();
	}

	private String typeStringOf(Code code) {
		return Stream.of(code.getClass().getInterfaces()).map(Class::getSimpleName).toList().toString();
	}

	@Override
	public Y.Package defaultPackage() {
		return defaultPackage;
	}

	public static Package createDefaultPackage(List<Y.Class> defaultPackageClasses, List<Y.Interface> defaultPackageInterfaces) {
		return new Y.Package() {

			@Override
			public String name() {
				throw new NoSuchElementException("No name for default package");
			}

			@Override
			public void rename(String newName) {
				throw new UnsupportedOperationException("No name for default package");
			}

			@Override
			public Stream<Y.Class> classes() {
				return defaultPackageClasses.stream();
			}

			@Override
			public Stream<Y.Interface> interfaces() {
				return defaultPackageInterfaces.stream();
			}
		};
	}
}
