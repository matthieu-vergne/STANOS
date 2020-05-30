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
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

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
import fr.vergne.stanos.gui.scene.graph.model.builder.GraphModelBuilder;
import fr.vergne.stanos.gui.scene.graph.model.builder.GraphModelBuilder.GraphModelBuilderNode;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
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
import javafx.scene.shape.Rectangle;

//TODO Generalize by removing CodeItem dependencies
//TODO Extract algorithm for easy testing (remove javaFX dependencies)
public class TreeLayout implements GraphLayout {

	private static final NumberExpression ZERO_EXPRESSION = DoubleProperty
			.readOnlyDoubleProperty(new SimpleDoubleProperty(0));

	@FunctionalInterface
	public interface ExpressionAccessor {
		NumberExpression get(GraphLayerNode node);
	}

	@FunctionalInterface
	public interface PropertyAccessor {
		DoubleProperty get(GraphLayerNode node);
	}

	public static enum Direction {
		NORMAL(UnaryOperator.identity(), UnaryOperator.identity()), OPPOSITE(NumberExpression::negate, Anchor::revert);

		private final UnaryOperator<NumberExpression> transformation;
		private final UnaryOperator<Anchor> transformation2;

		private Direction(UnaryOperator<NumberExpression> transformation, UnaryOperator<Anchor> transformation2) {
			this.transformation = transformation;
			this.transformation2 = transformation2;
		}

		private NumberExpression apply(NumberExpression property) {
			return transformation.apply(property);
		}

		private Anchor apply(Anchor anchor) {
			return transformation2.apply(anchor);
		}
	}

	public static enum Anchor {
		SURFACE(thickness -> ZERO_EXPRESSION), CENTER(thickness -> thickness.divide(2)), GROUND(thickness -> thickness);

		private final UnaryOperator<NumberExpression> transformation;

		private Anchor(UnaryOperator<NumberExpression> transformation) {
			this.transformation = transformation;
		}

		private NumberExpression compute(NumberExpression thickness) {
			return transformation.apply(thickness);
		}

		private Anchor revert() {
			switch (this) {
			case SURFACE:
				return Anchor.GROUND;
			case CENTER:
				return Anchor.CENTER;
			case GROUND:
				return Anchor.SURFACE;
			default:
				throw new RuntimeException("Unmanaged anchor: " + this);
			}
		}
	}

	private final Direction direction;
	private final Anchor layerAnchor;
	private final Anchor nodeAnchor;

	private final PropertyAccessor depthCoordinate;
	private final ExpressionAccessor thickness;

	private final PropertyAccessor spreadCoordinate;
	private final ExpressionAccessor spreading;
	private final NumberExpression neighborsSpacing;
	private final NumberExpression layersSpacing;

	public TreeLayout(//
			Direction direction, //
			Anchor anchor, //
			PropertyAccessor depthCoordinate, //
			ExpressionAccessor depthSize, //
			NumberExpression depthSpacing, //
			PropertyAccessor spreadCoordinate, //
			ExpressionAccessor spreadSize, //
			NumberExpression spreadSpacing) {
		this.direction = direction;
		this.layerAnchor = anchor;
		this.nodeAnchor = direction.apply(anchor);// TODO why this one is directed?

		this.depthCoordinate = depthCoordinate;
		this.thickness = depthSize;
		this.layersSpacing = depthSpacing;

		this.spreadCoordinate = spreadCoordinate;
		this.spreading = spreadSize;
		this.neighborsSpacing = spreadSpacing;
	}

	@Override
	public GraphLayer layout(GraphModel model) {
		GraphModelBuilder<Object> newModel = GraphModelBuilder.createFromModel(obj -> {
			if (obj instanceof CodeItem) {
				return ((CodeItem) obj).getId();
			} else if (obj instanceof Intermediary) {
				return ((Intermediary) obj).getId();
			} else {
				throw new RuntimeException("Unmanaged type of content: " + obj.getClass());
			}
		}, model);

		List<Collection<GraphModelBuilderNode<Object>>> modelLayers = distributeIntoLayers(newModel);
		addIntermediaries(newModel, modelLayers);
		sort(newModel, modelLayers);

		List<List<GraphLayerNode>> guiLayers = prepareGuiLayers(newModel, modelLayers);
		bindLayersCoordinates(guiLayers);
		bindNeighborsCoordinates(guiLayers);

		Collection<GraphLayerNode> layerNodes = guiLayers.stream()//
				.flatMap(layer -> layer.stream())//
				.collect(toList());
		Collection<GraphLayerEdge> layerEdges = layerNodes.stream().//
				flatMap(parent -> parent.getGraphNodeChildren().stream()//
						.map(child -> new GraphLayerEdge(parent, child)))//
				.collect(toList());
		GraphLayer graphLayer = new GraphLayer(layerNodes, layerEdges);

		// TODO Remove
		addBackgrounds(graphLayer, guiLayers, layerNodes);

		return graphLayer;
	}

