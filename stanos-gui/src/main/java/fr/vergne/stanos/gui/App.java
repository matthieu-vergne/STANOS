package fr.vergne.stanos.gui;

import fr.vergne.stanos.code.CodeSelector;
import fr.vergne.stanos.gui.configuration.Configuration;
import fr.vergne.stanos.gui.configuration.Configuration.Workspace;
import fr.vergne.stanos.gui.scene.CodeSelectorPane;
import fr.vergne.stanos.gui.scene.DependencyListPane;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.stage.Stage;

@SuppressWarnings("exports") // This is not a lib, it is not expected to be imported anywhere
public class App extends Application {

	@Override
	public void start(Stage primaryStage) {
		Configuration configuration = Configuration.load();

		ensureWorkspaceIsSet(configuration);

		primaryStage.setTitle("STANOS");

		CodeSelectorPane codeSelectorPane = new CodeSelectorPane();
		CodeSelector codeSelector = codeSelectorPane.getCodeSelector();
		DependencyListPane dependencyListPane = new DependencyListPane(codeSelector);
		
		TabPane tabPane = new TabPane();
		tabPane.getTabs().add(new Tab("Classes", codeSelectorPane));
		tabPane.getTabs().add(new Tab("Dependencies", dependencyListPane));
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		primaryStage.setScene(new Scene(tabPane, 640, 480));

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
