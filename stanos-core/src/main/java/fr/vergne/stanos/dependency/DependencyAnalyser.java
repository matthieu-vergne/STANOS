package fr.vergne.stanos.dependency;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

import fr.vergne.stanos.code.Code;
import fr.vergne.stanos.code.CodeSelector;

public interface DependencyAnalyser {
	Collection<Dependency> analyse(InputStream inputStream);

	default Collection<Dependency> analyse(Path classFile) {
		if (Files.isDirectory(classFile)) {
			throw new IllegalArgumentException("Not a class file: " + classFile);
		}
		try (InputStream fileStream = Files.newInputStream(classFile)) {
			return analyse(fileStream);
		} catch (IOException cause) {
			throw new IllegalArgumentException("Cannot open class file " + classFile, cause);
		}
	}

	default Collection<Dependency> analyse(Class<?> classObject) {
		String classPath = "/" + classObject.getName().replace('.', '/') + ".class";
		InputStream resourceStream = getClass().getResourceAsStream(classPath);
		return analyse(resourceStream);
	}

	default Collection<Dependency> analyse(CodeSelector codes) {
		return codes.getCodes().map(Code::open).map(this::analyse).flatMap(Collection<Dependency>::stream)
				.collect(Collectors.toList());
	}
}
