package fr.vergne.stanos.sample;

public class MyClass {

	static int MY_CONSTANT = 42;
	int myField = 123;

	int myMethod(int myArgument) {
		int myLocalField = myField - myArgument + MY_CONSTANT;
		return myLocalField;
	}

	void myEmptyMethod() {
	}

	public static void main(String[] args) {
		MyClass myClass = new MyClass();
		int myResult = myClass.myMethod(456);
		System.out.println(myResult);
	}
}
