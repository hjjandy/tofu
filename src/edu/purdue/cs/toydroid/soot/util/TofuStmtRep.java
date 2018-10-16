package edu.purdue.cs.toydroid.soot.util;


import soot.SootMethod;
import soot.jimple.Stmt;

public class TofuStmtRep {
	private static final TofuStmtRep TempStmtRep = new TofuStmtRep();

	private SootMethod Method;
	private Stmt Statement;
	private TofuCtxRep Ctx;

	private TofuStmtRep() {

	}

	public TofuStmtRep(SootMethod method, Stmt stmt, TofuCtxRep ctx) {
		Method = method;
		Statement = stmt;
		Ctx = ctx;
	}

	public static TofuStmtRep makeTempStmtRep(SootMethod method, Stmt stmt, TofuCtxRep ctx) {
		TempStmtRep.Method = method;
		TempStmtRep.Statement = stmt;
		TempStmtRep.Ctx = ctx;
		return TempStmtRep;
	}

	public SootMethod getMethod() {
		return Method;
	}

	public Stmt getLabel() {
		return Statement;
	}

	public TofuCtxRep getCtx() {
		return Ctx;
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TofuStmtRep)) {
			return false;
		}
		TofuStmtRep thatRep = (TofuStmtRep) obj;
		if (Method.equals(thatRep.Method) && Statement.equals(thatRep.Statement)) {
			if (Ctx == null && thatRep.Ctx == null) {
				return true;
			} else if (Ctx != null && Ctx.equals(thatRep.Ctx)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hc = Method.hashCode() + Statement.hashCode() * 13;
		if (Ctx != null) {
			hc += Ctx.hashCode() * 23;
		}
		return hc;
	}
}
