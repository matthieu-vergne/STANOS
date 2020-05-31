package fr.vergne.stanos.gui.scene.graph;

import java.util.Collections;

import fr.vergne.stanos.dependency.codeitem.CodeItem;
import fr.vergne.stanos.gui.scene.graph.layer.GraphLayer;
import fr.vergne.stanos.gui.scene.graph.layout.GraphLayout;
import fr.vergne.stanos.gui.scene.graph.layout.NoLayout;
import fr.vergne.stanos.gui.scene.graph.model.GraphModel;
import fr.vergne.stanos.gui.scene.graph.model.SimpleGraphModel;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public class GraphView extends Pane {

	private final ObjectProperty<GraphModel<CodeItem>> modelProperty;
	private final ObjectProperty<GraphLayout<CodeItem>> layoutProperty;
	private final ReadOnlyObjectWrapper<GraphLayer> graphLayerProperty;

	public GraphView() {
		this(new SimpleGraphModel<>(Collections.emptyList(), Collections.emptyList()));
	}

	public GraphView(GraphModel<CodeItem> model) {
		this(new NoLayout<>(), model);
	}

	public GraphView(GraphLayout<CodeItem> layout, GraphModel<CodeItem> model) {
		this.modelProperty = new SimpleObjectProperty<>();
		this.layoutProperty = new SimpleObjectProperty<>();
		this.graphLayerProperty = new ReadOnlyObjectWrapper<>();

		this.layoutProperty.set(layout);
		this.modelProperty.set(model);

		this.layoutProperty.addListener(observable -> redraw());
		this.modelProperty.addListener(observable -> redraw());
	}

	private void redraw() {
		System.err.println("redraw");
		/**
		 * the pane wrapper is necessary or else the scrollpane would always align the
		 * top-most and left-most child to the top and left eg when you drag the top
		 * child down, the entire scrollpane would move down
		 */
		GraphLayer graphLayer = getLayout().layout(getModel());

		ObservableList<Node> children = getChildren();
		if (children.isEmpty()) {
			// First call
			children.add(graphLayer);
		} else {
			children.set(0, graphLayer);
		}

		graphLayerProperty.set(graphLayer);
	}

	public GraphLayer getGraphLayer() {
		return graphLayerProperty.get();
	}

	public ReadOnlyObjectProperty<GraphLayer> graphLayerProperty() {
		return graphLayerProperty.getReadOnlyProperty();
	}

	public ObjectProperty<GraphModel<CodeItem>> modelProperty() {
		return modelProperty;
	}

	public GraphModel<CodeItem> getModel() {
		return modelProperty.get();
	}

	public void setModel(GraphModel<CodeItem> model) {
		modelProperty.set(model);
	}

	public ObjectProperty<GraphLayout<CodeItem>> layoutProperty() {
		return layoutProperty;
	}

	public GraphLayout<CodeItem> getLayout() {
		return layoutProperty.get();
	}

	public void setLayout(GraphLayout<CodeItem> layout) {
		layoutProperty.set(layout);
	}
}
