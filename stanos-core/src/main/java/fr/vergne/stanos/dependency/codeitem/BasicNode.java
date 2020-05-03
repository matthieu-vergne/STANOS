package fr.vergne.stanos.dependency.codeitem;

import java.util.Objects;

class BasicNode implements CodeItem {

	private final String id;

	public BasicNode(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return getId();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof BasicNode) {
			BasicNode that = (BasicNode) obj;
			return Objects.equals(this.id, that.id);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
