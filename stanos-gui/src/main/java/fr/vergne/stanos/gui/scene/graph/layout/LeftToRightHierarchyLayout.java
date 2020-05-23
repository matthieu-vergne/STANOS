package fr.vergne.stanos.gui.scene.graph.layout;

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
import fr.vergne.stanos.dependency.codeitem.StaticBlock;
import fr.vergne.stanos.dependency.codeitem.Type;
import fr.vergne.stanos.gui.scene.graph.layer.GraphLayer;
import fr.vergne.stanos.gui.scene.graph.layer.GraphLayerEdge;
import fr.vergne.stanos.gui.scene.graph.layer.GraphLayerNode;
import fr.vergne.stanos.gui.scene.graph.model.GraphModel;
import fr.vergne.stanos.gui.scene.graph.model.GraphModelNode;
import fr.vergne.stanos.gui.scene.graph.model.SimpleGraphModelEdge;
import fr.vergne.stanos.gui.scene.graph.model.SimpleGraphModelNode;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.NumberExpression;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

// TODO Generalize by removing CodeItem dependencies
public class LeftToRightHierarchyLayout implements GraphLayout {

	private static int layerSpacingX = 50;// TODO store in conf
	private static int layerSpacingY = 0;// TODO store in conf

	@Override
	public GraphLayer layout(GraphModel model) {
		List<Collection<GraphModelNode>> modelLayers = distributeIntoLayers(model);
		addIntermediaries(modelLayers, model);
		sort(modelLayers);

		List<List<GraphLayerNode>> guiLayers = prepareGuiLayers(modelLayers);
		bindNodesXCoordinates(guiLayers);
		bindNodesYCoordinates(guiLayers);

		Collection<GraphLayerNode> layerNodes = guiLayers.stream()//
				.flatMap(layer -> layer.stream())//
				.collect(toList());
		Collection<GraphLayerEdge> layerEdges = layerNodes.stream().//
				flatMap(parent -> parent.getGraphNodeChildren().stream()//
						.map(child -> new GraphLayerEdge(parent, child)))//
				.collect(toList());
		return new GraphLayer(layerNodes, layerEdges);
	}

	private void bindNodesYCoordinates(List<List<GraphLayerNode>> guiLayers) {
		if (guiLayers.isEmpty()) {
			// Nothing to relocate
		} else {
			spreadLeaves(guiLayers);
			centerParentsOnChildren(guiLayers);
		}
	}

	private void centerParentsOnChildren(List<List<GraphLayerNode>> guiLayers) {
		for (int i = guiLayers.size() - 2; i >= 0; i--) {
			for (GraphLayerNode node : guiLayers.get(i)) {
				Collection<GraphLayerNode> children = node.getGraphNodeChildren();
				DoubleExpression centerSum = null;
				for (GraphLayerNode child : children) {
					DoubleExpression y = child.layoutYProperty();
					DoubleExpression height = child.heightProperty();
					DoubleExpression center = y.add(height.divide(2));
					centerSum = centerSum == null ? center : centerSum.add(center);
				}
				DoubleExpression center = centerSum.divide(children.size());
				DoubleExpression height = node.heightProperty();
				DoubleExpression y = center.subtract(height.divide(2));
				node.layoutYProperty().bind(y);
			}
		}
	}

	private void spreadLeaves(List<List<GraphLayerNode>> guiLayers) {
		List<GraphLayerNode> layer = guiLayers.get(guiLayers.size() - 1);
		GraphLayerNode aboveNode = null;
		for (GraphLayerNode node : layer) {
			if (aboveNode == null) {
				node.setLayoutY(0);
			} else {
				DoubleExpression aboveHeight = aboveNode.heightProperty();
				DoubleExpression aboveY = aboveNode.layoutYProperty();
				DoubleExpression nodeY = aboveY.add(aboveHeight).add(layerSpacingY);
				node.layoutYProperty().bind(nodeY);
			}
			aboveNode = node;
		}
	}

	private void bindNodesXCoordinates(List<List<GraphLayerNode>> guiLayers) {
		List<NumberExpression> layerWidths = createLayerWidthProperties(guiLayers);
		for (int i = 0; i < guiLayers.size(); i++) {
			for (GraphLayerNode node : guiLayers.get(i)) {
				Collection<GraphLayerNode> parents = node.getGraphNodeParents();
				// TODO Support centered in layer
				// TODO Support right alignment
				if (parents.isEmpty()) {
					// Root starts at the origin
					node.setLayoutX(0);
				} else {
					GraphLayerNode parent = parents.iterator().next();// Assume single parent because tree
					NumberExpression parentLayerWidth = layerWidths.get(i - 1);
					DoubleBinding xProperty = parent.layoutXProperty().add(parentLayerWidth.add(layerSpacingX));
					node.layoutXProperty().bind(xProperty);
				}
			}
		}
	}

	private List<NumberExpression> createLayerWidthProperties(List<List<GraphLayerNode>> guiLayers) {
		List<NumberExpression> layerWidths = new LinkedList<>();
		for (List<GraphLayerNode> layer : guiLayers) {
			NumberExpression layerWidth = null;
			for (GraphLayerNode node : layer) {
				ReadOnlyDoubleProperty nodeWidth = node.widthProperty();
				layerWidth = layerWidth == null ? nodeWidth : Bindings.max(layerWidth, nodeWidth);
			}
			layerWidths.add(layerWidth);
		}
		return layerWidths;
	};

