package fr.vergne.stanos.gui.scene.graph.model.builder;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import fr.vergne.stanos.gui.scene.graph.model.GraphModel;
import fr.vergne.stanos.gui.scene.graph.model.GraphModelEdge;
import fr.vergne.stanos.gui.scene.graph.model.GraphModelNode;
import fr.vergne.stanos.gui.scene.graph.model.SimpleGraphModel;
import fr.vergne.stanos.gui.scene.graph.model.SimpleGraphModelEdge;
import fr.vergne.stanos.gui.scene.graph.model.SimpleGraphModelNode;

public class GraphModelBuilder<N> implements GraphModel {

	static class GraphModelNodeBuilder {

		private final String id;
		private final Object content;
		private final Collection<GraphModelNodeBuilder> children = new LinkedList<>();
		private final Collection<GraphModelNodeBuilder> parents = new LinkedList<>();

		public GraphModelNodeBuilder(String id, Object content) {
			this.id = id;
			this.content = content;
		}

		public String getId() {
			return id;
		}

		public Object getContent() {
			return content;
		}

		public void addChild(GraphModelNodeBuilder child) {
			// TODO add edge
			children.add(child);
		}

		public void removeChild(GraphModelNodeBuilder child) {
			// TODO remove edge
			children.remove(child);
		}

		public Collection<GraphModelNodeBuilder> getChildren() {
			return children;
		}

		public void addParent(GraphModelNodeBuilder parent) {
			// TODO add edge
			parents.add(parent);
		}

		public void removeParent(GraphModelNodeBuilder parent) {
			// TODO remove edge
			parents.remove(parent);
		}

		public Collection<GraphModelNodeBuilder> getParents() {
			return parents;
		}

		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			} else if (obj instanceof GraphModelNodeBuilder) {
				var that = (GraphModelNodeBuilder) obj;
				return Objects.equals(this.getId(), that.getId());
			} else {
				return false;
			}
		}

		public GraphModelNode build() {
			// TODO
			// return new SimpleGraphModelNode(id, content, children, parents);
			throw new RuntimeException("Not implemented yet");
		}

		public int hashCode() {
			return Objects.hash(getId());
		}

		public String toString() {
			return getId();
		}
	}

	static class GraphModelEdgeBuilder {

		private final GraphModelNodeBuilder source;
		private final GraphModelNodeBuilder target;

		public GraphModelEdgeBuilder(GraphModelNodeBuilder source, GraphModelNodeBuilder target) {
			this.source = source;
			this.target = target;
		}

		public GraphModelNodeBuilder getSource() {
			return source;
		}

		public GraphModelNodeBuilder getTarget() {
			return target;
		}

		public GraphModelEdgeBuilder build() {
			return this;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			} else if (obj instanceof GraphModelEdgeBuilder) {
				var that = (GraphModelEdgeBuilder) obj;
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

	private final Map<String, GraphModelNode> nodes = new HashMap<>();
	private final Collection<GraphModelEdge> edges = new LinkedList<>();
	private final Function<N, String> nodeIdentifier;
	// TODO remove
	private GraphModel model = new SimpleGraphModel(new LinkedList<>(), new LinkedList<>());

	private GraphModelBuilder(Function<N, String> nodeIdentifier) {
		this.nodeIdentifier = nodeIdentifier;
	}

	public Collection<GraphModelNode> getNodes() {
		return nodes.values();
	}

	public Collection<GraphModelEdge> getEdges() {
		return edges;
	}

	// TODO document node reuse
	public GraphModelNode addNode(N content) {
		return nodes.computeIfAbsent(nodeIdentifier.apply(content), //
				id -> {
					SimpleGraphModelNode node = new SimpleGraphModelNode(id, content);
					model.getNodes().add(node);
					return node;
				});
	}

	// TODO document node reuse
	public void addEdge(N source, N target) {
		GraphModelNode srcNode = addNode(source);
		GraphModelNode tgtNode = addNode(target);
		model.addChild(srcNode, tgtNode);
		model.addParent(tgtNode, srcNode);
		SimpleGraphModelEdge edge = new SimpleGraphModelEdge(srcNode, tgtNode);
		model.addEdge(edge);
		edges.add(edge);
	}

	public GraphModel build() {
		return new SimpleGraphModel(nodes.values(), edges);
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

	@Override
	public Collection<GraphModelNode> getChildren(GraphModelNode parent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<GraphModelNode> getParents(GraphModelNode child) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addChild(GraphModelNode parent, GraphModelNode child) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeChild(GraphModelNode parent, GraphModelNode child) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addParent(GraphModelNode child, GraphModelNode parent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeParent(GraphModelNode child, GraphModelNode parent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addEdge(GraphModelEdge edge) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addNode(GraphModelNode node) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeNode(GraphModelNode node) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeEdge(GraphModelEdge edge) {
		// TODO Auto-generated method stub
		
	}
}
