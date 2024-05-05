package fr.vergne.stanos.test.refactorer.javaparser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;

import fr.vergne.stanos.core.refactorer.javaparser.TokenSerializer;
import fr.vergne.stanos.core.refactorer.javaparser.TokensTreeRenderer;

class TokensTreeRendererMVE {

	public static void main(String[] args) {
		JavaParser parser = new JavaParser();
		parser.getParserConfiguration().setSymbolResolver(new JavaSymbolSolver(new CombinedTypeSolver()));
		Node node = parser.parseStatement("int x = 14, y = 3;").getResult().orElseThrow();
		new TokensTreeRenderer(TokenSerializer.textOnly().withMinLength(9)).renderAll(node, System.out::println);
		System.out.println("----------");
		node.walk(VariableDeclarator.class, declarator -> display(declarator));
	}

	static Type previous = null;

	private static void display(VariableDeclarator decl) {
		Type type = decl.getType();
		System.out.println("type=" + type //
				+ " range=" + type.getRange().orElseThrow() //
				+ " parent='" + type.getParentNode().orElseThrow() + "'"//
		);
		if (previous == null) {
			previous = type;
		} else {
			System.out.println("Same instance? " + (type == previous));
		}
	}

}
