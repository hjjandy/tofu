package edu.purdue.cs.toydroid.soot.util;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

import java.util.LinkedList;
import java.util.List;

public class TofuCtxRep {
	private static final Pair<SootMethod, Stmt> TempContext = new Pair<>();
	private static final TofuCtxRep TempCtxRep = new TofuCtxRep();
	private LinkedList<Pair> CallStack;

	public TofuCtxRep() {
		CallStack = new LinkedList<>();
	}

	public static Pair<SootMethod, Stmt> makeTempCtxElement(SootMethod method, Stmt label) {
		TempContext.setPair(method, label);
		return TempContext;
	}

	public static TofuCtxRep concatToTempCtxRep(TofuCtxRep ctx, SootMethod method, Stmt label) {
		TempCtxRep.CallStack.addAll(ctx.CallStack);
		Pair pair = makeTempCtxElement(method, label);
		TempCtxRep.CallStack.add(pair);
		return TempCtxRep;
	}

	public TofuCtxRep concatToNewCtxRep(SootMethod method, Stmt label) {
		TofuCtxRep ctx = new TofuCtxRep();
		ctx.CallStack.addAll(CallStack);
		ctx.addCallingContext(method, label);
		return ctx;
	}

	public void addCallsite(Pair<SootMethod, Stmt> pair) {
		CallStack.add(pair);
	}

	public boolean containsCtx(SootMethod method, Stmt label) {
		Pair<SootMethod, Stmt> pair = makeTempCtxElement(method, label);
		return CallStack.contains(pair);
	}

	public void addCallingContext(SootMethod method, Stmt label) {
		Pair<SootMethod, Stmt> pair = new Pair<>(method, label);
		CallStack.add(pair);
	}

	public List<Pair> getCallStack() {
		return CallStack;
	}

	public int size() {
		return CallStack.size();
	}

	public Pair<SootMethod, Stmt> topCtxElement() {
		if (CallStack.isEmpty()) {
			return null;
		}
		return CallStack.getLast();
	}

	@Override
	public int hashCode() {
		int hc = 11;
		for (Pair<SootMethod, Stmt> pair : CallStack) {
			hc += pair.hashCode();
		}
		return hc;
	}

	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TofuCtxRep)) {
			return false;
		}
		TofuCtxRep thatRep = (TofuCtxRep) obj;
		if (CallStack.size() != thatRep.CallStack.size()) {
			return false;
		}
		int n = CallStack.size();
		for (int i = 0; i < n; i++) {
			Pair thisC = CallStack.get(i);
			Pair thatC = thatRep.CallStack.get(i);
			if (!thisC.equals(thatC)) {
				return false;
			}
		}
		return true;
	}
}
