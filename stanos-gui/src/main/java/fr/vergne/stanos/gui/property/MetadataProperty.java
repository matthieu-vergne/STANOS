package fr.vergne.stanos.gui.property;

import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;

public class MetadataProperty {
	private final MapProperty<MetadataKey<?>, Object> metadata = new SimpleMapProperty<>(FXCollections.observableHashMap());

	public static class MetadataKey<T> {
		private MetadataKey() {
		}
	}
	
	public static <T> MetadataKey<T> createMetadataKey() {
		return new MetadataKey<T>();
	}
	
	public <T> void put(MetadataKey<T> key, T value) {
		metadata.put(key, value);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(MetadataKey<T> key) {
		return (T) metadata.get(key);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T remove(MetadataKey<T> key) {
		return (T) metadata.remove(key);
	}

}
