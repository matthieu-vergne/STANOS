package fr.vergne.stanos.gui.scene.graph.layout;

import fr.vergne.stanos.gui.scene.graph.Graph;

public class RightToLeftHierarchyLayout implements GraphLayout {

	private final GraphLayout delegate = new LeftToRightHierarchyLayout();

	@Override
	public void layout(Graph graph) {
		delegate.layout(graph);
		graph.getModel().getAllCells().forEach(cell -> {
			double x = cell.getLayoutX();
			double y = cell.getLayoutY();
			cell.relocate(-x, y);// inverse X
		});
	}
}
