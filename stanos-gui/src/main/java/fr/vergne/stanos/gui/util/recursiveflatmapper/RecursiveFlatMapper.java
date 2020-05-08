package fr.vergne.stanos.gui.util.recursiveflatmapper;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

public class RecursiveFlatMapper<T> implements Function<T, Stream<T>> {
	
	private final Function<T, Stream<T>> childrenStream;
	
	private RecursiveFlatMapper(Function<T, Stream<T>> childrenStream) {
		this.childrenStream = childrenStream;
	}

	@Override
	public Stream<T> apply(T item) {
		Stream<T> currentItem = Stream.of(item);
		Stream<T> allChildren = childrenStream.apply(item).flatMap(this);
		return Stream.concat(currentItem, allChildren);
	}

	public static <T> RecursiveFlatMapper<T> recursive(Function<T, Stream<T>> childrenStream) {
		return new RecursiveFlatMapper<T>(childrenStream);
	}

	public static <T> RecursiveFlatMapper<T> recursiveMapper(Function<T, Collection<T>> childrenStream) {
		return recursive(item -> childrenStream.apply(item).stream());
	}
}
