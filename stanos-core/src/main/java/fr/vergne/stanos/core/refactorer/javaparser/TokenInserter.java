package fr.vergne.stanos.core.refactorer.javaparser;

import com.github.javaparser.JavaToken;

class TokenInserter {
	private JavaToken lastToken;

	private TokenInserter(JavaToken lastToken) {
		this.lastToken = lastToken;
	}

	public static TokenInserter from(JavaToken startToken) {
		return new TokenInserter(startToken);
	}

	TokenInserter insertAfter(JavaToken token) {
		lastToken.insertAfter(token);
		lastToken = token;
		return this;
	}
}
