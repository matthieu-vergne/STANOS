package fr.vergne.stanos.dependency;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;

import fr.vergne.stanos.code.CodeSelector;

public interface DependencyAnalyser {
	Collection<Dependency> analyse(InputStream inputStream);

	default Collection<Dependency> analyse(Path classFile) {
		try (InputStream fileStream = Files.newInputStream(classFile)) {
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

	default Collection<Dependency> analyse(CodeSelector codes) {
		return codes.getCodes().map(code -> analyse(code.open())).reduce(new LinkedList<>(), (l1, l2) -> {
			l1.addAll(l2);
			return l1;
		});
	}

	@SuppressWarnings("serial")
	class CannotOpenClassFileException extends RuntimeException {
		public CannotOpenClassFileException(IOException cause) {
			super("Cannot open class file exception", cause);
		}
	}
}
