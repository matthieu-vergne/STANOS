package fr.vergne.stanos.gui.scene.graph.model;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;

public class SimpleGraphModelNode implements GraphModelNode {

	private final String id;
	private final Object content;
	private final Collection<GraphModelNode> children;
	private final Collection<GraphModelNode> parents;

	public SimpleGraphModelNode(String id, Object item, Collection<GraphModelNode> children,
			Collection<GraphModelNode> parents) {
		this.id = id;
		this.content = item;
		this.children = children;
		this.parents = parents;
	}

	// TODO remove for immutability?
	public SimpleGraphModelNode(String id, Object item) {
		this(id, item, new LinkedList<>(), new LinkedList<>());
	}
	
	@Override
	public String getId() {
		return id;
	}

	public Object getContent() {
		return content;
	}

	@Override
	public void addChild(GraphModelNode child) {
		children.add(child);
	}
	
	@Override
	public void removeChild(GraphModelNode child) {
		children.remove(child);
	}

	@Override
	public Collection<GraphModelNode> getChildren() {
		return children;
	}

	@Override
	public void addParent(GraphModelNode parent) {
		parents.add(parent);
	}
	
	@Override
	public void removeParent(GraphModelNode parent) {
		parents.remove(parent);
	}

	@Override
	public Collection<GraphModelNode> getParents() {
		return parents;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof SimpleGraphModelNode) {
			SimpleGraphModelNode that = (SimpleGraphModelNode) obj;
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
