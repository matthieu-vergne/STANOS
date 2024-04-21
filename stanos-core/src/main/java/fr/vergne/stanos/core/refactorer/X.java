package fr.vergne.stanos.core.refactorer;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import fr.vergne.stanos.core.refactorer.JavaParserUtils.SearchContext;

class X {
	private final Code code;
	private final SearchContext ctx;
	private final Scope.Context scopeCtx;

	public X(Scope.Context scopeCtx, Code code, SearchContext ctx) {
		this.scopeCtx = scopeCtx;
		this.code = code;
		this.ctx = ctx;
	}

	public Scope.Context scopeCtx() {
		return scopeCtx;
	}

	public Code code() {
		return code;
	}

	public SearchContext ctx() {
		return ctx;
	}

	public <C1 extends Code, C2 extends Code> X derive(Function<C1, C2> f) {
		requireNonNull(f, "No derivator provided");
		@SuppressWarnings("unchecked")
		C1 sourceCode = (C1) code;
		Scope scope = scopeCtx.getCurrent();
		Optional<Scope> parent = scope.parent();
		while (parent.isPresent()) {
			parent = parent.flatMap(Scope::parent);
			System.out.print("  ");
		}
		System.out.print(scope.hashCode() + scope.accessibleVariables().map(v -> v.name()).toList().toString() + ": ");
		System.out.print(codeClassOf(sourceCode) + " → ");
		C2 derivedCode = f.apply(sourceCode);
		System.out.println(codeClassOf(derivedCode));
		return new X(scopeCtx, derivedCode, ctx);
	}

	public void underive() {
		System.out.println(codeClassOf(code) + " ↲");
	}

	private List<String> codeClassOf(Code code) {
		return code == null ? List.of("null") : Stream.of(code.getClass().getInterfaces()).map(cls -> cls.getSimpleName()).toList();
	}
}
