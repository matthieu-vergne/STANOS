package fr.vergne.stanos.dependency;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import fr.vergne.stanos.code.CodeSelector;
import fr.vergne.stanos.dependency.codeitem.Package;

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
		Set<Dependency> declarations = new HashSet<>();
		return codes.getCodes().map(code -> analyse(code.open())).flatMap(deps -> deps.stream()).filter(dep -> {
			if (!(dep.getSource() instanceof Package)) {
				return true;
			}
			if (declarations.contains(dep)) {
				return false;
			}
			declarations.add(dep);
			return true;
		}).collect(Collectors.toList());
	}
}
