package fr.vergne.stanos.gui.scene.graph.layout;

import static fr.vergne.stanos.gui.property.MetadataProperty.*;
import static java.util.Comparator.*;
import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import fr.vergne.stanos.dependency.codeitem.CodeItem;
import fr.vergne.stanos.dependency.codeitem.Constructor;
import fr.vergne.stanos.dependency.codeitem.Lambda;
import fr.vergne.stanos.dependency.codeitem.Method;
import fr.vergne.stanos.dependency.codeitem.Package;
import fr.vergne.stanos.dependency.codeitem.Type;
import fr.vergne.stanos.gui.property.MetadataProperty.MetadataKey;
import fr.vergne.stanos.gui.scene.graph.layer.GraphLayer;
import fr.vergne.stanos.gui.scene.graph.layer.GraphLayerEdge;
import fr.vergne.stanos.gui.scene.graph.layer.GraphLayerNode;
import fr.vergne.stanos.gui.scene.graph.model.GraphModel;
import fr.vergne.stanos.gui.scene.graph.model.GraphModelEdge;
import fr.vergne.stanos.gui.scene.graph.model.GraphModelNode;
import fr.vergne.stanos.gui.scene.graph.model.SimpleGraphModel;
import fr.vergne.stanos.gui.scene.graph.model.SimpleGraphModelEdge;
import fr.vergne.stanos.gui.scene.graph.model.SimpleGraphModelNode;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;

public class LeftToRightHierarchyLayout implements GraphLayout {

	private static int layerSpacing = 20;// TODO store in conf

	@Override
	public GraphLayer layout(GraphModel model) {
		return LAYERS_ALGORITHM.apply(model);
	}

	interface LayoutAlgorithm {
		GraphLayer apply(GraphModel layoutModel);
	}

	private static final LayoutAlgorithm LAYERS_ALGORITHM = model -> {
		GraphModel layoutModel = mutableCopy(model);

		List<Collection<GraphModelNode>> modelLayers = distributeIntoLayers(layoutModel);
		addIntermediaries(modelLayers, layoutModel);
		sort(modelLayers);

		List<List<GraphLayerNode>> guiLayers = toGuiLayers(modelLayers);

		// Start coordinates to origin
		MetadataKey<Double> preX = createMetadataKey();
		MetadataKey<Double> preY = createMetadataKey();
		guiLayers.forEach(layer -> {
			layer.forEach(node -> {
				node.setMetadata(preX, 0.0);
				node.setMetadata(preY, 0.0);
			});
		});

		// Set x coordinates to space layers from roots to leaves
		{
			double x = 0;
			for (Collection<GraphLayerNode> layer : guiLayers) {
				double maxWidth = 50;// TODO set to 0 when cell.getWidth() not zero anymore
				for (GraphLayerNode cell : layer) {
					cell.setMetadata(preX, x);
					maxWidth = Math.max(maxWidth, cell.getWidth());
				}
				x += maxWidth + layerSpacing;
			}
		}

		// Spread y coordinates of leaves
		{
			Collection<GraphLayerNode> layer = guiLayers.get(guiLayers.size() - 1);
			double y = 0;
			for (GraphLayerNode cell : layer) {
				double height = Math.max(20, cell.getHeight());// TODO set to cell.getHeight() when not zero anymore
				cell.setMetadata(preY, y);
				y += height + layerSpacing;
			}
		}

		// Adapt y coordinates of parents as average of children
		{
			for (int i = guiLayers.size() - 2; i >= 0; i--) {
				Collection<GraphLayerNode> layer = guiLayers.get(i);
				for (GraphLayerNode node : layer) {
					node.getGraphNodeChildren().stream()
							// Compute average of children
							.mapToDouble(child -> child.getMetadata(preY)).average()
							// Update Y if we could compute it
							.ifPresent(y -> node.setMetadata(preY, y));
				}
			}
		}

		// Apply coordinates
		guiLayers.forEach(layer -> {
			layer.forEach(node -> {
				double x = node.removeMetadata(preX);
				double y = node.removeMetadata(preY);
				node.relocate(x, y);
			});
		});

		Collection<GraphLayerNode> layerNodes = guiLayers.stream().flatMap(layer -> layer.stream()).collect(toList());
		Collection<GraphLayerEdge> layerEdges = layerNodes.stream().flatMap(
				parent -> parent.getGraphNodeChildren().stream().map(child -> new GraphLayerEdge(parent, child)))
				.collect(toList());

		return new GraphLayer(layerNodes, layerEdges);
	};

	private static List<List<GraphLayerNode>> toGuiLayers(List<Collection<GraphModelNode>> modelLayers) {
		// Create isolated layer nodes
		Map<GraphModelNode, GraphLayerNode> nodesMap = new HashMap<>();
		List<List<GraphLayerNode>> guiLayers = modelLayers.stream()
				.map(layer -> layer.stream().map(
						modelNode -> nodesMap.computeIfAbsent(modelNode, LeftToRightHierarchyLayout::createLayerNode))
						.collect(toUnmodifiableList()))
				.collect(toUnmodifiableList());

		// Retrieve parents & children
		nodesMap.entrySet().forEach(entry -> {
			GraphModelNode modelNode = entry.getKey();
			GraphLayerNode layerNode = entry.getValue();
			modelNode.getChildren().stream().map(nodesMap::get).forEach(layerNode::addGraphNodeChild);
			modelNode.getParents().stream().map(nodesMap::get).forEach(layerNode::addGraphNodeParent);
		});

		return guiLayers;
	}

