package fr.vergne.stanos.gui.scene.graph.model;

import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

// TODO once modifiers are removed, simplify class
public class SimpleGraphModel implements GraphModel {

	private final Collection<GraphModelNode> nodes;
	private final Collection<GraphModelEdge> edges;
	private final Map<GraphModelNode, Collection<GraphModelNode>> parents;
	private final Map<GraphModelNode, Collection<GraphModelNode>> children;

	public SimpleGraphModel(Collection<GraphModelNode> nodes, Collection<GraphModelEdge> edges) {
		Set<GraphModelNode> extraNodes = getEdgeOnlyNodes(nodes, edges);
		if (!extraNodes.isEmpty()) {
			throw new IllegalArgumentException("Some nodes are only in edges: " + extraNodes);
		}

		this.nodes = nodes;
		this.edges = edges;
		this.children = new HashMap<>();
		nodes.forEach(node -> this.children.put(node, new LinkedList<>()));
		edges.forEach(edge -> this.children.get(edge.getSource()).add(edge.getTarget()));
		this.parents = new HashMap<>();
		nodes.forEach(node -> this.parents.put(node, new LinkedList<>()));
		edges.forEach(edge -> this.parents.get(edge.getTarget()).add(edge.getSource()));
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
	public void addNode(GraphModelNode node) {
		nodes.add(node);
	}
	
	@Override
	public void removeNode(GraphModelNode node) {
		nodes.remove(node);
	}

	@Override
	public Collection<GraphModelEdge> getEdges() {
		return Collections.unmodifiableCollection(edges);
	}
	
	@Override
	public void addEdge(GraphModelEdge edge) {
		edges.add(edge);
	}
	
	@Override
	public void removeEdge(GraphModelEdge edge) {
		edges.remove(edge);
	}

	@Override
	public Collection<GraphModelNode> getChildren(GraphModelNode parent) {
		return Collections.unmodifiableCollection(new ArrayList<>(children.get(parent)));
	}

	@Override
	public Collection<GraphModelNode> getParents(GraphModelNode child) {
		return Collections.unmodifiableCollection(new ArrayList<>(parents.get(child)));
	}

	// TODO remove
	@Override
	public void addChild(GraphModelNode parent, GraphModelNode child) {
		children.computeIfAbsent(parent, k -> new LinkedList<>()).add(child);
	}

	// TODO remove
	@Override
	public void removeChild(GraphModelNode parent, GraphModelNode child) {
		children.computeIfAbsent(parent, k -> new LinkedList<>()).remove(child);
	}

	// TODO remove
	@Override
	public void addParent(GraphModelNode child, GraphModelNode parent) {
		parents.computeIfAbsent(child, k -> new LinkedList<>()).add(parent);
	}

	// TODO remove
	@Override
	public void removeParent(GraphModelNode child, GraphModelNode parent) {
		parents.computeIfAbsent(child, k -> new LinkedList<>()).remove(parent);
	}
}
