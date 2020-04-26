package fr.vergne.stanos.node;

import java.util.Objects;

public class Type extends BasicNode {

	private Type(String id) {
		super(Objects.requireNonNull(id, "No ID provided"));
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
