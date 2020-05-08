package fr.vergne.stanos.code;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public interface Code {
	InputStream open() throws CannotOpenCodeException;

	public static Code fromFile(Path filePath) {
		return () -> {
			if (!Files.exists(filePath)) {
				throw new NoCodeFileException(filePath);
			}
			if (Files.isDirectory(filePath)) {
				throw new NoCodeForDirectoryException(filePath);
			}
			if (!Files.isReadable(filePath)) {
				throw new CanotReadCodeException(filePath);
			}
			try {
				return Files.newInputStream(filePath);
			} catch (IOException cause) {
				throw new CannotOpenCodeException(filePath, cause);
			}
		};
	}

	@SuppressWarnings("serial")
	public static class CannotOpenCodeException extends RuntimeException {
		public CannotOpenCodeException(Path path, Throwable cause) {
			super("Cannot open file " + path, cause);
		}
	}

	@SuppressWarnings("serial")
	public static class NoCodeFileException extends RuntimeException {
		public NoCodeFileException(Path path) {
			super("No file " + path);
		}
	}

	@SuppressWarnings("serial")
	public static class NoCodeForDirectoryException extends RuntimeException {
		public NoCodeForDirectoryException(Path path) {
			super("No code for directory " + path);
		}
	}

	@SuppressWarnings("serial")
	public static class CanotReadCodeException extends RuntimeException {
		public CanotReadCodeException(Path path) {
			super("Cannot read code from " + path);
		}
	}
}
