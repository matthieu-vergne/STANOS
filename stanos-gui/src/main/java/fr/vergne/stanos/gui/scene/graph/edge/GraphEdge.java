package fr.vergne.stanos.gui.scene.graph.edge;

import java.util.Objects;

import fr.vergne.stanos.gui.scene.graph.node.GraphNode;
import javafx.scene.Group;
import javafx.scene.shape.Line;

public class GraphEdge extends Group {

	private final GraphNode source;
	private final GraphNode target;

	Line line;

	public GraphEdge(GraphNode source, GraphNode target) {

		this.source = source;
		this.target = target;

		line = new Line();

		line.startXProperty().bind(source.layoutXProperty().add(source.getBoundsInParent().getWidth() / 2.0));
		line.startYProperty().bind(source.layoutYProperty().add(source.getBoundsInParent().getHeight() / 2.0));

		line.endXProperty().bind(target.layoutXProperty().add(target.getBoundsInParent().getWidth() / 2.0));
		line.endYProperty().bind(target.layoutYProperty().add(target.getBoundsInParent().getHeight() / 2.0));

		getChildren().add(line);

	}

	public GraphNode getSource() {
		return source;
	}

	public GraphNode getTarget() {
		return target;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof GraphEdge) {
			GraphEdge that = (GraphEdge) obj;
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
