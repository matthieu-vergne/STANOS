package fr.vergne.stanos.test.refactorer.javaparser;

import fr.vergne.stanos.core.refactorer.Refactorer.ForCode;
import fr.vergne.stanos.core.refactorer.javaparser.JavaParserRefactorer;
import fr.vergne.stanos.test.refactorer.RefactorerTest;

class JavaParserRefactorerTest extends RefactorerTest {
	@Override
	protected ForCode parseCode(String code) {
		return JavaParserRefactorer.forCode(code);
	}
}
