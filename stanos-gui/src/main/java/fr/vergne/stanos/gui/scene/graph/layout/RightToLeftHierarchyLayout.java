package fr.vergne.stanos.gui.scene.graph.layout;

import fr.vergne.stanos.gui.scene.graph.model.GraphModel;

public class RightToLeftHierarchyLayout implements GraphLayout {

	private final GraphLayout delegate = new LeftToRightHierarchyLayout();

	@Override
	public void layout(GraphModel model) {
		delegate.layout(model);
		model.getNodes().forEach(cell -> {
			double x = cell.getLayoutX();
			double y = cell.getLayoutY();
			cell.relocate(-x, y);// inverse X
		});
	}
}
