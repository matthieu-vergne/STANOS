package fr.vergne.stanos.gui.scene.graph.model;

import static java.util.stream.Collectors.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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

		this.nodes = nodes;
		this.edges = edges;
	}

	private Set<GraphNode> getEdgeOnlyNodes(Collection<GraphNode> nodes, Collection<GraphEdge> edges) {
		Set<GraphNode> extraNodes = edges.stream().flatMap(edge -> Stream.of(edge.getSource(), edge.getTarget()))
				.collect(toSet());
		extraNodes.removeAll(nodes);
		return extraNodes;
	}

	public Collection<GraphNode> getNodes() {
		return nodes;
	}

	public Collection<GraphEdge> getEdges() {
		return edges;
	}

	public GraphModel immutable() {
		Map<GraphNode, GraphNode> nodeMap = nodes.stream().collect(toMap(node -> node, node -> node.immutable()));

		Function<? super GraphNode, ? extends GraphNode> nodeAdapter = node -> nodeMap.get(node);
		Function<? super GraphEdge, ? extends GraphEdge> edgeAdapter = edge -> {
			GraphNode newSource = nodeMap.get(edge.getSource());
			GraphNode newTarget = nodeMap.get(edge.getTarget());
			return new GraphEdge(newSource, newTarget);
		};

		List<GraphNode> immutableNodes = nodes.stream().map(nodeAdapter).collect(toUnmodifiableList());
		List<GraphEdge> immutableEdges = edges.stream().map(edgeAdapter).collect(toUnmodifiableList());
		return new GraphModel(immutableNodes, immutableEdges);
	}
}
