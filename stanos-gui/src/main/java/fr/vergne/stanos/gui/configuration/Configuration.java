package fr.vergne.stanos.gui.configuration;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Properties;

public class Configuration {

	private static final Charset CHARSET = Charset.forName("UTF-8");

	private static final File CONF_FILE = new File("configuration.properties");

	private final Properties properties;
	private final Workspace workspace;

	private Configuration(Properties properties) {
		this.properties = properties;
		this.workspace = new Workspace();
	}

	public static Configuration load() {
		Properties properties = new Properties();
		if (!CONF_FILE.exists()) {
			System.out.println("No configuration file, use defaults");
		} else {
			System.out.println("Load configuration from " + CONF_FILE);
			try (FileReader reader = new FileReader(CONF_FILE, CHARSET)) {
				properties.load(reader);
			} catch (IOException cause) {
				throw new RuntimeException(cause);
			}
		}

		return new Configuration(properties);
	}

	public void save() {
		if (!CONF_FILE.exists()) {
			try {
				CONF_FILE.createNewFile();
			} catch (IOException cause) {
				throw new IllegalArgumentException(cause);
			}
		}
		if (!CONF_FILE.canWrite()) {
			throw new IllegalArgumentException("Cannot write configuration file " + CONF_FILE);
		}

		System.out.println("Save configuration to " + CONF_FILE);
		try (FileWriter writer = new FileWriter(CONF_FILE, CHARSET)) {
			properties.store(writer, "");
		} catch (IOException cause) {
			throw new RuntimeException(cause);
		}
	}

	public Workspace workspace() {
		return workspace;
	};

	public class Workspace {
		private final Accessor<File> directory;
		private final Accessor<Boolean> useDefaultDirectory;

		public Workspace() {
			AccessorFactory accessorFactory = new AccessorFactory(properties);

			directory = accessorFactory.createFileAccessor("workspace.directory", new File("./workspace"));
			if (!directory.get().exists()) {
				directory.get().mkdirs();
			}

			useDefaultDirectory = accessorFactory.createBooleanAccessor("workspace.useDefaultDirectory", true);
		}

		public File directory() {
			return directory.get();
		}

		public void directory(File workspaceDirectory) {
			Objects.requireNonNull(workspaceDirectory, "No workspace directory provided");
			if (!workspaceDirectory.exists()) {
				throw new IllegalArgumentException("Cannot found workspace directory: " + workspaceDirectory);
			}
			if (!workspaceDirectory.isDirectory()) {
				throw new IllegalArgumentException("Non-directory workspace: " + workspaceDirectory);
			}
			this.directory.set(workspaceDirectory);
		}

		public boolean useDefaultDirectory() {
			return useDefaultDirectory.get();
		}

		public void useDefaultDirectory(boolean useDefaultDirectory) {
			this.useDefaultDirectory.set(useDefaultDirectory);
		}
	}
}
