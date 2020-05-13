package fr.vergne.stanos.gui.scene.graph.layout;

import fr.vergne.stanos.gui.scene.graph.model.GraphModel;

public class RightToLeftHierarchyLayout implements GraphLayout {

	private final GraphLayout delegate = new LeftToRightHierarchyLayout();

	@Override
	public GraphModel layout(GraphModel model) {
		model = delegate.layout(model);
		model.getNodes().forEach(node -> {
			double x = node.getLayoutX();
			double y = node.getLayoutY();
			node.relocate(-x, y);// inverse X
		});
		return model;
	}
}
