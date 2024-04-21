package fr.vergne.stanos.core.refactorer;

import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import fr.vergne.stanos.core.refactorer.Y.Variable;

public interface Scope {
	Optional<Scope> parent();

	Stream<Y.Variable> accessibleVariables();

	Stream<Y.Variable> createdVariables();

	Scope createVariable(Y.Variable variable);

	Scope createNewScope();

	class Base implements Scope {

		private final Optional<Scope> parent;
		private final List<Scope> subscopes = new LinkedList<>();

		public Base(Scope parent) {
			this.parent = Optional.of(requireNonNull(parent));
		}

		public Base() {
			this.parent = Optional.empty();
		}

		@Override
		public Optional<Scope> parent() {
			return parent;
		}

		@Override
		public Stream<Y.Variable> accessibleVariables() {
			return parent.map(Scope::accessibleVariables).orElse(Stream.empty());
		}

		@Override
		public Stream<Variable> createdVariables() {
			// FIXME Remove dependency to Visitor Variable
			Comparator<fr.vergne.stanos.core.refactorer.Visitor.Variable> variableComparator = comparing(Visitor.Variable::declaration, comparing(Code.VariableDeclarator::codeIndex));
			return subscopes.stream().flatMap(Scope::createdVariables).map(v -> (Visitor.Variable) v).sorted(variableComparator).map(v -> (Y.Variable) v);
		}

		@Override
		public Scope createNewScope() {
			Scope subscope = new Scope.WithNothing(this);
			subscopes.add(subscope);
			return subscope;
		}

		@Override
		public Scope createVariable(Y.Variable variable) {
			Scope subscope = new Scope.WithVariable(this, variable);
			subscopes.add(subscope);
			return subscope;
		}
	}

	class WithNothing extends Base {
		public WithNothing(Scope parent) {
			super(parent);
		}
	}

	class WithVariable extends Base {

		private final Variable variable;

		public WithVariable(Scope parent, Y.Variable variable) {
			super(parent);
			this.variable = requireNonNull(variable);
		}

		@Override
		public Stream<Y.Variable> accessibleVariables() {
			return Stream.concat(super.accessibleVariables(), Stream.of(variable));
		}

		@Override
		public Stream<Variable> createdVariables() {
			return Stream.concat(Stream.of(variable), super.createdVariables());
		}
	}

	class Context {
		private Scope current = empty();

		public Scope getCurrent() {
			return current;
		}

		public void setCurrent(Scope current) {
			this.current = current;
		}
	}

	static Scope empty() {
		return new Base();
	}
}
