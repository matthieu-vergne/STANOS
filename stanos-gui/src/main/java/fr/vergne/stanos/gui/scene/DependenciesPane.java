package fr.vergne.stanos.gui.scene;

import static fr.vergne.stanos.gui.util.recursiveflatmapper.RecursiveFlatMapper.recursiveMapper;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.vergne.stanos.dependency.Action;
import fr.vergne.stanos.dependency.Dependency;
import fr.vergne.stanos.dependency.codeitem.CodeItem;
import fr.vergne.stanos.dependency.codeitem.Package;
import fr.vergne.stanos.gui.configuration.Configuration;
import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

//TODO shorten tree items
public class DependenciesPane extends BorderPane {

	public DependenciesPane(Configuration configuration, ObservableList<Dependency> dependencies) {

		ObjectProperty<TableFilterFactory> tableFilterProperty = new SimpleObjectProperty<>(() -> dep -> true);
		FilteredList<Dependency> filteredDependencies = dependencies.filtered(tableFilterProperty.get().create());
		InvalidationListener tableFilterUpdate = observable -> {
			filteredDependencies.predicateProperty().set(tableFilterProperty.get().create());
		};
		tableFilterProperty.addListener(tableFilterUpdate);
		
		TreeView<CodeItem> treeView = createTreeView(dependencies);
		treeView.getSelectionModel().selectedItemProperty().addListener(tableFilterUpdate);
		ObservableList<TableFilterFactory> factories = createTableFilterFactories(treeView);// TODO move up
		ChoiceBox<TableFilterFactory> tableFilterChoiceBox = createTableFilterChoiceBox(factories);
		tableFilterChoiceBox.getSelectionModel().select(2);// TODO default as a conf property
		TableView<Dependency> table = createTableView(filteredDependencies);
		
		tableFilterProperty.bind(tableFilterChoiceBox.valueProperty());
		

		VBox treePane = new VBox(treeView, tableFilterChoiceBox);
		VBox.setVgrow(treeView, Priority.ALWAYS);

		VBox tablePane = new VBox(table);
		VBox.setVgrow(table, Priority.ALWAYS);

		SplitPane splitPane = new SplitPane(treePane, tablePane);
		SplitPane.setResizableWithParent(treeView, false);
		DoubleProperty sliderPosition = splitPane.getDividers().get(0).positionProperty();
		sliderPosition.set(configuration.gui().dependencies().slider());// TODO default as a conf property
		sliderPosition.addListener((observable, oldValue, newValue) -> {
			configuration.gui().dependencies().slider(newValue.doubleValue());
		});

		setCenter(splitPane);
	}

	private ChoiceBox<TableFilterFactory> createTableFilterChoiceBox(ObservableList<TableFilterFactory> factories) {
		ChoiceBox<TableFilterFactory> displayChildrenCheck = new ChoiceBox<>(factories);
		return displayChildrenCheck;
	}

	private ObservableList<TableFilterFactory> createTableFilterFactories(TreeView<CodeItem> tree) {
		TableFilterFactory displayAll = new TableFilterFactory() {
			@Override
			public Predicate<Dependency> create() {
				return dep -> true;
			}
			
			@Override
			public String toString() {
				return "Display all";
			}
		};
		TableFilterFactory displayLevel = new TableFilterFactory() {
			@Override
			public Predicate<Dependency> create() {
				MultipleSelectionModel<TreeItem<CodeItem>> selectionModel = tree.getSelectionModel();
				if (selectionModel.isEmpty()) {
					return dep -> true;
				} else {
					CodeItem codeItem = selectionModel.getSelectedItem().getValue();
					return dep -> codeItem.equals(dep.getSource());
				}
			}
			
			@Override
			public String toString() {
				return "Display selected level";
			}
		};
		TableFilterFactory displaySubtree = new TableFilterFactory() {
			@Override
			public Predicate<Dependency> create() {
				MultipleSelectionModel<TreeItem<CodeItem>> selectionModel = tree.getSelectionModel();
				if (selectionModel.isEmpty()) {
					return dep -> true;
				} else {
					List<CodeItem> codeItems = Stream.of(selectionModel.getSelectedItem())
							.flatMap(recursiveMapper(TreeItem<CodeItem>::getChildren)).map(TreeItem::getValue)
							.collect(Collectors.toList());
					return dep -> codeItems.contains(dep.getSource());
				}
			}
			
			@Override
			public String toString() {
				return "Display selected subtree";
			}
		};
		ObservableList<TableFilterFactory> factories = FXCollections.observableArrayList(displayAll, displayLevel, displaySubtree);
		return factories;
	}

