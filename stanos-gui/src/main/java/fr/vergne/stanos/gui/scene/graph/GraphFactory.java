package fr.vergne.stanos.gui.scene.graph;

import fr.vergne.stanos.dependency.Dependency;
import fr.vergne.stanos.dependency.codeitem.CodeItem;
import fr.vergne.stanos.dependency.codeitem.Constructor;
import fr.vergne.stanos.dependency.codeitem.Lambda;
import fr.vergne.stanos.dependency.codeitem.Method;
import fr.vergne.stanos.dependency.codeitem.Package;
import fr.vergne.stanos.dependency.codeitem.Type;
import fr.vergne.stanos.gui.scene.graph.cell.Cell;
import fr.vergne.stanos.gui.scene.graph.cell.CellFactory;
import fr.vergne.stanos.gui.scene.graph.model.Model;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;

public class GraphFactory {

	public Graph createDependencyGraph(ObservableList<Dependency> dependencies) {
		Graph graph = new Graph();

		Model model = graph.getModel();

		CellFactory cellFactory = model.getCellFactory();
		// TODO use proper graphics
		cellFactory.registerFactory(CodeItem.class, item -> {
			String name = item.getId().replaceAll("\\(.+\\)", "(...)").replaceAll("\\.?[^()]+\\.", "")
					.replaceAll("\\).+", ")");
			char prefix = item instanceof Package ? 'P'
					: item instanceof Type ? 'T'// TODO 'C' & 'I'
							: item instanceof Method ? 'M'
									: item instanceof Constructor ? 'Z' : item instanceof Lambda ? 'L' : '?';
			return new Cell(item.getId(), new Label(String.format("[%s] %s", prefix, name)));
		});

		graph.beginUpdate();

		// TODO support auto update
		dependencies.forEach(dep -> {
			CodeItem source = dep.getSource();
			CodeItem target = dep.getTarget();
			model.addCell(source);
			model.addCell(target);
			model.addEdge(source, target);
		});

		graph.endUpdate();

		return graph;
	}

}
