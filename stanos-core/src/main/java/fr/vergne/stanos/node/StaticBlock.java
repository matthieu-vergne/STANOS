package fr.vergne.stanos.node;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StaticBlock extends Executable {

	public static final String NAME = "<clinit>";

	private StaticBlock(Type classType, List<Type> argsTypes) {
		super(classType.getId() + "." + NAME + "("
				+ argsTypes.stream().map(Type::getId).collect(Collectors.joining(",")) + ")");
	}

	public static StaticBlock staticBlock(Type classType, List<Type> argsTypes) {
		return new StaticBlock(classType, argsTypes);
	}

	public static StaticBlock staticBlock(Type classType, Type... argsTypes) {
		return staticBlock(classType, Arrays.asList(argsTypes));
	}

	public static StaticBlock staticBlock(Class<?> clazz, List<Class<?>> argsClasses) {
		return staticBlock(Type.type(clazz), argsClasses.stream().map(Type::type).collect(Collectors.toList()));
	}

	public static StaticBlock staticBlock(Class<?> clazz, Class<?>... argsClasses) {
		return staticBlock(clazz, Arrays.asList(argsClasses));
	}
}
