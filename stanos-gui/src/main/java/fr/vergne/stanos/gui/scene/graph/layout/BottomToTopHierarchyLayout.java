package fr.vergne.stanos.gui.scene.graph.layout;

import fr.vergne.stanos.gui.scene.graph.layer.GraphLayer;
import fr.vergne.stanos.gui.scene.graph.model.GraphModel;

public class BottomToTopHierarchyLayout implements GraphLayout {

	private final GraphLayout delegate = new TopToBottomHierarchyLayout();

	@Override
	public GraphLayer layout(GraphModel model) {
		GraphLayer layer = delegate.layout(model);
		layer.getGraphNodes().forEach(node -> {
			double x = node.getLayoutX();
			double y = node.getLayoutY();
			node.relocate(x, -y);// inverse Y
		});
		return layer;
	}
}