package fr.vergne.stanos.core.refactorer.javaparser;

import com.github.javaparser.JavaToken;
import com.github.javaparser.Position;
import com.github.javaparser.Range;

class TokenInserter {
	private JavaToken lastToken;

	private TokenInserter(JavaToken lastToken) {
		this.lastToken = lastToken;
	}

	public static TokenInserter from(JavaToken startToken) {
		return new TokenInserter(startToken);
	}

	TokenInserter insertAfter(JavaToken token) {
		Range lastRange = lastToken.getRange().orElseThrow();
		Position lastEnd = lastRange.end;
		Position begin = lastEnd.withColumn(lastEnd.column + 1);
		Position end = begin.withColumn(begin.column + token.getText().length() - 1);
		token.setRange(new Range(begin, end));
		lastToken.insertAfter(token);
		lastToken = token;
		return this;
	}
}
