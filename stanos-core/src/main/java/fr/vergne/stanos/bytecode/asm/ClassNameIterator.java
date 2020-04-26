package fr.vergne.stanos.bytecode.asm;

import java.util.Iterator;
import java.util.PrimitiveIterator.OfInt;

class ClassNameIterator implements Iterator<String> {

	private final OfInt chars;

	public ClassNameIterator(String byteCodeDescriptor) {
		this.chars = byteCodeDescriptor.chars().iterator();
	}

	@Override
	public boolean hasNext() {
		return chars.hasNext();
	}

	@Override
	public String next() {
		int character = chars.nextInt();
		switch (character) {
		case 'V':
			return "void";
		case 'Z':
			return "boolean";
		case 'C':
			return "char";
		case 'B':
			return "byte";
		case 'S':
			return "short";
		case 'I':
			return "int";
		case 'F':
			return "float";
		case 'J':
			return "long";
		case 'D':
			return "double";
		case 'L':
			StringBuffer buf = new StringBuffer();
			while ((character = chars.nextInt()) != ';') {
				buf.appendCodePoint(character);
			}
			return buf.toString().replace('/', '.');
		case '[':
			return next() + "[]";
		default:
			throw new RuntimeException("Not supported character: " + character);
		}
	}

}
