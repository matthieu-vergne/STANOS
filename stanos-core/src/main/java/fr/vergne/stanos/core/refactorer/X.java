package fr.vergne.stanos.core.refactorer;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import fr.vergne.stanos.core.refactorer.JavaParserUtils.SearchContext;

class X {
	Code code;
	SearchContext ctx;

	public X(Code code, SearchContext ctx) {
		this.code = code;
		this.ctx = ctx;
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
		System.out.print(codeClassOf(sourceCode) + " → ");
		C2 derivedCode = f.apply(sourceCode);
		System.out.println(codeClassOf(derivedCode));
		return new X(derivedCode, ctx);
	}

	public void underive() {
		System.out.println(codeClassOf(code) + " ↲");
	}

	private List<String> codeClassOf(Code code) {
		return code == null ? List.of("null") : Stream.of(code.getClass().getInterfaces()).map(cls -> cls.getSimpleName()).toList();
	}
}
