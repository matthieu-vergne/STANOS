package fr.vergne.stanos.gui.scene.graph.model;

import java.util.Objects;

public class SimpleGraphModelNode implements GraphModelNode {

	private final String id;
	private final Object content;

	public SimpleGraphModelNode(String id, Object content) {
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

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof SimpleGraphModelNode) {
			var that = (SimpleGraphModelNode) obj;
			return Objects.equals(this.getId(), that.getId());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId());
	}

	@Override
	public String toString() {
		return getId();
	}
}
