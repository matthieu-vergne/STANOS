package fr.vergne.stanos.gui.scene.graph.model;

import static java.util.stream.Collectors.*;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

public class SimpleGraphModel implements GraphModel {

	private final Collection<GraphModelNode> nodes;
	private final Collection<GraphModelEdge> edges;

	public SimpleGraphModel(Collection<GraphModelNode> nodes, Collection<GraphModelEdge> edges) {
		Set<GraphModelNode> extraNodes = getEdgeOnlyNodes(nodes, edges);
		if (!extraNodes.isEmpty()) {
			throw new IllegalArgumentException("Some nodes are only in edges: " + extraNodes);
		}

		this.nodes = nodes;
		this.edges = edges;
	}

	private Set<GraphModelNode> getEdgeOnlyNodes(Collection<GraphModelNode> nodes, Collection<GraphModelEdge> edges) {
		Set<GraphModelNode> extraNodes = edges.stream().flatMap(edge -> Stream.of(edge.getSource(), edge.getTarget()))
				.collect(toSet());
		extraNodes.removeAll(nodes);
		return extraNodes;
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
