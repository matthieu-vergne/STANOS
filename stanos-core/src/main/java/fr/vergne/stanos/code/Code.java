package fr.vergne.stanos.code;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public interface Code {
	InputStream open();

	public static Code fromFile(Path file) {
		return () -> {
			try {
				return Files.newInputStream(file);
			} catch (IOException cause) {
				throw new RuntimeException(cause);
			}
		};
	}
}
