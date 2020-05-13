package fr.vergne.stanos.gui.scene.graph.layer;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Objects;

import fr.vergne.stanos.gui.property.MetadataProperty;
import fr.vergne.stanos.gui.property.MetadataProperty.MetadataKey;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public class GraphLayerNode extends Pane {
	
	private final Node node;
	private final Collection<GraphLayerNode> children;
	private final Collection<GraphLayerNode> parents;
	private MetadataProperty metadataProperty;// TODO final

	private GraphLayerNode(String id, Node node, Collection<GraphLayerNode> children, Collection<GraphLayerNode> parents, MetadataProperty metadataProperty) {
		this.node = node;
		this.children = children;
		this.parents = parents;
		this.metadataProperty = metadataProperty;
		
		setId(id);
		getChildren().add(node);
	}

	public GraphLayerNode(String id, Node node) {
		this(id, node, new LinkedList<>(), new LinkedList<>(), new MetadataProperty());
	}
	
	public Node getGraphNodeContent() {
		return node;
	}
	
	public MetadataProperty metadataProperty() {
		return metadataProperty;
	}
	
	public void setMetadataProperty(MetadataProperty metadataProperty) {
		this.metadataProperty = metadataProperty;
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
	
	public void addGraphNodeChild(GraphLayerNode node) {
		children.add(node);
	}

	public Collection<GraphLayerNode> getGraphNodeChildren() {
		return children;
	}

	public void addGraphNodeParent(GraphLayerNode node) {
		parents.add(node);
	}

	public Collection<GraphLayerNode> getGraphNodeParents() {
		return parents;
	}

	public void removeGraphNodeChild(GraphLayerNode node) {
		children.remove(node);
	}
	
	public void removeGraphNodeParent(GraphLayerNode node) {
		parents.remove(node);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof GraphLayerNode) {
			GraphLayerNode that = (GraphLayerNode) obj;
			return Objects.equals(this.getId(), that.getId());
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(getId());
	}

	public GraphLayerNode immutable() {
		Collection<GraphLayerNode> immutableChildren = Collections.unmodifiableCollection(children);
		Collection<GraphLayerNode> immutableParents = Collections.unmodifiableCollection(parents);
		GraphLayerNode copy = new GraphLayerNode(getId(), node, immutableChildren, immutableParents, metadataProperty.immutable());
		copy.relocate(getLayoutX(), getLayoutY());
		return copy;
	}
}
