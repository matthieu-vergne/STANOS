package fr.vergne.stanos.gui.configuration;

import java.io.File;
import java.util.Objects;
import java.util.Properties;

class AccessorFactory {
	private final Properties properties;
	private final String keyPrefix;

	public AccessorFactory(Properties properties) {
		this(properties, null);
	}

	public AccessorFactory(Properties properties, String subKey) {
		this.properties = Objects.requireNonNull(properties);
		this.keyPrefix = subKey == null ? "" : subKey + ".";
	}

	public AccessorFactory sub(String subKey) {
		return new AccessorFactory(properties, keyPrefix + subKey);
	}

	public Accessor<Boolean> createBooleanAccessor(String key, boolean defaultValue) {
		return new Accessor<>(properties, keyPrefix + key, bool -> bool.toString(), Boolean::valueOf, defaultValue);
	}

	public Accessor<Integer> createIntegerAccessor(String key, int defaultValue) {
		return new Accessor<>(properties, keyPrefix + key, i -> i.toString(), Integer::valueOf, defaultValue);
	}

	public Accessor<Double> createDoubleAccessor(String key, double defaultValue) {
		return new Accessor<>(properties, keyPrefix + key, d -> d.toString(), Double::valueOf, defaultValue);
	}

	public Accessor<File> createFileAccessor(String key, File defaultValue) {
		return new Accessor<>(properties, keyPrefix + key, File::getPath, File::new, defaultValue);
	}
}
