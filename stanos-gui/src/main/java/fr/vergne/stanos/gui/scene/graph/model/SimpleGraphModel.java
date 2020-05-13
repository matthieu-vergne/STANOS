package fr.vergne.stanos.gui.scene.graph.model;

import static java.util.stream.Collectors.*;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

import fr.vergne.stanos.gui.scene.graph.layer.GraphLayerEdge;
import fr.vergne.stanos.gui.scene.graph.layer.GraphLayerNode;

public class SimpleGraphModel implements GraphModel {

	private final Collection<GraphLayerNode> layerNodes;// TODO remove
	private final Collection<GraphLayerEdge> layerEdges;// TODO remove
	private final Collection<GraphModelNode> nodes;
	private final Collection<GraphModelEdge> edges;

	// TODO remove
	public SimpleGraphModel(Collection<GraphLayerNode> nodes, Collection<GraphLayerEdge> edges) {
		Set<GraphLayerNode> extraNodes = getEdgeOnlyNodesLayer(nodes, edges);
		if (!extraNodes.isEmpty()) {
			throw new IllegalArgumentException("Some nodes are only in edges: " + extraNodes);
		}

		this.layerNodes = nodes;
		this.layerEdges = edges;
		this.nodes = nodes.stream().map(GraphModelNode.LAYER_MAP::get).collect(toList());
		this.edges = edges.stream().map(GraphModelEdge.LAYER_MAP::get).collect(toList());
	}

	public SimpleGraphModel(Collection<GraphModelNode> nodes, Collection<GraphModelEdge> edges, boolean b) {
		Set<GraphModelNode> extraNodes = getEdgeOnlyNodes(nodes, edges);
		if (!extraNodes.isEmpty()) {
			throw new IllegalArgumentException("Some nodes are only in edges: " + extraNodes);
		}

		this.nodes = nodes;
		this.edges = edges;
		this.layerNodes = nodes.stream().map(GraphModelNode::toLayerNode).collect(toList());
		this.layerEdges = edges.stream().map(GraphModelEdge::toLayerNode).collect(toList());
	}

	private Set<GraphLayerNode> getEdgeOnlyNodesLayer(Collection<GraphLayerNode> nodes, Collection<GraphLayerEdge> edges) {
		Set<GraphLayerNode> extraNodes = edges.stream().flatMap(edge -> Stream.of(edge.getSource(), edge.getTarget()))
				.collect(toSet());
		extraNodes.removeAll(nodes);
		return extraNodes;
	}

	private Set<GraphModelNode> getEdgeOnlyNodes(Collection<GraphModelNode> nodes, Collection<GraphModelEdge> edges) {
		Set<GraphModelNode> extraNodes = edges.stream().flatMap(edge -> Stream.of(edge.getSource(), edge.getTarget()))
				.collect(toSet());
		extraNodes.removeAll(nodes);
		return extraNodes;
	}

	@Override
	public Collection<GraphLayerNode> getGraphNodes() {
		return layerNodes;
	}

	@Override
	public Collection<GraphLayerEdge> getGraphEdges() {
		return layerEdges;
	}

	@Override
	public Collection<GraphModelNode> getNodes() {
		return nodes;
	}

	@Override
	public Collection<GraphModelEdge> getEdges() {
		return edges;
	}
}
