package edu.purdue.cs.toydroid.soot.util;


import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.scalar.Pair;
import soot.util.Chain;

import java.util.*;

public class TofuCGContext {
	private static int IndexCtx = 0;
	private static Map<TofuCtxRep, Integer> Ctx2Index = new HashMap<>();
	private static Map<Integer, TofuCtxRep> Index2Ctx = new TreeMap<>();
	private static Map<SootMethod, Set<TofuCtxRep>> Method2Ctx = new HashMap<>();

	public static Set<TofuCtxRep> getCtxForMethod(SootMethod method) {
		return Method2Ctx.get(method);
	}

	public static TofuCtxRep getCorrespondingCtxForCaller(CallGraph cg, TofuCtxRep ctx, SootMethod callee) {
		TofuCtxRep collectedCtx = null;
		TofuCtxRep tempCtx = null;
		if (ctx != null && ctx.size() > 1) {
			tempCtx = new TofuCtxRep();
			List<Pair> callstack = ctx.getCallStack();
			int n = callstack.size();
			for (int i = 0; i < n - 1; i++) {
				Pair<SootMethod, Stmt> pair = callstack.get(i);
				tempCtx.addCallsite(pair);
			}
			Integer idx = Ctx2Index.get(tempCtx);
			if (idx != null) {
				collectedCtx = Index2Ctx.get(idx);
			}
			tempCtx = null;
		}
		return collectedCtx;
	}

	public static Set<TofuCtxRep> getCorrespondingCtxForCallee(SootMethod caller, Stmt callsite, TofuCtxRep ctx,
															   SootMethod callee) {
		Set<TofuCtxRep> collectedCtx = new HashSet<>();
		TofuCtxRep tempCtx = null;
		if (ctx != null) {
			tempCtx = TofuCtxRep.concatToTempCtxRep(ctx, caller, callsite);
			Set<TofuCtxRep> ctx4Callee = Method2Ctx.get(callee);
			if (ctx4Callee != null) {
				for (TofuCtxRep calleeCtx : ctx4Callee) {
					if (calleeCtx.equals(tempCtx)) {
						collectedCtx.add(calleeCtx);
						break;
					}
				}
			}
		} else {
			Set<TofuCtxRep> ctx4Callee = Method2Ctx.get(callee);
			Set<TofuCtxRep> ctx4Caller = Method2Ctx.get(caller);
			if (ctx4Callee != null && ctx4Caller != null && ctx4Caller.size() < ctx4Callee.size()) {
				for (TofuCtxRep callerCtx : ctx4Caller) {
					tempCtx = TofuCtxRep.concatToTempCtxRep(callerCtx, caller, callsite);
					Integer idx = Ctx2Index.get(tempCtx);
					if (idx != null) {
						TofuCtxRep calleeCtx = Index2Ctx.get(idx);
						if (calleeCtx != null) {
							collectedCtx.add(calleeCtx);
						}
					}
				}
			}
		}
		if (collectedCtx.isEmpty()) {
			collectedCtx = null;
		}
		return collectedCtx;
	}

	public static void buildContextForCGNodes(CallGraph cg, SootMethod fakeEntry, Set<Stmt> startingLabels) {
		System.out.println("Begin Building Context for Analysis.");
		List<Pair> worklist = new LinkedList<>();
		startFromFakeEntryMethod(cg, fakeEntry, startingLabels, worklist);
		while (!worklist.isEmpty()) {
			Pair<SootMethod, TofuCtxRep> pair = worklist.remove(0);
			checkMethod(cg, pair.getO1(), pair.getO2(), worklist);
		}
//		Set<TofuCtxRep> xxset = new HashSet<>();
//		for (Set<TofuCtxRep> xs : Method2Ctx.values()) {
//			xxset.addAll(xs);
//		}
		System.out.println("End Building Context for " + Method2Ctx.size() + " Methods.");
	}

