package fr.vergne.stanos.core.refactorer;

import static fr.vergne.stanos.core.refactorer.JavaParserUtils.is;
import static fr.vergne.stanos.core.refactorer.JavaParserUtils.stream;
import static fr.vergne.stanos.core.refactorer.JavaParserUtils.textEquals;
import static fr.vergne.stanos.core.refactorer.JavaParserUtils.tokenRangeToCodeRange;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.JavaToken;
import com.github.javaparser.JavaToken.Category;
import com.github.javaparser.Range;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.ReceiverParameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.PatternExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleExportsDirective;
import com.github.javaparser.ast.modules.ModuleOpensDirective;
import com.github.javaparser.ast.modules.ModuleProvidesDirective;
import com.github.javaparser.ast.modules.ModuleRequiresDirective;
import com.github.javaparser.ast.modules.ModuleUsesDirective;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.LocalRecordDeclarationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.UnparsableStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.stmt.YieldStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.IntersectionType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VarType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import fr.vergne.stanos.core.refactorer.Refactorer.CodeRange;
import fr.vergne.stanos.core.refactorer.Scope.Context;

class Visitor extends VoidVisitorAdapter<X> {
	private final String code;
	private final StringBuilder refactoringCode;

	public Visitor(String code, StringBuilder refactoringCode) {
		this.code = code;
		this.refactoringCode = refactoringCode;
	}

	@Override
	public void visit(CompilationUnit n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		super.visit(n, x.derive(code -> code));
		x.underive();
	}

