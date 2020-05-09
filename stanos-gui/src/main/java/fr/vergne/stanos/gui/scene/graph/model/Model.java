package fr.vergne.stanos.gui.scene.graph.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.vergne.stanos.gui.scene.graph.Edge;
import fr.vergne.stanos.gui.scene.graph.cell.Cell;
import fr.vergne.stanos.gui.scene.graph.cell.CellFactory;

public class Model {

	private final CellFactory cellFactory = new CellFactory();
	
	private final Cell graphRoot = new Cell("_ROOT_", Cell.NO_NODE);

	private final List<Cell> allCells = new ArrayList<>();
	private final List<Cell> addedCells = new ArrayList<>();
	private final List<Cell> removedCells = new ArrayList<>();

	private final List<Edge> allEdges = new ArrayList<>();
	private final List<Edge> addedEdges = new ArrayList<>();
	private final List<Edge> removedEdges = new ArrayList<>();

	private final Map<Object, Cell> cellMap = new HashMap<>();
	
	public CellFactory getCellFactory() {
		return cellFactory;
	}

	public List<Cell> getAddedCells() {
		return addedCells;
	}

	public List<Cell> getRemovedCells() {
		return removedCells;
	}

	public List<Cell> getAllCells() {
		return allCells;
	}

	public List<Edge> getAddedEdges() {
		return addedEdges;
	}

	public List<Edge> getRemovedEdges() {
		return removedEdges;
	}

	public List<Edge> getAllEdges() {
		return allEdges;
	}

	public void addCell(Object object) {
		cellMap.computeIfAbsent(object, obj -> {
			Cell cell = cellFactory.create(object);
			addedCells.add(cell);
			return cell;
		});
	}

	public void addEdge(Object source, Object target) {

		// TODO add only if not added yet
		// TODO for multiple edges, attach a counter to the edge
		Cell sourceCell = cellMap.get(source);
		Cell targetCell = cellMap.get(target);

		Edge edge = new Edge(sourceCell, targetCell);

		addedEdges.add(edge);

	}

	/**
	 * Attach all cells which don't have a parent to graphParent
	 * 
	 * @param cellList
	 */
	public void attachOrphansToGraphParent(List<Cell> cellList) {

		for (Cell cell : cellList) {
			if (cell.getCellParents().size() == 0) {
				graphRoot.addCellChild(cell);
			}
		}

	}

	/**
	 * Remove the graphParent reference if it is set
	 * 
	 * @param cellList
	 */
	public void disconnectFromGraphParent(List<Cell> cellList) {

		for (Cell cell : cellList) {
			graphRoot.removeCellChild(cell);
		}
	}

	public void merge() {

		// cells
		allCells.addAll(addedCells);
		allCells.removeAll(removedCells);

		addedCells.clear();
		removedCells.clear();

		// edges
		allEdges.addAll(addedEdges);
		allEdges.removeAll(removedEdges);

		addedEdges.clear();
		removedEdges.clear();

	}
}
