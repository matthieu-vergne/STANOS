package fr.vergne.stanos.gui.scene.graph.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import fr.vergne.stanos.dependency.codeitem.CodeItem;
import fr.vergne.stanos.gui.scene.graph.layer.GraphLayerNode;

public interface GraphModelNode {
	// TODO abstract from CodeItem
	String getId();
	CodeItem getContent();
	
	// TODO refactor for immutability? (builder)
	void addChild(GraphModelNode child);
	void removeChild(GraphModelNode child);
	Collection<GraphModelNode> getChildren();
	void addParent(GraphModelNode parent);
	void removeParent(GraphModelNode parent);
	Collection<GraphModelNode> getParents();
	GraphModelNode immutable();

	// TODO remove
	GraphLayerNode toLayerNode();
	public static Map<GraphModelNode, GraphLayerNode> CACHE = new HashMap<>();
	public static Map<GraphLayerNode, GraphModelNode> LAYER_MAP = new HashMap<>();
}
