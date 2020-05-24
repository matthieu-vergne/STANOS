package fr.vergne.stanos.gui.scene.graph.layout;

import fr.vergne.stanos.gui.scene.graph.layer.GraphLayer;
import fr.vergne.stanos.gui.scene.graph.layer.GraphLayerNode;
import fr.vergne.stanos.gui.scene.graph.layout.TreeLayout.Anchor;
import fr.vergne.stanos.gui.scene.graph.layout.TreeLayout.Direction;
import fr.vergne.stanos.gui.scene.graph.layout.TreeLayout.PropertySupplier;
import fr.vergne.stanos.gui.scene.graph.model.GraphModel;
import javafx.beans.property.SimpleDoubleProperty;

public class LeftGroundTreeLayout implements GraphLayout {

	private static int layerSpacingX = 50;// TODO store in conf
	private static int layerSpacingY = 0;// TODO store in conf

	private final GraphLayout delegate;

	public LeftGroundTreeLayout() {
		// TODO parameters
		PropertySupplier xSpacing = () -> new SimpleDoubleProperty(layerSpacingX);
		PropertySupplier ySpacing = () -> new SimpleDoubleProperty(layerSpacingY);

		this.delegate = new TreeLayout(//
				Direction.OPPOSITE, Anchor.GROUND,//
				GraphLayerNode::layoutXProperty, GraphLayerNode::widthProperty, xSpacing,//
				GraphLayerNode::layoutYProperty, GraphLayerNode::heightProperty, ySpacing);
	}

	@Override
	public GraphLayer layout(GraphModel model) {
		return delegate.layout(model);
	}
}
