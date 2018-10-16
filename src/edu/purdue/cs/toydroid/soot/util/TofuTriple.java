package edu.purdue.cs.toydroid.soot.util;


public class TofuTriple<T1, T2, T3> {
	private T1 obj1;
	private T2 obj2;
	private T3 obj3;

	public TofuTriple(T1 t1, T2 t2, T3 t3) {
		obj1 = t1;
		obj2 = t2;
		obj3 = t3;
	}

	public T1 get01() {
		return obj1;
	}

	public T2 get02() {
		return obj2;
	}

	public T3 get03() {
		return obj3;
	}
}
