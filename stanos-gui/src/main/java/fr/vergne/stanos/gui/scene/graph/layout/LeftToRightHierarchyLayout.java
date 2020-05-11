package fr.vergne.stanos.gui.scene.graph.layout;

import static fr.vergne.stanos.gui.property.MetadataProperty.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import fr.vergne.stanos.gui.property.MetadataProperty.MetadataKey;
import fr.vergne.stanos.gui.scene.graph.model.GraphModel;
import fr.vergne.stanos.gui.scene.graph.node.GraphNode;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;

public class LeftToRightHierarchyLayout implements GraphLayout {

	private static int layerSpacing = 20;// TODO store in conf

	@Override
	public void layout(GraphModel model) {
		Collection<GraphNode> allCells = Collections.unmodifiableCollection(model.getNodes());

		LayoutAlgorithm algorithm = LAYERS_ALGORITHM;

		algorithm.apply(allCells);
	}

	interface LayoutAlgorithm {
		void apply(Collection<GraphNode> allCells);
	}

	@SuppressWarnings("unused")
	private static final LayoutAlgorithm PARENTS_BINDS_ALGORITHM = allCells -> {
		allCells.forEach(cell -> {
			ObservableValue<Number> xValue = cell.getGraphNodeParents().stream()
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
		List<Set<GraphNode>> layers = new LinkedList<>();

		// Copy cells in higher layers as long as they are parents of same layer cells
		layers.add(new HashSet<GraphNode>(allCells));
		do {
			Set<GraphNode> currentRoots = layers.get(0);
			Set<GraphNode> newRoots = new HashSet<GraphNode>(currentRoots.size());
			for (GraphNode currentRoot : currentRoots) {
				newRoots.addAll(currentRoot.getGraphNodeParents());
			}
			layers.add(0, newRoots);
		} while (!layers.get(0).isEmpty());
		layers.remove(0);

		// Clean duplicates from roots to leaves
		List<GraphNode> alreadySeen = new LinkedList<>();
		for (int i = 0; i < layers.size(); i++) {
			Set<GraphNode> currentLayer = layers.get(i);
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
			for (Set<GraphNode> layer : layers) {
				double maxWidth = 50;// TODO set to 0 when cell.getWidth() not zero anymore
				for (GraphNode cell : layer) {
					cell.setMetadata(preX, x);
					maxWidth = Math.max(maxWidth, cell.getWidth());
				}
				x += maxWidth + layerSpacing;
			}
		}

		// Spread y coordinates of leaves
		{
			Set<GraphNode> layer = layers.get(layers.size() - 1);
			double y = 0;
			for (GraphNode cell : layer) {
				double height = Math.max(20, cell.getHeight());// TODO set to cell.getHeight() when not zero anymore
				cell.setMetadata(preY, y);
				y += height + layerSpacing;
			}
		}

		// Adapt y coordinates of parents as average of children
		{
			for (int i = layers.size() - 2; i >= 0; i--) {
				Set<GraphNode> layer = layers.get(i);
				for (GraphNode cell : layer) {
					cell.getGraphNodeChildren().stream()
							// Compute average of children
							.mapToDouble(child -> child.getMetadata(preY)).average()
							// Update Y if we could compute it
							.ifPresent(y -> cell.setMetadata(preY, y));
				}
			}
		}

		// Apply coordinates
		allCells.forEach(cell -> {
			double x = cell.removeMetadata(preX);
			double y = cell.removeMetadata(preY);
			cell.relocate(x, y);
		});
	};
}
