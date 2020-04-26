package fr.vergne.stanos;

import java.util.Objects;

import fr.vergne.stanos.node.Node;

public class Dependency {

	private final Node source;
	private final Node target;
	private final DependencyType type;

	public Dependency(Node source, DependencyType type, Node target) {
		this.source = source;
		this.target = target;
		this.type = type;
	}

	public Node getSource() {
		return source;
	}

	public Node getTarget() {
		return target;
	}

	public DependencyType getType() {
		return type;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof Dependency) {
			Dependency that = (Dependency) obj;
			return Objects.equals(this.source, that.source) && Objects.equals(this.target, that.target)
					&& Objects.equals(this.type, that.type);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(source, target, type);
	}

	@Override
	public String toString() {
		return String.format("%s:%s:%s", source, type, target);
	}
}
