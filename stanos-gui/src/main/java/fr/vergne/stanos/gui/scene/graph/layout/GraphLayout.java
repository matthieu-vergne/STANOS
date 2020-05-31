package fr.vergne.stanos.gui.scene.graph.layout;

import fr.vergne.stanos.gui.scene.graph.layer.GraphLayer;
import fr.vergne.stanos.gui.scene.graph.model.GraphModel;

public interface GraphLayout<T> {
	GraphLayer layout(GraphModel<T> model);
}
