package fr.vergne.stanos.gui.scene.graph.layout;

import static java.util.stream.Collectors.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.vergne.stanos.gui.scene.graph.layer.GraphLayer;
import fr.vergne.stanos.gui.scene.graph.layer.GraphLayerEdge;
import fr.vergne.stanos.gui.scene.graph.layer.GraphLayerNode;
import fr.vergne.stanos.gui.scene.graph.model.GraphModel;
import fr.vergne.stanos.gui.scene.graph.model.GraphModelNode;
import javafx.scene.control.Label;

public class NoLayout implements GraphLayout {

	@Override
	public GraphLayer layout(GraphModel model) {
		// Create isolated layer nodes
		Map<GraphModelNode, GraphLayerNode> nodesMap = new HashMap<>();
		List<GraphLayerNode> layerNodes = model.getNodes().stream()
				.map(modelNode -> nodesMap.computeIfAbsent(modelNode, n -> {
					return new GraphLayerNode(new Label(n.getId()));
				})).collect(toList());

		// Retrieve parents & children
		nodesMap.entrySet().forEach(entry -> {
			GraphModelNode modelNode = entry.getKey();
			GraphLayerNode layerNode = entry.getValue();
			modelNode.getChildren().stream().map(nodesMap::get).forEach(layerNode::addGraphNodeChild);
			modelNode.getParents().stream().map(nodesMap::get).forEach(layerNode::addGraphNodeParent);
		});

		// Create layer edges
		// TODO edges & parents/children redundant, simplify
		List<GraphLayerEdge> layerEdges = layerNodes.stream().//
				flatMap(parent -> parent.getGraphNodeChildren().stream()//
						.map(child -> new GraphLayerEdge(parent, child)))//
				.collect(toList());
		
		return new GraphLayer(layerNodes, layerEdges);
	}

}
