package fr.vergne.stanos.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Constructor extends Executable {

	public static final String NAME = "<init>";

	private Constructor(Type classType, List<Type> argsTypes) {
		super(classType.getId() + "." + NAME + "("
				+ argsTypes.stream().map(Type::getId).collect(Collectors.joining(",")) + ")");
	}

	public static Constructor constructor(Type classType, List<Type> argsTypes) {
		return new Constructor(classType, argsTypes);
	}

	public static Constructor constructor(Type classType, Type... argsTypes) {
		return constructor(classType, Arrays.asList(argsTypes));
	}

	public static Constructor constructor(Class<?> clazz, List<Class<?>> argsClasses) {
		return constructor(Type.type(clazz), argsClasses.stream().map(Type::type).collect(Collectors.toList()));
	}

	public static Constructor constructor(Class<?> clazz, Class<?>... argsClasses) {
		return constructor(clazz, Arrays.asList(argsClasses));
	}

	public static Constructor nestedConstructor(Class<?> clazz, Class<?>... argsClasses) {
		List<Class<?>> args = new ArrayList<>(argsClasses.length+1);
		args.add(clazz.getDeclaringClass());
		args.addAll(Arrays.asList(argsClasses));
		return constructor(clazz, args);
	}
}
