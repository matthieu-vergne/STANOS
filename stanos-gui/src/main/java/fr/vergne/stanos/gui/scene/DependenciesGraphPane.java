package fr.vergne.stanos.gui.scene;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import fr.vergne.stanos.dependency.Action;
import fr.vergne.stanos.dependency.Dependency;
import fr.vergne.stanos.dependency.codeitem.CodeItem;
import fr.vergne.stanos.gui.configuration.Configuration;
import fr.vergne.stanos.gui.scene.graph.GraphView;
import fr.vergne.stanos.gui.scene.graph.layout.DownCenterTreeLayout;
import fr.vergne.stanos.gui.scene.graph.layout.DownGroundTreeLayout;
import fr.vergne.stanos.gui.scene.graph.layout.DownSurfaceTreeLayout;
import fr.vergne.stanos.gui.scene.graph.layout.GraphLayout;
import fr.vergne.stanos.gui.scene.graph.layout.LeftCenterTreeLayout;
import fr.vergne.stanos.gui.scene.graph.layout.LeftGroundTreeLayout;
import fr.vergne.stanos.gui.scene.graph.layout.LeftSurfaceTreeLayout;
import fr.vergne.stanos.gui.scene.graph.layout.RightCenterTreeLayout;
import fr.vergne.stanos.gui.scene.graph.layout.RightGroundTreeLayout;
import fr.vergne.stanos.gui.scene.graph.layout.RightSurfaceTreeLayout;
import fr.vergne.stanos.gui.scene.graph.layout.UpTreeCenterLayout;
import fr.vergne.stanos.gui.scene.graph.layout.UpTreeGroundLayout;
import fr.vergne.stanos.gui.scene.graph.layout.UpTreeSurfaceLayout;
import fr.vergne.stanos.gui.scene.graph.model.GraphModel;
import fr.vergne.stanos.gui.scene.graph.model.GraphModelEdge;
import fr.vergne.stanos.gui.scene.graph.model.GraphModelNode;
import fr.vergne.stanos.gui.scene.graph.model.SimpleGraphModelEdge;
import fr.vergne.stanos.gui.scene.graph.model.SimpleGraphModelNode;
import fr.vergne.stanos.gui.scene.graph.model.builder.GraphModelBuilder;
import javafx.beans.InvalidationListener;
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
			graphView.setModel(createGraphModel(filteredDependencies));
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
		return FXCollections.observableArrayList(//
				new RightSurfaceTreeLayout() {
					@Override
					public String toString() {
						return "→";
					}
				}, new RightCenterTreeLayout() {
					@Override
					public String toString() {
						return "→.";
					}
				}, new RightGroundTreeLayout() {
					@Override
					public String toString() {
						return "→→";
					}
				}, new LeftSurfaceTreeLayout() {
					@Override
					public String toString() {
						return "←";
					}
				}, new LeftCenterTreeLayout() {
					@Override
					public String toString() {
						return ".←";
					}
				}, new LeftGroundTreeLayout() {
					@Override
					public String toString() {
						return "←←";
					}
				}, new DownSurfaceTreeLayout() {
					@Override
					public String toString() {
						return "↓";
					}
				}, new DownCenterTreeLayout() {
					@Override
					public String toString() {
						return "↓.";
					}
				}, new DownGroundTreeLayout() {
					@Override
					public String toString() {
						return "↓↓";
					}
				}, new UpTreeSurfaceLayout() {
					@Override
					public String toString() {
						return "↑";
					}
				}, new UpTreeCenterLayout() {
					@Override
					public String toString() {
						return "↑.";
					}
				}, new UpTreeGroundLayout() {
					@Override
					public String toString() {
						return "↑↑";
					}
				});
	}

	private GraphModel createGraphModel(ObservableList<Dependency> dependencies) {
		GraphModelBuilder modelBuilder = new GraphModelBuilder();

		Map<CodeItem, GraphModelNode> nodesMap = new HashMap<>();
		Function<CodeItem, GraphModelNode> nodeBuilder = codeItem -> {
			return nodesMap.computeIfAbsent(codeItem, item -> {
				SimpleGraphModelNode node = new SimpleGraphModelNode(item.getId(), item);
				modelBuilder.addNode(node);
				return node;
			});
		};
		BiFunction<GraphModelNode, GraphModelNode, GraphModelEdge> edgeBuilder = (source, target) -> {
			// TODO update counter instead of multiple edges
			GraphModelEdge edge = new SimpleGraphModelEdge(source, target);
			modelBuilder.addEdge(edge);
			return edge;
		};

		dependencies.forEach(dep -> {
			GraphModelNode source = nodeBuilder.apply(dep.getSource());
			GraphModelNode target = nodeBuilder.apply(dep.getTarget());
			source.addChild(target);
			target.addParent(source);
			edgeBuilder.apply(source, target);
		});

		return modelBuilder.build();
	}
}
