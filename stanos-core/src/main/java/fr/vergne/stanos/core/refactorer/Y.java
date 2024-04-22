package fr.vergne.stanos.core.refactorer;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

public interface Y {

	public interface Package {
		String name();

		void rename(String newName);

		Stream<Class> classes();

		default Class clazz(String name) {
			return this.classes()//
					.filter(c -> c.name().equals(name))//
					.findFirst().orElseThrow(() -> {
						throw new NoSuchElementException("No class " + name);
					});
		}

		Stream<Interface> interfaces();

		default Interface interf(String name) {
			return this.interfaces()//
					.filter(c -> c.name().equals(name))//
					.findFirst().orElseThrow(() -> {
						throw new NoSuchElementException("No interface " + name);
					});
		}

		Stream<Record> records();

		default Record record(String name) {
			return this.records()//
					.filter(c -> c.name().equals(name))//
					.findFirst().orElseThrow(() -> {
						throw new NoSuchElementException("No record " + name);
					});
		}
	}

	public interface Interface {
		String name();

		void rename(String newName);

		Stream<Method> methods();

		default Method method(String name, List<String> parameterTypes) {
			return this.methods()//
					.filter(m -> m.name().equals(name) && m.parameterTypes().equals(parameterTypes))//
					.findFirst().orElseThrow(() -> {
						throw new NoSuchElementException("No method " + name + parameterTypes);
					});
		}

		Stream<Class> classes();

		default Class clazz(String name) {
			return this.classes()//
					.filter(c -> c.name().equals(name))//
					.findFirst().orElseThrow(() -> {
						throw new NoSuchElementException("No class " + name);
					});
		}

		Stream<Interface> interfaces();

		default Interface interf(String name) {
			return this.interfaces()//
					.filter(c -> c.name().equals(name))//
					.findFirst().orElseThrow(() -> {
						throw new NoSuchElementException("No interface " + name);
					});
		}
	}

	public interface Record {
		String name();

		void rename(String newName);
	}

	public interface Class {
		String name();

		void rename(String newName);

		Stream<Method> methods();

		default Method method(String name, List<String> parameterTypes) {
			return this.methods()//
					.filter(m -> m.name().equals(name) && m.parameterTypes().equals(parameterTypes))//
					.findFirst().orElseThrow(() -> {
						throw new NoSuchElementException("No method " + name + parameterTypes);
					});
		}

		Stream<Class> classes();

		default Class clazz(String name) {
			return this.classes()//
					.filter(c -> c.name().equals(name))//
					.findFirst().orElseThrow(() -> {
						throw new NoSuchElementException("No class " + name);
					});
		}

		Stream<Interface> interfaces();

		default Interface interf(String name) {
			return this.interfaces()//
					.filter(c -> c.name().equals(name))//
					.findFirst().orElseThrow(() -> {
						throw new NoSuchElementException("No interface " + name);
					});
		}
	}

	public interface Method {
		String name();

		List<String> parameterTypes();

		void rename(String newName);

		Stream<Variable> variables();

		default Variable variable(String name, int index) {
			return this.variables()//
					.filter(v -> v.name().equals(name))//
					.skip(index)//
					.findFirst().orElseThrow(() -> {
						throw new NoSuchElementException("No variable " + name + " #" + index);
					});
		}

		Stream<Class> classes();

		default Class clazz(String name) {
			return this.classes()//
					.filter(c -> c.name().equals(name))//
					.findFirst().orElseThrow(() -> {
						throw new NoSuchElementException("No class " + name);
					});
		}

		Stream<Interface> interfaces();

		default Interface interf(String name) {
			return this.interfaces()//
					.filter(c -> c.name().equals(name))//
					.findFirst().orElseThrow(() -> {
						throw new NoSuchElementException("No interface " + name);
					});
		}
	}

	public interface Variable extends Y {
		String name();

		void rename(String newName);

		Stream<Method> methods();

		default Method method(String name, List<String> parameterTypes) {
			return this.methods()//
					.filter(m -> m.name().equals(name) && m.parameterTypes().equals(parameterTypes))//
					.findFirst().orElseThrow(() -> {
						throw new NoSuchElementException("No method " + name + parameterTypes);
					});
		}

	}
}
