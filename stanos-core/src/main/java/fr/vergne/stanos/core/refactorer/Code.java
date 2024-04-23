package fr.vergne.stanos.core.refactorer;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

interface Code {

	@Deprecated
	List<Code> subCodes();

	interface Source extends Code, PackageDeclarationContainer, ImportDeclarationContainer, ClassDeclarationContainer, InterfaceDeclarationContainer, RecordDeclarationContainer {
		Y.Package defaultPackage();
	}

	interface PackageDeclarationContainer extends Code {
		PackageDeclaration createPackageDeclaration();
	}

	interface PackageDeclaration extends Code, ClassDeclarationContainer, InterfaceDeclarationContainer {
	}

	interface ImportDeclarationContainer extends Code {
		ImportDeclaration createImportDeclaration();
	}

	interface ImportDeclaration extends Code, NameContainer {
	}

	interface ClassDeclarationContainer extends Code {
		ClassDeclaration createClassDeclaration(int codeIndex);

		Code.ClassDeclaration getClassDeclaration(String name);
	}

	interface ClassDeclaration extends Code, ClassDeclarationContainer, InterfaceDeclarationContainer, MethodDeclarationContainer, SimpleNameContainer, FieldDeclarationContainer {
		String name();

		int codeIndex();// TODO Generalize to Code
	}

	interface RecordDeclarationContainer extends Code {
		RecordDeclaration createRecordDeclaration(int codeIndex);

		Code.RecordDeclaration getRecordDeclaration(String name);
	}

	interface RecordDeclaration extends Code, SimpleNameContainer {
		String name();

		int codeIndex();// TODO Generalize to Code
	}

	interface LocalClassDeclarationContainer extends Code {
		LocalClassDeclaration createLocalClassDeclaration();
	}

	interface LocalClassDeclaration extends Code, ClassDeclarationContainer, InterfaceDeclarationContainer {
	}

	interface InterfaceDeclarationContainer extends Code {
		InterfaceDeclaration createInterfaceDeclaration(int codeIndex);
	}

	interface InterfaceDeclaration extends Code, ClassDeclarationContainer, InterfaceDeclarationContainer, MethodDeclarationContainer, SimpleNameContainer {
		String name();

		int codeIndex();// TODO Generalize to Code
	}

	interface TypeContainer extends Code {
		Type createType();
	}

	interface Type extends Code, SimpleNameContainer, TypeContainer {
		Stream<String> genericTypes();

		String name();
	}

	interface PrimitiveTypeContainer extends Code {
		PrimitiveType createPrimitiveType(String keyword);
	}

	interface PrimitiveType extends Code {
		String keyword();
	}

	interface VoidContainer extends Code {
		Void createVoid();
	}

	interface Void extends Code {
	}

	interface SimpleNameContainer extends Code {
		SimpleName createSimpleName(String identifier);
	}

	interface SimpleName extends Code {
		String identifier();
	}

	interface NameContainer extends Code {
		Name createName(String identifier);
	}

	interface Name extends Code, NameContainer {
		Stream<String> identifiers();
	}

	interface MethodDeclarationContainer extends Code {
		MethodDeclaration createMethodDeclaration(int codeIndex);

		MethodDeclaration getMethodDeclaration(String name, String... parameterTypes);
	}

	interface MethodDeclaration extends Code, TypeContainer, SimpleNameContainer, ClassDeclarationContainer, ObjectCreationContainer, BlockContainer, ModifierContainer, VoidContainer, ParameterContainer, MarkerAnnotationContainer {
		Optional<String> returnType();

		String name();

		Stream<Parameter> parameters();

		int codeIndex();// TODO Generalize to Code
	}

	interface MethodCallContainer extends Code {
		MethodCall createMethodCall();
	}

	interface MethodCall extends Code, NameExprContainer, SimpleNameContainer {
		String name();
	}

	interface ObjectCreationContainer extends Code {
		ObjectCreation createObjectCreation();
	}

	interface ObjectCreation extends Code, MethodDeclarationContainer, TypeContainer {
		String type();
	}

	interface BlockContainer extends Code {
		Block createBlock();

		Stream<VariableDeclaration> variables();
	}

	interface Block extends Code, BlockContainer, ObjectCreationContainer, ExpressionContainer, ReturnContainer, LocalClassDeclarationContainer, IfContainer {
	}

