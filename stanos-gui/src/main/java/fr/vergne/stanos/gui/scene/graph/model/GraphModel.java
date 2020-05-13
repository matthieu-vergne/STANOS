package fr.vergne.stanos.gui.scene.graph.model;

import static java.util.stream.Collectors.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import fr.vergne.stanos.gui.scene.graph.layer.GraphLayerEdge;
import fr.vergne.stanos.gui.scene.graph.layer.GraphLayerNode;

public interface GraphModel {

	Collection<GraphModelNode> getNodes();

	Collection<GraphModelEdge> getEdges();

	Collection<GraphLayerNode> getGraphNodes();

	Collection<GraphLayerEdge> getGraphEdges();

	default GraphModel immutable() {
		Map<GraphModelNode, GraphModelNode> nodeMap = getNodes().stream().collect(toMap(node -> node, node -> node.immutable()));

		Function<? super GraphModelNode, ? extends GraphModelNode> nodeAdapter = node -> nodeMap.get(node);
		Function<? super GraphModelEdge, ? extends GraphModelEdge> edgeAdapter = edge -> {
			GraphModelNode newSource = nodeMap.get(edge.getSource());
			GraphModelNode newTarget = nodeMap.get(edge.getTarget());
			return new SimpleGraphModelEdge(newSource, newTarget);
		};

		List<GraphModelNode> immutableNodes = getNodes().stream().map(nodeAdapter).collect(toUnmodifiableList());
		List<GraphModelEdge> immutableEdges = getEdges().stream().map(edgeAdapter).collect(toUnmodifiableList());
		return new SimpleGraphModel(immutableNodes, immutableEdges, true);
	}

}