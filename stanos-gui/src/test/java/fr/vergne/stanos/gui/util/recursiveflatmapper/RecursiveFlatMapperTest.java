package fr.vergne.stanos.gui.util.recursiveflatmapper;

import static fr.vergne.stanos.gui.util.recursiveflatmapper.RecursiveFlatMapper.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
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

	static Stream<RecursiveFlatMapper<Item>> replacers() {
		// TODO give them toString() for more readable test report
		Predicate<Item> stopCondition = item -> item.children.isEmpty();
		return Stream.of(recursiveReplaceCollection(item -> item.children, stopCondition),
				recursiveReplace(item -> item.children.stream(), stopCondition));
	}

	@ParameterizedTest
	@MethodSource("replacers")
	void testReplacerRetrievesAllChildren(RecursiveFlatMapper<Item> mapper) {
		Item a = new Item("a");
		Item b = new Item("b");
		Item c = new Item("c");
		Item root = new Item("root", a, b, c);

		List<Item> result = Stream.of(root).flatMap(mapper).collect(Collectors.toList());

		assertEquals(Arrays.asList(a, b, c), result);
	}

	@ParameterizedTest
	@MethodSource("replacers")
	void testRepalcerRetrievesLeavesOnly(RecursiveFlatMapper<Item> mapper) {
		Item leaf = new Item("leaf");
		Item inter = new Item("intermediate", leaf);
		Item root = new Item("root", inter);

		List<Item> result = Stream.of(root).flatMap(mapper).collect(Collectors.toList());

		assertEquals(Arrays.asList(leaf), result);
	}

	static Stream<RecursiveFlatMapper<Item>> appenders() {
		// TODO give them toString() for more readable test report
		return Stream.of(recursiveAppendCollection(item -> item.children),
				recursiveAppend(item -> item.children.stream()));
	}

	@ParameterizedTest
	@MethodSource("appenders")
	void testAppenderRetrievesRootAndChildren(RecursiveFlatMapper<Item> mapper) {
		Item a = new Item("a");
		Item b = new Item("b");
		Item c = new Item("c");
		Item root = new Item("root", a, b, c);

		List<Item> result = Stream.of(root).flatMap(mapper).collect(Collectors.toList());

		assertEquals(Arrays.asList(root, a, b, c), result);
	}

	@ParameterizedTest
	@MethodSource("appenders")
	void testAppenderRetrievesRootToLeaves(RecursiveFlatMapper<Item> mapper) {
		Item leaf = new Item("leaf");
		Item inter = new Item("intermediate", leaf);
		Item root = new Item("root", inter);

		List<Item> result = Stream.of(root).flatMap(mapper).collect(Collectors.toList());

		assertEquals(Arrays.asList(root, inter, leaf), result);
	}
}
