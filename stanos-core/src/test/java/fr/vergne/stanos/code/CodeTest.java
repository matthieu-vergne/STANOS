package fr.vergne.stanos.code;

import static java.nio.file.attribute.PosixFilePermissions.asFileAttribute;
import static java.nio.file.attribute.PosixFilePermissions.fromString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import fr.vergne.stanos.code.Code.CanotReadCodeException;
import fr.vergne.stanos.code.Code.NoCodeForDirectoryException;
import fr.vergne.stanos.code.Code.NoCodeFileException;

class CodeTest {

	@Test
	void testCodeFromFileFailsOpeningWhenAbsentFile() {
		Path filePath = Paths.get("my/inexistent/file");

		Code code = Code.fromFile(filePath);
		Executable opening = () -> code.open();

		assertThrows(NoCodeFileException.class, opening);
	}

	@Test
	void testCodeFromFileFailsOpeningWhenDirectory() throws IOException {
		Path dirPath = Files.createTempDirectory("dir");

		Code code = Code.fromFile(dirPath);
		Executable opening = () -> code.open();

		assertThrows(NoCodeForDirectoryException.class, opening);
	}

	@Test
	void testCodeFromFileFailsOpeningWhenUnreadableFile() throws IOException {
		Path filePath = Files.createTempFile("notReadableFile", "", asFileAttribute(fromString("-wx-wx-wx")));
		Files.write(filePath, "my test content".getBytes());// ensures can write

		Code code = Code.fromFile(filePath);
		Executable opening = () -> code.open();

		assertThrows(CanotReadCodeException.class, opening);
	}

	@Test
	void testCodeFromFileRetrievesFileContent() throws IOException {
		String expectedContent = "my test content";
		byte[] expectedBytes = expectedContent.getBytes();
		Path filePath = Files.createTempFile("test", "CodeFromFile");
		Files.write(filePath, expectedBytes);

		InputStream inputStream = Code.fromFile(filePath).open();

		byte[] actualBytes = new byte[expectedBytes.length + 1];
		int bytesCount = inputStream.read(actualBytes);
		assertEquals(expectedBytes.length, bytesCount);
		assertEquals(expectedContent, new String(actualBytes, 0, bytesCount));
	}

}
