package fr.vergne.stanos.core.refactorer;

public interface Y {

	interface Variable extends Y {
		String name();

		void rename(String newName);
	}
}