	private void addBackgrounds(GraphLayer graphLayer, List<List<GraphLayerNode>> guiLayers,
			Collection<GraphLayerNode> layerNodes) {
		Function<GraphLayerNode, NumberExpression> x1 = node -> node.layoutXProperty();
		Function<GraphLayerNode, NumberExpression> y1 = node -> node.layoutYProperty();
		Function<GraphLayerNode, NumberExpression> x2 = node -> node.layoutXProperty().add(node.widthProperty());
		Function<GraphLayerNode, NumberExpression> y2 = node -> node.layoutYProperty().add(node.heightProperty());
		BinaryOperator<NumberExpression> min = (e1, e2) -> Bindings.min(e1, e2);
		BinaryOperator<NumberExpression> max = (e1, e2) -> Bindings.max(e1, e2);

		// Layers
		for (List<GraphLayerNode> layer : guiLayers) {
			NumberExpression startX = reduceCoordinates(layer, x1, min);
			NumberExpression startY = reduceCoordinates(layer, y1, min);
			NumberExpression endX = reduceCoordinates(layer, x2, max);
			NumberExpression endY = reduceCoordinates(layer, y2, max);

			Rectangle rectangle = new Rectangle();
			rectangle.setFill(Color.CYAN);
			rectangle.layoutXProperty().bind(startX);
			rectangle.layoutYProperty().bind(startY);
			rectangle.widthProperty().bind(endX.subtract(startX));
			rectangle.heightProperty().bind(endY.subtract(startY));

			graphLayer.getChildren().add(0, rectangle);
		}

		// Packages
		int extraSize = 3;
		{
			Collection<GraphLayerNode> layer = layerNodes.stream()//
					.filter(node -> node.toString().contains("[P]"))//
					.collect(Collectors.toList());

			NumberExpression startX = reduceCoordinates(layer, x1, min);
			NumberExpression startY = reduceCoordinates(layer, y1, min);
			NumberExpression endX = reduceCoordinates(layer, x2, max);
			NumberExpression endY = reduceCoordinates(layer, y2, max);

			Rectangle rectangle = new Rectangle();
			rectangle.setFill(Color.RED);
			rectangle.layoutXProperty().bind(startX.subtract(extraSize));
			rectangle.layoutYProperty().bind(startY.subtract(extraSize));
			rectangle.widthProperty().bind(endX.subtract(startX).add(2 * extraSize));
			rectangle.heightProperty().bind(endY.subtract(startY).add(2 * extraSize));

			graphLayer.getChildren().add(0, rectangle);
		}

		// Types
		{
			Collection<GraphLayerNode> layer = layerNodes.stream()//
					.filter(node -> node.toString().contains("[T]"))//
					.collect(Collectors.toList());

			NumberExpression startX = reduceCoordinates(layer, x1, min);
			NumberExpression startY = reduceCoordinates(layer, y1, min);
			NumberExpression endX = reduceCoordinates(layer, x2, max);
			NumberExpression endY = reduceCoordinates(layer, y2, max);

			Rectangle rectangle = new Rectangle();
			rectangle.setFill(Color.GREEN);
			rectangle.layoutXProperty().bind(startX.subtract(extraSize));
			rectangle.layoutYProperty().bind(startY.subtract(extraSize));
			rectangle.widthProperty().bind(endX.subtract(startX).add(2 * extraSize));
			rectangle.heightProperty().bind(endY.subtract(startY).add(2 * extraSize));

			graphLayer.getChildren().add(0, rectangle);
		}

		// Methods
		{
			Collection<GraphLayerNode> layer = layerNodes.stream()//
					.filter(node -> node.toString().contains("[M]")//
							|| node.toString().contains("[Z]")//
							|| node.toString().contains("[L]")//
							|| node.toString().contains("[S]"))//
					.collect(Collectors.toList());

			NumberExpression startX = reduceCoordinates(layer, x1, min);
			NumberExpression startY = reduceCoordinates(layer, y1, min);
			NumberExpression endX = reduceCoordinates(layer, x2, max);
			NumberExpression endY = reduceCoordinates(layer, y2, max);

			Rectangle rectangle = new Rectangle();
			rectangle.setFill(Color.BLUE);
			rectangle.layoutXProperty().bind(startX.subtract(extraSize));
			rectangle.layoutYProperty().bind(startY.subtract(extraSize));
			rectangle.widthProperty().bind(endX.subtract(startX).add(extraSize));
			rectangle.heightProperty().bind(endY.subtract(startY).add(extraSize));

			graphLayer.getChildren().add(0, rectangle);
		}
	}

