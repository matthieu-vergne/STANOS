package fr.vergne.stanos.gui.scene.graph.layer;

import java.util.Collection;
import java.util.LinkedList;

import fr.vergne.stanos.gui.scene.graph.model.GraphModelNode;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public class GraphLayerNode extends Pane {

	private final GraphModelNode modelNode;
	// TODO remove
	private final Collection<GraphLayerNode> children;
	// TODO remove
	private final Collection<GraphLayerNode> parents;

	public GraphLayerNode(GraphModelNode modelNode, Node node) {
		this.modelNode = modelNode;
		this.children = new LinkedList<>();
		this.parents = new LinkedList<>();

		getChildren().add(node);
	}
	
	public GraphModelNode getGraphModelNode() {
		return modelNode;
	}

	// TODO remove
	public void addGraphNodeChild(GraphLayerNode node) {
		children.add(node);
	}

	// TODO remove
	public void removeGraphNodeChild(GraphLayerNode node) {
		children.remove(node);
	}

	// TODO remove
	public Collection<GraphLayerNode> getGraphNodeChildren() {
		return children;
	}

	// TODO remove
	public void addGraphNodeParent(GraphLayerNode node) {
		parents.add(node);
	}

	// TODO remove
	public void removeGraphNodeParent(GraphLayerNode node) {
		parents.remove(node);
	}
	
	// TODO remove
	public Collection<GraphLayerNode> getGraphNodeParents() {
		return parents;
	}

	@Override
	public String toString() {
		Node node = getChildren().iterator().next();
		return node.toString();
	}
}
