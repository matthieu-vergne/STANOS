package fr.vergne.stanos.junit;

import static java.util.function.Predicate.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Assertions {

	public static <T> List<T> assertFindInCollection(T item, int count, Collection<T> collection) {
		List<T> found = collection.stream().filter(isEqual(item)).collect(Collectors.toList());
		assertEquals(found.size(), count);
		return found;
	}
}
