package fr.vergne.stanos;

import static fr.vergne.stanos.Action.*;
import static fr.vergne.stanos.codeitem.Constructor.*;
import static fr.vergne.stanos.codeitem.Method.*;
import static fr.vergne.stanos.codeitem.StaticBlock.*;
import static fr.vergne.stanos.codeitem.Type.*;
import static fr.vergne.stanos.util.Formatter.*;

import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Stream;

import fr.vergne.stanos.codeitem.CodeItem;
import fr.vergne.stanos.codeitem.Executable;
import fr.vergne.stanos.codeitem.Lambda;
import fr.vergne.stanos.codeitem.Type;

class DependencyTestCasesBuilder {
	private final Collection<DependencyTestCase> cases = new LinkedList<>();
	private final Class<?> testClass;

	public DependencyTestCasesBuilder() {
		this.testClass = getCallerClass();
	}

	private Class<?> getCallerClass() {
		try {
			return Class.forName(Thread.currentThread().getStackTrace()[3].getClassName());
		} catch (ClassNotFoundException cause) {
			throw new RuntimeException(cause);
		}
	}

	private Class<?> lastAnalysedClass = null;

	public Class<?> getLastAnalysedClass() {
		return lastAnalysedClass;
	}

	public Tester analyse(Class<?> analysedClass) {
		lastAnalysedClass = analysedClass;
		return new Tester(analysedClass);
	}

	public Stream<DependencyTestCase> build() {
		return Stream.of(cases.toArray(new DependencyTestCase[0]));
	}

	public class Tester {
		private final Class<?> analysedClass;

		private Tester(Class<?> analysedClass) {
			this.analysedClass = analysedClass;
		}

		public <S extends CodeItem> Targeter<S> test(S source) {
			return new Targeter<>(analysedClass, source);
		}
	}

	public class Targeter<S extends CodeItem> {
		private final Class<?> analysedClass;
		private final S source;

		private Targeter(Class<?> analysedClass, S source) {
			this.analysedClass = analysedClass;
			this.source = source;
		}

		public <T extends CodeItem> Aggregator<T> calls(int count, T target) {
			cases.add(new DependencyTestCase(analysedClass, CALLS, source, target, count));
			return (c, t) -> calls(c, t);
		}

		public <T extends CodeItem> Aggregator<T> declares(int count, T target) {
			cases.add(new DependencyTestCase(analysedClass, DECLARES, source, target, count));
			return (c, t) -> declares(c, t);
		}
	}

	public interface Aggregator<T> {
		Aggregator<T> and(int count, T t);
	}

	public class DependencyTestCase {
		private final Class<?> analysedClass;
		private final Action action;
		private final CodeItem source;
		private final CodeItem target;
		private final int count;
		private final String name;

		private DependencyTestCase(Class<?> analysedClass, Action action, CodeItem source, CodeItem target, int count) {
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

		public CodeItem source() {
			return source;
		}

		public CodeItem target() {
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

	public Targeter<Type> testType(Class<?> clazz) {
		return analyse(clazz).test(type(clazz));
	}

	public Targeter<Executable> testConstructor(Class<?> clazz) {
		return analyse(clazz).test(constructor(clazz));
	}

	public Targeter<Executable> testStaticBlock(Class<?> clazz) {
		return analyse(clazz).test(staticBlock(clazz));
	}

	public Targeter<Executable> testMethod(Class<?> clazz, String name) {
		return analyse(clazz).test(method(clazz, void.class, name));
	}

	public Targeter<Lambda> testLambda(Class<?> clazz, Lambda lambda) {
		return analyse(clazz).test(lambda);
	}
}
