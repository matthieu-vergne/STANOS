package fr.vergne.stanos.code;

import static fr.vergne.stanos.TestUtils.currentTestName;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class CodeSelectorTest {

	private static final Function<Path, CodeSelector> ON_FILE_FACTORY = name("onFile", CodeSelector::onFile);
	private static final Function<Path, CodeSelector> ON_DIR_FACTORY = name("onDirectory", CodeSelector::onDirectory);
	private static final Function<Path, CodeSelector> ON_PATH_FACTORY = name("onPath", CodeSelector::onPath);
	private static final Function<Path, CodeSelector> ON_PATHS_FACTORY = name("onPaths",
			path -> CodeSelector.onPaths(Arrays.asList(path)));

	static Stream<Function<Path, CodeSelector>> singleFileFactory() {
		return Stream.of(ON_FILE_FACTORY, ON_PATH_FACTORY, ON_PATHS_FACTORY);
	}

	static Stream<Function<Path, CodeSelector>> singleDirectoryFactory() {
		return Stream.of(ON_DIR_FACTORY, ON_PATH_FACTORY, ON_PATHS_FACTORY);
	}

	@ParameterizedTest
	@MethodSource("singleFileFactory")
	void testSelectorOnFileReturnsOnlyProvidedFile(Function<Path, CodeSelector> factory) throws IOException {
		Path filePath = Files.createTempFile(currentTestName(), "");
		String expectedContent = "check this content is the only one retrieved";
		Files.write(filePath, expectedContent.getBytes());

		List<Code> codes = factory.apply(filePath).getCodes().collect(Collectors.toList());

		List<String> actualContents = readAll(codes).collect(Collectors.toList());
		List<String> expectedContents = Arrays.asList(expectedContent);
		assertEquals(expectedContents, actualContents);
	}

	@ParameterizedTest
	@MethodSource("singleDirectoryFactory")
	void testSelectorOnDirectoryReturnsNothingWhenEmptyDirectory(Function<Path, CodeSelector> factory)
			throws IOException {
		Path dirPath = Files.createTempDirectory(currentTestName());

		List<Code> codes = factory.apply(dirPath).getCodes().collect(Collectors.toList());

		assertEquals(0, codes.size(), () -> readAll(codes).collect(Collectors.toList()).toString());
	}

	@ParameterizedTest
	@MethodSource("singleDirectoryFactory")
	void testSelectorOnDirectoryReturnsAllFilesInDirectory(Function<Path, CodeSelector> factory) throws IOException {
		Path dirPath = Files.createTempDirectory(currentTestName());
		Path file1 = Files.createTempFile(dirPath, "file", "");
		Path file2 = Files.createTempFile(dirPath, "file", "");
		String content1 = "content 1";
		String content2 = "content 2";
		Files.write(file1, content1.getBytes());
		Files.write(file2, content2.getBytes());

		List<Code> codes = factory.apply(dirPath).getCodes().collect(Collectors.toList());

		List<String> actualContents = readAll(codes).sorted().collect(Collectors.toList());
		List<String> expectedContents = Stream.of(content1, content2).sorted().collect(Collectors.toList());
		assertEquals(expectedContents, actualContents);
	}

	@ParameterizedTest
	@MethodSource("singleDirectoryFactory")
	void testSelectorOnDirectoryReturnsAllFilesInSubDirectory(Function<Path, CodeSelector> factory) throws IOException {
		Path dirPath = Files.createTempDirectory(currentTestName());
		Path subdirPath = Files.createTempDirectory(dirPath, "subdir");
		Path file1 = Files.createTempFile(subdirPath, "file", "");
		Path file2 = Files.createTempFile(subdirPath, "file", "");
		String content1 = "content 1";
		String content2 = "content 2";
		Files.write(file1, content1.getBytes());
		Files.write(file2, content2.getBytes());

		List<Code> codes = factory.apply(dirPath).getCodes().collect(Collectors.toList());

		List<String> actualContents = readAll(codes).sorted().collect(Collectors.toList());
		List<String> expectedContents = Stream.of(content1, content2).sorted().collect(Collectors.toList());
		assertEquals(expectedContents, actualContents);
	}

	@Test
	void testSelectorOnPathsReturnsFilesOfAllProvidedPaths() throws IOException {
		Path path1 = Files.createTempFile(currentTestName() + "1", "");
		Path path2 = Files.createTempFile(currentTestName() + "2", "");
		String content1 = "content 1";
		String content2 = "content 2";
		Files.write(path1, content1.getBytes());
		Files.write(path2, content2.getBytes());

		List<Code> codes = CodeSelector.onPaths(Arrays.asList(path1, path2)).getCodes().collect(Collectors.toList());

		List<String> actualContents = readAll(codes).sorted().collect(Collectors.toList());
		List<String> expectedContents = Stream.of(content1, content2).sorted().collect(Collectors.toList());
		assertEquals(expectedContents, actualContents);
	}

	private Stream<String> readAll(List<Code> codes) {
		return codes.stream().map(this::readContent);
	}

	private String readContent(Code code) {
		InputStream inputStream = code.open();
		byte[] actualBytes = new byte[100];
		int bytesCount;
		try {
			bytesCount = inputStream.read(actualBytes);
		} catch (IOException cause) {
			throw new RuntimeException(cause);
		}
		return new String(actualBytes, 0, bytesCount);
	}

	private static Function<Path, CodeSelector> name(String name, Function<Path, CodeSelector> function) {
		return new Function<Path, CodeSelector>() {

			@Override
			public CodeSelector apply(Path path) {
				return function.apply(path);
			}

			@Override
			public String toString() {
				return name;
			}
		};
	}
}
