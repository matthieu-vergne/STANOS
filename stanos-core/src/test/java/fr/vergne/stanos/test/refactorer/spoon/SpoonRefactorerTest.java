package fr.vergne.stanos.test.refactorer.spoon;

import fr.vergne.stanos.core.refactorer.Refactorer.ForCode;
import fr.vergne.stanos.core.refactorer.spoon.SpoonRefactorer;
import fr.vergne.stanos.test.refactorer.RefactorerTest;

class SpoonRefactorerTest extends RefactorerTest {
	@Override
	protected ForCode parseCode(String code) {
		return SpoonRefactorer.forCode(code);
	}
}
