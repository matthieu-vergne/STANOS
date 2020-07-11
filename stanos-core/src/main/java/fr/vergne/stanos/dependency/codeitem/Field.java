package fr.vergne.stanos.dependency.codeitem;

public class Field extends CodeItemBase {

	private Field(CodeItem declarator, String fieldName, Type fieldType) {
		super(declarator.getId() + "." + fieldName + ":" + fieldType.getId());
	}

	public static Field field(CodeItem declarator, Type fieldType, String fieldName) {
		return new Field(declarator, fieldName, fieldType);
	}

	public static Field field(Class<?> clazz, Class<?> fieldClass, String fieldName) {
		return field(Type.type(clazz), Type.type(fieldClass), fieldName);
	}
}
