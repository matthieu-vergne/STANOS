package fr.vergne.stanos.dependency;

import java.util.Objects;

import fr.vergne.stanos.dependency.codeitem.CodeItem;

public class Dependency {

	private final CodeItem source;
	private final CodeItem target;
	private final Action action;

	public Dependency(CodeItem source, Action action, CodeItem target) {
		this.source = source;
		this.target = target;
		this.action = action;
	}

	public CodeItem getSource() {
		return source;
	}

	public CodeItem getTarget() {
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
