package fr.vergne.stanos.gui;

import fr.vergne.stanos.gui.configuration.Configuration;
import javafx.stage.Stage;

class WorkspaceSelectionStage extends Stage {

	WorkspaceSelectionStage(Configuration configuration) {
		System.out.println("Workspace dir: " + configuration.workspace().directory());
		
		setTitle("Select workspace directory");
		setX(50);
		setY(50);
		setWidth(200);
		setHeight(100);
	}
}