	private NumberExpression reduceCoordinates(Collection<GraphLayerNode> nodes,
			Function<GraphLayerNode, NumberExpression> mapper, BinaryOperator<NumberExpression> reducer) {
		return nodes.stream().map(mapper).reduce(reducer).orElse(ZERO_EXPRESSION);
	}

	private void bindNeighborsCoordinates(List<List<GraphLayerNode>> guiLayers) {
		if (guiLayers.isEmpty()) {
			// Nothing to relocate
		} else {
			spreadLeaves(guiLayers);
			centerParentsOnChildren(guiLayers);
			// FIXME parents overlap due to small children
		}
	}

	private void centerParentsOnChildren(List<List<GraphLayerNode>> guiLayers) {
		for (int i = guiLayers.size() - 2; i >= 0; i--) {
			for (GraphLayerNode node : guiLayers.get(i)) {
				Collection<GraphLayerNode> children = node.getGraphNodeChildren();

				NumberExpression centerSum = null;
				for (GraphLayerNode child : children) {
					NumberExpression childCoordinate = spreadCoordinate.get(child);
					NumberExpression childSize = spreading.get(child);
					NumberExpression childCenter = childCoordinate.add(childSize.divide(2));
					centerSum = centerSum == null ? childCenter : centerSum.add(childCenter);
				}

				NumberExpression nodeCenter = centerSum.divide(children.size());
				NumberExpression nodeSize = spreading.get(node);
				NumberExpression nodeCoordinate = nodeCenter.subtract(nodeSize.divide(2));
				spreadCoordinate.get(node).bind(nodeCoordinate);
			}
		}
	}

	private void spreadLeaves(List<List<GraphLayerNode>> guiLayers) {
		List<GraphLayerNode> layer = guiLayers.get(guiLayers.size() - 1);
		GraphLayerNode previousNode = null;
		for (GraphLayerNode node : layer) {
			if (previousNode == null) {
				spreadCoordinate.get(node).set(0);
			} else {
				NumberExpression previousSize = spreading.get(previousNode);
				NumberExpression previousCoordinate = spreadCoordinate.get(previousNode);
				NumberExpression nodeCoordinate = previousCoordinate.add(previousSize).add(neighborsSpacing);
				spreadCoordinate.get(node).bind(nodeCoordinate);
			}
			previousNode = node;
		}
	}

