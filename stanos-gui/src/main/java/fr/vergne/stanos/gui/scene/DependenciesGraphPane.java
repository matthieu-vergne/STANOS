package fr.vergne.stanos.gui.scene;

import static java.util.Comparator.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import fr.vergne.stanos.dependency.Action;
import fr.vergne.stanos.dependency.Dependency;
import fr.vergne.stanos.dependency.codeitem.CodeItem;
import fr.vergne.stanos.dependency.codeitem.Constructor;
import fr.vergne.stanos.dependency.codeitem.Lambda;
import fr.vergne.stanos.dependency.codeitem.Method;
import fr.vergne.stanos.dependency.codeitem.Package;
import fr.vergne.stanos.dependency.codeitem.StaticBlock;
import fr.vergne.stanos.dependency.codeitem.Type;
import fr.vergne.stanos.gui.configuration.Configuration;
import fr.vergne.stanos.gui.scene.graph.GraphView;
import fr.vergne.stanos.gui.scene.graph.layer.GraphLayerNode;
import fr.vergne.stanos.gui.scene.graph.layout.GraphLayout;
import fr.vergne.stanos.gui.scene.graph.layout.TreeLayout;
import fr.vergne.stanos.gui.scene.graph.layout.TreeLayout.Anchor;
import fr.vergne.stanos.gui.scene.graph.layout.TreeLayout.Direction;
import fr.vergne.stanos.gui.scene.graph.layout.TreeLayout.ExpressionAccessor;
import fr.vergne.stanos.gui.scene.graph.layout.TreeLayout.NodeRenderer;
import fr.vergne.stanos.gui.scene.graph.layout.TreeLayout.PropertyAccessor;
import fr.vergne.stanos.gui.scene.graph.model.GraphModel;
import fr.vergne.stanos.gui.scene.graph.model.builder.GraphModelBuilder;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.NumberExpression;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class DependenciesGraphPane extends BorderPane {

	// Store it as member to avoid GC
	private final FilteredList<Dependency> filteredDependencies;

	public DependenciesGraphPane(Configuration configuration, ObservableList<Dependency> dependencies) {
		// TODO support more filters
		filteredDependencies = dependencies.filtered(dep -> dep.getAction().equals(Action.DECLARES));

		/*
		 * COMPONENTS
		 */

		int spacing = configuration.gui().globalSpacing();

		ChoiceBox<GraphLayout<CodeItem>> layoutBox = new ChoiceBox<>(createGraphLayouts());
		HBox options = new HBox(spacing, new Label("Layout:"), layoutBox);
		options.setAlignment(Pos.CENTER_LEFT);

		// TODO Provide a model which auto updates upon filteredDependencies update
		// TODO Move filteredDependencies field to model
		GraphView graphView = new GraphView();
		ZoomableScrollPane graphPane = new ZoomableScrollPane(graphView);
		graphPane.setFitToWidth(true);
		graphPane.setFitToHeight(true);
		MouseGestures mouseGestures = new MouseGestures(graphPane::getScaleValue);
		graphPane.zoomTo(1);// TODO remove

		setCenter(new VBox(spacing, options, graphPane));
		VBox.setVgrow(graphPane, Priority.ALWAYS);

		/*
		 * OBSERVERS
		 */

		layoutBox.getSelectionModel().selectedItemProperty().addListener((observable, oldLayout, newLayout) -> {
			graphView.setLayout(newLayout);
		});
		filteredDependencies.addListener((InvalidationListener) observable -> {
			GraphModel<CodeItem> model = GraphModelBuilder.createFromEdges(CodeItem::getId, filteredDependencies,
					Dependency::getSource, Dependency::getTarget).build();
			graphView.setModel(model);
		});
		graphView.graphLayerProperty().addListener((observable, oldLayer, newLayer) -> {
			newLayer.getGraphNodes().forEach(mouseGestures::makeDraggable);
		});

		/**
		 * DEFAULTS
		 */

		layoutBox.getSelectionModel().select(9);// TODO from conf
	}

	private ObservableList<GraphLayout<CodeItem>> createGraphLayouts() {
		NumberExpression layersSpacing = new SimpleDoubleProperty(50);// TODO from conf
		NumberExpression neighborsSpacing = new SimpleDoubleProperty(0);// TODO from conf
		PropertyAccessor nodeX = GraphLayerNode::layoutXProperty;
		PropertyAccessor nodeY = GraphLayerNode::layoutYProperty;
		ExpressionAccessor nodeWidth = GraphLayerNode::widthProperty;
		ExpressionAccessor nodeHeight = GraphLayerNode::heightProperty;
		
		NodeRenderer nodeRenderer = this::renderNode;
		Comparator<Object> nodeContentComparator = createNodeComparator();
		Function<Object, String> nodeIdentifier = this::identifyNode;
		
		return FXCollections.observableArrayList(Arrays.asList(//
				new TreeLayout<CodeItem>(//
						Direction.NORMAL, Anchor.SURFACE, //
						nodeX, nodeWidth, layersSpacing, //
						nodeY, nodeHeight, neighborsSpacing, //
						nodeRenderer, nodeContentComparator, nodeIdentifier) {
					@Override
					public String toString() {
						return "→";
					}
				}, new TreeLayout<CodeItem>(//
						Direction.NORMAL, Anchor.CENTER, //
						nodeX, nodeWidth, layersSpacing, //
						nodeY, nodeHeight, neighborsSpacing, //
						nodeRenderer, nodeContentComparator, nodeIdentifier) {
					@Override
					public String toString() {
						return "→.";
					}
				}, new TreeLayout<CodeItem>(//
						Direction.NORMAL, Anchor.GROUND, //
						nodeX, nodeWidth, layersSpacing, //
						nodeY, nodeHeight, neighborsSpacing, //
						nodeRenderer, nodeContentComparator, nodeIdentifier) {
					@Override
					public String toString() {
						return "→→";
					}
				}, new TreeLayout<CodeItem>(//
						Direction.OPPOSITE, Anchor.SURFACE, //
						nodeX, nodeWidth, layersSpacing, //
						nodeY, nodeHeight, neighborsSpacing, //
						nodeRenderer, nodeContentComparator, nodeIdentifier) {
					@Override
					public String toString() {
						return "←";
					}
				}, new TreeLayout<CodeItem>(//
						Direction.OPPOSITE, Anchor.CENTER, //
						nodeX, nodeWidth, layersSpacing, //
						nodeY, nodeHeight, neighborsSpacing, //
						nodeRenderer, nodeContentComparator, nodeIdentifier) {
					@Override
					public String toString() {
						return ".←";
					}
				}, new TreeLayout<CodeItem>(//
						Direction.OPPOSITE, Anchor.GROUND, //
						nodeX, nodeWidth, layersSpacing, //
						nodeY, nodeHeight, neighborsSpacing, //
						nodeRenderer, nodeContentComparator, nodeIdentifier) {
					@Override
					public String toString() {
						return "←←";
					}
				}, new TreeLayout<CodeItem>(//
						Direction.NORMAL, Anchor.SURFACE, //
						nodeY, nodeHeight, layersSpacing, //
						nodeX, nodeWidth, neighborsSpacing, //
						nodeRenderer, nodeContentComparator, nodeIdentifier) {
					@Override
					public String toString() {
						return "↓";
					}
				}, new TreeLayout<CodeItem>(//
						Direction.NORMAL, Anchor.CENTER, //
						nodeY, nodeHeight, layersSpacing, //
						nodeX, nodeWidth, neighborsSpacing, //
						nodeRenderer, nodeContentComparator, nodeIdentifier) {
					@Override
					public String toString() {
						return "↓.";
					}
				}, new TreeLayout<CodeItem>(//
						Direction.NORMAL, Anchor.GROUND, //
						nodeY, nodeHeight, layersSpacing, //
						nodeX, nodeWidth, neighborsSpacing, //
						nodeRenderer, nodeContentComparator, nodeIdentifier) {
					@Override
					public String toString() {
						return "↓↓";
					}
				}, new TreeLayout<CodeItem>(//
						Direction.OPPOSITE, Anchor.SURFACE, //
						nodeY, nodeHeight, layersSpacing, //
						nodeX, nodeWidth, neighborsSpacing, //
						nodeRenderer, nodeContentComparator, nodeIdentifier) {
					@Override
					public String toString() {
						return "↑";
					}
				}, new TreeLayout<CodeItem>(//
						Direction.OPPOSITE, Anchor.CENTER, //
						nodeY, nodeHeight, layersSpacing, //
						nodeX, nodeWidth, neighborsSpacing, //
						nodeRenderer, nodeContentComparator, nodeIdentifier) {
					@Override
					public String toString() {
						return "↑.";
					}
				}, new TreeLayout<CodeItem>(//
						Direction.OPPOSITE, Anchor.GROUND, //
						nodeY, nodeHeight, layersSpacing, //
						nodeX, nodeWidth, neighborsSpacing, //
						nodeRenderer, nodeContentComparator, nodeIdentifier) {
					@Override
					public String toString() {
						return "↑↑";
					}
				}));
	}

	private String identifyNode(Object obj) {
		if (obj instanceof CodeItem) {
			return ((CodeItem) obj).getId();
		} else {
			throw new RuntimeException("Unmanaged type of content: " + obj.getClass());
		}
	}

	private Comparator<Object> createNodeComparator() {
		Map<Class<?>, Integer> scores = new HashMap<>();
		Stream.of(Method.class, Constructor.class, StaticBlock.class, Lambda.class)//
				.forEach(c -> scores.put(c, 0));
		Stream.of(Type.class)//
				.forEach(c -> scores.put(c, 1));
		Stream.of(Package.class)//
				.forEach(c -> scores.put(c, 2));

		Comparator<Object> nodeContentComparator = comparing(obj -> {
			return scores.computeIfAbsent(obj.getClass(), c -> {
				throw new RuntimeException("Unmanaged item: " + c);
			});
		});
		return nodeContentComparator;
	}

	private Node renderNode(Object content) {
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
		return label;
	}
}
