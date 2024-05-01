package NO_COMMIT;

import java.nio.file.Path;
import java.util.List;

import org.openrewrite.Parser.Input;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;

public class OpenRewrite {

	public static void main(String[] args) {
		JavaParser.Builder<?, ?> parser = JavaParser.fromJavaVersion().classpath("rewrite-java-17");
		JavaParser parser2 = parser.build();
		String code = "class MyClass{void myMethod(){String myParam = null;}}";
		parser2.parseInputs(List.of(Input.fromString(code)), null, null);
//		JavaTemplate template = JavaTemplate.builder("withString(#{any(java.lang.String)}).length()") // Code Snippet
//				.javaParser(parser) // Classpath lookup
//				.staticImports("org.example.StringUtils.withString") // Additional import
//				.build();
	}
}