	private void bindLayersCoordinates(List<List<GraphLayerNode>> guiLayers) {
		List<NumberExpression> layerThicknesses = createLayerThicknessProperties(guiLayers);
		for (int i = 0; i < guiLayers.size(); i++) {
			for (GraphLayerNode node : guiLayers.get(i)) {
				Collection<GraphLayerNode> parents = node.getGraphNodeParents();
				if (parents.isEmpty()) {
					// Root starts at the origin
					depthCoordinate.get(node).set(0);
				} else {
					// Assume single parent because assumed to be a tree
					GraphLayerNode parentNode = parents.iterator().next();

					/**
					 * The coordinate of the node is based on its parents coordinate. Consequently,
					 * the separation of the two goes through 2 consecutive layers: the parent layer
					 * and the current layer. Assuming no space is added between the layers, the
					 * diagram below shows that the nodes ('o' symbols) always have a distance equal
					 * to the thickness of a whole layer:
					 * 
					 * <pre>
					 * Anchors | Surface | Center  | Ground  |
					 *         |────o────|─────────|─────────|
					 * Parent  |         |         |         |
					 * layer   |         |    o----|---------|--^
					 *         |         |         |         |  │
					 *         |────o────|─────────|────o────|  │ 1 layer thickness
					 *         |         |         |         |  │
					 * Current |         |    o----|---------|--v
					 * layer   |         |         |         |
					 *         |─────────|─────────|────o────|
					 * </pre>
					 * 
					 * This diagram also shows that each layer contributes to this distance in a
					 * complementary way: what is not consumed in one is consumed in the other.
					 * 
					 * However, layers do not always have the same thickness, so the result cannot
					 * be computed simply by taking one layer or the other. We must take the
					 * relevant part of each layer based on their respective thickness.
					 * 
					 * Since the part consumed in a given layer depends on the anchor of the node in
					 * that layer, we also need to consider this aspect.
					 * 
					 * The computation below brings together all these elements:
					 * <ul>
					 * <li>we retrieve a part for each layer
					 * <li>we compute it from the anchor and thickness of the corresponding layer
					 * <li>the complementariness of both layers leads to use opposite anchors
					 * </ul>
					 */
					var parentLayerPart = layerAnchor.revert().compute(layerThicknesses.get(i - 1));
					var currentLayerPart = layerAnchor.compute(layerThicknesses.get(i));

					/**
					 * Before to combine them, we must also consider the space which separates the
					 * two layers, as shown in the more complete diagram below:
					 * 
					 * <pre>
					 * Anchors | Surface | Center  | Ground  |
					 *         |────o────|─────────|─────────|
					 * Parent  |         |         |         |
					 * layer   |         |    o    |         |
					 *         |         |         |         |
					 *         |─────────|─────────|─────────|--^
					 *         |         |         |         |  │ Layer spacing
					 *         |────o────|─────────|────o────|--v
					 *         |         |         |         |
					 * Current |         |    o    |         |
					 * layer   |         |         |         |
					 *         |─────────|─────────|────o────|
					 * </pre>
					 * 
					 * This spacing is constant, so we just need to add it to the two layer parts.
					 * 
					 * Then, because this layout is generic, it can cope with both positive and
					 * negative directions. Thus, the sum needs to be adapted based on the direction
					 * to consider.
					 */
					var layersPart = direction.apply(parentLayerPart.add(currentLayerPart).add(layersSpacing));

					/**
					 * With the previous elements, we considered nodes as points in the layers. Now
					 * we also need to consider their own thickness. The diagram below shows
					 * different examples for the center anchor:
					 * 
					 * <pre>
					 * Bigger  |  Same   | Parent  | Current |
					 *         |─────────|─────────|─────────|
					 * Parent  |         |    █    |         |
					 * layer   |    █    |    █    |    █    |
					 *         |         |    █    |         |
					 *         |─────────|─────────|─────────|
					 *         |         |         |         |
					 *         |─────────|─────────|─────────|
					 *         |         |         |    █    |
					 * Current |    █    |    █    |    █    |
					 * layer   |         |         |    █    |
					 *         |─────────|─────────|─────────|
					 * </pre>
					 * 
					 * Let's consider the trivial case in the first example, where both nodes have
					 * the same thickness. Because the nodes are placed within the same coordinate
					 * system, they use the same origin. With a center anchor, the origin is at the
					 * center of the nodes:
					 * 
					 * <pre>
					 *         |─────────|
					 * Parent  |         | origin of parent node
					 * layer   |    █────|────────────────────────
					 *         |         |                        ^
					 *         |─────────|                        │ 1 layer thickness
					 *         |         |                        │ + layer spacing
					 *         |─────────|                        │
					 *         |         | origin of current node v
					 * Current |    █────|────────────────────────
					 * layer   |         |
					 *         |─────────|
					 * </pre>
					 * 
					 * In this trivial case the distance remains always the same. The thickness of
					 * the nodes is irrelevant, because we always consider the center of each node.
					 * 
					 * Now, let's add complexity by changing the anchor. We keep the current model
					 * to facilitate the explanation, but let's take a surface anchor for the node:
					 * 
					 * <pre>
					 *         |─────────|
					 * Parent  |         | origin of parent node
					 * layer   |    █¯¯¯¯|¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯^
					 *         |         |                        │
					 *         |─────────|                        │ 1 layer thickness
					 *         |         |                        │ + layer spacing
					 *         |─────────|                        │
					 *         |         | origin of current node v
					 * Current |    █¯¯¯¯|¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯
					 * layer   |         |
					 *         |─────────|
					 * </pre>
					 * 
					 * In this case, the thickness of each node is the same, so no adaptation is
					 * required: the distance between the two nodes origins is exactly the layer
					 * part. If the anchor of the node changes, both origins are translated as much,
					 * so no adaptation is required as long as the nodes have the same thickness.
					 * 
					 * However, if the parent node is bigger as shown below, its origin moves. Now,
					 * the current node should be placed farther to compensate the difference of
					 * thickness between the two nodes:
					 * 
					 * <pre>
					 *         |─────────|   ___________________
					 * Parent  |    █¯¯¯¯|¯¯║ Adaptation        ^
					 * layer   |    █¯¯¯¯|¯¯^                   │
					 *         |    █    |  │                   │ Total
					 *         |─────────|  │ 1 layer thickness │ distance
					 *         |         |  │ + layer spacing   │
					 *         |─────────|  │                   │
					 *         |         |  v                   v
					 * Current |    █¯¯¯¯|¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯
					 * layer   |         |
					 *         |─────────|
					 * </pre>
					 * 
					 * If the current node is the thickest, the adaptation is reversed and the
					 * distance reduced.
					 * 
					 * In the general case, the adaptation is related to the difference in thickness
					 * between the two nodes. Moreover, it also depends on the anchor, as we
					 * explained before that a center anchor never needs an adaptation.
					 * 
					 * The code below reflects all these elements:
					 * <ul>
					 * <li>we retrieve a part for each node
					 * <li>we compute it from the anchor and thickness of the corresponding node
					 * <li>the adaptation is the difference between both parts
					 * <li>the total distance is the layer part + the adaptation
					 * </ul>
					 */
					var parentNodePart = nodeAnchor.compute(thickness.get(parentNode));
					var currentNodePart = nodeAnchor.compute(thickness.get(node));
					var adaptation = parentNodePart.subtract(currentNodePart);
					var totalDistance = layersPart.add(adaptation);

					// Place the current node at distance from the parent node
					depthCoordinate.get(node).bind(depthCoordinate.get(parentNode).add(totalDistance));
				}
			}
		}
	}

