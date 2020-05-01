package fr.vergne.stanos.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

@SuppressWarnings("exports") // This is not a lib, it is not expected to be imported anywhere
public class App extends Application {

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("STANOS");
		
		var label = new Label("Hello world!");
		var scene = new Scene(new StackPane(label), 640, 480);
		primaryStage.setScene(scene);
		
		primaryStage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
