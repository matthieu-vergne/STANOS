package fr.vergne.stanos.gui.util.recursiveflatmapper;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface RecursiveFlatMapper<T> extends Function<T, Stream<T>> {

	@Override
	Stream<T> apply(T item);

	public static <T> RecursiveFlatMapper<T> recursiveReplace(Function<T, Stream<T>> childrenStream,
			Predicate<T> stopCondition) {
		@SuppressWarnings("unchecked")
		RecursiveFlatMapper<T>[] mapper = new RecursiveFlatMapper[1];
		mapper[0] = item -> {
			if (stopCondition.test(item)) {
				return Stream.of(item);
			} else {
				return childrenStream.apply(item).flatMap(mapper[0]);
			}
		};
		return mapper[0];
	}

	public static <T> RecursiveFlatMapper<T> recursiveReplaceCollection(Function<T, Collection<T>> childrenStream,
			Predicate<T> stopCondition) {
		return recursiveReplace(item -> childrenStream.apply(item).stream(), stopCondition);
	}

	public static <T> RecursiveFlatMapper<T> recursiveAppend(Function<T, Stream<T>> childrenStream) {
		@SuppressWarnings("unchecked")
		RecursiveFlatMapper<T>[] mapper = new RecursiveFlatMapper[1];
		mapper[0] = item -> {
			Stream<T> currentItem = Stream.of(item);
			Stream<T> allChildren = childrenStream.apply(item).flatMap(mapper[0]);
			return Stream.concat(currentItem, allChildren);
		};
		return mapper[0];
	}

	public static <T> RecursiveFlatMapper<T> recursiveAppendCollection(Function<T, Collection<T>> childrenStream) {
		return recursiveAppend(item -> childrenStream.apply(item).stream());
	}
}
