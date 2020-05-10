package fr.vergne.stanos.gui.scene;

import fr.vergne.stanos.dependency.Action;
import fr.vergne.stanos.dependency.Dependency;
import fr.vergne.stanos.gui.configuration.Configuration;
import fr.vergne.stanos.gui.scene.graph.Graph;
import fr.vergne.stanos.gui.scene.graph.GraphFactory;
import fr.vergne.stanos.gui.scene.graph.layout.BottomToTopHierarchyLayout;
import fr.vergne.stanos.gui.scene.graph.layout.GraphLayout;
import fr.vergne.stanos.gui.scene.graph.layout.LeftToRightHierarchyLayout;
import fr.vergne.stanos.gui.scene.graph.layout.RightToLeftHierarchyLayout;
import fr.vergne.stanos.gui.scene.graph.layout.TopToBottomHierarchyLayout;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DependenciesGraphPane extends BorderPane {

	public DependenciesGraphPane(Configuration configuration, ObservableList<Dependency> dependencies) {
		int spacing = configuration.gui().globalSpacing();

		ObservableList<GraphLayout> layouts = createGraphLayouts();
		Graph graph = createGraph(dependencies);

		ChoiceBox<GraphLayout> layoutBox = new ChoiceBox<>(layouts);
		ReadOnlyObjectProperty<GraphLayout> selectedLayoutProperty = layoutBox.getSelectionModel()
				.selectedItemProperty();
		selectedLayoutProperty.addListener(observable -> {
			selectedLayoutProperty.getValue().layout(graph);
		});
		layoutBox.getSelectionModel().select(0);// TODO from conf
		HBox options = new HBox(spacing, new Label("Layout:"), layoutBox);
		options.setAlignment(Pos.CENTER_LEFT);

		ScrollPane graphPane = graph.getScrollPane();
		setCenter(new VBox(spacing, options, graphPane));
		VBox.setVgrow(graphPane, Priority.ALWAYS);
	}

	private ObservableList<GraphLayout> createGraphLayouts() {
		return FXCollections.observableArrayList(new LeftToRightHierarchyLayout() {
			@Override
			public String toString() {
				return "→";
			}
		}, new RightToLeftHierarchyLayout() {
			@Override
			public String toString() {
				return "←";
			}
		}, new TopToBottomHierarchyLayout() {
			@Override
			public String toString() {
				return "↓";
			}
		}, new BottomToTopHierarchyLayout() {
			@Override
			public String toString() {
				return "↑";
			}
		});
	}

	private Graph createGraph(ObservableList<Dependency> dependencies) {
		// TODO support more filters
		FilteredList<Dependency> filteredList = dependencies.filtered(dep -> dep.getAction().equals(Action.DECLARES));
		Graph graph = new GraphFactory().createDependencyGraph(filteredList);

		GraphLayout layout = new LeftToRightHierarchyLayout();
		layout.layout(graph);

		return graph;
	}
}
