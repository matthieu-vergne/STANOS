package fr.vergne.stanos.gui;

import java.nio.file.Path;
import java.util.LinkedList;

import fr.vergne.stanos.code.CodeSelector;
import fr.vergne.stanos.dependency.Dependency;
import fr.vergne.stanos.dependency.DependencyAnalyser;
import fr.vergne.stanos.dependency.bytecode.asm.ASMByteCodeAnalyser;
import fr.vergne.stanos.gui.configuration.Configuration;
import fr.vergne.stanos.gui.configuration.Configuration.Workspace;
import fr.vergne.stanos.gui.scene.DependenciesPane;
import fr.vergne.stanos.gui.scene.PathsSelectorPane;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.input.KeyCode;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class App extends Application {

	@Override
	public void start(Stage primaryStage) {
		Configuration configuration = Configuration.load();

		ensureWorkspaceIsSet(configuration);

		ObservableList<Path> paths = FXCollections.observableList(new LinkedList<Path>());
		paths.add(Path.of(
				"/home/matthieu/Programing/Java/Pester/pester-core/target/classes/fr/vergne/pester/util/cache"));
		ObservableList<Dependency> dependencies = FXCollections.observableList(new LinkedList<>());
		DependencyAnalyser dependencyAnalyser = new ASMByteCodeAnalyser();

		Runnable refreshAction = () -> {
			System.out.println("Refresh dependencies");
			dependencies.clear();
			dependencies.addAll(dependencyAnalyser.analyse(CodeSelector.onPaths(paths)));
			System.out.println(dependencies.size() + " dependencies retrieved");
		};
		refreshAction.run();// TODO remove

		Tab pathsSelectorTab = new Tab("Classes", new PathsSelectorPane(configuration, paths, refreshAction));
		Tab dependenciesTab = new Tab("Dependencies", new DependenciesPane(configuration, dependencies));
		TabPane tabPane = new TabPane(pathsSelectorTab, dependenciesTab);
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		tabPane.setOnKeyPressed(event -> {
			if (KeyCode.F5.equals(event.getCode())) {
				refreshAction.run();
			}
		});

		Rectangle2D bounds = Screen.getPrimary().getBounds();
		Scene scene = new Scene(tabPane, bounds.getWidth() * .66, bounds.getHeight() * .66);

		primaryStage.setTitle("STANOS");
		primaryStage.setScene(scene);
		primaryStage.show();
		
		// TODO remove
		tabPane.getSelectionModel().select(dependenciesTab);
	}

	private void ensureWorkspaceIsSet(Configuration configuration) {
		Workspace workspaceConf = configuration.workspace();
		if (workspaceConf.useDefaultDirectory()) {
			System.out.println("Use default workspace directory: " + workspaceConf.directory());
		} else {
			requestWorkspace(configuration);
			System.out.println("Use provided workspace directory: " + workspaceConf.directory());
		}
	}

	private void requestWorkspace(Configuration configuration) {
		new WorkspaceSelectionStage(configuration).showAndWait();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
