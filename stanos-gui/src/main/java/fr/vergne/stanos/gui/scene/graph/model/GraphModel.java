package fr.vergne.stanos.gui.scene.graph.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.vergne.stanos.gui.scene.graph.edge.GraphEdge;
import fr.vergne.stanos.gui.scene.graph.node.GraphNode;

public class GraphModel {

	private final Collection<GraphNode> nodes;
	private final Collection<GraphEdge> edges;

	public GraphModel(Collection<GraphNode> nodes, Collection<GraphEdge> edges) {
		Set<GraphNode> extraNodes = getEdgeOnlyNodes(nodes, edges);
		if (!extraNodes.isEmpty()) {
			throw new IllegalArgumentException("Some nodes are only in edges: " + extraNodes);
		}

		this.nodes = Collections.unmodifiableCollection(new ArrayList<>(nodes));
		this.edges = Collections.unmodifiableCollection(new ArrayList<>(edges));
	}

	private Set<GraphNode> getEdgeOnlyNodes(Collection<GraphNode> nodes, Collection<GraphEdge> edges) {
		Set<GraphNode> extraNodes = edges.stream().flatMap(edge -> Stream.of(edge.getSource(), edge.getTarget()))
				.collect(Collectors.toSet());
		extraNodes.removeAll(nodes);
		return extraNodes;
	}

	public Collection<GraphNode> getNodes() {
		return nodes;
	}

	public Collection<GraphEdge> getEdges() {
		return edges;
	}
}
