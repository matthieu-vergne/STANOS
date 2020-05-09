package fr.vergne.stanos.gui.scene.graph.cell;

import static fr.vergne.stanos.gui.util.recursiveflatmapper.RecursiveFlatMapper.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class CellFactory {

	private final Map<Class<?>, Function<?, Cell>> factories = new HashMap<>();

	public CellFactory() {
		registerFactory(Label.class, label -> new Cell(label.getId(), label));
		registerFactory(String.class, text -> create(new Label(text)));
		registerFactory(ImageView.class, image -> {
			image.setFitWidth(100);
			image.setFitHeight(80);
			String id = image.getImage().getUrl();
			return new Cell(id, image);
		});
		registerFactory(Image.class, image -> create(new ImageView(image)));
	}

	public <T> void registerFactory(Class<T> clazz, Function<T, Cell> factory) {
		factories.put(clazz, factory);
	}

	@SuppressWarnings("unchecked")
	private <T> Function<T, Cell> getFactory(T object) {
		return Stream.of(object.getClass())
				// Retrieve parent classes/interfaces
				.flatMap(recursiveAppend(clazz -> {
					Stream<Class<?>> superclass = Optional.ofNullable(clazz.getSuperclass()).map(Stream::<Class<?>>of).orElse(Stream.empty());
					Stream<Class<?>> interfaces = Stream.of(clazz.getInterfaces());
					return Stream.concat(superclass, interfaces);
				}))
				// Retrieve factory from closest to farest class/interface
				.map(clazz -> (Function<T, Cell>) factories.get(clazz))
				// Ignore the ones with no factory
				.filter(factory -> factory != null)
				// Return the first factory we get
				.findFirst()
				// Throw an exception if we found no factory at all
				.orElseThrow(() -> new IllegalArgumentException("No cell factory for " + object.getClass()));
	}

	public Cell create(Object object) {
		return getFactory(object).apply(object);
	}
}
