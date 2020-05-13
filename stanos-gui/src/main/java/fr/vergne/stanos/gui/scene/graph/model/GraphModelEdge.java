package fr.vergne.stanos.gui.scene.graph.model;

import java.util.HashMap;
import java.util.Map;

import fr.vergne.stanos.gui.scene.graph.layer.GraphLayerEdge;

public interface GraphModelEdge {

	GraphModelNode getSource();
	GraphModelNode getTarget();

	// TODO remove
	GraphLayerEdge toLayerNode();
	public static Map<GraphModelEdge, GraphLayerEdge> CACHE = new HashMap<>();
	public static Map<GraphLayerEdge, GraphModelEdge> LAYER_MAP = new HashMap<>();
}
