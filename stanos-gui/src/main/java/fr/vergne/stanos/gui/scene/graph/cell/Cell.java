package fr.vergne.stanos.gui.scene.graph.cell;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import fr.vergne.stanos.gui.property.MetadataProperty;
import fr.vergne.stanos.gui.property.MetadataProperty.MetadataKey;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public class Cell extends Pane {
	
	public static final Node NO_NODE = new Node() {
	};

	private final List<Cell> children = new ArrayList<>();
	private final List<Cell> parents = new ArrayList<>();

	private final MetadataProperty metadataProperty = new MetadataProperty();

	public Cell(String cellId, Node node) {
		if (!node.equals(NO_NODE)) {
			getChildren().add(node);
		}
		setId(cellId);
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
	
	public void addCellChild(Cell cell) {
		children.add(cell);
	}

	public List<Cell> getCellChildren() {
		return children;
	}

	public void addCellParent(Cell cell) {
		parents.add(cell);
	}

	public List<Cell> getCellParents() {
		return parents;
	}

	public void removeCellChild(Cell cell) {
		children.remove(cell);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof Cell) {
			Cell that = (Cell) obj;
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
