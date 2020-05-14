package fr.vergne.stanos.gui.scene.graph.layer;

import java.util.Collection;
import java.util.LinkedList;

import javafx.scene.Node;
import javafx.scene.layout.Pane;

public class GraphLayerNode extends Pane {

	private final Collection<GraphLayerNode> children;
	private final Collection<GraphLayerNode> parents;

	public GraphLayerNode(Node node) {
		this.children = new LinkedList<>();
		this.parents = new LinkedList<>();

		getChildren().add(node);
	}

	public void addGraphNodeChild(GraphLayerNode node) {
		children.add(node);
	}

	public Collection<GraphLayerNode> getGraphNodeChildren() {
		return children;
	}

	public void addGraphNodeParent(GraphLayerNode node) {
		parents.add(node);
	}

	public Collection<GraphLayerNode> getGraphNodeParents() {
		return parents;
	}

	public void removeGraphNodeChild(GraphLayerNode node) {
		children.remove(node);
	}

	public void removeGraphNodeParent(GraphLayerNode node) {
		parents.remove(node);
	}
}
