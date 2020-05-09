package fr.vergne.stanos.gui.scene.graph;

import fr.vergne.stanos.gui.scene.graph.model.CellType;
import fr.vergne.stanos.gui.scene.graph.model.Model;

public class GraphFactory {

	public Graph createDependencyGraph() {
		Graph graph = new Graph();

		Model model = graph.getModel();

		graph.beginUpdate();

		model.addCell("Cell A", CellType.RECTANGLE);
		model.addCell("Cell B", CellType.RECTANGLE);
		model.addCell("Cell C", CellType.RECTANGLE);
		model.addCell("Cell D", CellType.TRIANGLE);
		model.addCell("Cell E", CellType.TRIANGLE);
		model.addCell("Cell F", CellType.RECTANGLE);
		model.addCell("Cell G", CellType.RECTANGLE);

		model.addEdge("Cell A", "Cell B");
		model.addEdge("Cell A", "Cell C");
		model.addEdge("Cell B", "Cell C");
		model.addEdge("Cell C", "Cell D");
		model.addEdge("Cell B", "Cell E");
		model.addEdge("Cell D", "Cell F");
		model.addEdge("Cell D", "Cell G");

		graph.endUpdate();
		
		return graph;
	}

}
