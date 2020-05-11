package fr.vergne.stanos.gui.scene.graph.node;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import fr.vergne.stanos.gui.property.MetadataProperty;
import fr.vergne.stanos.gui.property.MetadataProperty.MetadataKey;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public class GraphNode extends Pane {
	
	private final List<GraphNode> children = new LinkedList<>();
	private final List<GraphNode> parents = new LinkedList<>();

	private final MetadataProperty metadataProperty = new MetadataProperty();

	public GraphNode(String id, Node node) {
		setId(id);
		getChildren().add(node);
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

	public List<GraphNode> getGraphNodeChildren() {
		return children;
	}

	public void addGraphNodeParent(GraphNode node) {
		parents.add(node);
	}

	public List<GraphNode> getGraphNodeParents() {
		return parents;
	}

	public void removeGraphNodeChild(GraphNode node) {
		children.remove(node);
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
}
