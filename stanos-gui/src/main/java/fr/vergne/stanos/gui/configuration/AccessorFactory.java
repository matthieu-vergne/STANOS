package fr.vergne.stanos.gui.configuration;

import java.io.File;
import java.util.Objects;
import java.util.Properties;

class AccessorFactory {
	private final Properties properties;

	public AccessorFactory(Properties properties) {
		this.properties = Objects.requireNonNull(properties);
	}

	public Accessor<File> createFileAccessor(String key, File defaultValue) {
		return new Accessor<File>(properties, key, File::getPath, File::new, defaultValue);
	}

	public Accessor<Boolean> createBooleanAccessor(String key, boolean defaultValue) {
		return new Accessor<Boolean>(properties, key, bool -> bool.toString(), Boolean::getBoolean, defaultValue);
	}
}
