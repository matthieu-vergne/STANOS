package fr.vergne.stanos.gui.scene;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import fr.vergne.stanos.code.CodeSelector;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class CodeSelectorPane extends VBox {

	private final CodeSelector codeSelector;

	public CodeSelectorPane() {
		final ObservableList<Path> entries;
		entries = FXCollections.observableList(new LinkedList<Path>());

		entries.add(Path.of(
				"/home/matthieu/Programing/Java/Pester/pester-core/target/classes/fr/vergne/pester/util/cache/Cache.class"));
		TableView<Path> table = createTable(entries);

		Button addFileButton = createAddFilesButton(entries, table);
		Button addDirectoryButton = createAddDirectoryButton(entries, table);
		Button removeSelectionButton = createRemoveSelectionButton(entries, table);

		setAlignment(Pos.CENTER);
		setFillWidth(true);
		getChildren().add(table);
		getChildren().add(new HBox(addFileButton, addDirectoryButton, removeSelectionButton));
		
		this.codeSelector = CodeSelector.onCollection(entries);
	}
	
	public CodeSelector getCodeSelector() {
		return codeSelector;
	}

	private Button createAddFilesButton(final ObservableList<Path> entries, TableView<Path> table) {
		Button button = new Button("Add files");
		button.setOnAction(event -> {
			FileChooser fileChooser = new FileChooser();
			Path pathSuggestion = suggestPath(entries, table);
			fileChooser.setInitialDirectory(pathSuggestion.getParent().toFile());
			fileChooser.setInitialFileName(pathSuggestion.toString());
			List<File> selectedFiles = fileChooser.showOpenMultipleDialog(Window.getWindows().get(0));

			if (selectedFiles == null || selectedFiles.isEmpty()) {
				// Do nothing
			} else {
				table.getSelectionModel().clearSelection();
				selectedFiles.forEach(selectedFile -> {
					System.out.println("Add file " + selectedFile);
					Path entry = selectedFile.toPath();
					entries.add(entry);
					table.getSelectionModel().select(entry);
				});
			}
		});
		return button;
	}

	private Button createAddDirectoryButton(final ObservableList<Path> entries, TableView<Path> table) {
		Button button = new Button("Add directory");
		button.setOnAction(event -> {
			DirectoryChooser dirChooser = new DirectoryChooser();
			File file = suggestPath(entries, table).toFile();
			dirChooser.setInitialDirectory(file.isDirectory() ? file : file.getParentFile());
			File selectedDirectory = dirChooser.showDialog(Window.getWindows().get(0));

			if (selectedDirectory == null) {
				// Do nothing
			} else {
				table.getSelectionModel().clearSelection();
				System.out.println("Add directory " + selectedDirectory);
				Path entry = selectedDirectory.toPath();
				entries.add(entry);
				table.getSelectionModel().select(entry);
			}
		});
		return button;
	}

	private Path suggestPath(final ObservableList<Path> entries, TableView<Path> table) {
		Path pathSuggestion;
		if (entries.isEmpty()) {
			pathSuggestion = Path.of(".");
		} else {
			pathSuggestion = table.getSelectionModel().getSelectedItem();
			if (pathSuggestion == null) {
				pathSuggestion = table.getItems().get(0);
			}
		}
		return pathSuggestion;
	}

	private <T> Button createRemoveSelectionButton(final ObservableList<T> entries, TableView<T> table) {
		Button button = new Button("Remove");

		// Deactivate unless something is selected
		button.disableProperty().bind(Bindings.isEmpty(table.getSelectionModel().getSelectedItems()));

		button.setOnAction(event -> removeSelectedItems(entries, table));

		return button;
	}

	private <T> TableView<T> createTable(ObservableList<T> entries) {
		SortedList<T> sortedList = new SortedList<>(entries);
		TableView<T> table = new TableView<>(sortedList);
		sortedList.comparatorProperty().bind(table.comparatorProperty());

		TableColumn<T, T> pathColumn = new TableColumn<>("Path");
		pathColumn.setCellValueFactory(param -> new SimpleObjectProperty<T>(param.getValue()));
		table.getColumns().add(pathColumn);

		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		table.getSortOrder().add(pathColumn);
		table.sort();

		table.setOnKeyPressed(event -> removeSelectedItems(entries, table));

		return table;
	}

	private <T> void removeSelectedItems(final ObservableList<T> entries, TableView<T> table) {
		ObservableList<T> selectedItems = table.getSelectionModel().getSelectedItems();
		if (selectedItems == null || selectedItems.isEmpty()) {
			return;
		}

		entries.removeAll(selectedItems);
		table.getSelectionModel().clearSelection();
	}
}
