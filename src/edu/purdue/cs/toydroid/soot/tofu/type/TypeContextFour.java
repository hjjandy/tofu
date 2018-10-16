package edu.purdue.cs.toydroid.soot.tofu.type;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TypeContextFour {
	private static final Pair<SootMethod, Stmt> TempPair = new Pair<>();
	private static Map<Pair<SootMethod, Stmt>, Integer> GammaStmt = new HashMap<>();

	public static boolean containsStmt(SootMethod method, Stmt stmt) {
		TempPair.setPair(method, stmt);
		return GammaStmt.containsKey(TempPair);
	}

	public static Integer getTypeOf(SootMethod method, Stmt stmt) {
		TempPair.setPair(method, stmt);
		return getTypeOf(TempPair);
	}

	public static Integer getTypeOf(Pair<SootMethod, Stmt> pair) {
		return GammaStmt.get(pair);
	}

	public static Pair<SootMethod, Stmt> updateStmt(SootMethod method, Stmt stmt, Integer type) {
		Pair<SootMethod, Stmt> pair = TempPair;
		pair.setPair(method, stmt);
		if (!GammaStmt.containsKey(pair)) {
			pair = new Pair<>(method, stmt);
		}
		if (!type.equals(GammaStmt.put(pair, type))) {
			return pair;
		}
		return null;
	}

	public static Set<Pair<SootMethod, Stmt>> getTypedStmts() {
		return GammaStmt.keySet();
	}

	public static boolean isRemovable(Pair<SootMethod, Stmt> stmt) {
		Integer obj = GammaStmt.get(stmt);
		return TypeSystem.isRemovable(obj);
	}

	public static boolean isUnremovable(Pair<SootMethod, Stmt> stmt) {
		Integer obj = GammaStmt.get(stmt);
		return TypeSystem.isUnremovable(obj);
	}
}
