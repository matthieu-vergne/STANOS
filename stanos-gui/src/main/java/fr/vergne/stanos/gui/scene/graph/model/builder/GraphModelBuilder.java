package fr.vergne.stanos.gui.scene.graph.model.builder;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import fr.vergne.stanos.gui.scene.graph.model.GraphModel;
import fr.vergne.stanos.gui.scene.graph.model.GraphModelEdge;
import fr.vergne.stanos.gui.scene.graph.model.GraphModelNode;
import fr.vergne.stanos.gui.scene.graph.model.SimpleGraphModel;
import fr.vergne.stanos.gui.scene.graph.model.SimpleGraphModelEdge;
import fr.vergne.stanos.gui.scene.graph.model.SimpleGraphModelNode;

public class GraphModelBuilder<N> {

	private final Map<String, GraphModelBuilderNode<N>> nodes = new HashMap<>();
	private final Collection<GraphModelBuilderEdge<N>> edges = new LinkedList<>();
	private final Function<N, String> nodeIdentifier;

	private GraphModelBuilder(Function<N, String> nodeIdentifier) {
		this.nodeIdentifier = nodeIdentifier;
	}

	public Collection<GraphModelBuilderNode<N>> getNodes() {
		return nodes.values();
	}

	public Collection<GraphModelBuilderEdge<N>> getEdges() {
		return edges;
	}

	// TODO document node reuse
	public GraphModelBuilderNode<N> addNode(N content) {
		return nodes.computeIfAbsent(nodeIdentifier.apply(content), //
				id -> new GraphModelBuilderNode<>(id, content));
	}

	public void removeNode(N content) {
		nodes.remove(nodeIdentifier.apply(content));
	}

	// TODO document node reuse
	public GraphModelBuilderEdge<N> addEdge(N source, N target) {
		GraphModelBuilderNode<N> srcNode = addNode(source);
		GraphModelBuilderNode<N> tgtNode = addNode(target);
		GraphModelBuilderEdge<N> edge = new GraphModelBuilderEdge<>(srcNode, tgtNode);
		edges.add(edge);
		return edge;
	}

	public void removeEdge(N source, N target) {
		GraphModelBuilderNode<N> srcNode = nodes.get(nodeIdentifier.apply(source));
		if (srcNode == null) {
			return;// No node, so no edge already
		}
		GraphModelBuilderNode<N> tgtNode = nodes.get(nodeIdentifier.apply(target));
		if (tgtNode == null) {
			return;// No node, so no edge already
		}
		edges.remove(new GraphModelBuilderEdge<>(srcNode, tgtNode));
	}

	public GraphModel build() {
		return new SimpleGraphModel(//
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

	public static <N> GraphModelBuilder<N> createFromModel(Function<N, String> nodeIdentifier, GraphModel model) {
		GraphModelBuilder<N> builder = createEmpty(nodeIdentifier);
		model.getNodes().forEach(node -> {
			builder.addNode((N) node.getContent());
		});
		model.getEdges().forEach(edge -> {
			builder.addEdge((N) edge.getSource().getContent(), (N) edge.getTarget().getContent());
		});
		return builder;
	}

	public Collection<GraphModelBuilderNode<N>> getChildren(GraphModelBuilderNode<N> parent) {
		return edges.stream()//
				.filter(edge -> edge.getSource().equals(parent))//
				.map(edge -> edge.getTarget())//
				.collect(Collectors.toList());
	}

	public Collection<GraphModelBuilderNode<N>> getParents(GraphModelBuilderNode<N> child) {
		return edges.stream()//
				.filter(edge -> edge.getTarget().equals(child))//
				.map(edge -> edge.getSource())//
				.collect(Collectors.toList());
	}

	public static class GraphModelBuilderNode<N> {

		private final SimpleGraphModelNode node;

		public GraphModelBuilderNode(String id, Object content) {
			node = new SimpleGraphModelNode(id, content);
		}

		public GraphModelNode getModel() {
			return node;
		}

		public String getId() {
			return node.getId();
		}

		@SuppressWarnings("unchecked")
		public N getContent() {
			return (N) node.getContent();
		}

		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			} else if (obj instanceof GraphModelBuilderNode) {
				var that = (GraphModelBuilderNode<?>) obj;
				return Objects.equals(this.getId(), that.getId());
			} else {
				return false;
			}
		}

		public int hashCode() {
			return Objects.hash(getId());
		}

		public String toString() {
			return getId();
		}
	}

	public static class GraphModelBuilderEdge<N> {

		private final GraphModelBuilderNode<N> source;
		private final GraphModelBuilderNode<N> target;
		private final SimpleGraphModelEdge model;

		public GraphModelBuilderEdge(GraphModelBuilderNode<N> source, GraphModelBuilderNode<N> target) {
			this.source = source;
			this.target = target;
			model = new SimpleGraphModelEdge(source.getModel(), target.getModel());
		}

		public GraphModelEdge getModel() {
			return model;
		}

		public GraphModelBuilderNode<N> getSource() {
			return source;
		}

		public GraphModelBuilderNode<N> getTarget() {
			return target;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			} else if (obj instanceof GraphModelBuilderEdge) {
				var that = (GraphModelBuilderEdge<?>) obj;
				return Objects.equals(this.source, that.source) && Objects.equals(this.target, that.target);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(source, target);
		}
	}
}
