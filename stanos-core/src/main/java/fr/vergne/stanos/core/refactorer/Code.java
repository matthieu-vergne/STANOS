package fr.vergne.stanos.core.refactorer;

public interface Code {

	interface Source extends Code {
		Component.Package defaultPackage();
	}
}
