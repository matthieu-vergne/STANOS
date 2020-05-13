package fr.vergne.stanos.gui.scene.graph.layout;

import fr.vergne.stanos.gui.scene.graph.layer.GraphLayer;
import fr.vergne.stanos.gui.scene.graph.model.GraphModel;

public class TopToBottomHierarchyLayout implements GraphLayout {

	private final GraphLayout delegate = new LeftToRightHierarchyLayout();

	@Override
	public GraphLayer layout(GraphModel model) {
		GraphLayer layer = delegate.layout(model);
		layer.getGraphNodes().forEach(node -> {
			double x = node.getLayoutX();
			double y = node.getLayoutY();
			node.relocate(y, x);// inverse X-Y
		});
		return layer;
	}
}
