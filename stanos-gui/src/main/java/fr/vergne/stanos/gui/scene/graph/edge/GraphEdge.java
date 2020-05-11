package fr.vergne.stanos.gui.scene.graph.edge;

import fr.vergne.stanos.gui.scene.graph.node.GraphNode;
import javafx.scene.Group;
import javafx.scene.shape.Line;

public class GraphEdge extends Group {

    protected GraphNode source;
    protected GraphNode target;

    Line line;

    public GraphEdge(GraphNode source, GraphNode target) {

        this.source = source;
        this.target = target;

        source.addGraphNodeChild(target);
        target.addGraphNodeParent(source);

        line = new Line();

        line.startXProperty().bind( source.layoutXProperty().add(source.getBoundsInParent().getWidth() / 2.0));
        line.startYProperty().bind( source.layoutYProperty().add(source.getBoundsInParent().getHeight() / 2.0));

        line.endXProperty().bind( target.layoutXProperty().add( target.getBoundsInParent().getWidth() / 2.0));
        line.endYProperty().bind( target.layoutYProperty().add( target.getBoundsInParent().getHeight() / 2.0));

        getChildren().add( line);

    }

    public GraphNode getSource() {
        return source;
    }

    public GraphNode getTarget() {
        return target;
    }

}
