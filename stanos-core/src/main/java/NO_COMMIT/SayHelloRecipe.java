package NO_COMMIT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.*;
import org.openrewrite.internal.lang.NonNull;

import java.util.Objects;

// Making your recipe immutable helps make them idempotent and eliminates a variety of possible bugs.
// Configuring your recipe in this way also guarantees that basic validation of parameters will be done for you by rewrite.
// Also note: All recipes must be serializable. This is verified by RewriteTest.rewriteRun() in your tests.
public class SayHelloRecipe extends Recipe {
    @Option(displayName = "Fully Qualified Class Name",
            description = "A fully qualified class name indicating which class to add a hello() method to.",
            example = "com.yourorg.FooBar")
    @NonNull
    String fullyQualifiedClassName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SayHelloRecipe that = (SayHelloRecipe) o;
        return Objects.equals(fullyQualifiedClassName, that.fullyQualifiedClassName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fullyQualifiedClassName);
    }

    public void setFullyQualifiedClassName(String fullyQualifiedClassName) {
        this.fullyQualifiedClassName = fullyQualifiedClassName;
    }

    public String getFullyQualifiedClassName() {
        return fullyQualifiedClassName;
    }

    // All recipes must be serializable. This is verified by RewriteTest.rewriteRun() in your tests.
    @JsonCreator
    public SayHelloRecipe(@NonNull @JsonProperty("fullyQualifiedClassName") String fullyQualifiedClassName) {
        this.fullyQualifiedClassName = fullyQualifiedClassName;
    }

    @Override
    public String getDisplayName() {
        return "Say Hello";
    }

    @Override
    public String getDescription() {
        return "Adds a \"hello\" method to the specified class.";
    }

    @Override
    public String toString() {
        return "SayHelloRecipe{" +
                "fullyQualifiedClassName='" + fullyQualifiedClassName + '\'' +
                '}';
    }

    // TODO: Override getVisitor() to return a JavaIsoVisitor to perform the refactoring
}