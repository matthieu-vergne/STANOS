package fr.vergne.stanos.core.refactorer;

public interface Code {

	public interface Source extends Code {
		Component.Package defaultPackage();
	}
}
