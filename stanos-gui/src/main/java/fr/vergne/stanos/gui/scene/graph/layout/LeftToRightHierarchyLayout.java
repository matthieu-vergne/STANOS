package fr.vergne.stanos.gui.scene.graph.layout;

import static fr.vergne.stanos.gui.property.MetadataProperty.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import fr.vergne.stanos.gui.property.MetadataProperty.MetadataKey;
import fr.vergne.stanos.gui.scene.graph.Graph;
import fr.vergne.stanos.gui.scene.graph.cell.Cell;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;

public class LeftToRightHierarchyLayout implements GraphLayout {

	private static int layerSpacing = 20;// TODO store in conf

	@Override
	public void layout(Graph graph) {
		List<Cell> allCells = graph.getModel().getAllCells();

		LayoutAlgorithm algorithm = LAYERS_ALGORITHM;

		algorithm.apply(allCells);
	}

	interface LayoutAlgorithm {
		void apply(Collection<Cell> allCells);
	}

	@SuppressWarnings("unused")
	private static final LayoutAlgorithm PARENTS_BINDS_ALGORITHM = allCells -> {
		allCells.forEach(cell -> {
			ObservableValue<Number> xValue = cell.getCellParents().stream()
					// Compute each parent constraint
					.map(parent -> parent.layoutXProperty().add(parent.widthProperty()).add(layerSpacing))
					// Reduce to a single constraint
					.map(binding -> (NumberBinding) binding).reduce(Bindings::max)
					// Convert into property
					.map(binding -> (ObservableValue<Number>) binding)
					// Default to origin
					.orElse(new SimpleDoubleProperty(0));
			cell.layoutXProperty().bind(xValue);

			// TODO y
		});
	};

	private static final LayoutAlgorithm LAYERS_ALGORITHM = allCells -> {
		List<Set<Cell>> layers = new LinkedList<>();

		// Copy cells in higher layers as long as they are parents of same layer cells
		layers.add(new HashSet<Cell>(allCells));
		do {
			Set<Cell> currentRoots = layers.get(0);
			Set<Cell> newRoots = new HashSet<Cell>(currentRoots.size());
			for (Cell currentRoot : currentRoots) {
				newRoots.addAll(currentRoot.getCellParents());
			}
			layers.add(0, newRoots);
		} while (!layers.get(0).isEmpty());
		layers.remove(0);

		// Clean duplicates from roots to leaves
		List<Cell> alreadySeen = new LinkedList<>();
		for (int i = 0; i < layers.size(); i++) {
			Set<Cell> currentLayer = layers.get(i);
			currentLayer.removeAll(alreadySeen);
			alreadySeen.addAll(currentLayer);
		}

		// Start coordinates to origin
		MetadataKey<Double> preX = createMetadataKey();
		MetadataKey<Double> preY = createMetadataKey();
		allCells.forEach(cell -> {
			cell.setMetadata(preX, 0.0);
			cell.setMetadata(preY, 0.0);
		});

		// Set x coordinates to space layers from roots to leaves
		{
			double x = 0;
			for (Set<Cell> layer : layers) {
				double maxWidth = 50;// TODO set to 0 when cell.getWidth() not zero anymore
				for (Cell cell : layer) {
					cell.setMetadata(preX, x);
					maxWidth = Math.max(maxWidth, cell.getWidth());
				}
				x += maxWidth + layerSpacing;
			}
		}

		// Set y coordinates to space siblings from roots to leaves
		{
			for (Set<Cell> layer : layers) {
				double y = 0;
				for (Cell cell : layer) {
					double height = Math.max(20, cell.getHeight());// TODO set to cell.getHeight() when not zero anymore
					cell.setMetadata(preY, y);
					y += height + layerSpacing;
				}
			}
		}

		// Adapt parents y coordinates as average of children
		{
			Collections.reverse(layers);
			for (Set<Cell> layer : layers) {
				for (Cell cell : layer) {
					cell.getCellChildren().stream()
							// Compute average of children
							.mapToDouble(child -> child.getMetadata(preY)).average()
							// Update Y if we could compute it
							.ifPresent(y -> cell.setMetadata(preY, y));
				}
			}
			Collections.reverse(layers);
		}

		// Apply coordinates
		allCells.forEach(cell -> {
			double x = cell.removeMetadata(preX);
			double y = cell.removeMetadata(preY);
			cell.relocate(x, y);
		});
	};
}
