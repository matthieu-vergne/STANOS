package fr.vergne.stanos.gui;

import java.nio.file.Path;
import java.util.LinkedList;

import fr.vergne.stanos.code.CodeSelector;
import fr.vergne.stanos.dependency.Dependency;
import fr.vergne.stanos.dependency.DependencyAnalyser;
import fr.vergne.stanos.dependency.bytecode.asm.ASMByteCodeAnalyser;
import fr.vergne.stanos.gui.configuration.Configuration;
import fr.vergne.stanos.gui.configuration.Configuration.Workspace;
import fr.vergne.stanos.gui.scene.DependencyListPane;
import fr.vergne.stanos.gui.scene.PathsSelectorPane;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

@SuppressWarnings("exports") // This is not a lib, it is not expected to be imported anywhere
public class App extends Application {

	@Override
	public void start(Stage primaryStage) {
		// mvn clean package && java -jar ./stanos-gui/target/stanos-gui-1.0-SNAPSHOT.jar
		Configuration configuration = Configuration.load();

		ensureWorkspaceIsSet(configuration);

		ObservableList<Path> paths = FXCollections.observableList(new LinkedList<Path>());
		paths.add(Path.of(
				"/home/matthieu/Programing/Java/Pester/pester-core/target/classes/fr/vergne/pester/util/cache/Cache.class"));
		ObservableList<Dependency> dependencies = FXCollections.observableList(new LinkedList<>());
		DependencyAnalyser dependencyAnalyser = new ASMByteCodeAnalyser();

		Runnable refreshAction = () -> {
			System.out.println("Refresh dependencies");
			dependencies.clear();
			dependencies.addAll(dependencyAnalyser.analyse(CodeSelector.onCollection(paths)));
			System.out.println(dependencies.size() + " dependencies retrieved");
		};
		
		PathsSelectorPane codeSelectorPane = new PathsSelectorPane(paths, refreshAction);
		DependencyListPane dependencyListPane = new DependencyListPane(dependencies);

		TabPane tabPane = new TabPane();
		tabPane.getTabs().add(new Tab("Classes", codeSelectorPane));
		tabPane.getTabs().add(new Tab("Dependencies", dependencyListPane));
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		tabPane.setOnKeyPressed(event -> {
			if (KeyCode.F5.equals(event.getCode())) {
				refreshAction.run();
			}
		});

		Scene scene = new Scene(tabPane, 640, 480);

		primaryStage.setTitle("STANOS");
		primaryStage.setScene(scene);
		primaryStage.show();
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
