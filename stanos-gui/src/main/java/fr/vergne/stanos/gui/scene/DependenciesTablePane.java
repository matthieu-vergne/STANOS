package fr.vergne.stanos.gui.scene;

import static fr.vergne.stanos.gui.util.recursiveflatmapper.RecursiveFlatMapper.*;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.vergne.stanos.dependency.Dependency;
import fr.vergne.stanos.dependency.codeitem.CodeItem;
import fr.vergne.stanos.gui.configuration.Configuration;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DependenciesTablePane extends BorderPane {

	public DependenciesTablePane(Configuration configuration, ObservableList<Dependency> dependencies,
			ObservableValue<TreeItem<CodeItem>> selectedItemProperty) {
		
		ChoiceBox<TableFilterFactory> tableFilterChoiceBox = createTableFilterChoiceBox(selectedItemProperty);

		ObjectProperty<TableFilterFactory> tableFilterProperty = tableFilterChoiceBox.valueProperty();
		FilteredList<Dependency> filteredDependencies = dependencies.filtered(tableFilterProperty.getValue().create());
		
		InvalidationListener tableFilterUpdate = observable -> {
			filteredDependencies.predicateProperty().set(tableFilterProperty.getValue().create());
		};
		tableFilterProperty.addListener(tableFilterUpdate);
		selectedItemProperty.addListener(tableFilterUpdate);

		TableView<Dependency> table = createDependencyTableView(filteredDependencies);

		int spacing = configuration.gui().globalSpacing();
		HBox options = new HBox(spacing, new Label("Shows dependencies:"), tableFilterChoiceBox);
		options.setAlignment(Pos.CENTER_LEFT);
		setCenter(new VBox(spacing, options, table));
		VBox.setVgrow(table, Priority.ALWAYS);
		setPadding(new Insets(spacing));
	}

	private ChoiceBox<TableFilterFactory> createTableFilterChoiceBox(
			ObservableValue<TreeItem<CodeItem>> selectedItemProperty) {
		ObservableList<TableFilterFactory> factories = createTableFilterFactories(selectedItemProperty);
		ChoiceBox<TableFilterFactory> tableFilterChoiceBox = new ChoiceBox<>(factories);
		tableFilterChoiceBox.getSelectionModel().select(3);// TODO use conf default
		return tableFilterChoiceBox;
	}

	private ObservableList<TableFilterFactory> createTableFilterFactories(
			ObservableValue<TreeItem<CodeItem>> selectedItem) {
		return FXCollections.observableArrayList(new TableFilterFactory("all", () -> dep -> true),
				new TableFilterFactory("selected source", () -> {
					TreeItem<CodeItem> treeItem = selectedItem.getValue();
					if (treeItem == null) {
						return dep -> true;
					} else {
						CodeItem codeItem = treeItem.getValue();
						return dep -> codeItem.equals(dep.getSource());
					}
				}), new TableFilterFactory("selected target", () -> {
					TreeItem<CodeItem> treeItem = selectedItem.getValue();
					if (treeItem == null) {
						return dep -> true;
					} else {
						CodeItem codeItem = treeItem.getValue();
						return dep -> codeItem.equals(dep.getTarget());
					}
				}), new TableFilterFactory("selected subtree", () -> {
					TreeItem<CodeItem> treeItem = selectedItem.getValue();
					if (treeItem == null) {
						return dep -> true;
					} else {
						List<CodeItem> codeItems = Stream.of(treeItem)
								.flatMap(recursiveAppendCollection(TreeItem<CodeItem>::getChildren))
								.map(TreeItem::getValue).collect(Collectors.toList());
						return dep -> codeItems.contains(dep.getSource());
					}
				}));
	}

	private TableView<Dependency> createDependencyTableView(ObservableList<Dependency> dependencies) {
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

	private static class TableFilterFactory {
		private final String name;
		private final Supplier<Predicate<Dependency>> factory;

		public TableFilterFactory(String name, Supplier<Predicate<Dependency>> factory) {
			this.name = name;
			this.factory = factory;
		}

		Predicate<Dependency> create() {
			return factory.get();
		}
		
		@Override
		public String toString() {
			return name;
		}
	}
}
