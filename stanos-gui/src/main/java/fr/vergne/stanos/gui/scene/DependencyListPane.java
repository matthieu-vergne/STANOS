package fr.vergne.stanos.gui.scene;

import java.util.function.Function;

import fr.vergne.stanos.dependency.Dependency;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

public class DependencyListPane extends BorderPane {

//	class Dependency {}
	
	public DependencyListPane(ObservableList<Dependency> dependencies) {
		setCenter(createTable(dependencies));
	}

	private Node createTable(ObservableList<Dependency> dependencies) {
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