	private List<NumberExpression> createLayerThicknessProperties(List<List<GraphLayerNode>> guiLayers) {
		List<NumberExpression> layerThicknesses = new LinkedList<>();
		for (List<GraphLayerNode> layer : guiLayers) {
			NumberExpression layerThickness = null;
			for (GraphLayerNode node : layer) {
				NumberExpression nodeThickness = thickness.get(node);
				layerThickness = layerThickness == null ? nodeThickness : Bindings.max(layerThickness, nodeThickness);
			}
			layerThicknesses.add(layerThickness);
		}
		return layerThicknesses;
	};

	private List<List<GraphLayerNode>> prepareGuiLayers(GraphModelBuilder<Object> model,
			List<Collection<GraphModelBuilderNode<Object>>> modelLayers) {
		// Create isolated layer nodes
		Map<GraphModelBuilderNode<Object>, GraphLayerNode> nodesMap = new HashMap<>();
		List<List<GraphLayerNode>> guiLayers = modelLayers.stream()
				.map(layer -> layer.stream()
						.map(modelNode -> nodesMap.computeIfAbsent(modelNode, this::createLayerNode))
						.collect(toUnmodifiableList()))
				.collect(toUnmodifiableList());

		// Retrieve parents & children
		nodesMap.entrySet().forEach(entry -> {
			GraphModelBuilderNode<Object> modelNode = entry.getKey();
			GraphLayerNode layerNode = entry.getValue();
			model.getChildren(modelNode).stream().map(nodesMap::get).forEach(layerNode::addGraphNodeChild);
			model.getParents(modelNode).stream().map(nodesMap::get).forEach(layerNode::addGraphNodeParent);
		});

		return guiLayers;
	}

