package fr.vergne.stanos.code;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Stream;

public interface CodeSelector {
	Stream<Code> getCodes();

	public static CodeSelector onFile(Path file) {
		if (Files.isDirectory(file)) {
			throw new IllegalArgumentException("Not a file: " + file);
		}
		return () -> Stream.of(Code.fromFile(file));
	}

	@SuppressWarnings("resource") // Assume the stream will be closed anyway
	public static CodeSelector onDirectory(Path directory) {
		if (!Files.isDirectory(directory)) {
			throw new IllegalArgumentException("Not a directory: " + directory);
		}
		Stream<Path> stream;
		try {
			stream = Files.walk(directory);
		} catch (IOException cause) {
			throw new RuntimeException(cause);
		}
		return () -> stream.map(Code::fromFile);
	}

	public static CodeSelector onCollection(Collection<Path> paths) {
		return () -> paths.stream().flatMap(path -> {
			CodeSelector project = Files.isDirectory(path) ? onDirectory(path) : onFile(path);
			return project.getCodes();
		});
	}
}
