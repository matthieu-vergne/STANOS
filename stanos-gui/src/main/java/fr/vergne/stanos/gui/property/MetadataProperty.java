package fr.vergne.stanos.gui.property;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;

// TODO make each key-value a property on its own
public class MetadataProperty {
	private final MapProperty<MetadataKey<?>, Object> metadataProperty;

	private MetadataProperty(Map<MetadataKey<?>, Object> map) {
		this.metadataProperty = new SimpleMapProperty<>(FXCollections.observableMap(map));
	}

	public MetadataProperty() {
		this(new HashMap<>());
	}

	public static class MetadataKey<T> {
		private MetadataKey() {
		}
	}

	public static <T> MetadataKey<T> createMetadataKey() {
		return new MetadataKey<T>();
	}

	public <T> void put(MetadataKey<T> key, T value) {
		metadataProperty.put(key, value);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(MetadataKey<T> key) {
		return (T) metadataProperty.get(key);
	}

	@SuppressWarnings("unchecked")
	public <T> T remove(MetadataKey<T> key) {
		return (T) metadataProperty.remove(key);
	}
}
