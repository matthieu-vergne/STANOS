package fr.vergne.stanos.gui.scene.graph.model;

import java.util.Collection;

public interface GraphModelNode {
	// TODO abstract from CodeItem
	String getId();
	Object getContent();
	
	Collection<GraphModelNode> getChildren();
	Collection<GraphModelNode> getParents();
	
	// TODO refactor for immutability? (builder)
	void addChild(GraphModelNode child);
	void removeChild(GraphModelNode child);
	void addParent(GraphModelNode parent);
	void removeParent(GraphModelNode parent);
}
