package fr.vergne.stanos.gui.scene.graph.model.builder;

import static java.util.Collections.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.vergne.stanos.gui.scene.graph.model.GraphModel;
import fr.vergne.stanos.gui.scene.graph.model.SimpleGraphModel;

public class GraphModelBuilder<T> {

	private final Map<String, GraphModelBuilderNode<T>> nodes = new HashMap<>();
	private final Collection<GraphModelBuilderEdge<T>> edges = new LinkedList<>();
	// TODO remove to use instance directly?
	private final Function<T, String> nodeIdentifier;

	private GraphModelBuilder(Function<T, String> nodeIdentifier) {
		this.nodeIdentifier = nodeIdentifier;
	}

	public Collection<GraphModelBuilderNode<T>> getNodes() {
		return unmodifiableCollection(nodes.values());
	}

	public Stream<GraphModelBuilderNode<T>> streamNodes() {
		return nodes.values().stream();
	}

	public Collection<GraphModelBuilderEdge<T>> getEdges() {
		return unmodifiableCollection(edges);
	}

	public Stream<GraphModelBuilderEdge<T>> streamEdges() {
		return edges.stream();
	}

	// TODO document node reuse
	public GraphModelBuilderNode<T> addNode(T content) {
		return nodes.computeIfAbsent(nodeIdentifier.apply(content), //
				id -> new GraphModelBuilderNode<>(this, id, content));
	}

	public Optional<GraphModelBuilderNode<T>> getNode(T content) {
		return Optional.ofNullable(nodes.get(nodeIdentifier.apply(content)));
	}

	public void removeNode(T content) {
		nodes.remove(nodeIdentifier.apply(content));
	}

	// TODO document node reuse
	public GraphModelBuilderEdge<T> addEdge(T source, T target) {
		GraphModelBuilderNode<T> srcNode = addNode(source);
		GraphModelBuilderNode<T> tgtNode = addNode(target);
		GraphModelBuilderEdge<T> edge = new GraphModelBuilderEdge<>(this, srcNode, tgtNode);
		edges.add(edge);
		return edge;
	}

	public void removeEdge(T source, T target) {
		GraphModelBuilderNode<T> srcNode = nodes.get(nodeIdentifier.apply(source));
		if (srcNode == null) {
			return;// No node, so no edge already
		}
		GraphModelBuilderNode<T> tgtNode = nodes.get(nodeIdentifier.apply(target));
		if (tgtNode == null) {
			return;// No node, so no edge already
		}
		edges.remove(new GraphModelBuilderEdge<>(this, srcNode, tgtNode));
	}

	public Optional<GraphModelBuilderEdge<T>> getEdge(T source, T target) {
		GraphModelBuilderNode<T> srcNode = getNode(source).orElse(null);
		if (srcNode == null) {
			return Optional.empty();
		}
		GraphModelBuilderNode<T> tgtNode = getNode(target).orElse(null);
		if (tgtNode == null) {
			return Optional.empty();
		}
		GraphModelBuilderEdge<T> edge = new GraphModelBuilderEdge<>(this, srcNode, tgtNode);
		if (!edges.contains(edge)) {
			return Optional.empty();
		}
		return Optional.of(edge);
	}

	public GraphModel<T> build() {
		return new SimpleGraphModel<T>(//
				nodes.values().stream()//
						.map(GraphModelBuilderNode::getModel)//
						.collect(Collectors.toList()), //
				edges.stream()//
						.map(GraphModelBuilderEdge::getModel)//
						.collect(Collectors.toList()));
	}

	public static <N> GraphModelBuilder<N> createEmpty(Function<N, String> nodeIdentifier) {
		return new GraphModelBuilder<>(nodeIdentifier);
	}

	public static <N, E> GraphModelBuilder<N> createFromEdges(Function<N, String> nodeIdentifier, Collection<E> edges,
			Function<E, N> sourceExtractor, Function<E, N> targetExtractor) {
		GraphModelBuilder<N> builder = createEmpty(nodeIdentifier);
		edges.forEach(edge -> {
			builder.addEdge(sourceExtractor.apply(edge), targetExtractor.apply(edge));
		});
		return builder;
	}

	public static <T> GraphModelBuilder<T> createFromModel(Function<T, String> nodeIdentifier,
			GraphModel<? extends T> model) {
		GraphModelBuilder<T> builder = createEmpty(nodeIdentifier);
		model.getNodes().forEach(node -> {
			builder.addNode(node.getContent());
		});
		model.getEdges().forEach(edge -> {
			builder.addEdge(edge.getSource().getContent(), edge.getTarget().getContent());
		});
		return builder;
	}

}
