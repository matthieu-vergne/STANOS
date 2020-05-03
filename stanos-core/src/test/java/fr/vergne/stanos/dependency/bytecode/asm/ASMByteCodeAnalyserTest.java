package fr.vergne.stanos.dependency.bytecode.asm;

import fr.vergne.stanos.dependency.DependencyAnalyser;
import fr.vergne.stanos.dependency.DependencyAnalyserTest;

class ASMByteCodeAnalyserTest implements DependencyAnalyserTest {
	@Override
	public DependencyAnalyser createDependencyAnalyzer() {
		return new ASMByteCodeAnalyser();
	}
}
