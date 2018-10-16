package edu.purdue.cs.toydroid.soot.tofu.type;


import edu.purdue.cs.toydroid.soot.util.TofuCtxRep;
import edu.purdue.cs.toydroid.soot.util.TofuStmtRep;
import edu.purdue.cs.toydroid.soot.util.TofuVarRep;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;

import java.util.HashSet;
import java.util.Set;

public class TypeContextOne {
	private static Set<TofuVarRep> GammaVar = new HashSet<>();
	private static Set<TofuStmtRep> GammaStmt = new HashSet<>();
	private static int SizeGammaVarOfLastQuery = 0;
	private static long SizeGammaStmtOfLastQuery = 0;

	public static int sizeStmtInGamma() {
		return GammaStmt.size();
	}

	public static int sizeVarInGamma() {
		return GammaVar.size();
	}

	public static boolean containsVar(TofuVarRep tvr) {
		return GammaVar.contains(tvr);
	}

	public static TofuVarRep updateVar(TofuVarRep tvr) {
		if (GammaVar.add(tvr)) {
			return tvr;
		}
		return null;
	}

	public static TofuVarRep updateVar(Value var, SootMethod method, Stmt def, TofuCtxRep ctx) {
		TofuVarRep tvr = new TofuVarRep(var, method, def, ctx);
		return updateVar(tvr);
	}

	public static boolean containsStmt(TofuStmtRep tsr) {
		return GammaStmt.contains(tsr);
	}

	public static TofuStmtRep updateStmt(TofuStmtRep tsr) {
		if (GammaStmt.add(tsr)) {
			return tsr;
		}
		return null;
	}

	public static TofuStmtRep updateStmt(SootMethod method, Stmt stmt, TofuCtxRep ctx) {
		TofuStmtRep tsr = new TofuStmtRep(method, stmt, ctx);
		return updateStmt(tsr);
	}

	public static boolean isGammaChanged() {
		int sgv = GammaVar.size();
		int sgs = GammaStmt.size();
		boolean changed = false;
		if (sgv > SizeGammaVarOfLastQuery || sgs > SizeGammaStmtOfLastQuery) {
			changed = true;
		}
		SizeGammaVarOfLastQuery = sgv;
		SizeGammaStmtOfLastQuery = sgs;
		return changed;
	}

	public static Set<TofuStmtRep> getAllStmts() {
		return GammaStmt;
	}

}