	private GraphLayerNode createLayerNode(GraphModelBuilderNode<Object> modelNode) {
		Object content = modelNode.getContent();

		Node layerNodeContent;
		if (content instanceof Intermediary) {
			layerNodeContent = new Group();
		} else if (content instanceof CodeItem) {
			CodeItem item = (CodeItem) content;
			// TODO use proper graphics
			String name = item.getId()//
					.replaceAll("\\.?[^()]+\\.", "")// Remove packages
					.replaceAll(".+\\$([^0-9])", "$1")// Remove declaring class
					.replaceAll("\\(.+\\)", "(...)")// Reduce arguments types
					.replaceAll("\\).+", ")");// Remove return type
			char prefix = item instanceof Package ? 'P'//
					: item instanceof Type ? 'T'// TODO 'C' & 'I'
							: item instanceof Method ? 'M'//
									: item instanceof Constructor ? 'Z'//
											: item instanceof Lambda ? 'L'//
													: item instanceof StaticBlock ? 'S'//
															: '?';
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

		GraphLayerNode layerNode = new GraphLayerNode(modelNode.getModel(), layerNodeContent);
		// TODO remove border
		layerNode.setBorder(new Border(
				new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
		layerNode.relocate(0, 0);

		return layerNode;
	}

	private void sort(GraphModelBuilder<Object> model, List<Collection<GraphModelBuilderNode<Object>>> layers) {
		if (layers.isEmpty()) {
			// No need to sort
		} else {
			layers.replaceAll(ArrayList::new);

			List<GraphModelBuilderNode<Object>> roots = (List<GraphModelBuilderNode<Object>>) layers.get(0);
			Comparator<GraphModelBuilderNode<Object>> idComparator = comparing(node -> node.getId());
			roots.sort(idComparator);

			for (int i = 0; i < layers.size() - 1; i++) {
				List<GraphModelBuilderNode<Object>> parentsLayer = (List<GraphModelBuilderNode<Object>>) layers.get(i);
				Comparator<GraphModelBuilderNode<Object>> parentsComparator = comparing(node -> {
					// Assume single parent because assumed to be a tree
					GraphModelBuilderNode<Object> parent = model.getParents(node).iterator().next();
					return parentsLayer.indexOf(parent);
				});

				List<GraphModelBuilderNode<Object>> childrenLayer = (List<GraphModelBuilderNode<Object>>) layers.get(i + 1);
				childrenLayer.sort(parentsComparator.thenComparing(idComparator));
			}
		}
	}

	private void addIntermediaries(GraphModelBuilder<Object> model, List<Collection<GraphModelBuilderNode<Object>>> layers) {
		for (int i = 0; i < layers.size() - 1; i++) {
			Collection<GraphModelBuilderNode<Object>> currentLayer = layers.get(i);
			Collection<GraphModelBuilderNode<Object>> nextLayer = layers.get(i + 1);
			for (GraphModelBuilderNode<Object> parent : currentLayer) {
				for (GraphModelBuilderNode<Object> child : new ArrayList<>(model.getChildren(parent))) {
					if (nextLayer.contains(child)) {
						// No need for intermediary
					} else {
						GraphModelBuilderNode<Object> inter = model.addNode(new Intermediary(parent, child));
//						parent.getEdge(child.getContent()).insert(new Intermediary(parent, child));
						model.removeEdge(parent.getContent(), child.getContent());
						model.addEdge(parent.getContent(), inter.getContent());
						model.addEdge(inter.getContent(), child.getContent());

						nextLayer.add(inter);
					}
				}
			}
		}
	}

	private static class Intermediary {
		private final String id;

		public Intermediary(GraphModelBuilderNode<Object> parent, GraphModelBuilderNode<Object> child) {
			this.id = parent.getId() + "@" + child.getId();
		}

		public String getId() {
			return id;
		}
	}

	private List<Collection<GraphModelBuilderNode<Object>>> distributeIntoLayers(GraphModelBuilder<Object> model) {
		Collection<GraphModelBuilderNode<Object>> nodes = model.getNodes();
		List<Collection<GraphModelBuilderNode<Object>>> layers = new LinkedList<>();

		if (nodes.isEmpty()) {
			// Nothing to distribute
		} else {
			layers.add(new HashSet<>(nodes));
			do {
				Collection<GraphModelBuilderNode<Object>> currentRoots = layers.get(0);
				Collection<GraphModelBuilderNode<Object>> newRoots = new HashSet<>();
				insertParentsInNextLayer(model, currentRoots, newRoots);
				insertBiggerScopesInNextLayer(currentRoots, newRoots);
				currentRoots.removeAll(newRoots);
				layers.add(0, newRoots);
			} while (!layers.get(0).isEmpty());
			layers.remove(0);// Last added layer (empty)
		}

		return layers;
	}

	private void insertParentsInNextLayer(GraphModelBuilder<Object> model,
			Collection<GraphModelBuilderNode<Object>> currentLayer, Collection<GraphModelBuilderNode<Object>> aboveLayer) {
		currentLayer.stream()//
				.flatMap(node -> model.getParents(node).stream())//
				.forEach(aboveLayer::add);
	}

	private void insertBiggerScopesInNextLayer(Collection<GraphModelBuilderNode<Object>> currentLayer,
			Collection<GraphModelBuilderNode<Object>> aboveLayer) {
		List<GraphModelBuilderNode<Object>> methods = new LinkedList<>();
		List<GraphModelBuilderNode<Object>> types = new LinkedList<>();
		List<GraphModelBuilderNode<Object>> packages = new LinkedList<>();
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
