package fr.vergne.stanos.gui.scene.graph.model;

import java.util.Collection;

// TODO add edges features? Don't assume specific type of graph but allow optimizations
public interface GraphModel<T> {

	Collection<GraphModelNode<T>> getNodes();

	Collection<GraphModelEdge<T>> getEdges();

}