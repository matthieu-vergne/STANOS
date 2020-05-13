package fr.vergne.stanos.gui.scene.graph.layout;

import fr.vergne.stanos.gui.scene.graph.model.GraphModel;

public class BottomToTopHierarchyLayout implements GraphLayout {

	private final GraphLayout delegate = new TopToBottomHierarchyLayout();

	@Override
	public GraphModel layout(GraphModel model) {
		model = delegate.layout(model);
		model.getNodes().forEach(cell -> {
			double x = cell.getLayoutX();
			double y = cell.getLayoutY();
			cell.relocate(x, -y);// inverse Y
		});
		return model;
	}
}
