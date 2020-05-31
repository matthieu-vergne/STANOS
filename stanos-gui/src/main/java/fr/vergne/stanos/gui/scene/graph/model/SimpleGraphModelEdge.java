package fr.vergne.stanos.gui.scene.graph.model;

import java.util.Objects;

public class SimpleGraphModelEdge<T> implements GraphModelEdge<T> {

	private final GraphModelNode<T> source;
	private final GraphModelNode<T> target;

	public SimpleGraphModelEdge(GraphModelNode<T> source, GraphModelNode<T> target) {
		this.source = source;
		this.target = target;
	}

	@Override
	public GraphModelNode<T> getSource() {
		return source;
	}

	@Override
	public GraphModelNode<T> getTarget() {
		return target;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof SimpleGraphModelEdge) {
			var that = (SimpleGraphModelEdge<?>) obj;
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
