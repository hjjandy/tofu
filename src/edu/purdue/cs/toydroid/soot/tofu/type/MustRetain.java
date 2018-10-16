package edu.purdue.cs.toydroid.soot.tofu.type;


import soot.SootMethod;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

import java.util.HashSet;
import java.util.Set;

public class MustRetain {
	private static Pair<SootMethod, Stmt> TempPair = new Pair<>();
	private static Set<Integer> MustRetainedTypes = new HashSet<>();

	public static void retain(Integer type) {
		MustRetainedTypes.add(type);
	}

	public static void retain(Set<Integer> types) {
		MustRetainedTypes.addAll(types);
	}

	public static boolean mustRetain(Integer type) {
		return MustRetainedTypes.contains(type);
	}

	public static boolean mustRetain(Set<Integer> types) {
		return MustRetainedTypes.containsAll(types);
	}

	public static boolean mustRetain(SootMethod method, Stmt stmt) {
		TempPair.setPair(method, stmt);
		Integer type = TypeContextThree.typeOfDataGen(method, stmt);
		if (type != null) {
			return mustRetain(type);
		}
		return false;
	}
}
