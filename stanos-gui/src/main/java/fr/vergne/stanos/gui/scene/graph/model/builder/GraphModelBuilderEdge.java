package fr.vergne.stanos.gui.scene.graph.model.builder;

import java.util.Objects;

import fr.vergne.stanos.gui.scene.graph.model.GraphModelEdge;
import fr.vergne.stanos.gui.scene.graph.model.SimpleGraphModelEdge;

//TODO generalize to (un)ordered edges (nodes set + metadata)
public class GraphModelBuilderEdge<T> {

	private final GraphModelEdge<T> model;
	private final GraphModelBuilderNode<T> source;
	private final GraphModelBuilderNode<T> target;
	private final GraphModelBuilder<T> builder;

	public GraphModelBuilderEdge(GraphModelBuilder<T> builder, GraphModelBuilderNode<T> source,
			GraphModelBuilderNode<T> target) {
		this.model = new SimpleGraphModelEdge<>(source.getModel(), target.getModel());
		this.source = source;
		this.target = target;
		this.builder = builder;
	}

	public GraphModelEdge<T> getModel() {
		return model;
	}

	public GraphModelBuilderNode<T> getSource() {
		return source;
	}

	public GraphModelBuilderNode<T> getTarget() {
		return target;
	}

	// TODO fail if edge already removed
	public void insert(T intermediary) {
		T srcContent = source.getContent();
		T tgtContent = target.getContent();
		builder.removeEdge(srcContent, tgtContent);
		builder.addEdge(srcContent, intermediary);
		builder.addEdge(intermediary, tgtContent);
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
