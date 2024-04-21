package fr.vergne.stanos.core.refactorer;

import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import fr.vergne.stanos.core.refactorer.Y.Class;
import fr.vergne.stanos.core.refactorer.Y.Interface;
import fr.vergne.stanos.core.refactorer.Y.Method;
import fr.vergne.stanos.core.refactorer.Y.Variable;

public interface Scope {
	Optional<Scope> parent();

	Scope createNewScope();

	Scope createVariable(Y.Variable variable, Consumer<Y.Method> methodFeeder);

	Stream<Y.Variable> accessibleVariables();

	Scope createMethod(Y.Method method, Consumer<Y.Variable> variableFeeder, Consumer<Y.Class> classFeeder, Consumer<Y.Interface> interfaceFeeder);

	Stream<Y.Method> accessibleMethods();

	Scope createClass(Y.Class clazz, Consumer<Y.Method> methodFeeder, Consumer<Y.Class> classFeeder, Consumer<Y.Interface> interfaceFeeder);

	Stream<Y.Class> accessibleClasses();

	Stream<Y.Class> createdClasses();

	Scope createInterface(Y.Interface interf, Consumer<Y.Method> methodFeeder, Consumer<Y.Class> classFeeder, Consumer<Y.Interface> interfaceFeeder);

	Stream<Y.Interface> accessibleInterfaces();

	Stream<Y.Interface> createdInterfaces();

	class Base implements Scope {

		private final Optional<Scope> parent;
		private final List<Scope> subscopes = new LinkedList<>();
		private final Consumer<Y.Variable> variableFeeder;
		private final Consumer<Y.Method> methodFeeder;
		private final Consumer<Y.Class> classFeeder;
		private final Consumer<Y.Interface> interfaceFeeder;

		public Base(Scope parent, Consumer<Y.Variable> variableFeeder, Consumer<Y.Class> classFeeder, Consumer<Y.Interface> interfaceFeeder) {
			this.parent = Optional.of(requireNonNull(parent));
			this.variableFeeder = variableFeeder;
			this.methodFeeder = ((Base) parent).methodFeeder;
			this.classFeeder = classFeeder;
			this.interfaceFeeder = interfaceFeeder;
		}

		public Base(Consumer<Y.Method> methodFeeder, Scope parent, Consumer<Class> classFeeder, Consumer<Interface> interfaceFeeder) {
			this.parent = Optional.of(requireNonNull(parent));
			this.variableFeeder = ((Base) parent).variableFeeder;
			this.methodFeeder = methodFeeder;
			this.classFeeder = classFeeder;
			this.interfaceFeeder = interfaceFeeder;
		}

		public Base(Scope parent, Consumer<Y.Method> methodFeeder) {
			this.parent = Optional.of(requireNonNull(parent));
			this.variableFeeder = ((Base) parent).variableFeeder;
			this.methodFeeder = methodFeeder;
			this.classFeeder = ((Base) parent).classFeeder;
			this.interfaceFeeder = ((Base) parent).interfaceFeeder;
		}

		public Base(Scope parent) {
			this.parent = Optional.of(requireNonNull(parent));
			this.variableFeeder = ((Base) parent).variableFeeder;
			this.methodFeeder = ((Base) parent).methodFeeder;
			this.classFeeder = ((Base) parent).classFeeder;
			this.interfaceFeeder = ((Base) parent).interfaceFeeder;
		}

