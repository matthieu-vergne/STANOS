package fr.vergne.stanos.code;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Stream;

public interface CodeSelector {
	Stream<Code> getCodes();

	public static CodeSelector onFile(Path file) {
		return () -> Intern.streamFile(file).map(Code::fromFile);
	}

	public static CodeSelector onDirectory(Path directory) {
		return () -> Intern.streamDirectory(directory).map(Code::fromFile);
	}

	public static CodeSelector onPath(Path path) {
		return () -> Intern.streamPath(path).map(Code::fromFile);
	}

	public static CodeSelector onPaths(Collection<Path> paths) {
		return () -> paths.stream().flatMap(Intern::streamPath).distinct().map(Code::fromFile);
	}
	
	static class Intern {
		private static Stream<Path> streamFile(Path file) {
			return Stream.of(file);
		}
		
		private static Stream<Path> streamDirectory(Path directory) {
			try {
				return Files.walk(directory).filter(Files::isRegularFile);
			} catch (IOException cause) {
				throw new RuntimeException(cause);
			}
		}
		
		private static Stream<Path> streamPath(Path path) {
			return Files.isDirectory(path) ? Intern.streamDirectory(path) : Intern.streamFile(path);
		}
	}
}
