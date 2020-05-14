package fr.vergne.stanos.gui.scene.graph.layer;

import java.util.Collection;

import javafx.scene.layout.Pane;

// TODO merge with GraphView?
public class GraphLayer extends Pane {
	private final Collection<GraphLayerNode> nodes;
	private final Collection<GraphLayerEdge> edges;

	public GraphLayer(Collection<GraphLayerNode> nodes, Collection<GraphLayerEdge> edges) {
		getChildren().addAll(edges);
		getChildren().addAll(nodes);

		this.nodes = nodes;
		this.edges = edges;
	}

	public Collection<GraphLayerNode> getGraphNodes() {
		return nodes;
	}

	public Collection<GraphLayerEdge> getGraphEdges() {
		return edges;
	}
}
