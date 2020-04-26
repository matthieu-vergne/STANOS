package fr.vergne.stanos.bytecode.jd;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

import org.jd.core.v1.ClassFileToJavaSourceDecompiler;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.loader.LoaderException;
import org.jd.core.v1.api.printer.Printer;

import fr.vergne.stanos.Dependency;
import fr.vergne.stanos.DependencyAnalyser;

public class JDByteCodeAnalyser implements DependencyAnalyser {

	private final File rootFolder;
	private Loader createLoader() {
		return new Loader() {

			@Override
			public byte[] load(String internalName) throws LoaderException {
				InputStream is = this.getClass().getResourceAsStream("/" + internalName + ".class");
				if (is == null) {
					try {
						is = new FileInputStream(new File(rootFolder, internalName + ".class"));
					} catch (FileNotFoundException cause) {
						throw new RuntimeException(cause);
					}
				}

				{
					try (InputStream in = is; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
						byte[] buffer = new byte[1024];
						int read = in.read(buffer);

						while (read > 0) {
							out.write(buffer, 0, read);
							read = in.read(buffer);
						}

						return out.toByteArray();
					} catch (IOException e) {
						throw new LoaderException(e);
					}
				}
			}

			@Override
			public boolean canLoad(String internalName) {
				return this.getClass().getResource("/" + internalName + ".class") != null
						|| new File("/" + internalName + ".class").exists();
			}
		};
	}

	private Printer createPrinter() {
		return new Printer() {
			protected static final String TAB = "  ";
			protected static final String NEWLINE = "\n";

			protected int indentationCount = 0;
			protected StringBuilder sb = new StringBuilder();

			@Override
			public String toString() {
				return sb.toString();
			}

			@Override
			public void start(int maxLineNumber, int majorVersion, int minorVersion) {
			}

			@Override
			public void end() {
			}

			@Override
			public void printText(String text) {
				sb.append(text);
			}

			@Override
			public void printNumericConstant(String constant) {
				sb.append(constant);
			}

			@Override
			public void printStringConstant(String constant, String ownerInternalName) {
				sb.append(constant);
			}

			@Override
			public void printKeyword(String keyword) {
				sb.append(keyword);
			}

			@Override
			public void printDeclaration(int type, String internalTypeName, String name, String descriptor) {
				sb.append(name);
			}

			@Override
			public void printReference(int type, String internalTypeName, String name, String descriptor,
					String ownerInternalName) {
				sb.append(name);
			}

			@Override
			public void indent() {
				this.indentationCount++;
			}

			@Override
			public void unindent() {
				this.indentationCount--;
			}

			@Override
			public void startLine(int lineNumber) {
				for (int i = 0; i < indentationCount; i++)
					sb.append(TAB);
			}

			@Override
			public void endLine() {
				sb.append(NEWLINE);
			}

			@Override
			public void extraLine(int count) {
				while (count-- > 0)
					sb.append(NEWLINE);
			}

			@Override
			public void startMarker(int type) {
			}

			@Override
			public void endMarker(int type) {
			}
		};
	}

	public JDByteCodeAnalyser(File rootFolder) {
		this.rootFolder = rootFolder;
	}

	@Override
	public Collection<Dependency> analyse(InputStream inputStream) {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public Collection<Dependency> analyse(Class<?> classObject) {
		return analyse(classObject.getName().replace('.', '/'));
	}

	@Override
	public Collection<Dependency> analyse(File classFile) {
		Path classPath = classFile.toPath();
		if (classPath.isAbsolute()) {
			classPath = rootFolder.toPath().relativize(classPath);
		}
		return analyse(classPath.toString());
	}

	private Collection<Dependency> analyse(String internalName) {
		ClassFileToJavaSourceDecompiler decompiler = new ClassFileToJavaSourceDecompiler();

		Printer printer = createPrinter();
		try {
			decompiler.decompile(createLoader(), printer, internalName);
		} catch (Exception cause) {
			throw new RuntimeException(cause);
		}

		String source = printer.toString();
		System.out.println(source);

		return Collections.emptyList();
	}

	class X {
		public void xxx() {
		}
	}

	public static void main(String[] args) {
		JDByteCodeAnalyser analyser = new JDByteCodeAnalyser(
				new File("/home/matthieu/Programing/Java/Pester/pester-core/target/classes/"));

		analyser.analyse("fr/vergne/pester/util/cache/Cache");
		System.out.println("========");
		analyser.analyse(new File(
				"/home/matthieu/Programing/Java/Pester/pester-core/target/classes/fr/vergne/pester/util/cache/Cache"));
		System.out.println("========");
		analyser.analyse(new File("fr/vergne/pester/util/cache/Cache"));
		System.out.println("========");
		analyser.analyse(X.class);
	}
}
