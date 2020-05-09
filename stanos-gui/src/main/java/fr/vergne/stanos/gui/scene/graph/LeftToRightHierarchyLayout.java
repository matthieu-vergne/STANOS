package fr.vergne.stanos.gui.scene.graph;

import java.util.List;
import java.util.Random;

import fr.vergne.stanos.gui.scene.graph.cell.Cell;

public class LeftToRightHierarchyLayout extends Layout {

	private final Graph graph;
	private final Random rnd = new Random();

	public LeftToRightHierarchyLayout(Graph graph) {
		this.graph = graph;
	}

	public void execute() {
		// TODO place children at the right of their parents
		List<Cell> cells = graph.getModel().getAllCells();
		for (Cell cell : cells) {
			double x = rnd.nextDouble() * 500;
			double y = rnd.nextDouble() * 500;
			cell.relocate(x, y);
		}
	}
}
