package fr.vergne.stanos.gui.scene.graph.node;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Objects;

import fr.vergne.stanos.gui.property.MetadataProperty;
import fr.vergne.stanos.gui.property.MetadataProperty.MetadataKey;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public class GraphNode extends Pane {
	
	private final Node node;
	private final Collection<GraphNode> children;
	private final Collection<GraphNode> parents;
	private final MetadataProperty metadataProperty;

	private GraphNode(String id, Node node, Collection<GraphNode> children, Collection<GraphNode> parents, MetadataProperty metadataProperty) {
		this.node = node;
		this.children = children;
		this.parents = parents;
		this.metadataProperty = metadataProperty;
		
		setId(id);
		getChildren().add(node);
	}

	public Node getGraphNodeContent() {
		return node;
	}
	
	public GraphNode(String id, Node node) {
		this(id, node, new LinkedList<>(), new LinkedList<>(), new MetadataProperty());
	}
	
	public MetadataProperty metadataProperty() {
		return metadataProperty;
	}
	
	public <T> void setMetadata(MetadataKey<T> key, T value) {
		metadataProperty.put(key, value);
	}
	
	public <T> T getMetadata(MetadataKey<T> key) {
		return metadataProperty.get(key);
	}
	
	public <T> T removeMetadata(MetadataKey<T> key) {
		return metadataProperty.remove(key);
	}
	
	public void addGraphNodeChild(GraphNode node) {
		children.add(node);
	}

	public Collection<GraphNode> getGraphNodeChildren() {
		return children;
	}

	public void addGraphNodeParent(GraphNode node) {
		parents.add(node);
	}

	public Collection<GraphNode> getGraphNodeParents() {
		return parents;
	}

	public void removeGraphNodeChild(GraphNode node) {
		children.remove(node);
	}
	
	public void removeGraphNodeParent(GraphNode node) {
		parents.remove(node);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof GraphNode) {
			GraphNode that = (GraphNode) obj;
			return Objects.equals(this.getId(), that.getId());
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(getId());
	}

	public GraphNode immutable() {
		Collection<GraphNode> immutableChildren = Collections.unmodifiableCollection(children);
		Collection<GraphNode> immutableParents = Collections.unmodifiableCollection(parents);
		GraphNode copy = new GraphNode(getId(), node, immutableChildren, immutableParents, metadataProperty.immutable());
		copy.relocate(getLayoutX(), getLayoutY());
		return copy;
	}
}
