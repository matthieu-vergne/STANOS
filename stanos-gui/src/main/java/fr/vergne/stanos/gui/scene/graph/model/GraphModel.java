package fr.vergne.stanos.gui.scene.graph.model;

import java.util.Collection;

public interface GraphModel {

	Collection<GraphModelNode> getNodes();

	Collection<GraphModelEdge> getEdges();

}