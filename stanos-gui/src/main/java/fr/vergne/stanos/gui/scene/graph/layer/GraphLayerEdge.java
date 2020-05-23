package fr.vergne.stanos.gui.scene.graph.layer;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.beans.Observable;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.ObjectBinding;
import javafx.scene.Group;
import javafx.scene.shape.Line;

public class GraphLayerEdge extends Group {

	public GraphLayerEdge(GraphLayerNode source, GraphLayerNode target) {

		List<Point> sources = createAnchors(source);
		List<Point> targets = createAnchors(target);

		List<Link> links = createPossibleLinks(sources, targets);
		ObjectBinding<Link> shortestLink = identifyShortestLink(links);

		DoubleBinding startX = extractCoordinate(shortestLink, Link -> Link.src.x);
		DoubleBinding startY = extractCoordinate(shortestLink, link -> link.src.y);
		DoubleBinding endX = extractCoordinate(shortestLink, link -> link.tgt.x);
		DoubleBinding endY = extractCoordinate(shortestLink, link -> link.tgt.y);

		Line line = new Line();
		line.startXProperty().bind(startX);
		line.startYProperty().bind(startY);
		line.endXProperty().bind(endX);
		line.endYProperty().bind(endY);

		getChildren().add(line);

	}

	private DoubleBinding extractCoordinate(ObjectBinding<Link> shortestLink,
			Function<Link, DoubleExpression> extractor) {
		return new DoubleBinding() {

			{
				super.bind(shortestLink);
			}

			@Override
			protected double computeValue() {
				return extractor.apply(shortestLink.get()).get();
			}
		};
	}

	private ObjectBinding<Link> identifyShortestLink(List<Link> links) {
		return new ObjectBinding<Link>() {
			{
				super.bind(links.stream().map(link -> link.lengthProperty).toArray(length -> new Observable[length]));
			}

			@Override
			protected Link computeValue() {
				return links.stream()//
						.sorted((l1, l2) -> Double.compare(l1.getLength(), l2.getLength()))//
						.findFirst().get();
			}
		};
	}

	private List<Link> createPossibleLinks(List<Point> sources, List<Point> targets) {
		return sources.stream()//
				.flatMap(src -> targets.stream().map(tgt -> new Link(src, tgt)))//
				.collect(Collectors.toList());
	}

	private List<Point> createAnchors(GraphLayerNode node) {
		DoubleExpression x = node.layoutXProperty();
		DoubleExpression y = node.layoutYProperty();
		DoubleExpression w = node.widthProperty();
		DoubleExpression h = node.heightProperty();

		DoubleExpression leftX = x;
		DoubleExpression centerX = x.add(w.divide(2));
		DoubleExpression rightX = x.add(w);

		DoubleExpression topY = y;
		DoubleExpression centerY = y.add(h.divide(2));
		DoubleExpression bottomY = y.add(h);

		return Arrays.asList(//
				new Point(leftX, centerY), //
				new Point(rightX, centerY), //
				new Point(centerX, topY), //
				new Point(centerX, bottomY));
	}

	class Point {
		final DoubleExpression x;
		final DoubleExpression y;

		public Point(DoubleExpression x, DoubleExpression y) {
			this.x = x;
			this.y = y;
		}
	}

	class Link {
		private final Point src;
		private final Point tgt;
		private final DoubleExpression lengthProperty;

		public Link(Point src, Point tgt) {
			this.src = src;
			this.tgt = tgt;
			lengthProperty = new DoubleBinding() {

				{
					super.bind(src.x, src.y, tgt.x, tgt.y);
				}

				@Override
				protected double computeValue() {
					double dx = src.x.get() - tgt.x.get();
					double dy = src.y.get() - tgt.y.get();
					return Math.hypot(dx, dy);
				}
			};
		}

		public double getLength() {
			return lengthProperty.get();
		}
	}
}