	private static GraphLayerNode createLayerNode(GraphModelNode modelNode) {
		Object content = modelNode.getContent();

		Node layerNode;
		if (content instanceof Intermediary) {
			layerNode = new Group();
		} else if (content instanceof CodeItem) {
			CodeItem item = (CodeItem) content;
			// TODO use proper graphics
			String name = item.getId().replaceAll("\\(.+\\)", "(...)").replaceAll("\\.?[^()]+\\.", "")
					.replaceAll("\\).+", ")");
			char prefix = item instanceof Package ? 'P'
					: item instanceof Type ? 'T'// TODO 'C' & 'I'
							: item instanceof Method ? 'M'
									: item instanceof Constructor ? 'Z' : item instanceof Lambda ? 'L' : '?';
			layerNode = new Label(String.format("[%s] %s", prefix, name));
		} else {
			throw new IllegalStateException("Unmanaged case: " + content.getClass());
		}
		return new GraphLayerNode(modelNode.getId(), layerNode);
	}

	private static void sort(List<Collection<GraphModelNode>> layers) {
		layers.replaceAll(ArrayList::new);

		List<GraphModelNode> roots = (List<GraphModelNode>) layers.get(0);
		Comparator<GraphModelNode> defaultComparator = comparing(node -> node.getId());
		roots.sort(defaultComparator);

		for (int i = 0; i < layers.size() - 1; i++) {
			List<GraphModelNode> parentsLayer = (List<GraphModelNode>) layers.get(i);
			Comparator<GraphModelNode> parentsComparator = comparing(
					// Assume single parent because hierarchy
					node -> parentsLayer.indexOf(node.getParents().iterator().next()));

			List<GraphModelNode> childrenLayer = (List<GraphModelNode>) layers.get(i + 1);
			childrenLayer.sort(parentsComparator.thenComparing(defaultComparator));
		}
	}

	private static void addIntermediaries(List<Collection<GraphModelNode>> layers, GraphModel layoutModel) {
		for (int i = 0; i < layers.size() - 1; i++) {
			Collection<GraphModelNode> currentLayer = layers.get(i);
			Collection<GraphModelNode> nextLayer = layers.get(i + 1);
			for (GraphModelNode parent : currentLayer) {
				for (GraphModelNode child : new ArrayList<>(parent.getChildren())) {
					if (nextLayer.contains(child)) {
						// No need for intermediary
					} else {
						GraphModelNode intermediary = createIntermediary(parent, child);
						replaceParentsAndChildren(parent, child, intermediary);
						updateLayout(layoutModel, parent, child, intermediary);
						nextLayer.add(intermediary);
					}
				}
			}
		}
	}

	private static void updateLayout(GraphModel layoutModel, GraphModelNode parent, GraphModelNode child,
			GraphModelNode intermediary) {
		layoutModel.getNodes().add(intermediary);

		layoutModel.getEdges().remove(new SimpleGraphModelEdge(parent, child));
		layoutModel.getEdges().add(new SimpleGraphModelEdge(parent, intermediary));
		layoutModel.getEdges().add(new SimpleGraphModelEdge(intermediary, child));
	}

	private static void replaceParentsAndChildren(GraphModelNode parent, GraphModelNode child,
			GraphModelNode intermediary) {
		intermediary.addChild(child);
		intermediary.addParent(parent);
		parent.addChild(intermediary);
		parent.removeChild(child);
		child.addParent(intermediary);
		child.removeParent(parent);
	}

	private static class Intermediary {
		private static int counter = 0;

		private final String id;

		public Intermediary(GraphModelNode parent, GraphModelNode child) {
			id = parent.getId() + "@inter" + counter++ + "@" + child.getId();
		}

		public String getId() {
			return id;
		}
	}

	private static GraphModelNode createIntermediary(GraphModelNode parent, GraphModelNode child) {
		Intermediary inter = new Intermediary(parent, child);
		return new SimpleGraphModelNode(inter.getId(), inter);
	}

	private static List<Collection<GraphModelNode>> distributeIntoLayers(GraphModel layoutModel) {
		List<Collection<GraphModelNode>> layers = new LinkedList<>();
		layers.add(new HashSet<GraphModelNode>(layoutModel.getNodes()));

		do {
			Collection<GraphModelNode> currentRoots = layers.get(0);
			Collection<GraphModelNode> newRoots = currentRoots.stream().flatMap(root -> {
				return root.getParents().stream();
			}).collect(toSet());
			currentRoots.removeAll(newRoots);
			layers.add(0, newRoots);
		} while (!layers.get(0).isEmpty());
		layers.remove(0);// Last, empty layer

		return layers;
	}

	private static GraphModel mutableCopy(GraphModel model) {
		Map<GraphModelNode, GraphModelNode> nodesCopies = model.getNodes().stream().collect(
				toMap(node -> node, node -> new SimpleGraphModelNode(node.getId(), (CodeItem) node.getContent())));
		nodesCopies.entrySet().forEach(entry -> {
			GraphModelNode node = entry.getKey();
			GraphModelNode copy = entry.getValue();
			node.getChildren().stream().map(nodesCopies::get).forEach(copy::addChild);
			node.getParents().stream().map(nodesCopies::get).forEach(copy::addParent);
		});

		Collection<GraphModelEdge> edgesCopies = model.getEdges().stream().map(edge -> {
			GraphModelNode source = nodesCopies.get(edge.getSource());
			GraphModelNode target = nodesCopies.get(edge.getTarget());
			return new SimpleGraphModelEdge(source, target);
		}).collect(toList());

		return new SimpleGraphModel(new ArrayList<>(nodesCopies.values()), new ArrayList<>(edgesCopies));
	}
}
