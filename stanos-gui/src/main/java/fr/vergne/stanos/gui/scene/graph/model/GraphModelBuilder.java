package fr.vergne.stanos.gui.scene.graph.model;

import java.util.Collection;
import java.util.LinkedList;

import fr.vergne.stanos.gui.scene.graph.layer.GraphLayerEdge;
import fr.vergne.stanos.gui.scene.graph.layer.GraphLayerNode;

public class GraphModelBuilder {

	private final Collection<GraphModelNode> nodes = new LinkedList<>();
	private final Collection<GraphModelEdge> edges = new LinkedList<>();
	private final Collection<GraphLayerNode> layerNodes = new LinkedList<>();
	private final Collection<GraphLayerEdge> layerEdges = new LinkedList<>();

	public void addNode(GraphModelNode node) {
		nodes.add(node);
		layerNodes.add(node.toLayerNode());
	}

	public void addEdge(GraphModelEdge edge) {
		edges.add(edge);
		layerEdges.add(edge.toLayerNode());
	}

	public void addLayerNode(GraphLayerNode node) {
		layerNodes.add(node);
	}

	public void addLayerEdge(GraphLayerEdge edge) {
		layerEdges.add(edge);
	}

	public GraphModel build() {
		return new SimpleGraphModel(nodes, edges, true);
	}
}
