package fr.vergne.stanos.gui.scene;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import fr.vergne.stanos.dependency.Action;
import fr.vergne.stanos.dependency.Dependency;
import fr.vergne.stanos.dependency.codeitem.CodeItem;
import fr.vergne.stanos.dependency.codeitem.Package;
import fr.vergne.stanos.gui.configuration.Configuration;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;

public class DependenciesPane extends BorderPane {

	public DependenciesPane(Configuration configuration, ObservableList<Dependency> dependencies) {
		Node tree = createTreeView(dependencies);
		Node table = createTableView(dependencies);
		
		SplitPane splitPane = new SplitPane(tree, table);
		SplitPane.setResizableWithParent(tree, false);
		DoubleProperty sliderPosition = splitPane.getDividers().get(0).positionProperty();
		// TODO Use JavaFX observable properties for conf properties?
		// Bind both or set DoubleProperty into conf? (need to load from conf)
		sliderPosition.set(configuration.gui().dependencies().slider());
		sliderPosition.addListener((observable, oldValue, newValue) -> {
			configuration.gui().dependencies().slider(newValue.doubleValue());
		});
		
		setCenter(splitPane);
	}

	private Node createTreeView(ObservableList<Dependency> dependencies) {
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

	private Node createTableView(ObservableList<Dependency> dependencies) {
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
