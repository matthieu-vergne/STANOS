package fr.vergne.stanos.gui.scene.graph.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Objects;

import fr.vergne.stanos.dependency.codeitem.CodeItem;
import fr.vergne.stanos.dependency.codeitem.Constructor;
import fr.vergne.stanos.dependency.codeitem.Lambda;
import fr.vergne.stanos.dependency.codeitem.Method;
import fr.vergne.stanos.dependency.codeitem.Package;
import fr.vergne.stanos.dependency.codeitem.Type;
import fr.vergne.stanos.gui.scene.graph.layer.GraphLayerNode;
import javafx.scene.control.Label;

public class SimpleGraphModelNode implements GraphModelNode {

	private final CodeItem item;
	private final Collection<GraphModelNode> children;
	private final Collection<GraphModelNode> parents;

	public SimpleGraphModelNode(CodeItem item, Collection<GraphModelNode> children,
			Collection<GraphModelNode> parents) {
		this.item = item;
		this.children = children;
		this.parents = parents;
		// TODO remove
		children.stream().map(GraphModelNode::toLayerNode).forEach(toLayerNode()::addGraphNodeChild);
		parents.stream().map(GraphModelNode::toLayerNode).forEach(toLayerNode()::addGraphNodeParent);
	}

	// TODO remove for immutability?
	public SimpleGraphModelNode(CodeItem item) {
		this(item, new LinkedList<>(), new LinkedList<>());
	}
	
	@Override
	public String getId() {
		return item.getId();
	}

	public CodeItem getContent() {
		return item;
	}

	@Override
	public void addChild(GraphModelNode child) {
		children.add(child);
		// TODO remove
		toLayerNode().addGraphNodeChild(child.toLayerNode());
	}
	
	@Override
	public void removeChild(GraphModelNode child) {
		children.remove(child);
		// TODO remove
		toLayerNode().removeGraphNodeChild(child.toLayerNode());
	}

	@Override
	public Collection<GraphModelNode> getChildren() {
		return children;
	}

	@Override
	public void addParent(GraphModelNode parent) {
		parents.add(parent);
		// TODO remove
		toLayerNode().addGraphNodeParent(parent.toLayerNode());
	}
	
	@Override
	public void removeParent(GraphModelNode parent) {
		parents.remove(parent);
		// TODO remove
		toLayerNode().removeGraphNodeParent(parent.toLayerNode());
	}

	@Override
	public Collection<GraphModelNode> getParents() {
		return parents;
	}

	@Override
	public GraphLayerNode toLayerNode() {
		return CACHE.computeIfAbsent(this, n -> {
			// TODO move to layer or layout package
			CodeItem item = getContent();
			// TODO use proper graphics
			String name = item.getId().replaceAll("\\(.+\\)", "(...)").replaceAll("\\.?[^()]+\\.", "")
					.replaceAll("\\).+", ")");
			char prefix = item instanceof Package ? 'P'
					: item instanceof Type ? 'T'// TODO 'C' & 'I'
							: item instanceof Method ? 'M'
									: item instanceof Constructor ? 'Z' : item instanceof Lambda ? 'L' : '?';
			GraphLayerNode layerNode = new GraphLayerNode(item.getId(),
					new Label(String.format("[%s] %s", prefix, name)));
			LAYER_MAP.put(layerNode, this);
			return layerNode;
		});
	}

	@Override
	public GraphModelNode immutable() {
		Collection<GraphModelNode> immutableChildren = Collections.unmodifiableCollection(children);
		Collection<GraphModelNode> immutableParents = Collections.unmodifiableCollection(parents);
		SimpleGraphModelNode copy = new SimpleGraphModelNode(item, immutableChildren, immutableParents);
		copy.toLayerNode().setMetadataProperty(toLayerNode().metadataProperty().immutable());
		copy.toLayerNode().relocate(toLayerNode().getLayoutX(), toLayerNode().getLayoutY());
		return copy;
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
