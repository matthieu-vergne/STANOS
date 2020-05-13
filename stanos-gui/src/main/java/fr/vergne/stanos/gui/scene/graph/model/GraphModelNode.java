package fr.vergne.stanos.gui.scene.graph.model;

import java.util.Collection;

public interface GraphModelNode {
	String getId();
	Object getContent();
	
	Collection<GraphModelNode> getChildren();
	Collection<GraphModelNode> getParents();
	
	// TODO remove in favor of builder nodes
	void addChild(GraphModelNode child);
	void removeChild(GraphModelNode child);
	void addParent(GraphModelNode parent);
	void removeParent(GraphModelNode parent);
}
