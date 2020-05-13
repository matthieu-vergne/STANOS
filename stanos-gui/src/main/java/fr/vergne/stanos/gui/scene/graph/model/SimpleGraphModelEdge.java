package fr.vergne.stanos.gui.scene.graph.model;

import java.util.Objects;

public class SimpleGraphModelEdge implements GraphModelEdge {

	private final GraphModelNode source;
	private final GraphModelNode target;

	public SimpleGraphModelEdge(GraphModelNode source, GraphModelNode target) {
		this.source = source;
		this.target = target;
	}

	@Override
	public GraphModelNode getSource() {
		return source;
	}

	@Override
	public GraphModelNode getTarget() {
		return target;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof SimpleGraphModelEdge) {
			SimpleGraphModelEdge that = (SimpleGraphModelEdge) obj;
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
