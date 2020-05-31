package fr.vergne.stanos.gui.scene.graph.model;

// TODO generalize to (un)ordered edges (nodes set + metadata)
public interface GraphModelEdge<T> {

	GraphModelNode<T> getSource();

	GraphModelNode<T> getTarget();
}
