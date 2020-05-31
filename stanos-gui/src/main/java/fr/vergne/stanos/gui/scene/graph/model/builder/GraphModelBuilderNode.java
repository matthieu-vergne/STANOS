package fr.vergne.stanos.gui.scene.graph.model.builder;

import static java.util.stream.Collectors.*;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import fr.vergne.stanos.gui.scene.graph.model.GraphModelNode;
import fr.vergne.stanos.gui.scene.graph.model.SimpleGraphModelNode;

public class GraphModelBuilderNode<T> {

	private final GraphModelNode<T> model;
	private final GraphModelBuilder<T> builder;

	public GraphModelBuilderNode(GraphModelBuilder<T> builder, String id, T content) {
		this.model = new SimpleGraphModelNode<>(id, content);
		this.builder = builder;
	}

	public GraphModelNode<T> getModel() {
		return model;
	}

	public String getId() {
		return model.getId();
	}

	public T getContent() {
		return model.getContent();
	}

	public Collection<GraphModelBuilderEdge<T>> getEdges(T content) {
		return streamEdges(content).collect(toList());
	}

	public Stream<GraphModelBuilderEdge<T>> streamEdges(T content) {
		return builder.getNode(content)//
				.map(otherNode -> Set.of(this, otherNode))//
				.map(search -> builder.getEdges().stream()//
						.filter(edge -> search.equals(Set.of(edge.getSource(), edge.getTarget()))))//
				.orElse(Stream.empty());
	}

	// TODO generalize as getNeighbours(metadata)
	public Collection<GraphModelBuilderNode<T>> getChildren() {
		return streamChildren().collect(toList());
	}

	// TODO generalize as streamNeighbours(metadata)
	public Stream<GraphModelBuilderNode<T>> streamChildren() {
		return builder.streamEdges()//
				.filter(edge -> edge.getSource().equals(this))//
				.map(edge -> edge.getTarget());
	}

	// TODO generalize as getNeighbours(metadata)
	public Collection<GraphModelBuilderNode<T>> getParents() {
		return streamParents().collect(toList());
	}

	// TODO generalize as streamNeighbours(metadata)
	public Stream<GraphModelBuilderNode<T>> streamParents() {
		return builder.getEdges().stream()//
				.filter(edge -> edge.getTarget().equals(this))//
				.map(edge -> edge.getSource());
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
