package fr.vergne.stanos.gui.scene;

import fr.vergne.stanos.dependency.Action;
import fr.vergne.stanos.dependency.Dependency;
import fr.vergne.stanos.gui.configuration.Configuration;
import fr.vergne.stanos.gui.scene.graph.Graph;
import fr.vergne.stanos.gui.scene.graph.GraphFactory;
import fr.vergne.stanos.gui.scene.graph.Layout;
import fr.vergne.stanos.gui.scene.graph.LeftToRightHierarchyLayout;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class DependenciesGraphPane extends VBox {

	public DependenciesGraphPane(Configuration configuration, ObservableList<Dependency> dependencies) {
		Node graph = createGraph(dependencies);
		getChildren().add(graph);
		VBox.setVgrow(graph, Priority.ALWAYS);
		setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
	}

	private Node createGraph(ObservableList<Dependency> dependencies) {
		// TODO support more filters
		FilteredList<Dependency> filteredList = dependencies.filtered(dep -> dep.getAction().equals(Action.DECLARES));
		Graph graph = new GraphFactory().createDependencyGraph(filteredList);

		Layout layout = new LeftToRightHierarchyLayout(graph);
		layout.execute();

		return graph.getScrollPane();
	}
}
