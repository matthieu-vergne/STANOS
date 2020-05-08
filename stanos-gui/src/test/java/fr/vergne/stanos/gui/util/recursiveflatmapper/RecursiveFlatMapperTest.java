package fr.vergne.stanos.gui.util.recursiveflatmapper;

import static fr.vergne.stanos.gui.util.recursiveflatmapper.RecursiveFlatMapper.recursive;
import static fr.vergne.stanos.gui.util.recursiveflatmapper.RecursiveFlatMapper.recursiveMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class RecursiveFlatMapperTest {

	class Item {
		private final String value;
		private final List<Item> children;

		public Item(String value, Item... children) {
			this.value = value;
			this.children = Arrays.asList(children);
		}

		@Override
		public String toString() {
			return value;
		}
	}

	static Stream<RecursiveFlatMapper<Item>> factories() {
		// TODO name them for more readable test report
		return Stream.of(recursiveMapper(item -> item.children), recursive(item -> item.children.stream()));
	}

	@ParameterizedTest
	@MethodSource("factories")
	void testRetrievesAllChildrenOfSingleLevel(RecursiveFlatMapper<Item> mapper) {
		Item a = new Item("a");
		Item b = new Item("b");
		Item c = new Item("c");
		Item root = new Item("root", a, b, c);

		List<Item> result = Stream.of(root).flatMap(mapper).collect(Collectors.toList());

		assertEquals(Arrays.asList(root, a, b, c), result);
	}

	@ParameterizedTest
	@MethodSource("factories")
	void testRetrievesAllLevels(RecursiveFlatMapper<Item> mapper) {
		Item leaf = new Item("leaf");
		Item inter = new Item("intermediate", leaf);
		Item root = new Item("root", inter);

		List<Item> result = Stream.of(root).flatMap(mapper).collect(Collectors.toList());

		assertEquals(Arrays.asList(root, inter, leaf), result);
	}
}
