package fr.vergne.stanos.gui.scene.graph.layer;

import java.util.Objects;

import javafx.scene.Group;
import javafx.scene.shape.Line;

public class GraphLayerEdge extends Group {

	private final GraphLayerNode source;
	private final GraphLayerNode target;

	Line line;

	public GraphLayerEdge(GraphLayerNode source, GraphLayerNode target) {

		this.source = source;
		this.target = target;

		line = new Line();

		line.startXProperty().bind(source.layoutXProperty().add(source.getBoundsInParent().getWidth() / 2.0));
		line.startYProperty().bind(source.layoutYProperty().add(source.getBoundsInParent().getHeight() / 2.0));

		line.endXProperty().bind(target.layoutXProperty().add(target.getBoundsInParent().getWidth() / 2.0));
		line.endYProperty().bind(target.layoutYProperty().add(target.getBoundsInParent().getHeight() / 2.0));

		getChildren().add(line);

	}

	public GraphLayerNode getSource() {
		return source;
	}

	public GraphLayerNode getTarget() {
		return target;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof GraphLayerEdge) {
			GraphLayerEdge that = (GraphLayerEdge) obj;
			return Objects.equals(this.source, that.source) && Objects.equals(this.target, that.target);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(source, target);
	}
}
