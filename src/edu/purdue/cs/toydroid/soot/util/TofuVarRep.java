package edu.purdue.cs.toydroid.soot.util;


import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;

public class TofuVarRep {
	private static final TofuVarRep TempVarRep = new TofuVarRep();
	private Value Var;
	private SootMethod Method;
	private Stmt Def;
	private TofuCtxRep Ctx;

	private TofuVarRep() {

	}

	public TofuVarRep(Value var, SootMethod method, Stmt def, TofuCtxRep ctx) {
		Var = var;
		Method = method;
		Def = def;
		Ctx = ctx;
	}

	public static TofuVarRep makeTempVarRep(Value var, SootMethod method, Stmt def, TofuCtxRep ctx) {
		TempVarRep.Var = var;
		TempVarRep.Method = method;
		TempVarRep.Def = def;
		TempVarRep.Ctx = ctx;
		return TempVarRep;
	}

	public Value getVar() {
		return Var;
	}

	public SootMethod getMethod() {
		return Method;
	}

	public Stmt getDef() {
		return Def;
	}

	public TofuCtxRep getCtx() {
		return Ctx;
	}

	@Override
	public int hashCode() {
		int hc = Var.hashCode() + Method.hashCode() * 13;
		if (Def != null) {
			hc += Def.hashCode() * 23;
		}
		if (Ctx != null) {
			hc += Ctx.hashCode() * 31;
		}
		return hc;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof TofuVarRep)) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		TofuVarRep thatRep = (TofuVarRep) obj;
		if (Var.equals(thatRep.Var) && Method.equals(thatRep.Method)) {
			if ((Def == null && thatRep.Def == null) || (Def != null && Def.equals(thatRep.Def))) {
				if (Ctx == null && thatRep.Ctx == null) {
					return true;
				} else if (Ctx != null && Ctx.equals(thatRep.Ctx)) {
					return true;
				}
			}
		}
		return false;
	}
}
