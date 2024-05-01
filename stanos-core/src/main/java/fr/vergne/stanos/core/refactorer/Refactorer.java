package fr.vergne.stanos.core.refactorer;

public interface Refactorer {

	interface ForCode extends Refactorer {
		String code();

		Code.Source source();
	}

}
