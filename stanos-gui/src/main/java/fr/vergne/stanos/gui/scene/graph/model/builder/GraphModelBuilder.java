package fr.vergne.stanos.gui.scene.graph.model.builder;

import java.util.Collection;
import java.util.LinkedList;

import fr.vergne.stanos.gui.scene.graph.model.GraphModel;
import fr.vergne.stanos.gui.scene.graph.model.GraphModelEdge;
import fr.vergne.stanos.gui.scene.graph.model.GraphModelNode;
import fr.vergne.stanos.gui.scene.graph.model.SimpleGraphModel;

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
