package fr.vergne.stanos.core.refactorer;

import static java.util.Objects.requireNonNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import fr.vergne.stanos.core.refactorer.Y.Class;
import fr.vergne.stanos.core.refactorer.Y.Interface;
import fr.vergne.stanos.core.refactorer.Y.Method;
import fr.vergne.stanos.core.refactorer.Y.Parameter;
import fr.vergne.stanos.core.refactorer.Y.Variable;

public interface Scope {
	Optional<Scope> parent();

	Scope createNewScope();

	Scope createVariable(Y.Variable variable, Consumer<Y.Method> methodFeeder);

	Stream<Y.Variable> accessibleVariables();

	Scope createField(Y.Field field, Consumer<Y.Method> methodFeeder);

	Stream<Y.Field> accessibleFields();

	Scope createMethod(Y.Method method, Consumer<Y.Variable> variableFeeder, Consumer<Y.Class> classFeeder, Consumer<Y.Interface> interfaceFeeder, Consumer<Y.Parameter> parameterFeeder);

	Stream<Y.Method> accessibleMethods();

	Scope createParameter(Parameter parameter);

	Stream<Y.Parameter> accessibleParameters();

	Scope createRecord(Y.Record clazz/* , Consumer<Y.Method> methodFeeder, Consumer<Y.Class> classFeeder, Consumer<Y.Interface> interfaceFeeder */);

	Stream<Y.Record> accessibleRecords();

	Scope createClass(Y.Class clazz, Consumer<Y.Method> methodFeeder, Consumer<Y.Class> classFeeder, Consumer<Y.Interface> interfaceFeeder, Consumer<Y.Field> fieldFeeder);

	Stream<Y.Class> accessibleClasses();

	Scope createInterface(Y.Interface interf, Consumer<Y.Method> methodFeeder, Consumer<Y.Class> classFeeder, Consumer<Y.Interface> interfaceFeeder);

	Stream<Y.Interface> accessibleInterfaces();

	class Base implements Scope {

		private final Optional<Scope> parent;
		private final List<Scope> subscopes = new LinkedList<>();
		private final Consumer<Y.Variable> variableFeeder;
		private final Consumer<Y.Field> fieldFeeder;
		private final Consumer<Y.Method> methodFeeder;
		private final Consumer<Y.Class> classFeeder;
		private final Consumer<Y.Record> recordFeeder;
		private final Consumer<Y.Interface> interfaceFeeder;
		private final Consumer<Y.Parameter> parameterFeeder;

		public Base(Scope parent, Consumer<Y.Variable> variableFeeder, Consumer<Y.Class> classFeeder, Consumer<Y.Interface> interfaceFeeder, Consumer<Y.Parameter> parameterFeeder) {
			this.parent = Optional.of(requireNonNull(parent));
			this.variableFeeder = variableFeeder;
			this.methodFeeder = ((Base) parent).methodFeeder;
			this.classFeeder = classFeeder;
			this.interfaceFeeder = interfaceFeeder;
			this.recordFeeder = ((Base) parent).recordFeeder;
			this.fieldFeeder = ((Base) parent).fieldFeeder;
			this.parameterFeeder = parameterFeeder;
		}

		public Base(Consumer<Y.Method> methodFeeder, Scope parent, Consumer<Y.Class> classFeeder, Consumer<Y.Interface> interfaceFeeder) {
			this.parent = Optional.of(requireNonNull(parent));
			this.variableFeeder = ((Base) parent).variableFeeder;
			this.methodFeeder = methodFeeder;
			this.classFeeder = classFeeder;
			this.interfaceFeeder = interfaceFeeder;
			this.recordFeeder = ((Base) parent).recordFeeder;
			this.fieldFeeder = ((Base) parent).fieldFeeder;
			this.parameterFeeder = ((Base) parent).parameterFeeder;
		}

		public Base(Consumer<Y.Method> methodFeeder, Scope parent, Consumer<Y.Class> classFeeder, Consumer<Y.Interface> interfaceFeeder, Consumer<Y.Field> fieldFeeder) {
			this.parent = Optional.of(requireNonNull(parent));
			this.variableFeeder = ((Base) parent).variableFeeder;
			this.methodFeeder = methodFeeder;
			this.classFeeder = classFeeder;
			this.interfaceFeeder = interfaceFeeder;
			this.recordFeeder = ((Base) parent).recordFeeder;
			this.fieldFeeder = fieldFeeder;
			this.parameterFeeder = ((Base) parent).parameterFeeder;
		}

		public Base(Scope parent, Consumer<Y.Method> methodFeeder) {
			this.parent = Optional.of(requireNonNull(parent));
			this.variableFeeder = ((Base) parent).variableFeeder;
			this.methodFeeder = methodFeeder;
			this.classFeeder = ((Base) parent).classFeeder;
			this.interfaceFeeder = ((Base) parent).interfaceFeeder;
			this.recordFeeder = ((Base) parent).recordFeeder;
			this.fieldFeeder = ((Base) parent).fieldFeeder;
			this.parameterFeeder = ((Base) parent).parameterFeeder;
		}

