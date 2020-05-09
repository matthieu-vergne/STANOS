package fr.vergne.stanos.gui.scene;

import fr.vergne.stanos.gui.scene.graph.Graph;
import fr.vergne.stanos.gui.scene.graph.GraphFactory;
import fr.vergne.stanos.gui.scene.graph.Layout;
import fr.vergne.stanos.gui.scene.graph.RandomLayout;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class DependenciesGraphPane extends VBox {

	public DependenciesGraphPane() {
		Node graph = createGraph();
		getChildren().add(graph);
		VBox.setVgrow(graph, Priority.ALWAYS);
		setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
	}

	private Node createGraph() {
		Graph graph = new GraphFactory().createDependencyGraph();

		Layout layout = new RandomLayout(graph);
		layout.execute();

		return graph.getScrollPane();
	}
}
