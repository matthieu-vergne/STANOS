package fr.vergne.stanos.core.refactorer;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Component {

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

		Stream<Field> fields();

		default Field field(String name) {
			return this.fields()//
					.filter(m -> m.name().equals(name))//
					.findFirst().orElseThrow(() -> {
						throw new NoSuchElementException("No field " + name);
					});
		}

		Stream<Method> methods();

		default Method method(String name, List<String> parameterTypes) {
			return this.methods()//
					.filter(m -> m.name().equals(name) && m.parameterTypes().equals(parameterTypes))//
					.findFirst().orElseThrow(() -> {
						String args = parameterTypes.stream().collect(Collectors.joining(", ", "(", ")"));
						throw new NoSuchElementException("No method " + name + args);
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

		Stream<Parameter> parameters();

		default Parameter parameter(String name) {
			return this.parameters()//
					.filter(c -> c.name().equals(name))//
					.findFirst().orElseThrow(() -> {
						throw new NoSuchElementException("No parameter " + name);
					});
		}
	}

	public interface Variable extends Component {
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

		void splitDeclaration();

		void joinDeclaration();

		// TODO Replace clazz by visibility
		void increaseScope(Class clazz);

		void decreaseScope();

		void increaseScope();

		void decreaseScope(int blockIndex);

	}

	public interface Field extends Component {
		String name();

		void rename(String newName);

		// FIXME Move to anonymous class assigned to the variable
		Stream<Method> methods();

		default Method method(String name, List<String> parameterTypes) {
			return this.methods()//
					.filter(m -> m.name().equals(name) && m.parameterTypes().equals(parameterTypes))//
					.findFirst().orElseThrow(() -> {
						throw new NoSuchElementException("No method " + name + parameterTypes);
					});
		}

		void decreaseScope(Method method);
	}

	public interface Parameter extends Component {
		String name();

		void rename(String newName);
	}
}
