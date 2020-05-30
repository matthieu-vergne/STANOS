package fr.vergne.stanos.gui.scene.graph.model;

import java.util.Collection;

public interface GraphModel {

	Collection<GraphModelNode> getNodes();

	Collection<GraphModelEdge> getEdges();

	Collection<GraphModelNode> getChildren(GraphModelNode parent);

	Collection<GraphModelNode> getParents(GraphModelNode child);
	
	void addNode(GraphModelNode node);
	void removeNode(GraphModelNode node);
	void addEdge(GraphModelEdge edge);
	void removeEdge(GraphModelEdge edge);
	void addChild(GraphModelNode parent, GraphModelNode child);
	void removeChild(GraphModelNode parent, GraphModelNode child);
	void addParent(GraphModelNode child, GraphModelNode parent);
	void removeParent(GraphModelNode child, GraphModelNode parent);
}