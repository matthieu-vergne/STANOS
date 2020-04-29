package fr.vergne.stanos;

interface Reducer<T, U> {
	U x(T t);

	interface BiReducer<T, U, V> {
		V x(T t, U u);
	}
}
