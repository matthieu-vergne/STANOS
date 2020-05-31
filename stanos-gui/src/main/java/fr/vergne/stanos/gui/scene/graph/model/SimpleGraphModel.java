package fr.vergne.stanos.gui.scene.graph.model;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

public class SimpleGraphModel<T> implements GraphModel<T> {

	private final Collection<GraphModelNode<T>> nodes;
	private final Collection<GraphModelEdge<T>> edges;

	public SimpleGraphModel(Collection<GraphModelNode<T>> nodes, Collection<GraphModelEdge<T>> edges) {
		this.nodes = unmodifiableCollection(new ArrayList<>(nodes));
		this.edges = unmodifiableCollection(new ArrayList<>(edges));

		Set<GraphModelNode<T>> extraNodes = edges.stream()//
				.flatMap(edge -> Stream.of(edge.getSource(), edge.getTarget()))//
				.collect(toSet());
		extraNodes.removeAll(nodes);
		if (!extraNodes.isEmpty()) {
			throw new IllegalArgumentException("Some nodes are only in edges: " + extraNodes);
		}
	}

	@Override
	public Collection<GraphModelNode<T>> getNodes() {
		return nodes;
	}

	@Override
	public Collection<GraphModelEdge<T>> getEdges() {
		return edges;
	}

}
