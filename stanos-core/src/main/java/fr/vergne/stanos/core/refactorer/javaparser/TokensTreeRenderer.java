package fr.vergne.stanos.core.refactorer.javaparser;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import com.github.javaparser.JavaToken;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.Node;

class TokensTreeRenderer {

	private final TokenSerializer tokenSerializer;

	public TokensTreeRenderer(TokenSerializer tokenSerializer) {
		this.tokenSerializer = tokenSerializer;
	}

	public void render(Node rootNode, Consumer<String> output) {
		String rootType = typeOf(rootNode);
		String rootTokens = tokenSerializer.serializeAll(tokensOf(rootNode));

		output.accept(rootType);
		output.accept(rootTokens);
		List<Node> children = rootNode.getChildNodes();
		String emptyLine = rootTokens.replaceAll(".", " ");
		while (!children.isEmpty()) {
			String typesLine = emptyLine;
			String tokensLine = emptyLine;
			List<Node> nextChildren = new LinkedList<>();
			for (Node childNode : children) {
				String childType = typeOf(childNode);

				Node parentNode = rootNode;
//				Node parentNode = childNode.getParentNode().orElseThrow();
				String parentTokens = tokenSerializer.serializeAll(tokensOf(parentNode));
				int parentStartInRoot = rootTokens.indexOf(parentTokens);
				String childTokens = tokenSerializer.serializeAll(tokensOf(childNode));
				int childStartInParent = parentTokens.indexOf(childTokens);
				if (childStartInParent == -1) {
					String parentType = typeOf(parentNode);
					throw new IllegalStateException("Missing tokens in parent node:\n"//
							+ "Parent node " + parentType + " tokens:\n" //
							+ parentTokens + "\n" //
							+ "Child node " + childType + " tokens:\n" //
							+ childTokens);
				}
				int childStart = parentStartInRoot + childStartInParent;

				String beforeTokens = tokensLine.substring(0, childStart);
				String afterTokens = tokensLine.substring(childStart + childTokens.length(), tokensLine.length());
				tokensLine = beforeTokens + childTokens + afterTokens;

				String beforeType = typesLine.substring(0, childStart);
				String afterType = typesLine.substring(childStart + childType.length(), typesLine.length());
				typesLine = beforeType + childType + afterType;

				nextChildren.addAll(childNode.getChildNodes());
			}
			output.accept(typesLine);
			output.accept(tokensLine);
			children = nextChildren;
		}
	}

	private List<JavaToken> tokensOf(Node node) {
		TokenRange tokenRange = node.getTokenRange().orElseThrow();
		return StreamSupport.stream(tokenRange.spliterator(), false).toList();
	}

	private String typeOf(Node node) {
		return node.getClass().getSimpleName();
	}
}
