package fr.vergne.stanos.dependency.codeitem;

public class Lambda extends Callable {
	
	public static final String NAME_PREFIX = "lambda$";
	private final Method method;

	private Lambda(String lambdaId, Method method) {
		super(lambdaId);
		this.method = method;
	}

	public static String lambdaId(Type handleClassType, String handleName) {
		return handleClassType.getId() + "." + handleName(handleName);
	}
	
	private static String handleName(String name) {
		if (!name.startsWith(NAME_PREFIX)) {
			throw new IllegalArgumentException("Not a lambda name: " + name);
		}
		return name;
	}

	public Method getMethod() {
		return method;
	}

	public static Lambda lambda(Type handleClassType, String handleName, Method method) {
		return new Lambda(lambdaId(handleClassType, handleName), method);
	}

	public static Lambda lambda(String lambdaName, Method method) {
		return new Lambda(lambdaName, method);
	}
}
