package fr.vergne.stanos.gui.scene.graph;

import java.util.Collections;

import fr.vergne.stanos.gui.scene.graph.layer.GraphLayer;
import fr.vergne.stanos.gui.scene.graph.layout.GraphLayout;
import fr.vergne.stanos.gui.scene.graph.layout.TopToBottomHierarchyLayout;
import fr.vergne.stanos.gui.scene.graph.model.GraphModel;
import fr.vergne.stanos.gui.scene.graph.model.SimpleGraphModel;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public class GraphView extends Pane {

	private final ObjectProperty<GraphModel> modelProperty;
	private final ObjectProperty<GraphLayout> layoutProperty;
	private GraphLayer graphLayer;
	
	public GraphView() {
		this(new SimpleGraphModel(Collections.emptyList(), Collections.emptyList()));
	}
	
	public GraphView(GraphModel model) {
		this.modelProperty = new SimpleObjectProperty<>();
		this.layoutProperty = new SimpleObjectProperty<>();
		
		this.layoutProperty.set(new TopToBottomHierarchyLayout());
		this.modelProperty.set(model);
		
		this.layoutProperty.addListener((observable, oldLayout, newLayout) -> redraw());
		this.modelProperty.addListener((observable, oldModel, newModel) -> redraw());
	}
	
	private void redraw() {
		/**
		 * the pane wrapper is necessary or else the scrollpane would always align the
		 * top-most and left-most child to the top and left eg when you drag the top
		 * child down, the entire scrollpane would move down
		 */
		graphLayer = getLayout().layout(getModel());
		
		ObservableList<Node> children = getChildren();
		if (children.isEmpty()) {
			// First call
			children.add(graphLayer);
		} else {
			children.set(0, graphLayer);
		}
	}
	
	public GraphLayer getGraphLayer() {
		return graphLayer;
	}

	public ObjectProperty<GraphModel> modelProperty() {
		return modelProperty;
	}

	public GraphModel getModel() {
		return modelProperty.get();
	}

	public void setModel(GraphModel model) {
		modelProperty.set(model);
	}

	public ObjectProperty<GraphLayout> layoutProperty() {
		return layoutProperty;
	}

	public GraphLayout getLayout() {
		return layoutProperty.get();
	}

	public void setLayout(GraphLayout layout) {
		layoutProperty.set(layout);
	}
}
