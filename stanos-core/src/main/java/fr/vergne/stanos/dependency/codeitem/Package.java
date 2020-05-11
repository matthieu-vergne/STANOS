package fr.vergne.stanos.dependency.codeitem;

import java.util.Objects;

public class Package extends CodeItemBase {

	private Package(String id) {
		super(Objects.requireNonNull(id, "No ID provided"));
	}

	public static Package fromPackageName(String packageName) {
		Objects.requireNonNull(packageName, "No package name provided");
		if (packageName.isEmpty()) {
			throw new IllegalArgumentException("Empty package name");
		}
		return new Package(packageName);
	}

	public static Package fromPackagePath(String packagePath) {
		Objects.requireNonNull(packagePath, "No package path provided");
		if (packagePath.isEmpty()) {
			throw new IllegalArgumentException("Empty package path");
		}
		return new Package(packagePath.replace('/', '.'));
	}
}
