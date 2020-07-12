package fr.vergne.stanos.sample;

public class MyOtherClass {

	private final MyClass myClass;

	public MyOtherClass(MyClass myClass) {
		this.myClass = myClass;
	}

	MyOtherClass someMethod() {
		System.out.println("hello");
		return this;
	}

	MyOtherClass someOtherMethod() {
		myClass.myEmptyMethod();
		return this;
	}

	public static void main(String[] args) {
		MyClass myClass = new MyClass();
		MyOtherClass myOtherClass = new MyOtherClass(myClass);
		myOtherClass//
				.someMethod()//
				.someOtherMethod();
	}
}
