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
	Collection<Dependency> analyze(InputStream inputStream);

	default Collection<Dependency> analyze(Path classFile) {
		if (Files.isDirectory(classFile)) {
			throw new IllegalArgumentException("Not a class file: " + classFile);
		}
		try (InputStream fileStream = Files.newInputStream(classFile)) {
			return analyze(fileStream);
		} catch (IOException cause) {
			throw new IllegalArgumentException("Cannot open class file " + classFile, cause);
		}
	}

	default Collection<Dependency> analyze(Class<?> classObject) {
		String classPath = "/" + classObject.getName().replace('.', '/') + ".class";
		InputStream resourceStream = getClass().getResourceAsStream(classPath);
		return analyze(resourceStream);
	}

	default Collection<Dependency> analyze(CodeSelector codes) {
		return codes.getCodes().map(Code::open).map(this::analyze).flatMap(Collection<Dependency>::stream)
				.collect(Collectors.toList());
	}
}