	@Override
	public void visit(@SuppressWarnings("rawtypes") NodeList n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(AnnotationDeclaration n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(AnnotationMemberDeclaration n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ArrayAccessExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ArrayCreationExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ArrayCreationLevel n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ArrayInitializerExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ArrayType n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(AssertStmt n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(AssignExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		super.visit(n, x.derive(Code.AssignExprContainer::createAssignExpr));
		x.underive();
	}

	@Override
	public void visit(BinaryExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		super.visit(n, x.derive(Code.BinaryExprContainer::createBinaryExpr));
		x.underive();
	}

	@Override
	public void visit(BlockComment n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(BlockStmt n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		Scope.Context scopeCtx = x.scopeCtx();
		Scope parentScope = scopeCtx.getCurrent();
		Scope newScope = parentScope.createNewScope();
		scopeCtx.setCurrent(newScope);
		super.visit(n, x.derive(Code.BlockContainer::createBlock));
		x.underive();
		scopeCtx.setCurrent(parentScope);
	}

	@Override
	public void visit(BooleanLiteralExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(BreakStmt n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(CastExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(CatchClause n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(CharLiteralExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ClassExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ClassOrInterfaceDeclaration n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		super.visit(n, isClassDeclaration(n) ? x.derive(Code.ClassDeclarationContainer::createClassDeclaration) : x.derive(Code.InterfaceDeclarationContainer::createInterfaceDeclaration));
		x.underive();
	}

	private boolean isClassDeclaration(ClassOrInterfaceDeclaration decl) {
		return stream(decl.getTokenRange().orElseThrow())//
				.filter(is(Category.KEYWORD).and(textEquals("class")))//
				.findFirst().isPresent();
	}

	@Override
	public void visit(ClassOrInterfaceType n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		super.visit(n, x.derive(Code.TypeContainer::createType));
		x.underive();
	}

	@Override
	public void visit(ConditionalExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ConstructorDeclaration n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ContinueStmt n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(DoStmt n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(DoubleLiteralExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(EmptyStmt n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(EnclosedExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(EnumConstantDeclaration n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(EnumDeclaration n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ExplicitConstructorInvocationStmt n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ExpressionStmt n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		super.visit(n, x.derive(Code.ExpressionContainer::createExpression));
		x.underive();
	}

	@Override
	public void visit(FieldAccessExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(FieldDeclaration n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ForStmt n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ForEachStmt n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(IfStmt n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		super.visit(n, x.derive(Code.IfContainer::createIf));
		x.underive();
	}

	@Override
	public void visit(ImportDeclaration n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		super.visit(n, x.derive(Code.ImportDeclarationContainer::createImportDeclaration));
		x.underive();
	}

	@Override
	public void visit(InitializerDeclaration n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(InstanceOfExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(IntegerLiteralExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(IntersectionType n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(JavadocComment n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(LabeledStmt n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(LambdaExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		super.visit(n, x.derive(Code.LambdaExprContainer::createLambdaExpr));
		x.underive();
	}

	@Override
	public void visit(LineComment n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(LocalClassDeclarationStmt n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		super.visit(n, x.derive(Code.LocalClassDeclarationContainer::createLocalClassDeclaration));
		x.underive();
	}

	@Override
	public void visit(LocalRecordDeclarationStmt n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(LongLiteralExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(MarkerAnnotationExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		super.visit(n, x.derive(Code.MarkerAnnotationContainer::createMarkerAnnotation));
		x.underive();
	}

	@Override
	public void visit(MemberValuePair n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(MethodCallExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		super.visit(n, x.derive(Code.MethodCallContainer::createMethodCall));
		x.underive();
	}

	@Override
	public void visit(MethodDeclaration n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		super.visit(n, x.derive(Code.MethodDeclarationContainer::createMethodDeclaration));
		x.underive();
	}

	@Override
	public void visit(MethodReferenceExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(NameExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		super.visit(n, x.derive(Code.NameExprContainer::createNameExpr));
		x.underive();
	}

	@Override
	public void visit(Name n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ":" + n.getIdentifier() + ">");
		super.visit(n, x.<Code.NameContainer, Code.Name>derive(code -> code.createName(n.getIdentifier())));
		x.underive();
	}

	@Override
	public void visit(NormalAnnotationExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(NullLiteralExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		super.visit(n, x.derive(Code.NullLiteralContainer::createNullLiteral));
		x.underive();
	}

	@Override
	public void visit(ObjectCreationExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		super.visit(n, x.derive(Code.ObjectCreationContainer::createObjectCreation));
		x.underive();
	}

	@Override
	public void visit(PackageDeclaration n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(Parameter n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		super.visit(n, x.derive(Code.ParameterContainer::createParameter));
		x.underive();
	}

	@Override
	public void visit(PrimitiveType n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ":" + n.asString() + ">");
		super.visit(n, x.<Code.PrimitiveTypeContainer, Code.PrimitiveType>derive(code -> code.createPrimitiveType(n.asString())));
		x.underive();
	}

	@Override
	public void visit(RecordDeclaration n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(CompactConstructorDeclaration n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ReturnStmt n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		super.visit(n, x.derive(Code.ReturnContainer::createReturn));
		x.underive();
	}

	@Override
	public void visit(SimpleName n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ":" + n.getIdentifier() + ">");
		Scope scope = x.scopeCtx().getCurrent();
		scope.accessibleVariables().filter(v -> v.name().equals(n.getIdentifier())).findFirst().ifPresent(variable -> {
			((Variable) variable).nameTokens().add(n.getTokenRange().orElseThrow().getBegin());
		});
		super.visit(n, x.<Code.SimpleNameContainer, Code.SimpleName>derive(code -> code.createSimpleName(n.getIdentifier())));
		x.underive();
	}

	@Override
	public void visit(SingleMemberAnnotationExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(StringLiteralExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ":" + n.getValue() + ">");
		super.visit(n, x.<Code.StringLiteralContainer, Code.StringLiteral>derive(container -> container.createStringLiteral(n.getValue())));
		x.underive();
	}

	@Override
	public void visit(SuperExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(SwitchEntry n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(SwitchStmt n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(SynchronizedStmt n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ThisExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ThrowStmt n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(TryStmt n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(TypeExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(TypeParameter n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(UnaryExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(UnionType n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(UnknownType n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(VariableDeclarationExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		super.visit(n, x.derive(Code.VariableDeclarationContainer::createVariableDeclaration));
		x.underive();
	}

	@Override
	public void visit(VariableDeclarator n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		List<JavaToken> nameTokens = new LinkedList<>();
		// Don't add because will be added upon visit call after
		// nameTokens.add(nameToken);
		X derive = x.<Code.VariableDeclaratorContainer, Code.VariableDeclarator>derive(c -> c.createVariableDeclarator(tokenRangeToCodeRange(code, n.getRange().orElseThrow()).start()));
		Code.VariableDeclarator declarator = (Code.VariableDeclarator) derive.code();
		Y.Variable variable = new Variable(n.getNameAsString(), declarator, nameTokens, code, refactoringCode);
		Context scopeCtx = x.scopeCtx();
		Scope parentScope = scopeCtx.getCurrent();
		Scope scope = parentScope.createVariable(variable);
		scopeCtx.setCurrent(scope);
		super.visit(n, derive);
		x.underive();
	}

	@Override
	public void visit(VoidType n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		super.visit(n, x.derive(Code.VoidContainer::createVoid));
		x.underive();
	}

	@Override
	public void visit(WhileStmt n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(WildcardType n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ModuleDeclaration n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ModuleRequiresDirective n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ModuleExportsDirective n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ModuleProvidesDirective n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ModuleUsesDirective n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ModuleOpensDirective n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(UnparsableStmt n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(ReceiverParameter n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(VarType n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(Modifier n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ":" + n.getKeyword() + ">");
		super.visit(n, x.<Code.ModifierContainer, Code.Modifier>derive(code -> code.createModifier(adaptKeyword(n.getKeyword()))));
		x.underive();
	}

	@Override
	public void visit(SwitchExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(TextBlockLiteralExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(YieldStmt n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void visit(PatternExpr n, X x) {
		System.out.println("<" + n.getClass().getSimpleName() + ">");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	private Code.ModifierContainer.Keyword adaptKeyword(com.github.javaparser.ast.Modifier.Keyword keyword) {
		return switch (keyword) {
		case DEFAULT -> Code.ModifierContainer.Keyword.DEFAULT;
		case PUBLIC -> Code.ModifierContainer.Keyword.PUBLIC;
		case PROTECTED -> Code.ModifierContainer.Keyword.PROTECTED;
		case PRIVATE -> Code.ModifierContainer.Keyword.PRIVATE;
		case ABSTRACT -> Code.ModifierContainer.Keyword.ABSTRACT;
		case STATIC -> Code.ModifierContainer.Keyword.STATIC;
		case FINAL -> Code.ModifierContainer.Keyword.FINAL;
		case TRANSIENT -> Code.ModifierContainer.Keyword.TRANSIENT;
		case VOLATILE -> Code.ModifierContainer.Keyword.VOLATILE;
		case SYNCHRONIZED -> Code.ModifierContainer.Keyword.SYNCHRONIZED;
		case NATIVE -> Code.ModifierContainer.Keyword.NATIVE;
		case STRICTFP -> Code.ModifierContainer.Keyword.STRICTFP;
		case TRANSITIVE -> Code.ModifierContainer.Keyword.TRANSITIVE;
		case SEALED -> Code.ModifierContainer.Keyword.SEALED;
		case NON_SEALED -> Code.ModifierContainer.Keyword.NON_SEALED;
		default -> throw new IllegalArgumentException("Not supported: " + keyword);
		};
	}

	static class Variable implements Y.Variable {
		private final String name;
		private final Collection<JavaToken> nameTokens;
		private final String code;
		private final StringBuilder refactoringCode;
		private final Code.VariableDeclarator declaration;

		public Variable(String name, Code.VariableDeclarator declaration, Collection<JavaToken> nameTokens, String code, StringBuilder refactoringCode) {
			this.name = name;
			this.declaration = declaration;
			this.nameTokens = requireNonNull(nameTokens);
			this.code = code;
			this.refactoringCode = refactoringCode;
		}

		@Override
		public String name() {
			return name;
		}

		public Collection<JavaToken> nameTokens() {
			return nameTokens;
		}

		@Override
		public void rename(String newName) {
			nameTokens.stream()//
					.map(JavaToken::getRange)//
					.map(Optional<Range>::orElseThrow)//
					.map(range -> tokenRangeToCodeRange(code, range))//
					// Process from last to first, so the ranges are not shifted
					.sorted(Comparator.comparing(CodeRange::start).reversed())//
					.collect(() -> refactoringCode, (builder, nameRange) -> {
						builder.replace(nameRange.start(), nameRange.end() + 1, newName);
					}, (b1, b2) -> {
						throw new UnsupportedOperationException("Combiner not supported");
					});
		}

		public Code.VariableDeclarator declaration() {
			return declaration;
		}
	}
}
