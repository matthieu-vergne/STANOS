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
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

//TODO shorten tree items
public class CodeItemsTreePane extends VBox {

	private ObservableValue<TreeItem<CodeItem>> selectedItemProperty;

	public CodeItemsTreePane(Configuration configuration, ObservableList<Dependency> dependencies) {
		TreeView<CodeItem> treeView = createTreeView(dependencies);
		getChildren().add(treeView);
		VBox.setVgrow(treeView, Priority.ALWAYS);

		this.selectedItemProperty = treeView.getSelectionModel().selectedItemProperty();
	}

	public ObservableValue<TreeItem<CodeItem>> selectedItemProperty() {
		return selectedItemProperty;
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

	private void addRootPackagesToTreeRoot(Collection<TreeItem<CodeItem>> treeItems, TreeItem<CodeItem> root) {
		treeItems.stream().filter(item -> item.getParent() == null).forEach(item -> root.getChildren().add(item));
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
}
