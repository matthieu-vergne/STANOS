package fr.vergne.stanos.dependency.codeitem;

import java.util.Objects;

// TODO split into Classes & Interfaces
public class Type extends CodeItemBase {

	private Type(String id) {
		super(id);
	}

	public static Type type(Class<?> typeClass) {
		Objects.requireNonNull(typeClass, "No class provided");
		return new Type(typeClass.getName());
	}

	public static Type fromClassName(String className) {
		Objects.requireNonNull(className, "No class name provided");
		return new Type(className);
	}

	public static Type fromClassPath(String classPath) {
		Objects.requireNonNull(classPath, "No class path provided");
		return new Type(classPath.replace('/', '.'));
	}
}