	private static void checkMethod(CallGraph cg, SootMethod method, TofuCtxRep ctx, List<Pair> worklist) {
		if (!method.hasActiveBody()) {
			return;
		}
		Body body = method.getActiveBody();
		Chain<Unit> chain = body.getUnits();
		for (Unit unit : chain) {
			Stmt stmt = (Stmt) unit;
			if (stmt.containsInvokeExpr()) {
				TofuCtxRep newCtx = null;
				Iterator<Edge> iterator = cg.edgesOutOf(stmt);
				while (iterator.hasNext()) {
					Edge edge = iterator.next();
					SootMethod tgt = edge.tgt();
					if (TofuDexClasses.isLibClass(tgt.getDeclaringClass().getName()) || ctx.containsCtx(method, stmt)) {
						continue;
					}
					Set<TofuCtxRep> ctxSet = Method2Ctx.get(tgt);
					if (ctxSet == null) {
						ctxSet = new HashSet<>();
						Method2Ctx.put(tgt, ctxSet);
					}
					if (newCtx == null) {
						TofuCtxRep tempCtx = TofuCtxRep.concatToTempCtxRep(ctx, method, stmt);
						if (ctxSet.contains(tempCtx)) {
							continue;
						}
						newCtx = ctx.concatToNewCtxRep(method, stmt);
					} else if (ctxSet.contains(newCtx)) {
						continue;
					}
					ctxSet.add(newCtx);
					indexCtx(newCtx);
					worklist.add(new Pair(tgt, newCtx));
				}
			}
		}
	}

	private static void startFromFakeEntryMethod(CallGraph cg, SootMethod fakeEntry, Set<Stmt> startingLabels,
												 List<Pair> worklist) {
		for (Stmt stmt : startingLabels) {
			Iterator<Edge> iterator = cg.edgesOutOf(stmt);
			while (iterator.hasNext()) {
				Edge edge = iterator.next();
				SootMethod tgt = edge.tgt();
				if (TofuDexClasses.isLibClass(tgt.getDeclaringClass().getName())) {
					continue;
				}
				Set<TofuCtxRep> ctxSet = Method2Ctx.get(tgt);
				if (ctxSet == null) {
					ctxSet = new HashSet<>();
					Method2Ctx.put(tgt, ctxSet);
				}
				TofuCtxRep ctx = new TofuCtxRep();
				ctx.addCallingContext(fakeEntry, stmt);
				ctxSet.add(ctx);
				indexCtx(ctx);
				worklist.add(new Pair(tgt, ctx));
			}
		}
	}

	private static void indexCtx(TofuCtxRep ctx) {
		int idx = IndexCtx++;
		Ctx2Index.put(ctx, idx);
		Index2Ctx.put(idx, ctx);
	}

	public static TofuTriple<SootMethod, Stmt, Stmt> getInfoOfFirstUnsameCtx(TofuCtxRep fst, TofuCtxRep snd) {
		List<Pair> fststack = fst.getCallStack();
		List<Pair> sndstack = snd.getCallStack();
		int nfst = fststack.size();
		int nsnd = sndstack.size();
		TofuTriple<SootMethod, Stmt, Stmt> triple = null;

		for (int idx = 0; idx < nfst && idx < nsnd; idx++) {
			Pair<SootMethod, Stmt> fstCtxE = (Pair<SootMethod, Stmt>) fststack.get(idx);
			Pair<SootMethod, Stmt> sndCtxE = (Pair<SootMethod, Stmt>) sndstack.get(idx);
			SootMethod fstM = fstCtxE.getO1();
			Stmt fstS = fstCtxE.getO2();
			SootMethod sndM = sndCtxE.getO1();
			Stmt sndS = sndCtxE.getO2();
			if (fstM.equals(sndM)) {
				if (!fstS.equals(sndS)) {
					triple = new TofuTriple<>(fstM, fstS, sndS);
					break;
				}
			} else {
				break;
			}
		}
		return triple;
	}
}
