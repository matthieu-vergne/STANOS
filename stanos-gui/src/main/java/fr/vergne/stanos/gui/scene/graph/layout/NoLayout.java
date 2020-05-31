package fr.vergne.stanos.gui.scene.graph.layout;

import static java.util.stream.Collectors.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fr.vergne.stanos.gui.scene.graph.layer.GraphLayer;
import fr.vergne.stanos.gui.scene.graph.layer.GraphLayerEdge;
import fr.vergne.stanos.gui.scene.graph.layer.GraphLayerNode;
import fr.vergne.stanos.gui.scene.graph.model.GraphModel;
import fr.vergne.stanos.gui.scene.graph.model.GraphModelEdge;
import fr.vergne.stanos.gui.scene.graph.model.GraphModelNode;
import javafx.scene.control.Label;

public class NoLayout<T> implements GraphLayout<T> {

	@Override
	public GraphLayer layout(GraphModel<T> model) {
		// Create isolated layer nodes
		Map<GraphModelNode<T>, GraphLayerNode> nodesMap = new HashMap<>();
		List<GraphLayerNode> layerNodes = model.getNodes().stream()
				.map(modelNode -> nodesMap.computeIfAbsent(modelNode, n -> {
					return new GraphLayerNode(modelNode, new Label(n.getId()));
				})).collect(toList());

		// Retrieve parents & children
		nodesMap.entrySet().forEach(entry -> {
			GraphModelNode<T> modelNode = entry.getKey();
			GraphLayerNode layerNode = entry.getValue();
			model.getEdges().stream()//
					.filter(edge -> edge.getSource().equals(modelNode))//
					.map(GraphModelEdge::getTarget)//
					.map(nodesMap::get)//
					.forEach(layerNode::addGraphNodeChild);
			model.getEdges().stream()//
					.filter(edge -> edge.getTarget().equals(modelNode))//
					.map(GraphModelEdge::getSource)//
					.map(nodesMap::get)//
					.forEach(layerNode::addGraphNodeParent);
		});

		// Create layer edges
		List<GraphLayerEdge> layerEdges = model.getEdges().stream().map(edge -> {
			GraphLayerNode source = nodesMap.get(edge.getSource());
			GraphLayerNode target = nodesMap.get(edge.getTarget());
			return new GraphLayerEdge(source, target);
		}).collect(Collectors.toList());

		return new GraphLayer(layerNodes, layerEdges);
	}

}
