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

public class GraphModelBuilder<N> {

	private final Map<String, GraphModelBuilderNode> nodes = new HashMap<>();
	private final Collection<GraphModelBuilderEdge> edges = new LinkedList<>();
	private final Function<N, String> nodeIdentifier;

	private GraphModelBuilder(Function<N, String> nodeIdentifier) {
		this.nodeIdentifier = nodeIdentifier;
	}

	public Collection<GraphModelBuilderNode> getNodes() {
		return nodes.values();
	}

	public Collection<GraphModelBuilderEdge> getEdges() {
		return edges;
	}

	// TODO document node reuse
	public GraphModelBuilderNode addNode(N content) {
		return nodes.computeIfAbsent(nodeIdentifier.apply(content), //
				id -> new GraphModelBuilderNode(id, content));
	}

	public GraphModelBuilderNode removeNode(N content) {
		return nodes.remove(nodeIdentifier.apply(content));
	}

	// TODO document node reuse
	public void addEdge(N source, N target) {
		GraphModelBuilderNode srcNode = addNode(source);
		GraphModelBuilderNode tgtNode = addNode(target);
		edges.add(new GraphModelBuilderEdge(srcNode, tgtNode));
	}

	public void removeEdge(N source, N target) {
		GraphModelBuilderNode srcNode = nodes.get(nodeIdentifier.apply(source));
		if (srcNode == null) {
			return;// No node, so no edge already
		}
		GraphModelBuilderNode tgtNode = nodes.get(nodeIdentifier.apply(target));
		if (tgtNode == null) {
			return;// No node, so no edge already
		}
		edges.remove(new GraphModelBuilderEdge(srcNode, tgtNode));
	}

	public GraphModel build() {
		return new SimpleGraphModel(//
				nodes.values().stream().map(node -> node).collect(Collectors.toList()), //
				edges.stream().map(edge -> edge).collect(Collectors.toList()));
//		return new SimpleGraphModel(//
//				nodes.values().stream()//
//						.map(GraphModelBuilder<N>.GraphModelNodeBuilder::build)//
//						.collect(Collectors.toList()), //
//				edges.stream()//
//						.map(GraphModelBuilder<N>.GraphModelEdgeBuilder::build)//
//						.collect(Collectors.toList()));
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

	public Collection<GraphModelBuilderNode> getChildren(GraphModelNode parent) {
		return edges.stream()//
				.filter(edge -> edge.getSource().equals(parent))//
				.map(edge -> edge.getTarget())//
				.collect(Collectors.toList());
	}

	public Collection<GraphModelBuilderNode> getParents(GraphModelNode child) {
		return edges.stream()//
				.filter(edge -> edge.getTarget().equals(child))//
				.map(edge -> edge.getSource())//
				.collect(Collectors.toList());
	}

	public void addNode(GraphModelBuilderNode node) {
		nodes.put(node.getId(), node);
	}

	public void removeNode(GraphModelBuilderNode node) {
		nodes.remove(node.getId());
	}

	public void addEdge(GraphModelBuilderEdge edge) {
		edges.add(edge);
	}

	public void removeEdge(GraphModelBuilderEdge edge) {
		edges.remove(edge);
	}

	public static class GraphModelBuilderNode implements GraphModelNode {

		private final String id;
		private final Object content;

		public GraphModelBuilderNode(String id, Object content) {
			this.id = id;
			this.content = content;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public Object getContent() {
			return content;
		}

		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			} else if (obj instanceof GraphModelBuilderNode) {
				var that = (GraphModelBuilderNode) obj;
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

	public static class GraphModelBuilderEdge implements GraphModelEdge {

		private final GraphModelBuilderNode source;
		private final GraphModelBuilderNode target;

		public GraphModelBuilderEdge(GraphModelBuilderNode source, GraphModelBuilderNode target) {
			this.source = source;
			this.target = target;
		}

		@Override
		public GraphModelBuilderNode getSource() {
			return source;
		}

		@Override
		public GraphModelBuilderNode getTarget() {
			return target;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			} else if (obj instanceof GraphModelBuilderEdge) {
				var that = (GraphModelBuilderEdge) obj;
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
