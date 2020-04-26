package fr.vergne.stanos.bytecode.asm;

import fr.vergne.stanos.DependencyAnalyser;
import fr.vergne.stanos.DependencyAnalyserTest;

class ASMByteCodeAnalyserTest implements DependencyAnalyserTest {
	@Override
	public DependencyAnalyser createDependencyAnalyzer() {
		return new ASMByteCodeAnalyser();
	}
}