		public Base(Consumer<Class> defaultPackageClassFeeder, Consumer<Interface> defaultPackageInterfaceFeeder) {
			this.parent = Optional.empty();
			this.variableFeeder = variable -> {
				throw new UnsupportedOperationException("Root node cannot have variable");
			};
			this.methodFeeder = method -> {
				throw new UnsupportedOperationException("Root node cannot have methods");
			};
			this.classFeeder = defaultPackageClassFeeder;
			this.interfaceFeeder = defaultPackageInterfaceFeeder;
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
		public Scope createMethod(Y.Method method, Consumer<Variable> variableFeeder, Consumer<Y.Class> classFeeder, Consumer<Y.Interface> interfaceFeeder) {
			Scope subscope = new Scope.WithMethod(this, method, variableFeeder, classFeeder, interfaceFeeder);
			this.subscopes.add(subscope);
			this.methodFeeder.accept(method);
			return subscope;
		}

		@Override
		public Stream<Y.Method> accessibleMethods() {
			return parent.map(Scope::accessibleMethods).orElse(Stream.empty());
		}

		@Override
		public Scope createClass(Y.Class clazz, Consumer<Y.Method> methodFeeder, Consumer<Y.Class> classFeeder, Consumer<Y.Interface> interfaceFeeder) {
			Scope subscope = new Scope.WithClass(this, clazz, methodFeeder, classFeeder, interfaceFeeder);
			this.subscopes.add(subscope);
			this.classFeeder.accept(clazz);
			return subscope;
		}

		@Override
		public Stream<Y.Class> accessibleClasses() {
			return parent.map(Scope::accessibleClasses).orElse(Stream.empty());
		}

		@Override
		public Stream<Y.Class> createdClasses() {
			// FIXME Remove dependency to Visitor Method
			Comparator<fr.vergne.stanos.core.refactorer.Visitor.Class> classComparator = comparing(Visitor.Class::declaration, comparing(Code.ClassDeclaration::codeIndex));
			return subscopes.stream()

					.peek(x -> System.out.println(":: " + x))

					.flatMap(Scope::createdClasses).map(v -> (Visitor.Class) v).sorted(classComparator).map(v -> (Y.Class) v);
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

		@Override
		public Stream<Y.Interface> createdInterfaces() {
			// FIXME Remove dependency to Visitor Method
			Comparator<fr.vergne.stanos.core.refactorer.Visitor.Interface> interfaceComparator = comparing(Visitor.Interface::declaration, comparing(Code.InterfaceDeclaration::codeIndex));
			return subscopes.stream()

					.peek(x -> System.out.println(":: " + x))

					.flatMap(Scope::createdInterfaces).map(v -> (Visitor.Interface) v).sorted(interfaceComparator).map(v -> (Y.Interface) v);
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
			return Stream.concat(super.accessibleVariables(), Stream.of(variable));
		}
	}

	class WithMethod extends Base {

		private final Y.Method method;

		public WithMethod(Scope parent, Y.Method method, Consumer<Variable> variableFeeder, Consumer<Class> classFeeder, Consumer<Interface> interfaceFeeder) {
			super(parent, variableFeeder, classFeeder, interfaceFeeder);
			this.method = requireNonNull(method);
		}

		@Override
		public Stream<Y.Method> accessibleMethods() {
			return Stream.concat(super.accessibleMethods(), Stream.of(method));
		}
	}

	class WithClass extends Base {

		private final Y.Class clazz;

		public WithClass(Scope parent, Y.Class clazz, Consumer<Y.Method> methodFeeder, Consumer<Class> classFeeder, Consumer<Interface> interfaceFeeder) {
			super(methodFeeder, parent, classFeeder, interfaceFeeder);
			this.clazz = requireNonNull(clazz);
		}

		@Override
		public Stream<Y.Class> accessibleClasses() {
			return Stream.concat(super.accessibleClasses(), Stream.of(clazz));
		}

		@Override
		public Stream<Y.Class> createdClasses() {
			return Stream.concat(Stream.of(clazz), super.createdClasses());
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
			return Stream.concat(super.accessibleInterfaces(), Stream.of(Interf));
		}

		@Override
		public Stream<Y.Interface> createdInterfaces() {
			return Stream.concat(Stream.of(Interf), super.createdInterfaces());
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

	static Scope root(Consumer<Y.Class> defaultPackageClassFeeder, Consumer<Y.Interface> defaultPackageInterfaceFeeder) {
		return new Base(defaultPackageClassFeeder, defaultPackageInterfaceFeeder);
	}

}
