package fr.vergne.stanos;

import static fr.vergne.stanos.Action.*;
import static fr.vergne.stanos.util.Formatter.*;

import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Stream;

import fr.vergne.stanos.node.Node;

class CasesBuilder {
	private final Collection<Arguments<?, ?>> cases = new LinkedList<>();
	private final Class<?> testClass;

	public CasesBuilder() {
		this.testClass = getCallerClass();
	}

	private Class<?> getCallerClass() {
		try {
			return Class.forName(Thread.currentThread().getStackTrace()[3].getClassName());
		} catch (ClassNotFoundException cause) {
			throw new RuntimeException(cause);
		}
	}

	public Tester analyse(Class<?> analysedClass) {
		return new Tester(analysedClass);
	}

	public Stream<Arguments<?, ?>> buildAndClean() {
		Stream<Arguments<?, ?>> stream = Stream.of(cases.toArray(new Arguments[0]));
		cases.clear();
		return stream;
	}

	public class Tester {
		private final Class<?> analysedClass;

		private Tester(Class<?> analysedClass) {
			this.analysedClass = analysedClass;
		}

		public <S extends Node> Targeter<S> test(S source) {
			return new Targeter<>(analysedClass, source);
		}
	}

	public class Targeter<S extends Node> {
		private final Class<?> analysedClass;
		private final S source;

		private Targeter(Class<?> analysedClass, S source) {
			this.analysedClass = analysedClass;
			this.source = source;
		}

		public <T extends Node> Aggregator<T> calls(int count, T target) {
			cases.add(new Arguments<>(analysedClass, CALLS, source, target, count));
			return (c, t) -> calls(c, t);
		}

		public <T extends Node> Aggregator<T> declares(int count, T target) {
			cases.add(new Arguments<>(analysedClass, DECLARES, source, target, count));
			return (c, t) -> declares(c, t);
		}
	}

	public interface Aggregator<T> {
		Aggregator<T> and(int count, T t);
	}

	public class Arguments<S extends Node, T extends Node> {
		private final Class<?> analysedClass;
		private final Action action;
		private final S source;
		private final T target;
		private final int count;
		private final String name;

		private Arguments(Class<?> analysedClass, Action action, S source, T target, int count) {
			this.analysedClass = analysedClass;
			this.action = action;
			this.source = source;
			this.target = target;
			this.count = count;

			String extendedName = String.format("%s %s %s %s", source, action, count, target);
			this.name = removeClassPrefixes(extendedName, testClass);
		}

		public Class<?> analysedClass() {
			return analysedClass;
		}

		public Action action() {
			return action;
		}

		public S source() {
			return source;
		}

		public T target() {
			return target;
		}

		public int count() {
			return count;
		}
		
		public Dependency dependency() {
			return new Dependency(source, action, target);
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
