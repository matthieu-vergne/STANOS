package fr.vergne.stanos.gui.configuration;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

class Accessor<T> {
	private final Properties properties;
	private final String key;
	private final Function<T, String> serializer;
	private final Function<String, T> deserializer;
	private final T defaultValue;

	Accessor(Properties properties, String key, Function<T, String> serializer, Function<String, T> deserializer,
			T defaultValue) {
		this.properties = Objects.requireNonNull(properties);
		this.key = Objects.requireNonNull(key);
		this.serializer = Objects.requireNonNull(serializer);
		this.deserializer = Objects.requireNonNull(deserializer);
		this.defaultValue = Objects.requireNonNull(defaultValue);
	}

	T get() {
		String property = properties.getProperty(key);
		return Optional.ofNullable(property).map(deserializer).orElse(defaultValue);
	}

	void set(T value) {
		properties.setProperty(key, serializer.apply(value));
	}
}
