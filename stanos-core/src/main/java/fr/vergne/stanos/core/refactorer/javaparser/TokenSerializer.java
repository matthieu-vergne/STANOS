package fr.vergne.stanos.core.refactorer.javaparser;

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.function.BiFunction;

import com.github.javaparser.JavaToken;
import com.github.javaparser.JavaToken.Category;
import com.github.javaparser.JavaToken.Kind;
import com.github.javaparser.Position;
import com.github.javaparser.Range;

interface TokenSerializer {
	String serialize(JavaToken token);

	enum Align implements BiFunction<String, Integer, String> {
		RIGHT((content, minLength) -> {
			return minLength < content.length() //
					? content //
					: String.format("%" + minLength + "s", content);
		}), //
		LEFT((content, minLength) -> {
			return minLength < content.length() //
					? content //
					: String.format("%-" + minLength + "s", content);
		}), //
		CENTER((content, minLength) -> {
			int length = content.length();
			int totalPadding = minLength - length;
			if (totalPadding <= 0) {
				return content;
			} else {
				int halfPadding = totalPadding / 2;
				int minHalfLength = length + halfPadding;
				String leftPadded = String.format("%" + minHalfLength + "s", content);
				String leftRightPadded = String.format("%-" + minLength + "s", leftPadded);
				return leftRightPadded;
			}
		});

		private final BiFunction<String, Integer, String> padder;

		private Align(BiFunction<String, Integer, String> padder) {
			this.padder = padder;
		}

		@Override
		public String apply(String content, Integer minLength) {
			return padder.apply(content, minLength);
		}
	}

	default TokenSerializer withMinLength(int minLength) {
		return withMinLength(minLength, Align.LEFT);
	}

	default TokenSerializer withMinLength(int minLength, Align align) {
		if (minLength < 0) {
			throw new IllegalArgumentException("Min length must be positive");
		}
		return token -> align.apply(this.serialize(token), minLength);
	}

	default String serializeAll(List<JavaToken> tokens) {
		return tokens.stream().map(this::serialize).collect(joining());
	}

	record Positions(String begin, String end) {
	}

	public static TokenSerializer full() {
		return token -> {
			Positions positions = token.getRange()//
					.map(range -> new Positions(descriptionOf(range.begin), descriptionOf(range.end)))//
					.orElseGet(() -> new Positions("?", "?"));

			Kind kind = Kind.valueOf(token.getKind());
			Category category = token.getCategory();
			String text = escapedCharacters(token.getText());
			String content = category + "." + kind + ":" + text;

			return "[" + positions.begin + ">" + content + "<" + positions.end + "]";
		};
	}

	public static TokenSerializer textInRange() {
		return token -> {
			Positions positions = token.getRange()//
					.map(range -> new Positions(descriptionOf(range.begin), descriptionOf(range.end)))//
					.orElseGet(() -> new Positions("?", "?"));
			String text = escapedCharacters(token.getText());
			return "[" + positions.begin + " '" + text + "' " + positions.end + "]";
		};
	}

	public static TokenSerializer simple() {
		return token -> {
			if (Kind.valueOf(token.getKind()).equals(Kind.EOF)) {
				return "[EOF]";
			} else {
				Range range = token.getRange().orElseThrow();
				String col = range.begin.column == range.end.column //
						? "col " + range.begin.column //
						: "col " + range.begin.column + "-" + range.end.column;
				return "[" + col + ":" + escapedCharacters(token.getText()) + "]";
			}
		};
	}

	public static TokenSerializer textOnly() {
		return token -> {
			return token.getKind() == Kind.EOF.getKind() //
					? "[EOF]" //
					: "[" + escapedCharacters(token.getText()) + "]";
		};
	}

	private static String escapedCharacters(String text) {
		return text.replaceAll("\t", "\\\\t")//
				.replaceAll("\n", "\\\\n")//
				.replaceAll("\r", "\\\\r");
	}

	private static String descriptionOf(Position position) {
		return position.line + ":" + position.column;
	}
}
