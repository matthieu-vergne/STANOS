package fr.vergne.stanos.gui.scene.graph.layer;

import javafx.scene.Group;
import javafx.scene.shape.Line;

public class GraphLayerEdge extends Group {

	public GraphLayerEdge(GraphLayerNode source, GraphLayerNode target) {

		Line line = new Line();

		line.startXProperty().bind(source.layoutXProperty().add(source.getBoundsInParent().getWidth() / 2.0));
		line.startYProperty().bind(source.layoutYProperty().add(source.getBoundsInParent().getHeight() / 2.0));

		line.endXProperty().bind(target.layoutXProperty().add(target.getBoundsInParent().getWidth() / 2.0));
		line.endYProperty().bind(target.layoutYProperty().add(target.getBoundsInParent().getHeight() / 2.0));

		getChildren().add(line);

	}
}
