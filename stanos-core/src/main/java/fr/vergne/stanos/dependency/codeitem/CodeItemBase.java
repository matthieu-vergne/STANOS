package fr.vergne.stanos.dependency.codeitem;

import java.util.Objects;

class CodeItemBase implements CodeItem {

	private final String id;

	public CodeItemBase(String id) {
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
		} else if (obj instanceof CodeItemBase) {
			CodeItemBase that = (CodeItemBase) obj;
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
