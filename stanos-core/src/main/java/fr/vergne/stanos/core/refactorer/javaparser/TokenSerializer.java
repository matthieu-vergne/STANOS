package fr.vergne.stanos.core.refactorer.javaparser;

import static java.util.stream.Collectors.joining;

import java.util.List;

import com.github.javaparser.JavaToken;
import com.github.javaparser.JavaToken.Category;
import com.github.javaparser.JavaToken.Kind;
import com.github.javaparser.Position;
import com.github.javaparser.Range;

interface TokenSerializer {
	String serialize(JavaToken token);

	default String serializeAll(List<JavaToken> tokens) {
		return tokens.stream().map(this::serialize).collect(joining());
	}

	record Positions(String begin, String end) {
	}

	public static String full(JavaToken token) {
		Positions positions = token.getRange()//
				.map(range -> new Positions(descriptionOf(range.begin), descriptionOf(range.end)))//
				.orElseGet(() -> new Positions("?", "?"));

		Kind kind = Kind.valueOf(token.getKind());
		Category category = token.getCategory();
		String text = escapedCharacters(token.getText());
		String content = category + "." + kind + ":" + text;

		return "[" + positions.begin + ">" + content + "<" + positions.end + "]";
	}

	public static String textInRange(JavaToken token) {
		Positions positions = token.getRange()//
				.map(range -> new Positions(descriptionOf(range.begin), descriptionOf(range.end)))//
				.orElseGet(() -> new Positions("?", "?"));
		String text = escapedCharacters(token.getText());
		return "[" + positions.begin + " '" + text + "' " + positions.end + "]";
	}

	public static String simple(JavaToken token) {
		if (Kind.valueOf(token.getKind()).equals(Kind.EOF)) {
			return "[EOF]";
		} else {
			Range range = token.getRange().orElseThrow();
			String col = range.begin.column == range.end.column //
					? "col " + range.begin.column //
					: "col " + range.begin.column + "-" + range.end.column;
			return "[" + col + ":" + escapedCharacters(token.getText()) + "]";
		}
	}

	private static String escapedCharacters(String text) {
		return text.replaceAll("\t", "\\t")//
				.replaceAll("\n", "\\n")//
				.replaceAll("\r", "\\r");
	}

	private static String descriptionOf(Position position) {
		return position.line + ":" + position.column;
	}
}
