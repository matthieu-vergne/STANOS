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
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;

//TODO checkbox to ignore children (current level only)
//TODO shorten tree items
public class DependenciesPane extends BorderPane {

	public DependenciesPane(Configuration configuration, ObservableList<Dependency> dependencies) {

		TreeView<CodeItem> tree = createTreeView(dependencies);

		FilteredList<Dependency> filteredDependencies = dependencies.filtered(dep -> true);

		TableView<Dependency> table = createTableView(filteredDependencies);
		tree.getSelectionModel().selectedItemProperty().addListener(treeItem -> {
			boolean displayChildren = true;// TODO
			filteredDependencies.setPredicate(createTableFilter(tree.getSelectionModel(), displayChildren));
		});

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

	private Predicate<Dependency> createTableFilter(MultipleSelectionModel<TreeItem<CodeItem>> selectionModel,
			boolean displayChildren) {
		if (selectionModel.isEmpty()) {
			return dep -> true;
		}

		TreeItem<CodeItem> selectedItem = selectionModel.getSelectedItem();
		if (displayChildren) {
			List<CodeItem> codeItems = Stream.of(selectedItem).flatMap(recursiveMapper(TreeItem<CodeItem>::getChildren))
					.map(TreeItem::getValue).collect(Collectors.toList());
			return dep -> codeItems.contains(dep.getSource());
		} else {
			CodeItem codeItem = selectedItem.getValue();
			return dep -> codeItem.equals(dep.getSource());
		}
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
