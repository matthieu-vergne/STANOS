package fr.vergne.stanos.gui.scene.graph.model;

import java.util.Collection;
import java.util.LinkedList;

public class GraphModelBuilder {

	private final Collection<GraphModelNode> nodes = new LinkedList<>();
	private final Collection<GraphModelEdge> edges = new LinkedList<>();

	public void addNode(GraphModelNode node) {
		nodes.add(node);
	}

	public void addEdge(GraphModelEdge edge) {
		edges.add(edge);
	}

	public GraphModel build() {
		return new SimpleGraphModel(nodes, edges);
	}
}
