package fr.vergne.stanos.gui.scene;

import fr.vergne.stanos.dependency.Action;
import fr.vergne.stanos.dependency.Dependency;
import fr.vergne.stanos.dependency.codeitem.CodeItem;
import fr.vergne.stanos.gui.configuration.Configuration;
import fr.vergne.stanos.gui.scene.graph.GraphView;
import fr.vergne.stanos.gui.scene.graph.layer.GraphLayerNode;
import fr.vergne.stanos.gui.scene.graph.layout.GraphLayout;
import fr.vergne.stanos.gui.scene.graph.layout.TreeLayout;
import fr.vergne.stanos.gui.scene.graph.layout.TreeLayout.Anchor;
import fr.vergne.stanos.gui.scene.graph.layout.TreeLayout.Direction;
import fr.vergne.stanos.gui.scene.graph.layout.TreeLayout.ExpressionAccessor;
import fr.vergne.stanos.gui.scene.graph.layout.TreeLayout.PropertyAccessor;
import fr.vergne.stanos.gui.scene.graph.model.GraphModel;
import fr.vergne.stanos.gui.scene.graph.model.builder.GraphModelBuilder;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.NumberExpression;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DependenciesGraphPane extends BorderPane {

	// Store it as member to avoid GC
	private final FilteredList<Dependency> filteredDependencies;

	public DependenciesGraphPane(Configuration configuration, ObservableList<Dependency> dependencies) {
		// TODO support more filters
		filteredDependencies = dependencies.filtered(dep -> dep.getAction().equals(Action.DECLARES));

		/*
		 * COMPONENTS
		 */

		int spacing = configuration.gui().globalSpacing();

		ChoiceBox<GraphLayout> layoutBox = new ChoiceBox<>(createGraphLayouts());
		HBox options = new HBox(spacing, new Label("Layout:"), layoutBox);
		options.setAlignment(Pos.CENTER_LEFT);

		// TODO Provide a model which auto updates upon filteredDependencies update
		// TODO Move filteredDependencies field to model
		GraphView graphView = new GraphView();
		ZoomableScrollPane graphPane = new ZoomableScrollPane(graphView);
		graphPane.setFitToWidth(true);
		graphPane.setFitToHeight(true);
		MouseGestures mouseGestures = new MouseGestures(graphPane::getScaleValue);
		graphPane.zoomTo(1);// TODO remove

		setCenter(new VBox(spacing, options, graphPane));
		VBox.setVgrow(graphPane, Priority.ALWAYS);

		/*
		 * OBSERVERS
		 */

		layoutBox.getSelectionModel().selectedItemProperty().addListener((observable, oldLayout, newLayout) -> {
			graphView.setLayout(newLayout);
		});
		filteredDependencies.addListener((InvalidationListener) observable -> {
			GraphModel model = GraphModelBuilder.createFromEdges(CodeItem::getId, filteredDependencies, Dependency::getSource, Dependency::getTarget).build();
			graphView.setModel(model);
		});
		graphView.graphLayerProperty().addListener((observable, oldLayer, newLayer) -> {
			newLayer.getGraphNodes().forEach(mouseGestures::makeDraggable);
		});

		/**
		 * DEFAULTS
		 */

		layoutBox.getSelectionModel().select(9);// TODO from conf
	}

	private ObservableList<GraphLayout> createGraphLayouts() {
		NumberExpression layersSpacing = new SimpleDoubleProperty(50);// TODO from conf
		NumberExpression neighborsSpacing = new SimpleDoubleProperty(0);// TODO from conf
		PropertyAccessor nodeX = GraphLayerNode::layoutXProperty;
		PropertyAccessor nodeY = GraphLayerNode::layoutYProperty;
		ExpressionAccessor nodeWidth = GraphLayerNode::widthProperty;
		ExpressionAccessor nodeHeight = GraphLayerNode::heightProperty;
		return FXCollections.observableArrayList(//
				new TreeLayout(//
						Direction.NORMAL, Anchor.SURFACE, //
						nodeX, nodeWidth, layersSpacing, //
						nodeY, nodeHeight, neighborsSpacing) {
					@Override
					public String toString() {
						return "→";
					}
				}, new TreeLayout(//
						Direction.NORMAL, Anchor.CENTER, //
						nodeX, nodeWidth, layersSpacing, //
						nodeY, nodeHeight, neighborsSpacing) {
					@Override
					public String toString() {
						return "→.";
					}
				}, new TreeLayout(//
						Direction.NORMAL, Anchor.GROUND, //
						nodeX, nodeWidth, layersSpacing, //
						nodeY, nodeHeight, neighborsSpacing) {
					@Override
					public String toString() {
						return "→→";
					}
				}, new TreeLayout(//
						Direction.OPPOSITE, Anchor.SURFACE, //
						nodeX, nodeWidth, layersSpacing, //
						nodeY, nodeHeight, neighborsSpacing) {
					@Override
					public String toString() {
						return "←";
					}
				}, new TreeLayout(//
						Direction.OPPOSITE, Anchor.CENTER, //
						nodeX, nodeWidth, layersSpacing, //
						nodeY, nodeHeight, neighborsSpacing) {
					@Override
					public String toString() {
						return ".←";
					}
				}, new TreeLayout(//
						Direction.OPPOSITE, Anchor.GROUND, //
						nodeX, nodeWidth, layersSpacing, //
						nodeY, nodeHeight, neighborsSpacing) {
					@Override
					public String toString() {
						return "←←";
					}
				}, new TreeLayout(//
						Direction.NORMAL, Anchor.SURFACE, //
						nodeY, nodeHeight, layersSpacing, //
						nodeX, nodeWidth, neighborsSpacing) {
					@Override
					public String toString() {
						return "↓";
					}
				}, new TreeLayout(//
						Direction.NORMAL, Anchor.CENTER, //
						nodeY, nodeHeight, layersSpacing, //
						nodeX, nodeWidth, neighborsSpacing) {
					@Override
					public String toString() {
						return "↓.";
					}
				}, new TreeLayout(//
						Direction.NORMAL, Anchor.GROUND, //
						nodeY, nodeHeight, layersSpacing, //
						nodeX, nodeWidth, neighborsSpacing) {
					@Override
					public String toString() {
						return "↓↓";
					}
				}, new TreeLayout(//
						Direction.OPPOSITE, Anchor.SURFACE, //
						nodeY, nodeHeight, layersSpacing, //
						nodeX, nodeWidth, neighborsSpacing) {
					@Override
					public String toString() {
						return "↑";
					}
				}, new TreeLayout(//
						Direction.OPPOSITE, Anchor.CENTER, //
						nodeY, nodeHeight, layersSpacing, //
						nodeX, nodeWidth, neighborsSpacing) {
					@Override
					public String toString() {
						return "↑.";
					}
				}, new TreeLayout(//
						Direction.OPPOSITE, Anchor.GROUND, //
						nodeY, nodeHeight, layersSpacing, //
						nodeX, nodeWidth, neighborsSpacing) {
					@Override
					public String toString() {
						return "↑↑";
					}
				});
	}
}
