package fr.vergne.stanos.core.refactorer;

public interface Refactorer {

	static Refactorer.ForCode forCode(String code) {
		return new Refactorer.ForCode() {
			private String refactoredCode = code;

			@Override
			public String code() {
				return refactoredCode;
			}

			@Override
			public ForClass locateClass(String classPath) {
				return new ForClass() {
					@Override
					public void rename(String newName) {
						refactoredCode = refactoredCode.replaceAll(classPath, newName);
					}
				};
			}

			@Override
			public ForField locateField(String fieldPath) {
				return new ForField() {
					@Override
					public void rename(String newName) {
						refactoredCode = refactoredCode.replaceAll(fieldPath.substring(fieldPath.lastIndexOf(".") + 1), newName);
					}
				};
			}

		};
	}

	interface ForCode extends Refactorer {
		String code();

		Refactorer.ForClass locateClass(String classPath);

		Refactorer.ForField locateField(String fieldPath);
	}

	interface ForClass extends Refactorer, Renamable {
	}

	interface ForField extends Refactorer, Renamable {
	}

	interface Renamable {
		void rename(String newName);
	}
}
