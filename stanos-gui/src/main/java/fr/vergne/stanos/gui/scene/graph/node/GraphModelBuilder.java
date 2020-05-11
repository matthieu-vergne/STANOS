package fr.vergne.stanos.gui.scene.graph.node;

import java.util.Collection;
import java.util.LinkedList;

import fr.vergne.stanos.gui.scene.graph.edge.GraphEdge;
import fr.vergne.stanos.gui.scene.graph.model.GraphModel;

public class GraphModelBuilder {

	private final Collection<GraphNode> nodes = new LinkedList<>();
	private final Collection<GraphEdge> edges = new LinkedList<>();

	public void addNode(GraphNode node) {
		nodes.add(node);
	}

	public void addEdge(GraphEdge edge) {
		edges.add(edge);
	}

	public GraphModel build() {
		return new GraphModel(nodes, edges);
	}
}
