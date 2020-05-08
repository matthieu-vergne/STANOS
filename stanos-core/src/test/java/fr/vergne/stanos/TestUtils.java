package fr.vergne.stanos;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

public class TestUtils {
	public static String currentTestName() {
		return Stream.of(Thread.currentThread().getStackTrace())
				.filter(TestUtils::isTestMethodTrace)
				.map(StackTraceElement::getMethodName)
				.findAny().orElseThrow(() -> new IllegalStateException("Not invoked within a test method"));
	}

	private static boolean isTestMethodTrace(StackTraceElement element) {
		boolean isTestMethod = getMethods(element.getClassName())
				.filter(method -> method.getName().equals(element.getMethodName()))
				.filter(method -> method.getDeclaredAnnotation(Test.class) != null
						|| method.getDeclaredAnnotation(ParameterizedTest.class) != null)
				.findAny().isPresent();
		return isTestMethod;
	}

	private static Stream<Method> getMethods(String className) {
		try {
			return Stream.of(Class.forName(className).getDeclaredMethods());
		} catch (ClassNotFoundException cause) {
			throw new RuntimeException(cause);
		}
	}
}
