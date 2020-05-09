package fr.vergne.stanos.gui.scene;

import fr.vergne.stanos.dependency.Dependency;
import fr.vergne.stanos.dependency.codeitem.CodeItem;
import fr.vergne.stanos.gui.configuration.Configuration;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;

public class DependenciesPane extends BorderPane {

	public DependenciesPane(Configuration configuration, ObservableList<Dependency> dependencies) {
		var codeItemsTreePane = new CodeItemsTreePane(configuration, dependencies);
		ObservableValue<TreeItem<CodeItem>> selectedItemProperty = codeItemsTreePane.selectedItemProperty();

		Tab tableTab = new Tab("table", new DependenciesTablePane(configuration, dependencies, selectedItemProperty));
		Tab graphTab = new Tab("graph", new DependenciesGraphPane(configuration, dependencies));
		TabPane tabPane = new TabPane(tableTab, graphTab);
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);

		SplitPane splitPane = new SplitPane(codeItemsTreePane, tabPane);
		SplitPane.setResizableWithParent(codeItemsTreePane, false);
		DoubleProperty sliderPosition = splitPane.getDividers().get(0).positionProperty();
		sliderPosition.set(configuration.gui().dependencies().slider());// TODO default as a conf property
		sliderPosition.addListener((observable, oldValue, newValue) -> {
			configuration.gui().dependencies().slider(newValue.doubleValue());
		});

		setCenter(splitPane);
		
		// TODO remove
		tabPane.getSelectionModel().select(graphTab);
	}
}
