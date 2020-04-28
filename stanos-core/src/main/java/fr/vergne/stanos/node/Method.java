package fr.vergne.stanos.node;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Method extends BasicNode {

	private Method(Type classType, String name, List<Type> argsTypes, Type returnType) {
		super(classType.getId() + "." + methodName(name) + "("
				+ argsTypes.stream().map(Type::getId).collect(Collectors.joining(",")) + ")" + returnType.getId());
	}

	private static String methodName(String name) {
		if (Constructor.NAME.equals(name)) {
			throw new IllegalArgumentException("Constructor name " + name);
		}
		if (StaticBlock.NAME.equals(name)) {
			throw new IllegalArgumentException("Static block name " + name);
		}
		return name;
	}

	public static Method method(Type classType, Type returnType, String name, List<Type> argsTypes) {
		return new Method(classType, methodName(name), argsTypes, returnType);
	}

	public static Method method(Type classType, Type returnType, String name, Type... argsTypes) {
		return method(classType, returnType, methodName(name), Arrays.asList(argsTypes));
	}

	public static Method method(Class<?> clazz, Class<?> returnClass, String name, List<Class<?>> argsClasses) {
		return method(Type.type(clazz), Type.type(returnClass), methodName(name),
				argsClasses.stream().map(Type::type).collect(Collectors.toList()));
	}

	public static Method method(Class<?> clazz, Class<?> returnClass, String name, Class<?>... argsClasses) {
		return method(clazz, returnClass, methodName(name), Arrays.asList(argsClasses));
	}
}
