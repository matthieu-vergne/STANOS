package NO_COMMIT;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;

/**
 * Can be used to validate the code by compiling it. A proper validation would
 * also need to execute existing tests.
 */
public class JDKCompiler {

	public static void main(final String[] args) throws Exception {
		Path javaFilePath = Files.createTempFile("code", ".java");
		Files.writeString(javaFilePath, "class MyClass{void myMethod(){String myParam = null;}}");

		final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		try (final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null,
				StandardCharsets.UTF_8)) {
			final Iterable<? extends JavaFileObject> compilationUnits = fileManager
					.getJavaFileObjectsFromFiles(Arrays.asList(javaFilePath.toFile()));
			DiagnosticCollector<? super JavaFileObject> diagnosticListener = new DiagnosticCollector<>();
			Iterable<String> options = List.of("--release", "17");
			final JavacTask javacTask = (JavacTask) compiler.getTask(null, fileManager, diagnosticListener, options,
					null, compilationUnits);
			final Iterable<? extends CompilationUnitTree> compilationUnitTrees = javacTask.parse();
			final ClassTree classTree = (ClassTree) compilationUnitTrees.iterator().next().getTypeDecls().get(0);

			final List<? extends Tree> classMemberList = classTree.getMembers();
			final List<MethodTree> classMethodMemberList = classMemberList.stream()//
					.filter(MethodTree.class::isInstance)//
					.map(MethodTree.class::cast)//
					.collect(Collectors.toList());
			// just prints the names of the methods
			classMethodMemberList.stream().map(MethodTree::getName).forEachOrdered(System.out::println);
			diagnosticListener.getDiagnostics().forEach(diagnostic -> {
				System.err.println(diagnostic);
			});
			compilationUnits.forEach(cu -> {
				CharSequence content;
				try {
					content = cu.getCharContent(false);
				} catch (IOException cause) {
					throw new RuntimeException(cause);
				}
				System.out.println(content);
			});
		}
	}

}
