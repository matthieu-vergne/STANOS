package fr.vergne.stanos.core.utils;

import java.util.function.BiFunction;

public enum Padding implements BiFunction<String, Integer, String> {
	LEFT((content, minLength) -> {
		return minLength < content.length() //
				? content //
				: String.format("%" + minLength + "s", content);
	}), //
	RIGHT((content, minLength) -> {
		return minLength < content.length() //
				? content //
				: String.format("%-" + minLength + "s", content);
	}), //
	BOTH((content, minLength) -> {
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

	private Padding(BiFunction<String, Integer, String> padder) {
		this.padder = padder;
	}

	@Override
	public String apply(String content, Integer minLength) {
		return padder.apply(content, minLength);
	}
}
