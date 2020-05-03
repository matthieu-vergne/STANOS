module fr.vergne.stanos {
	requires java.logging;
	requires org.objectweb.asm;

	exports fr.vergne.stanos.code;
	exports fr.vergne.stanos.dependency;
	exports fr.vergne.stanos.dependency.bytecode.asm;
}