	interface TableFilterFactory {
		Predicate<Dependency> create();
	}

	private TreeView<CodeItem> createTreeView(ObservableList<Dependency> dependencies) {
		TreeView<CodeItem> treeView = new TreeView<>();
		treeView.setShowRoot(false);

		ListChangeListener<Dependency> listener = change -> treeView.setRoot(createTreeRoot(dependencies));
		dependencies.addListener(listener);

		// TODO customize display of tree items

		return treeView;
	}

	private TreeItem<CodeItem> createTreeRoot(ObservableList<Dependency> dependencies) {
		var treeItems = createTreeItemsFromDeclarations(dependencies);
		var treeRoot = new TreeItem<CodeItem>(null);
		addRootPackagesToTreeRoot(treeItems, treeRoot);
		reduceSingleChildPackages(treeItems);
		return treeRoot;
	}

	private Collection<TreeItem<CodeItem>> createTreeItemsFromDeclarations(ObservableList<Dependency> dependencies) {
		Map<CodeItem, TreeItem<CodeItem>> treeItemCache = new HashMap<>();
		Function<CodeItem, TreeItem<CodeItem>> treeItemProducer = codeItem -> new TreeItem<CodeItem>(codeItem);
		dependencies.stream().filter(dep -> dep.getAction().equals(Action.DECLARES)).forEach(dep -> {
			TreeItem<CodeItem> parent = treeItemCache.computeIfAbsent(dep.getSource(), treeItemProducer);
			TreeItem<CodeItem> child = treeItemCache.computeIfAbsent(dep.getTarget(), treeItemProducer);
			ObservableList<TreeItem<CodeItem>> children = parent.getChildren();
			if (!children.contains(child)) {
				children.add(child);
			}
		});
		return treeItemCache.values();
	}

	private void reduceSingleChildPackages(Collection<TreeItem<CodeItem>> treeItems) {
		treeItems.stream()
				// Single child items
				.filter(item -> item.getChildren().size() == 1)
				// Package items
				.filter(item -> item.getChildren().get(0).getValue() instanceof Package)
				// Reduction
				.forEach(item -> {
					TreeItem<CodeItem> parent = item.getParent();
					TreeItem<CodeItem> child = item.getChildren().get(0);
					int itemIndex = parent.getChildren().indexOf(item);
					parent.getChildren().set(itemIndex, child);
				});
	}

	private void addRootPackagesToTreeRoot(Collection<TreeItem<CodeItem>> treeItems, TreeItem<CodeItem> root) {
		treeItems.stream().filter(item -> item.getParent() == null).forEach(item -> root.getChildren().add(item));
	}

	private TableView<Dependency> createTableView(ObservableList<Dependency> dependencies) {
		TableView<Dependency> tableView = new TableView<>(dependencies);

		tableView.getColumns().add(createColumn("Source", dep -> dep.getSource()));
		tableView.getColumns().add(createColumn("Action", dep -> dep.getAction()));
		tableView.getColumns().add(createColumn("Target", dep -> dep.getTarget()));

		tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

		return tableView;
	}

	private <T, U> TableColumn<T, U> createColumn(String title, Function<T, U> supplier) {
		TableColumn<T, U> column = new TableColumn<T, U>(title);
		column.setCellValueFactory(param -> new SimpleObjectProperty<>(supplier.apply(param.getValue())));
		return column;
	}
}
