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
import fr.vergne.stanos.gui.scene.graph.model.GraphModelEdge;
import fr.vergne.stanos.gui.scene.graph.model.GraphModelNode;
import fr.vergne.stanos.gui.scene.graph.model.SimpleGraphModel;
import fr.vergne.stanos.gui.scene.graph.model.SimpleGraphModelEdge;
import fr.vergne.stanos.gui.scene.graph.model.SimpleGraphModelNode;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
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

// TODO Generalize by removing CodeItem dependencies
public class TreeLayout implements GraphLayout {

	private static final NumberExpression ZERO_EXPRESSION = DoubleProperty
			.readOnlyDoubleProperty(new SimpleDoubleProperty(0));

	@FunctionalInterface
	interface ReadOnlyPropertyAccessor {
		ReadOnlyDoubleProperty get(GraphLayerNode node);
	}

	@FunctionalInterface
	interface PropertyAccessor {
		DoubleProperty get(GraphLayerNode node);
	}

	@FunctionalInterface
	interface PropertySupplier {
		ReadOnlyDoubleProperty get();
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
	private final Anchor anchor;

	private final PropertyAccessor depthCoordinate;
	private final ReadOnlyPropertyAccessor thickness;
	private final PropertySupplier depthSpacing;

	private final PropertyAccessor spreadCoordinate;
	private final ReadOnlyPropertyAccessor spreadSize;
	private final PropertySupplier spreadSpacing;

	private static int layerSpacingX = 50;// TODO store in conf
	private static int layerSpacingY = 0;// TODO store in conf

	public TreeLayout() {
		this(//
				Direction.NORMAL, //
				Anchor.SURFACE, //
				GraphLayerNode::layoutXProperty, //
				GraphLayerNode::widthProperty, //
				() -> new SimpleDoubleProperty(layerSpacingX), //
				GraphLayerNode::layoutYProperty, //
				GraphLayerNode::heightProperty, //
				() -> new SimpleDoubleProperty(layerSpacingY));
	}

	public TreeLayout(//
			Direction direction, //
			Anchor anchor, //
			PropertyAccessor depthCoordinate, //
			ReadOnlyPropertyAccessor depthSize, //
			PropertySupplier depthSpacing, //
			PropertyAccessor spreadCoordinate, //
			ReadOnlyPropertyAccessor spreadSize, //
			PropertySupplier spreadSpacing) {
		this.direction = direction;
		this.anchor = anchor;

		this.depthCoordinate = depthCoordinate;
		this.thickness = depthSize;
		this.depthSpacing = depthSpacing;

		this.spreadCoordinate = spreadCoordinate;
		this.spreadSize = spreadSize;
		this.spreadSpacing = spreadSpacing;
	}

	@Override
	public GraphLayer layout(GraphModel model) {
		model = mutableCopy(model);

		List<Collection<GraphModelNode>> modelLayers = distributeIntoLayers(model);
		addIntermediaries(modelLayers, model);
		sort(modelLayers);

		List<List<GraphLayerNode>> guiLayers = prepareGuiLayers(modelLayers);
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
		}
	}

	private void centerParentsOnChildren(List<List<GraphLayerNode>> guiLayers) {
		for (int i = guiLayers.size() - 2; i >= 0; i--) {
			for (GraphLayerNode node : guiLayers.get(i)) {
				Collection<GraphLayerNode> children = node.getGraphNodeChildren();

				NumberExpression centerSum = null;
				for (GraphLayerNode child : children) {
					NumberExpression childCoordinate = spreadCoordinate.get(child);
					NumberExpression childSize = spreadSize.get(child);
					NumberExpression childCenter = childCoordinate.add(childSize.divide(2));
					centerSum = centerSum == null ? childCenter : centerSum.add(childCenter);
				}

				NumberExpression nodeCenter = centerSum.divide(children.size());
				NumberExpression nodeSize = spreadSize.get(node);
				NumberExpression nodeCoordinate = nodeCenter.subtract(nodeSize.divide(2));
				spreadCoordinate.get(node).bind(nodeCoordinate);
			}
		}
	}

	private void spreadLeaves(List<List<GraphLayerNode>> guiLayers) {
		ReadOnlyDoubleProperty spacing = spreadSpacing.get();
		List<GraphLayerNode> layer = guiLayers.get(guiLayers.size() - 1);
		GraphLayerNode previousNode = null;
		for (GraphLayerNode node : layer) {
			if (previousNode == null) {
				spreadCoordinate.get(node).set(0);
			} else {
				NumberExpression previousSize = spreadSize.get(previousNode);
				NumberExpression previousCoordinate = spreadCoordinate.get(previousNode);
				NumberExpression nodeCoordinate = previousCoordinate.add(previousSize).add(spacing);
				spreadCoordinate.get(node).bind(nodeCoordinate);
			}
			previousNode = node;
		}
	}

	private void bindLayersCoordinates(List<List<GraphLayerNode>> guiLayers) {
		NumberExpression layerSpacing = direction.apply(depthSpacing.get());
		List<NumberExpression> layerThicknesses = createLayerThicknessProperties(guiLayers);
		for (int i = 0; i < guiLayers.size(); i++) {
			NumberExpression parentLayerThickness = i == 0 ? null : direction.apply(layerThicknesses.get(i - 1));
			for (GraphLayerNode node : guiLayers.get(i)) {
				Collection<GraphLayerNode> parents = node.getGraphNodeParents();
				NumberExpression layerThickness = direction.apply(layerThicknesses.get(i));
				if (parents.isEmpty()) {
					// Root starts at the origin
					depthCoordinate.get(node).set(0);
				} else {
					GraphLayerNode parent = parents.iterator().next();// Assume single parent because tree
					NumberExpression parentThickness = thickness.get(parent);
					NumberExpression nodeThickness = thickness.get(node);

					// TODO clarify anchors: why some directed and others not?
					NumberExpression parentAnchor = direction.apply(anchor).compute(parentThickness);
					NumberExpression nodeAnchor = direction.apply(anchor).compute(nodeThickness);
					NumberExpression deltaAnchor = anchor.revert().compute(parentLayerThickness);
					NumberExpression layerAnchor = anchor.compute(layerThickness);
					NumberExpression layerCoordinate = depthCoordinate.get(parent)//
							.add(parentAnchor)//
							.add(deltaAnchor)//
							.add(layerSpacing)//
							.add(layerAnchor);

					NumberExpression nodeCoordinate = layerCoordinate.subtract(nodeAnchor);
					depthCoordinate.get(node).bind(nodeCoordinate);
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
		private final GraphModelNode parent;
		private final GraphModelNode child;
		private final String id;

		public Intermediary(GraphModelNode parent, GraphModelNode child) {
			while (parent instanceof Intermediary) {
				parent = ((Intermediary) parent).parent;
			}
			while (child instanceof Intermediary) {
				child = ((Intermediary) child).child;
			}
			this.id = parent.getId() + "@" + child.getId();
			this.parent = parent;
			this.child = child;
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

	private GraphModel mutableCopy(GraphModel model) {
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