	interface ExpressionContainer extends Code {
		Expression createExpression();
	}

	interface Expression extends Code, VariableDeclarationContainer, AssignExprContainer, NameExprContainer, LambdaExprContainer {
	}

	interface VariableDeclarationContainer extends Code {
		VariableDeclaration createVariableDeclaration();

		Optional<VariableDeclaration> variable();
	}

	interface VariableDeclaration extends Code, VariableDeclaratorContainer {
		VariableDeclarator declarator();
	}

	interface VariableDeclaratorContainer extends Code {
		VariableDeclarator createVariableDeclarator(int codeIndex);
	}

	interface VariableDeclarator extends Code, ObjectCreationContainer, StringLiteralContainer, SimpleNameContainer, TypeContainer, NullLiteralContainer, NameExprContainer, LambdaExprContainer, FieldDeclarationContainer {
		String type();

		String name();

		Code value();

		int codeIndex();// TODO Generalize to Code
	}

	interface FieldDeclarationContainer extends Code {
		FieldDeclaration createFieldDeclaration();

//		Optional<FieldDeclaration> field();
	}

	interface FieldDeclaration extends Code, FieldDeclaratorContainer {
		FieldDeclarator declarator();
	}

	interface FieldDeclaratorContainer extends Code {
		FieldDeclarator createFieldDeclarator(int codeIndex);
	}

	interface FieldDeclarator extends Code, ObjectCreationContainer, StringLiteralContainer, SimpleNameContainer, TypeContainer, NullLiteralContainer, NameExprContainer, LambdaExprContainer, FieldDeclarationContainer {
		String type();

		String name();

		Code value();

		int codeIndex();// TODO Generalize to Code
	}

	interface StringLiteralContainer extends Code {
		StringLiteral createStringLiteral(String value);
	}

	interface StringLiteral extends Code {
		String value();
	}

	interface NullLiteralContainer extends Code {
		NullLiteral createNullLiteral();
	}

	interface NullLiteral extends Code {
	}

	interface ReturnContainer extends Code {
		Return createReturn();
	}

	interface Return extends Code, NameExprContainer, NullLiteralContainer {
	}

	interface NameExprContainer extends Code {
		NameExpr createNameExpr();
	}

	interface NameExpr extends Code, SimpleNameContainer {
		String name();
	}

	interface BinaryExprContainer extends Code {
		BinaryExpr createBinaryExpr();
	}

	interface BinaryExpr extends Code, BinaryExprContainer, StringLiteralContainer, NameExprContainer {
	}

	interface ModifierContainer extends Code {
		enum Keyword {
			PUBLIC(), PROTECTED(), PRIVATE(), //
			ABSTRACT(), DEFAULT(), //
			STATIC(), //
			FINAL(), //
			SYNCHRONIZED(), TRANSIENT(), VOLATILE(), //
			SEALED(), NON_SEALED("non-sealed"), //
			NATIVE(), STRICTFP(), TRANSITIVE();

			private final String code;

			Keyword(String code) {
				this.code = code;
			}

			Keyword() {
				this.code = name().toLowerCase();
			}

			@Override
			public String toString() {
				return code;
			}
		}

		Modifier createModifier(Keyword keyword);
	}

	interface Modifier extends Code {
		Code.ModifierContainer.Keyword keyword();
	}

	interface IfContainer extends Code {
		If createIf();
	}

	interface If extends Code, NameExprContainer, BlockContainer {
	}

	interface ParameterContainer extends Code {
		Parameter createParameter();
	}

	interface Parameter extends Code, SimpleNameContainer, PrimitiveTypeContainer, TypeContainer {
		String type();

		String name();
	}

	interface MarkerAnnotationContainer extends Code {
		MarkerAnnotation createMarkerAnnotation();
	}

	interface MarkerAnnotation extends Code, NameContainer {
	}

	interface AssignExprContainer extends Code {
		AssignExpr createAssignExpr();
	}

	interface AssignExpr extends Code, NameExprContainer, StringLiteralContainer, BinaryExprContainer, MethodCallContainer {
	}

	interface LambdaExprContainer extends Code {
		LambdaExpr createLambdaExpr();
	}

	interface LambdaExpr extends Code, ExpressionContainer {
	}
}