		public Base(Scope parent) {
			this.parent = Optional.of(requireNonNull(parent));
			this.variableFeeder = ((Base) parent).variableFeeder;
			this.methodFeeder = ((Base) parent).methodFeeder;
			this.classFeeder = ((Base) parent).classFeeder;
			this.interfaceFeeder = ((Base) parent).interfaceFeeder;
			this.recordFeeder = ((Base) parent).recordFeeder;
			this.fieldFeeder = ((Base) parent).fieldFeeder;
			this.parameterFeeder = ((Base) parent).parameterFeeder;
		}

		public Base(Consumer<Y.Class> defaultPackageClassFeeder, Consumer<Y.Interface> defaultPackageInterfaceFeeder, Consumer<Y.Record> defaultPackageRecordFeeder) {
			this.parent = Optional.empty();
			this.variableFeeder = variable -> {
				throw new UnsupportedOperationException("Root node cannot have variable");
			};
			this.parameterFeeder = parameter -> {
				throw new UnsupportedOperationException("Root node cannot have parameter");
			};
			this.fieldFeeder = field -> {
				throw new UnsupportedOperationException("Root node cannot have field");
			};
			this.methodFeeder = method -> {
				throw new UnsupportedOperationException("Root node cannot have method");
			};
			this.classFeeder = defaultPackageClassFeeder;
			this.interfaceFeeder = defaultPackageInterfaceFeeder;
			this.recordFeeder = defaultPackageRecordFeeder;
		}

		@Override
		public Optional<Scope> parent() {
			return parent;
		}

		@Override
		public Scope createNewScope() {
			Scope subscope = new Scope.WithNothing(this);
			subscopes.add(subscope);
			return subscope;
		}

		@Override
		public Scope createVariable(Y.Variable variable, Consumer<Y.Method> methodFeeder) {
			Scope subscope = new Scope.WithVariable(this, variable, methodFeeder);
			this.subscopes.add(subscope);
			this.variableFeeder.accept(variable);
			return subscope;
		}

		@Override
		public Stream<Y.Variable> accessibleVariables() {
			return parent.map(Scope::accessibleVariables).orElse(Stream.empty());
		}
		
		@Override
		public Scope createParameter(Y.Parameter parameter) {
			Scope subscope = new Scope.WithParameter(this, parameter);
			this.subscopes.add(subscope);
			this.parameterFeeder.accept(parameter);
			return subscope;
		}

		@Override
		public Stream<Y.Parameter> accessibleParameters() {
			return parent.map(Scope::accessibleParameters).orElse(Stream.empty());
		}

		@Override
		public Scope createField(Y.Field field, Consumer<Y.Method> methodFeeder) {
			Scope subscope = new Scope.WithField(this, field, methodFeeder);
			this.subscopes.add(subscope);
			this.fieldFeeder.accept(field);
			return subscope;
		}

		@Override
		public Stream<Y.Field> accessibleFields() {
			return parent.map(Scope::accessibleFields).orElse(Stream.empty());
		}

		@Override
		public Scope createMethod(Y.Method method, Consumer<Variable> variableFeeder, Consumer<Y.Class> classFeeder, Consumer<Y.Interface> interfaceFeeder, Consumer<Y.Parameter> parameterFeeder) {
			Scope subscope = new Scope.WithMethod(this, method, variableFeeder, classFeeder, interfaceFeeder, parameterFeeder);
			this.subscopes.add(subscope);
			this.methodFeeder.accept(method);
			return subscope;
		}

		@Override
		public Stream<Y.Method> accessibleMethods() {
			return parent.map(Scope::accessibleMethods).orElse(Stream.empty());
		}

		@Override
		public Scope createRecord(Y.Record record/* , Consumer<Y.Method> methodFeeder, Consumer<Y.Class> classFeeder, Consumer<Y.Interface> interfaceFeeder */) {
			Scope subscope = new Scope.WithRecord(this, record/* , methodFeeder, classFeeder, interfaceFeeder */);
			this.subscopes.add(subscope);
			this.recordFeeder.accept(record);
			return subscope;
		}

		@Override
		public Stream<Y.Record> accessibleRecords() {
			return parent.map(Scope::accessibleRecords).orElse(Stream.empty());
		}

		@Override
		public Scope createClass(Y.Class clazz, Consumer<Y.Method> methodFeeder, Consumer<Y.Class> classFeeder, Consumer<Y.Interface> interfaceFeeder, Consumer<Y.Field> fieldFeeder) {
			Scope subscope = new Scope.WithClass(this, clazz, methodFeeder, classFeeder, interfaceFeeder, fieldFeeder);
			this.subscopes.add(subscope);
			this.classFeeder.accept(clazz);
			return subscope;
		}

