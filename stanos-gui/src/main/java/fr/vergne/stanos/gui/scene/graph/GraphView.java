package fr.vergne.stanos.gui.scene.graph;

import java.util.Collections;

import fr.vergne.stanos.gui.scene.graph.layout.GraphLayout;
import fr.vergne.stanos.gui.scene.graph.layout.TopToBottomHierarchyLayout;
import fr.vergne.stanos.gui.scene.graph.model.GraphModel;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Pane;

public class GraphView extends Pane {

	private final ObjectProperty<GraphModel> modelProperty;
	private final ObjectProperty<GraphLayout> layoutProperty;
	
	public GraphView() {
		this(new GraphModel(Collections.emptyList(), Collections.emptyList()));
	}
	
	public GraphView(GraphModel model) {
		this.modelProperty = new SimpleObjectProperty<>();
		this.layoutProperty = new SimpleObjectProperty<>();
		
		getChildren().add(new Pane());// init with empty cell layer
		this.modelProperty.addListener((observable, oldModel, newModel) -> {
			/**
			 * the pane wrapper is necessary or else the scrollpane would always align the
			 * top-most and left-most child to the top and left eg when you drag the top
			 * child down, the entire scrollpane would move down
			 */
			Pane cellLayer = new Pane();
			cellLayer.getChildren().addAll(newModel.getEdges());
			cellLayer.getChildren().addAll(newModel.getNodes());
			getChildren().set(0, cellLayer);
		});
		this.layoutProperty.addListener((observable, oldLayout, newLayout) -> {
			newLayout.layout(getModel());
		});
		
		this.modelProperty.set(model);
		this.layoutProperty.set(new TopToBottomHierarchyLayout());
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
