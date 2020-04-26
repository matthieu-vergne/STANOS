package fr.vergne.stanos.node;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Method extends BasicNode {

	private Method(Type classType, String name, List<Type> argsTypes, Type returnType) {
		super(classType.getId() + "." + name + "("
				+ argsTypes.stream().map(Type::getId).collect(Collectors.joining(",")) + ")" + returnType.getId());
	}

	public static Method method(Type classType, Type returnType, String name, List<Type> argsTypes) {
		return new Method(classType, name, argsTypes, returnType);
	}

	public static Method method(Type classType, Type returnType, String name, Type... argsTypes) {
		return method(classType, returnType, name, Arrays.asList(argsTypes));
	}

	public static Method method(Class<?> clazz, Class<?> returnClass, String name, List<Class<?>> argsClasses) {
		return method(Type.type(clazz), Type.type(returnClass), name,
				argsClasses.stream().map(Type::type).collect(Collectors.toList()));
	}

	public static Method method(Class<?> clazz, Class<?> returnClass, String name, Class<?>... argsClasses) {
		return method(clazz, returnClass, name, Arrays.asList(argsClasses));
	}
}
