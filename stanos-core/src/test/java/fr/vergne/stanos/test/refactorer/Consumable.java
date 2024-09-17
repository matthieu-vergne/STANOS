package fr.vergne.stanos.test.refactorer;

import java.util.List;
import java.util.stream.Stream;

import fr.vergne.stanos.core.refactorer.Component;

public interface Consumable {
	public static class Package implements Component.Package {
		private final ArgsConsumer consumer;

		public Package(ArgsConsumer consumer) {
			this.consumer = consumer;
		}

		@Override
		public String name() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public void rename(String newName) {
			consumer.consumeArgs(newName);
		}

		@Override
		public Stream<Component.Record> records() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public Component.Record record(String name) {
			consumer.consumeArgs(name);
			return new Record(consumer);
		};

		@Override
		public Stream<Component.Interface> interfaces() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public Component.Interface interf(String name) {
			consumer.consumeArgs(name);
			return new Interface(consumer);
		};

		@Override
		public Stream<Component.Class> classes() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public Component.Class clazz(String name) {
			consumer.consumeArgs(name);
			return new Class(consumer);
		}
	}

	public static class Interface implements Component.Interface {
		private final ArgsConsumer consumer;

		public Interface(ArgsConsumer consumer) {
			this.consumer = consumer;
		}

		@Override
		public String name() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public void rename(String newName) {
			consumer.consumeArgs(newName);
		}

		@Override
		public Stream<Component.Class> classes() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public Stream<Component.Interface> interfaces() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public Stream<Component.Method> methods() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public Component.Method method(String name, List<String> parameterTypes) {
			consumer.consumeArgs(name + ", " + parameterTypes);
			return new Method(consumer);
		};
	}

	public static class Class implements Component.Class {
		private final ArgsConsumer consumer;

		public Class(ArgsConsumer consumer) {
			this.consumer = consumer;
		}

		@Override
		public String name() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public void rename(String newName) {
			consumer.consumeArgs(newName);
		}

		@Override
		public Stream<Component.Field> fields() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public Component.Field field(String name) {
			consumer.consumeArgs(name);
			return new Field(consumer);
		}

		@Override
		public Stream<Component.Method> methods() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public Component.Method method(String name, List<String> parameterTypes) {
			consumer.consumeArgs(name + ", " + parameterTypes);
			return new Method(consumer);
		}

		@Override
		public Stream<Component.Class> classes() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public Component.Class clazz(String name) {
			consumer.consumeArgs(name);
			return new Class(consumer);
		}

		@Override
		public Stream<Component.Interface> interfaces() {
			throw new UnsupportedOperationException("Not implemented yet");
		}
	}

	public static class Record implements Component.Record {
		private final ArgsConsumer consumer;

		public Record(ArgsConsumer consumer) {
			this.consumer = consumer;
		}

		@Override
		public String name() {
			consumer.consumeArgs("");
			return null;
		}

		@Override
		public void rename(String newName) {
			consumer.consumeArgs(newName);
		}
	}

	public static class Method implements Component.Method {
		private final ArgsConsumer consumer;

		public Method(ArgsConsumer consumer) {
			this.consumer = consumer;
		}

		@Override
		public String name() {
			consumer.consumeArgs("");
			return null;
		}

		@Override
		public List<String> parameterTypes() {
			consumer.consumeArgs("");
			return null;
		}

		@Override
		public void rename(String newName) {
			consumer.consumeArgs(newName);
		}

		@Override
		public Stream<Component.Parameter> parameters() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public Component.Parameter parameter(String name) {
			consumer.consumeArgs(name);
			return new Parameter(consumer);
		};

		@Override
		public Stream<Component.Variable> variables() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public Component.Variable variable(String name, int index) {
			consumer.consumeArgs(name + ", " + index);
			return new Variable(consumer);
		};

		@Override
		public Stream<Component.Class> classes() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public Component.Class clazz(String name) {
			consumer.consumeArgs(name);
			return new Class(consumer);
		}

		@Override
		public Stream<Component.Interface> interfaces() {
			throw new UnsupportedOperationException("Not implemented yet");
		}
	}

	public static class Parameter implements Component.Parameter {
		private final ArgsConsumer consumer;

		public Parameter(ArgsConsumer consumer) {
			this.consumer = consumer;
		}

		@Override
		public String name() {
			consumer.consumeArgs("");
			return null;
		}

		@Override
		public void rename(String newName) {
			consumer.consumeArgs(newName);
		}
	}

	public static class Variable implements Component.Variable {
		private final ArgsConsumer consumer;

		public Variable(ArgsConsumer consumer) {
			this.consumer = consumer;
		}

		@Override
		public String name() {
			consumer.consumeArgs("");
			return null;
		}

		@Override
		public void rename(String newName) {
			consumer.consumeArgs(newName);
		}

		@Override
		public Stream<Method> methods() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public Method method(String name, List<String> parameterTypes) {
			consumer.consumeArgs(name + ", " + parameterTypes);
			return new Consumable.Method(consumer);
		}

		@Override
		public void splitDeclaration() {
			consumer.consumeArgs("");
		};

		@Override
		public void joinDeclaration() {
			consumer.consumeArgs("");
		};
	}

	public static class Field implements Component.Field {
		private final ArgsConsumer consumer;

		public Field(ArgsConsumer consumer) {
			this.consumer = consumer;
		}

		@Override
		public String name() {
			consumer.consumeArgs("");
			return null;
		}

		@Override
		public void rename(String newName) {
			consumer.consumeArgs(newName);
		}

		@Override
		public Stream<Method> methods() {
			throw new UnsupportedOperationException("Not implemented yet");
		}

		@Override
		public void scopeTo(Method method) {
			consumer.consumeArgs(method);
		}
	}

	public static interface ArgsConsumer {
		void consumeArgs(Object... args);
	}
}
