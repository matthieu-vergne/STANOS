package fr.vergne.stanos.gui.scene;

import java.util.function.Supplier;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;

public class MouseGestures {

	final DragContext dragContext = new DragContext();

	Supplier<Double> scaler;

	public MouseGestures(Supplier<Double> scale) {
		this.scaler = scale;
	}

	public void makeDraggable(final Node node) {

		node.setOnMousePressed(onMousePressedEventHandler);
		node.setOnMouseDragged(onMouseDraggedEventHandler);
		node.setOnMouseReleased(onMouseReleasedEventHandler);

	}

	EventHandler<MouseEvent> onMousePressedEventHandler = new EventHandler<MouseEvent>() {

		@Override
		public void handle(MouseEvent event) {

			Node node = (Node) event.getSource();

			double scale = scaler.get();

			dragContext.x = node.getBoundsInParent().getMinX() * scale - event.getScreenX();
			dragContext.y = node.getBoundsInParent().getMinY() * scale - event.getScreenY();

		}
	};

	EventHandler<MouseEvent> onMouseDraggedEventHandler = new EventHandler<MouseEvent>() {

		@Override
		public void handle(MouseEvent event) {

			Node node = (Node) event.getSource();

			double offsetX = event.getScreenX() + dragContext.x;
			double offsetY = event.getScreenY() + dragContext.y;

			// adjust the offset in case we are zoomed
			double scale = scaler.get();

			offsetX /= scale;
			offsetY /= scale;

			node.relocate(offsetX, offsetY);

		}
	};

	EventHandler<MouseEvent> onMouseReleasedEventHandler = new EventHandler<MouseEvent>() {

		@Override
		public void handle(MouseEvent event) {

		}
	};

	class DragContext {

		double x;
		double y;

	}
}
