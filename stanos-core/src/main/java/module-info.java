module fr.vergne.stanos.core {
//	requires com.github.javaparser.core;
	requires javaparser.core;
//	requires com.github.javaparser.symbolsolver.core;
	requires javaparser.symbol.solver.core;

	requires spoon.core;

	exports fr.vergne.stanos.core.refactorer;
	exports fr.vergne.stanos.core.utils;
}