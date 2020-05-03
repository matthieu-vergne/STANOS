package fr.vergne.stanos.gui.scene;

import java.util.Collection;

import fr.vergne.stanos.code.CodeSelector;
import fr.vergne.stanos.dependency.Dependency;
import fr.vergne.stanos.dependency.DependencyAnalyser;
import fr.vergne.stanos.dependency.bytecode.asm.ASMByteCodeAnalyser;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class DependencyListPane extends Parent {

	public DependencyListPane(CodeSelector codeSelector) {
		Node table = createTable(codeSelector);
		VBox vBox = new VBox(table);
		vBox.setAlignment(Pos.CENTER);
		getChildren().add(vBox);
	}

	private Node createTable(CodeSelector codeSelector) {
		DependencyAnalyser dependencyAnalyser = new ASMByteCodeAnalyser();
		Collection<Dependency> dependencies = dependencyAnalyser.analyse(codeSelector);
		// TODO Add table with dependencies
		// TODO Add F5 refresh
		// mvn clean package && java -jar ./stanos-gui/target/stanos-gui-1.0-SNAPSHOT.jar
		
		Node table = new Label("Dependencies: "+dependencies.size());
		return table;
	}
}