		@Override
		public Stream<Y.Class> accessibleClasses() {
			return parent.map(Scope::accessibleClasses).orElse(Stream.empty());
		}

		@Override
		public Scope createInterface(Y.Interface interf, Consumer<Y.Method> methodFeeder, Consumer<Y.Class> classFeeder, Consumer<Y.Interface> interfaceFeeder) {
			Scope subscope = new Scope.WithInterface(this, interf, methodFeeder, classFeeder, interfaceFeeder);
			this.subscopes.add(subscope);
			this.interfaceFeeder.accept(interf);
			return subscope;
		}

		@Override
		public Stream<Y.Interface> accessibleInterfaces() {
			return parent.map(Scope::accessibleInterfaces).orElse(Stream.empty());
		}
	}

	class WithNothing extends Base {
		public WithNothing(Scope parent) {
			super(parent);
		}
	}

	class WithVariable extends Base {

		private final Variable variable;

		public WithVariable(Scope parent, Y.Variable variable, Consumer<Method> methodFeeder) {
			super(parent, methodFeeder);
			this.variable = requireNonNull(variable);
		}

		@Override
		public Stream<Y.Variable> accessibleVariables() {
			return Stream.concat(Stream.of(variable), super.accessibleVariables());
		}
	}
	
	class WithParameter extends Base {

		private final Parameter parameter;

		public WithParameter(Scope parent, Y.Parameter parameter) {
			super(parent);
			this.parameter = requireNonNull(parameter);
		}

		@Override
		public Stream<Y.Parameter> accessibleParameters() {
			return Stream.concat(Stream.of(parameter), super.accessibleParameters());
		}
	}

	class WithField extends Base {

		private final Y.Field field;

		public WithField(Scope parent, Y.Field field, Consumer<Method> methodFeeder) {
			super(parent, methodFeeder);
			this.field = requireNonNull(field);
		}

		@Override
		public Stream<Y.Field> accessibleFields() {
			return Stream.concat(Stream.of(field), super.accessibleFields());
		}
	}

	class WithMethod extends Base {

		private final Y.Method method;

		public WithMethod(Scope parent, Y.Method method, Consumer<Y.Variable> variableFeeder, Consumer<Y.Class> classFeeder, Consumer<Y.Interface> interfaceFeeder, Consumer<Y.Parameter> parameterFeeder) {
			super(parent, variableFeeder, classFeeder, interfaceFeeder, parameterFeeder);
			this.method = requireNonNull(method);
		}

		@Override
		public Stream<Y.Method> accessibleMethods() {
			return Stream.concat(Stream.of(method), super.accessibleMethods());
		}
	}

	class WithRecord extends Base {

		private final Y.Record record;

		public WithRecord(Scope parent, Y.Record record/* , Consumer<Y.Method> methodFeeder, Consumer<Class> classFeeder, Consumer<Interface> interfaceFeeder */) {
			super(parent);
			this.record = requireNonNull(record);
		}

		@Override
		public Stream<Y.Record> accessibleRecords() {
			return Stream.concat(Stream.of(record), super.accessibleRecords());
		}
	}

	class WithClass extends Base {

		private final Y.Class clazz;

		public WithClass(Scope parent, Y.Class clazz, Consumer<Y.Method> methodFeeder, Consumer<Y.Class> classFeeder, Consumer<Y.Interface> interfaceFeeder, Consumer<Y.Field> fieldsFeeder) {
			super(methodFeeder, parent, classFeeder, interfaceFeeder, fieldsFeeder);
			this.clazz = requireNonNull(clazz);
		}

		@Override
		public Stream<Y.Class> accessibleClasses() {
			return Stream.concat(Stream.of(clazz), super.accessibleClasses());
		}
	}

	class WithInterface extends Base {

		private final Y.Interface Interf;

		public WithInterface(Scope parent, Y.Interface Interf, Consumer<Y.Method> methodFeeder, Consumer<Class> classFeeder, Consumer<Interface> interfaceFeeder) {
			super(methodFeeder, parent, classFeeder, interfaceFeeder);
			this.Interf = requireNonNull(Interf);
		}

		@Override
		public Stream<Y.Interface> accessibleInterfaces() {
			return Stream.concat(Stream.of(Interf), super.accessibleInterfaces());
		}
	}

	class Context {
		private Scope current;

		public Context(Scope root) {
			this.current = root;
		}

		public Scope getCurrent() {
			return current;
		}

		public void setCurrent(Scope current) {
			this.current = current;
		}
	}

	static Scope root(Consumer<Y.Class> defaultPackageClassFeeder, Consumer<Y.Interface> defaultPackageInterfaceFeeder, Consumer<Y.Record> defaultPackageRecordFeeder) {
		return new Base(defaultPackageClassFeeder, defaultPackageInterfaceFeeder, defaultPackageRecordFeeder);
	}

}
