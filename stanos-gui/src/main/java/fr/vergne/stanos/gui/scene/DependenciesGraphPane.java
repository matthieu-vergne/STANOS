package fr.vergne.stanos.gui.scene;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import fr.vergne.stanos.dependency.Action;
import fr.vergne.stanos.dependency.Dependency;
import fr.vergne.stanos.dependency.codeitem.CodeItem;
import fr.vergne.stanos.dependency.codeitem.Constructor;
import fr.vergne.stanos.dependency.codeitem.Lambda;
import fr.vergne.stanos.dependency.codeitem.Method;
import fr.vergne.stanos.dependency.codeitem.Package;
import fr.vergne.stanos.dependency.codeitem.Type;
import fr.vergne.stanos.gui.configuration.Configuration;
import fr.vergne.stanos.gui.scene.graph.GraphView;
import fr.vergne.stanos.gui.scene.graph.edge.GraphEdge;
import fr.vergne.stanos.gui.scene.graph.layout.BottomToTopHierarchyLayout;
import fr.vergne.stanos.gui.scene.graph.layout.GraphLayout;
import fr.vergne.stanos.gui.scene.graph.layout.LeftToRightHierarchyLayout;
import fr.vergne.stanos.gui.scene.graph.layout.RightToLeftHierarchyLayout;
import fr.vergne.stanos.gui.scene.graph.layout.TopToBottomHierarchyLayout;
import fr.vergne.stanos.gui.scene.graph.model.GraphModel;
import fr.vergne.stanos.gui.scene.graph.model.GraphModelBuilder;
import fr.vergne.stanos.gui.scene.graph.node.GraphNode;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DependenciesGraphPane extends BorderPane {

	public DependenciesGraphPane(Configuration configuration, ObservableList<Dependency> dependencies) {
		int spacing = configuration.gui().globalSpacing();
		
		// TODO support more filters
		FilteredList<Dependency> filteredDependencies = dependencies
				.filtered(dep -> dep.getAction().equals(Action.DECLARES));
		// TODO update upon dependencies refresh
		filteredDependencies.addListener((ListChangeListener<Dependency>) x -> {
			System.err.println("********");
		});

		GraphView graphView = createGraphView(filteredDependencies);

		ObservableList<GraphLayout> layouts = createGraphLayouts();
		ChoiceBox<GraphLayout> layoutBox = new ChoiceBox<>(layouts);
		ReadOnlyObjectProperty<GraphLayout> selectedLayoutProperty = layoutBox.getSelectionModel()
				.selectedItemProperty();
		selectedLayoutProperty.addListener((observable, oldLayout, newLayout) -> {
			graphView.setLayout(newLayout);
		});
		layoutBox.getSelectionModel().select(0);// TODO from conf
		HBox options = new HBox(spacing, new Label("Layout:"), layoutBox);
		options.setAlignment(Pos.CENTER_LEFT);

		var graphPane = new ZoomableScrollPane(graphView);
		graphPane.setFitToWidth(true);
		graphPane.setFitToHeight(true);

		// TODO redo after each update (after graph.endUpdate())
		MouseGestures mouseGestures = new MouseGestures(graphPane::getScaleValue);
		graphView.getModel().getNodes().forEach(cell -> {
			mouseGestures.makeDraggable(cell);
		});

		setCenter(new VBox(spacing, options, graphPane));
		VBox.setVgrow(graphPane, Priority.ALWAYS);
	}

	private ObservableList<GraphLayout> createGraphLayouts() {
		return FXCollections.observableArrayList(new LeftToRightHierarchyLayout() {
			@Override
			public String toString() {
				return "→";
			}
		}, new RightToLeftHierarchyLayout() {
			@Override
			public String toString() {
				return "←";
			}
		}, new TopToBottomHierarchyLayout() {
			@Override
			public String toString() {
				return "↓";
			}
		}, new BottomToTopHierarchyLayout() {
			@Override
			public String toString() {
				return "↑";
			}
		});
	}

	private GraphView createGraphView(ObservableList<Dependency> dependencies) {
		GraphModel model = createGraphModel(dependencies);
		GraphView graphView = new GraphView(model);
		dependencies.addListener((InvalidationListener) observable -> {
			System.err.println("change");
			graphView.setModel(createGraphModel(dependencies));
		});
		return graphView;
	}

	private GraphModel createGraphModel(ObservableList<Dependency> dependencies) {
		GraphModelBuilder modelBuilder = new GraphModelBuilder();

		Map<CodeItem, GraphNode> nodesMap = new HashMap<>();
		Function<CodeItem, GraphNode> nodeBuilder = codeItem -> {
			return nodesMap.computeIfAbsent(codeItem, item -> {
				GraphNode node = createGraphNode(item);
				modelBuilder.addNode(node);
				return node;
			});
		};
		BiFunction<GraphNode, GraphNode, GraphEdge> edgeBuilder = (source, target) -> {
			// TODO update counter instead of multiple edges
			GraphEdge edge = new GraphEdge(source, target);
			modelBuilder.addEdge(edge);
			return edge;
		};

		dependencies.forEach(dep -> {
			GraphNode source = nodeBuilder.apply(dep.getSource());
			GraphNode target = nodeBuilder.apply(dep.getTarget());
			source.addGraphNodeChild(target);
			target.addGraphNodeParent(source);
			edgeBuilder.apply(source, target);
		});

		return modelBuilder.build().immutable();
	}

	private GraphNode createGraphNode(CodeItem item) {
		// TODO use proper graphics
		String name = item.getId().replaceAll("\\(.+\\)", "(...)").replaceAll("\\.?[^()]+\\.", "").replaceAll("\\).+",
				")");
		char prefix = item instanceof Package ? 'P'
				: item instanceof Type ? 'T'// TODO 'C' & 'I'
						: item instanceof Method ? 'M'
								: item instanceof Constructor ? 'Z' : item instanceof Lambda ? 'L' : '?';
		return new GraphNode(item.getId(), new Label(String.format("[%s] %s", prefix, name)));
	}
}
