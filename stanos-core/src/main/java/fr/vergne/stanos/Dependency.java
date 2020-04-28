package fr.vergne.stanos;

import java.util.Objects;

import fr.vergne.stanos.node.Node;

public class Dependency {

	private final Node source;
	private final Node target;
	private final Action action;

	public Dependency(Node source, Action action, Node target) {
		this.source = source;
		this.target = target;
		this.action = action;
	}

	public Node getSource() {
		return source;
	}

	public Node getTarget() {
		return target;
	}

	public Action getAction() {
		return action;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof Dependency) {
			Dependency that = (Dependency) obj;
			return Objects.equals(this.source, that.source) && Objects.equals(this.target, that.target)
					&& Objects.equals(this.action, that.action);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(source, action, target);
	}

	@Override
	public String toString() {
		return String.format("%s:%s:%s", source, action, target);
	}
}