	private List<List<GraphLayerNode>> prepareGuiLayers(List<Collection<GraphModelNode>> modelLayers) {
		// Create isolated layer nodes
		Map<GraphModelNode, GraphLayerNode> nodesMap = new HashMap<>();
		List<List<GraphLayerNode>> guiLayers = modelLayers.stream()
				.map(layer -> layer.stream()
						.map(modelNode -> nodesMap.computeIfAbsent(modelNode, this::createLayerNode))
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

	private GraphLayerNode createLayerNode(GraphModelNode modelNode) {
		Object content = modelNode.getContent();

		Node layerNodeContent;
		if (content instanceof Intermediary) {
			layerNodeContent = new Group();
		} else if (content instanceof CodeItem) {
			CodeItem item = (CodeItem) content;
			// TODO use proper graphics
			String name = item.getId()//
					.replaceAll("\\.?[^()]+\\.", "")// Remove packages
					.replaceAll(".+\\$", "")// Remove parent class & "lambda"
					.replaceAll("\\(.+\\)", "(...)")// Reduce arguments types
					.replaceAll("\\).+", ")");// Remove return type
			char prefix = item instanceof Package ? 'P'
					: item instanceof Type ? 'T'// TODO 'C' & 'I'
							: item instanceof Method ? 'M'
									: item instanceof Constructor ? 'Z' : item instanceof Lambda ? 'L' : '?';
			Label label = new Label(String.format("[%s] %s", prefix, name));
			label.setAlignment(Pos.CENTER_LEFT);
			// TODO remove border
			label.setBorder(new Border(
					new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
			label.setTooltip(new Tooltip(item.getId()));
			layerNodeContent = label;
		} else {
			throw new IllegalStateException("Unmanaged case: " + content.getClass());
		}

		GraphLayerNode layerNode = new GraphLayerNode(layerNodeContent);
		// TODO remove border
		layerNode.setBorder(new Border(
				new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
		layerNode.relocate(0, 0);

		return layerNode;
	}

	private void sort(List<Collection<GraphModelNode>> layers) {
		if (layers.isEmpty()) {
			// No need to sort
		} else {
			layers.replaceAll(ArrayList::new);

			List<GraphModelNode> roots = (List<GraphModelNode>) layers.get(0);
			Comparator<GraphModelNode> idComparator = comparing(node -> node.getId());
			roots.sort(idComparator);

			for (int i = 0; i < layers.size() - 1; i++) {
				List<GraphModelNode> parentsLayer = (List<GraphModelNode>) layers.get(i);
				Comparator<GraphModelNode> parentsComparator = comparing(node -> {
					// Assume single parent because hierarchy
					GraphModelNode parent = node.getParents().iterator().next();
					return parentsLayer.indexOf(parent);
				});

				List<GraphModelNode> childrenLayer = (List<GraphModelNode>) layers.get(i + 1);
				childrenLayer.sort(parentsComparator.thenComparing(idComparator));
			}
		}
	}

	private void addIntermediaries(List<Collection<GraphModelNode>> layers, GraphModel layoutModel) {
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

	private void updateLayout(GraphModel layoutModel, GraphModelNode parent, GraphModelNode child,
			GraphModelNode intermediary) {
		layoutModel.getNodes().add(intermediary);

		layoutModel.getEdges().remove(new SimpleGraphModelEdge(parent, child));
		layoutModel.getEdges().add(new SimpleGraphModelEdge(parent, intermediary));
		layoutModel.getEdges().add(new SimpleGraphModelEdge(intermediary, child));
	}

	private void replaceParentsAndChildren(GraphModelNode parent, GraphModelNode child, GraphModelNode intermediary) {
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

	private GraphModelNode createIntermediary(GraphModelNode parent, GraphModelNode child) {
		Intermediary inter = new Intermediary(parent, child);
		return new SimpleGraphModelNode(inter.getId(), inter);
	}

	private List<Collection<GraphModelNode>> distributeIntoLayers(GraphModel layoutModel) {
		Collection<GraphModelNode> nodes = layoutModel.getNodes();
		List<Collection<GraphModelNode>> layers = new LinkedList<>();

		if (nodes.isEmpty()) {
			// Nothing to distribute
		} else {
			layers.add(new HashSet<GraphModelNode>(nodes));
			do {
				Collection<GraphModelNode> currentRoots = layers.get(0);
				Collection<GraphModelNode> newRoots = new HashSet<>();
				insertParentsInNextLayer(currentRoots, newRoots);
				insertBiggerScopesInNextLayer(currentRoots, newRoots);
				currentRoots.removeAll(newRoots);
				layers.add(0, newRoots);
			} while (!layers.get(0).isEmpty());
			layers.remove(0);// Last added layer (empty)
		}

		return layers;
	}

	private void insertParentsInNextLayer(Collection<GraphModelNode> currentLayer,
			Collection<GraphModelNode> aboveLayer) {
		currentLayer.stream()//
				.flatMap(node -> node.getParents().stream())//
				.forEach(aboveLayer::add);
	}

	private void insertBiggerScopesInNextLayer(Collection<GraphModelNode> currentLayer,
			Collection<GraphModelNode> aboveLayer) {
		List<GraphModelNode> methods = new LinkedList<>();
		List<GraphModelNode> types = new LinkedList<>();
		List<GraphModelNode> packages = new LinkedList<>();
		currentLayer.forEach(node -> {
			CodeItem item = (CodeItem) node.getContent();
			if (item instanceof Method || item instanceof Constructor || item instanceof StaticBlock
					|| item instanceof Lambda) {
				methods.add(node);
			} else if (item instanceof Type) {
				types.add(node);
			} else if (item instanceof Package) {
				packages.add(node);
			} else {
				throw new RuntimeException("Unmanaged item: " + item.getClass());
			}
		});
		if (!methods.isEmpty()) {
			aboveLayer.addAll(types);
			aboveLayer.addAll(packages);
		} else if (!types.isEmpty()) {
			aboveLayer.addAll(packages);
		} else {
			// Already package level, nothing above
		}
	}
}
