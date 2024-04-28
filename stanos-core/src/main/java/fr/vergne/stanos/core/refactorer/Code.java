package fr.vergne.stanos.core.refactorer;

interface Code {

	interface Source extends Code {
		Component.Package defaultPackage();
	}
}
