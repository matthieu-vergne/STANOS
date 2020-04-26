package fr.vergne.stanos;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public interface DependencyAnalyser {
	Collection<Dependency> analyse(InputStream inputStream);

	default Collection<Dependency> analyse(File classFile) {
		try (FileInputStream fileStream = new FileInputStream(classFile)) {
			return analyse(fileStream);
		} catch (IOException cause) {
			throw new CannotOpenClassFileException(cause);
		}
	}

	default Collection<Dependency> analyse(Class<?> classObject) {
		String classPath = "/" + classObject.getName().replace('.', '/') + ".class";
		InputStream resourceStream = getClass().getResourceAsStream(classPath);
		return analyse(resourceStream);
	}

	@SuppressWarnings("serial")
	class CannotOpenClassFileException extends RuntimeException {
		public CannotOpenClassFileException(IOException cause) {
			super("Cannot open class file exception", cause);
		}
	}
}
