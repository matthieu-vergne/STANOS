package fr.vergne.stanos.gui.scene.graph.layout;

import static fr.vergne.stanos.gui.property.MetadataProperty.*;
import static java.util.Comparator.*;
import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import fr.vergne.stanos.gui.property.MetadataProperty.MetadataKey;
import fr.vergne.stanos.gui.scene.graph.edge.GraphEdge;
import fr.vergne.stanos.gui.scene.graph.model.GraphModel;
import fr.vergne.stanos.gui.scene.graph.node.GraphNode;
import javafx.scene.Group;
import javafx.scene.Node;

public class LeftToRightHierarchyLayout implements GraphLayout {

	private static int layerSpacing = 20;// TODO store in conf

	@Override
	public GraphModel layout(GraphModel model) {
		return LAYERS_ALGORITHM.apply(model);
	}

	interface LayoutAlgorithm {
		GraphModel apply(GraphModel layoutModel);
	}

	private static final LayoutAlgorithm LAYERS_ALGORITHM = model -> {
		GraphModel layoutModel = mutableCopy(model);
		
		List<Collection<GraphNode>> layers = distributeIntoLayers(layoutModel);
		addIntermediaries(layers, layoutModel);
		sort(layers);

		// From here, we don't want to change the layers anymore
		layers.replaceAll(Collections::unmodifiableCollection);

		// Start coordinates to origin
		MetadataKey<Double> preX = createMetadataKey();
		MetadataKey<Double> preY = createMetadataKey();
		layoutModel.getNodes().forEach(node -> {
			node.setMetadata(preX, 0.0);
			node.setMetadata(preY, 0.0);
		});

		// Set x coordinates to space layers from roots to leaves
		{
			double x = 0;
			for (Collection<GraphNode> layer : layers) {
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
			Collection<GraphNode> layer = layers.get(layers.size() - 1);
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
				Collection<GraphNode> layer = layers.get(i);
				for (GraphNode node : layer) {
					node.getGraphNodeChildren().stream()
							// Compute average of children
							.mapToDouble(child -> child.getMetadata(preY)).average()
							// Update Y if we could compute it
							.ifPresent(y -> node.setMetadata(preY, y));
				}
			}
		}

		// Apply coordinates
		layoutModel.getNodes().forEach(node -> {
			double x = node.removeMetadata(preX);
			double y = node.removeMetadata(preY);
			node.relocate(x, y);
		});

		return layoutModel.immutable();
	};

	private static void sort(List<Collection<GraphNode>> layers) {
		layers.replaceAll(ArrayList::new);

		List<GraphNode> roots = (List<GraphNode>) layers.get(0);
		Comparator<GraphNode> defaultComparator = comparing(node -> node.getId());
		roots.sort(defaultComparator);

		for (int i = 0; i < layers.size() - 1; i++) {
			List<GraphNode> parentsLayer = (List<GraphNode>) layers.get(i);
			Comparator<GraphNode> parentsComparator = comparing(
					// Assume single parent because hierarchy
					node -> parentsLayer.indexOf(node.getGraphNodeParents().iterator().next()));

			List<GraphNode> childrenLayer = (List<GraphNode>) layers.get(i + 1);
			childrenLayer.sort(parentsComparator.thenComparing(defaultComparator));
		}
	}

	private static void addIntermediaries(List<Collection<GraphNode>> layers, GraphModel layoutModel) {
		int intermediaryCounter = 0;
		for (int i = 0; i < layers.size() - 1; i++) {
			Collection<GraphNode> currentLayer = layers.get(i);
			Collection<GraphNode> nextLayer = layers.get(i + 1);
			for (GraphNode parent : currentLayer) {
				for (GraphNode child : new ArrayList<>(parent.getGraphNodeChildren())) {
					if (nextLayer.contains(child)) {
						// No need for intermediary
					} else {
						GraphNode intermediary = createIntermediary(parent, child, intermediaryCounter++);
						replaceParentsAndChildren(parent, child, intermediary);
						updateLayout(layoutModel, parent, child, intermediary);
						nextLayer.add(intermediary);
					}
				}
			}
		}
	}

	private static void updateLayout(GraphModel layoutModel, GraphNode parent, GraphNode child, GraphNode intermediary) {
		layoutModel.getNodes().add(intermediary);
		
		layoutModel.getEdges().remove(new GraphEdge(parent, child));
		layoutModel.getEdges().add(new GraphEdge(parent, intermediary));
		layoutModel.getEdges().add(new GraphEdge(intermediary, child));
	}

	private static void replaceParentsAndChildren(GraphNode parent, GraphNode child, GraphNode intermediary) {
		intermediary.addGraphNodeChild(child);
		intermediary.addGraphNodeParent(parent);
		parent.addGraphNodeChild(intermediary);
		parent.removeGraphNodeChild(child);
		child.addGraphNodeParent(intermediary);
		child.removeGraphNodeParent(parent);
	}

	private static GraphNode createIntermediary(GraphNode parent, GraphNode child, int intermediaryIndex) {
		Node intermediaryContent = new Group();
		String intermediaryId = parent.getId() + "@inter" + intermediaryIndex + "@"
				+ child.getId();
		GraphNode intermediary = new GraphNode(intermediaryId, intermediaryContent);
		return intermediary;
	}

	private static List<Collection<GraphNode>> distributeIntoLayers(GraphModel layoutModel) {
		List<Collection<GraphNode>> layers = new LinkedList<>();
		layers.add(new HashSet<GraphNode>(layoutModel.getNodes()));

		do {
			Collection<GraphNode> currentRoots = layers.get(0);
			Collection<GraphNode> newRoots = currentRoots.stream().flatMap(root -> {
				return root.getGraphNodeParents().stream();
			}).collect(toSet());
			currentRoots.removeAll(newRoots);
			layers.add(0, newRoots);
		} while (!layers.get(0).isEmpty());
		layers.remove(0);// Last, empty layer

		return layers;
	}

	private static GraphModel mutableCopy(GraphModel model) {
		Map<GraphNode, GraphNode> nodesCopies = model.getNodes().stream()
				.collect(toMap(node -> node, node -> new GraphNode(node.getId(), node.getGraphNodeContent())));
		nodesCopies.entrySet().forEach(entry -> {
			GraphNode node = entry.getKey();
			GraphNode copy = entry.getValue();
			node.getGraphNodeChildren().stream().map(nodesCopies::get).forEach(copy::addGraphNodeChild);
			node.getGraphNodeParents().stream().map(nodesCopies::get).forEach(copy::addGraphNodeParent);
		});

		Collection<GraphEdge> edgesCopies = model.getEdges().stream().map(edge -> {
			GraphNode source = nodesCopies.get(edge.getSource());
			GraphNode target = nodesCopies.get(edge.getTarget());
			return new GraphEdge(source, target);
		}).collect(toList());

		return new GraphModel(new ArrayList<>(nodesCopies.values()), new ArrayList<>(edgesCopies));
	}
}
