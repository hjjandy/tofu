package edu.purdue.cs.toydroid.soot.tofu.type;

import edu.purdue.cs.toydroid.soot.util.*;
import soot.SootMethod;
import soot.Value;
import soot.ValueBox;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.scalar.Pair;

import java.util.*;

public class UIRelatedPass {
	private CallGraph CG;
	private SootMethod FakeEntryMethod;
	private List<TofuStmtRep> Worklist;
	private Set<SootMethod> TopMethodsOfUIReturn;
	private Set<TofuStmtRep> DataPutLocations;
	private boolean IsBannerOrClick = false;

	public UIRelatedPass(CallGraph cg, SootMethod fakeEntry, Collection<TofuStmtRep> seeds, boolean bannerOrClick) {
		CG = cg;
		FakeEntryMethod = fakeEntry;
		Worklist = new LinkedList<>(seeds);
		TopMethodsOfUIReturn = new HashSet<>();
		DataPutLocations = new HashSet<>();
		IsBannerOrClick = bannerOrClick;
	}

	public Set<SootMethod> getTopMethodsOfUIReturn() {
		return TopMethodsOfUIReturn;
	}

	public Set<TofuStmtRep> getDataPutLocations() {
		return DataPutLocations;
	}

	public void solve() {
		List<TofuStmtRep> worklist = Worklist;
		while (!worklist.isEmpty()) {
			TofuStmtRep tsr = worklist.remove(0);
			solve(tsr, worklist);
		}
		System.err.println("Size[var] = " + TypeContextOne.sizeVarInGamma() + ";  Size[stmt] = " +
						   TypeContextOne.sizeStmtInGamma());
	}

	private void solve(TofuStmtRep tsr, List<TofuStmtRep> worklist) {
		Stmt stmt = tsr.getLabel();
		System.out.println("[UIPass] Visit: " + stmt + " in method " + tsr.getMethod());
		if (stmt.containsInvokeExpr()) {
			SootMethod callee = stmt.getInvokeExpr().getMethod();
			String mName = callee.getName();
			if (TofuDexClasses.isLibClass(callee.getDeclaringClass().getName())) {
				if ("inflate".equals(mName) || "findViewById".equals(mName)) {
					UI_Intro(tsr, worklist);
				} else {
					UI_Call_API(tsr, worklist);
				}
			} else if (TofuUISigConstants.isTargetUISubSig(callee.getSubSignature())) {
				// special cases.
				UI_Intro(tsr, worklist);
			} else {
				UI_Call_Normal(tsr, worklist);
			}
		} else if (stmt instanceof IdentityStmt) {
			UI_Assign(tsr, ((IdentityStmt) stmt).getLeftOp(), worklist);
		} else if (stmt instanceof ReturnStmt) {
			ReturnStmt ret = (ReturnStmt) stmt;
			UI_Return(tsr, ret.getOp(), worklist);
		} else if (stmt instanceof AssignStmt) {
			AssignStmt assign = (AssignStmt) stmt;
			Value left = assign.getLeftOp();
			if (left instanceof FieldRef) {//field write
				UI_AssignToField(tsr, (FieldRef) left, worklist);
			} else if (left instanceof ArrayRef) {
				// skip
			} else {
				UI_Assign(tsr, left, worklist);
			}
		}
	}

	private void UI_Intro(TofuStmtRep stmtRep, List<TofuStmtRep> worklist) {
		SootMethod method = stmtRep.getMethod();
		Stmt stmt = stmtRep.getLabel();
		TofuCtxRep ctx = stmtRep.getCtx();
		Value vdef = stmt.getDefBoxes().get(0).getValue();
		TypeContextOne.updateStmt(stmtRep);
		TofuVarRep tempVar = TofuVarRep.makeTempVarRep(vdef, method, stmt, ctx);
		if (!TypeContextOne.containsVar(tempVar)) {
			TypeContextOne.updateVar(vdef, method, stmt, ctx);
			propagate(method, stmt, vdef, ctx, worklist);
		}
	}

	private void UI_Call_API(TofuStmtRep stmtRep, List<TofuStmtRep> worklist) {
		if (null == TypeContextOne.updateStmt(stmtRep)) {
			return;
		}
		TofuVarRep tempVar = null;
		SootMethod method = stmtRep.getMethod();
		Stmt stmt = stmtRep.getLabel();
		TofuCtxRep ctx = stmtRep.getCtx();
		InvokeExpr invoke = stmt.getInvokeExpr();
		boolean dataPutAPIProcessed = false;
		if (invoke instanceof InstanceInvokeExpr) {
			boolean baseNewlyTyped = true;
			Value base = ((InstanceInvokeExpr) invoke).getBase();
			List<Stmt> useDefs = TofuDefUse.getDefs(method, stmt, base);
			for (Stmt udef : useDefs) {
				tempVar = TofuVarRep.makeTempVarRep(base, method, udef, ctx);
				if (!TypeContextOne.containsVar(tempVar)) {
					// some other args must have been typed. only type 'base' for Collections
					String mClass = method.getDeclaringClass().getName();
					String mName = method.getName();
					if (mClass.startsWith("java.util.") &&
						(mName.startsWith("set") || mName.startsWith("add") || mName.startsWith("put") ||
						 mName.startsWith("insert") ||
						 mName.startsWith("push"))) {
						TypeContextOne.updateVar(base, method, udef, ctx);// type 'this'
						if (baseNewlyTyped) {
							propagate(method, stmt, base, ctx, worklist);
						}
						baseNewlyTyped = false;
					}
				}
				if (!dataPutAPIProcessed && TypeContextOne.containsVar(tempVar) &&
					TofuUISigConstants.isUIDataPutMethod(invoke.getMethod().getName())) {
					DataPutLocations.add(stmtRep);
					dataPutAPIProcessed = true;
				}
			}
		}
		// type definition: for API call, any arg/base/stmt being typed indicates typing the def
		List<ValueBox> defBoxes = stmt.getDefBoxes();
		if (!defBoxes.isEmpty()) {
			Value def = defBoxes.get(0).getValue();
			tempVar = TofuVarRep.makeTempVarRep(def, method, stmt, ctx);
			if (!TypeContextOne.containsVar(tempVar)) {
				TypeContextOne.updateVar(def, method, stmt, ctx);
			}
			propagate(method, stmt, def, ctx, worklist);
		}
	}

	private void UI_Call_Normal(TofuStmtRep stmtRep, List<TofuStmtRep> worklist) {
		TofuVarRep tempVar = null;
		SootMethod method = stmtRep.getMethod();
		Stmt stmt = stmtRep.getLabel();
		TofuCtxRep ctx = stmtRep.getCtx();
		InvokeExpr invoke = stmt.getInvokeExpr();
		Set<Pair<SootMethod, IdentityStmt>> paramsInCallee = new HashSet<>();
		if (invoke instanceof InstanceInvokeExpr) {
			Value base = ((InstanceInvokeExpr) invoke).getBase();
			List<Stmt> useDefs = TofuDefUse.getDefs(method, stmt, base);
			for (Stmt udef : useDefs) {
				tempVar = TofuVarRep.makeTempVarRep(base, method, udef, ctx);
				if (TypeContextOne.containsVar(tempVar) && !TypeContextOne.containsStmt(stmtRep)) {
					// type stmt
					if (null != TypeContextOne.updateStmt(stmtRep)) {
						// type definition
						List<ValueBox> stmtDefs = stmt.getDefBoxes();
						if (!stmtDefs.isEmpty()) {
							Value vdef = stmtDefs.get(0).getValue();
							tempVar = TofuVarRep.makeTempVarRep(vdef, method, stmt, ctx);
							if (!TypeContextOne.containsVar(tempVar)) {
								TypeContextOne.updateVar(vdef, method, stmt, ctx);
							}
							propagate(method, stmt, vdef, ctx, worklist);
						}
					}
					// collect 'this' in callee
					if (!IsBannerOrClick) {
						List<Pair<SootMethod, IdentityStmt>> thisInCallee = TofuDefUse.getParam(CG, stmt, 0, true);
						paramsInCallee.addAll(thisInCallee);
					}
				}
			}
		}
		int nArgs = invoke.getArgCount();
		for (int i = 0; i < nArgs; i++) {
			Value arg = invoke.getArg(i);
			if (arg instanceof Constant) {
				continue;
			}
			List<Stmt> argDefs = TofuDefUse.getDefs(method, stmt, arg);
			for (Stmt udef : argDefs) {
				tempVar = TofuVarRep.makeTempVarRep(arg, method, udef, ctx);
				if (TypeContextOne.containsVar(tempVar)) {// arg is typed
					if (IsBannerOrClick) {
						// type stmt
						if (null != TypeContextOne.updateStmt(stmtRep)) {
							// type definition
							List<ValueBox> stmtDefs = stmt.getDefBoxes();
							if (!stmtDefs.isEmpty()) {
								Value vdef = stmtDefs.get(0).getValue();
								tempVar = TofuVarRep.makeTempVarRep(vdef, method, stmt, ctx);
								if (!TypeContextOne.containsVar(tempVar)) {
									TypeContextOne.updateVar(vdef, method, stmt, ctx);
								}
								propagate(method, stmt, vdef, ctx, worklist);
							}
						}
					} //else {
						List<Pair<SootMethod, IdentityStmt>> pInCallee = TofuDefUse.getParam(CG, stmt, i, false);
						paramsInCallee.addAll(pInCallee);
					//}
				}
			}
		}
		// type param
		//if (!IsBannerOrClick) {
			for (Pair<SootMethod, IdentityStmt> pair : paramsInCallee) {
				SootMethod callee = pair.getO1();
				IdentityStmt idstmt = pair.getO2();
				Value pval = idstmt.getRightOp();
				Set<TofuCtxRep> ctx4Calee = TofuCGContext.getCorrespondingCtxForCallee(method, stmt, ctx, callee);
				TofuCtxRep calleeCtx = null;
				if (ctx4Calee == null || ctx4Calee.isEmpty()) {
					tempVar = TofuVarRep.makeTempVarRep(pval, callee, null, calleeCtx);
					if (!TypeContextOne.containsVar(tempVar)) {
						TypeContextOne.updateVar(pval, callee, null, calleeCtx);
						TofuStmtRep newStmt = new TofuStmtRep(callee, idstmt, calleeCtx);
						worklist.add(newStmt);
					}
				} else {
					for (TofuCtxRep cctx : ctx4Calee) {
						tempVar = TofuVarRep.makeTempVarRep(pval, callee, null, cctx);
						if (!TypeContextOne.containsVar(tempVar)) {
							TypeContextOne.updateVar(pval, callee, null, cctx);
							TofuStmtRep newStmt = new TofuStmtRep(callee, idstmt, cctx);
							worklist.add(newStmt);
						}
					}
				}
			}
		//}
		paramsInCallee = null;
	}

	private void UI_Assign(TofuStmtRep stmtRep, Value left, List<TofuStmtRep> worklist) {
		if (null != TypeContextOne.updateStmt(stmtRep)) {
			SootMethod method = stmtRep.getMethod();
			Stmt stmt = stmtRep.getLabel();
			TofuCtxRep ctx = stmtRep.getCtx();
			TofuVarRep tempVar = TofuVarRep.makeTempVarRep(left, method, stmt, ctx);
			if (!TypeContextOne.containsVar(tempVar)) {
				TypeContextOne.updateVar(left, method, stmt, ctx);
			}
			// anyway, propagate to all uses of 'left' in case some uses in certain stmts have been typed due to
			// the other variables but some uses have not been typed.
			propagate(method, stmt, left, ctx, worklist);
		}
	}

	private void UI_AssignToField(TofuStmtRep stmtRep, FieldRef field, List<TofuStmtRep> worklist) {
		if (null == TypeContextOne.updateStmt(stmtRep)) {
			return;
		}
		SootMethod method = stmtRep.getMethod();
		Stmt stmt = stmtRep.getLabel();
		TofuCtxRep ctx = stmtRep.getCtx();
		String fSig = field.getField().getSignature();
		Set<Pair<SootMethod, Stmt>> fieldReads = TofuFieldCollector.getLocationsOfFieldRead(fSig);
		if (fieldReads != null && !fieldReads.isEmpty()) {
			Set<Pair<SootMethod, Stmt>> tempFR = new HashSet<>();
			for (Pair<SootMethod, Stmt> fr : fieldReads) {
				SootMethod frMethod = fr.getO1();
				Stmt frStmt = fr.getO2();
//					System.out.println("Found Field Read: " + frStmt + " in method " + frMethod);
				if (method.equals(frMethod)) {
					if (TofuDefUse.happensBefore(method, stmt, frStmt)) {
						TofuStmtRep tempStmt = TofuStmtRep.makeTempStmtRep(frMethod, frStmt, ctx);
						if (!TypeContextOne.containsStmt(tempStmt)) {
							TofuStmtRep newStmt = new TofuStmtRep(frMethod, frStmt, ctx);
							worklist.add(newStmt);
//								System.out.println("  Wait to analyze: " + frStmt + " in method " + frMethod);
						}
					}
				} else {
					tempFR.add(fr);
				}
			}
			if (ctx == null) {
				Set<TofuCtxRep> mCtx = TofuCGContext.getCtxForMethod(method);
				if (mCtx != null && !mCtx.isEmpty()) {
					Set<Pair<SootMethod, Stmt>> hitted = new HashSet<>();
					for (TofuCtxRep mc : mCtx) {
						for (Pair<SootMethod, Stmt> fr : tempFR) {
							SootMethod frMethod = fr.getO1();
							Stmt frStmt = fr.getO2();
							Set<TofuCtxRep> frmCtx = TofuCGContext.getCtxForMethod(frMethod);
							if (frmCtx != null && !frmCtx.isEmpty()) {
								for (TofuCtxRep frmc : frmCtx) {
									TofuTriple<SootMethod, Stmt, Stmt> ctxDispatch = TofuCGContext
											.getInfoOfFirstUnsameCtx(mc, frmc);
									SootMethod dMethod = ctxDispatch.get01();
									Stmt dMcStmt = ctxDispatch.get02();
									Stmt dFrmcStmt = ctxDispatch.get03();
									if (FakeEntryMethod.equals(dMethod) ||
										TofuDefUse.happensBefore(dMethod, dMcStmt, dFrmcStmt)) {
										TofuStmtRep tempStmt = TofuStmtRep.makeTempStmtRep(frMethod, frStmt, frmc);
										if (!TypeContextOne.containsStmt(tempStmt)) {
											TofuStmtRep newStmt = new TofuStmtRep(frMethod, frStmt, frmc);
											worklist.add(newStmt);
										}
										hitted.add(fr);
									}
								}
							}
						}
						tempFR.removeAll(hitted);
						hitted.clear();
						if (tempFR.isEmpty()) {
							break;
						}
					}
				} else {
					// analysis is not ctx-sensitive. then all field reads match to all field writes.
					for (Pair<SootMethod, Stmt> fr : tempFR) {
						SootMethod frMethod = fr.getO1();
						Stmt frStmt = fr.getO2();
						TofuStmtRep tempStmt = TofuStmtRep.makeTempStmtRep(frMethod, frStmt, null);
						if (!TypeContextOne.containsStmt(tempStmt)) {
							TofuStmtRep newStmt = new TofuStmtRep(frMethod, frStmt, null);
							worklist.add(newStmt);
						}
					}
				}
			} else {
				for (Pair<SootMethod, Stmt> fr : tempFR) {
					SootMethod frMethod = fr.getO1();
					Stmt frStmt = fr.getO2();
					Set<TofuCtxRep> frmCtx = TofuCGContext.getCtxForMethod(frMethod);
					if (frmCtx != null && !frmCtx.isEmpty()) {
						for (TofuCtxRep frmc : frmCtx) {
							TofuTriple<SootMethod, Stmt, Stmt> ctxDispatch = TofuCGContext
									.getInfoOfFirstUnsameCtx(ctx, frmc);
							SootMethod dMethod = ctxDispatch.get01();
							Stmt dMcStmt = ctxDispatch.get02();
							Stmt dFrmcStmt = ctxDispatch.get03();
							if (FakeEntryMethod.equals(dMethod) ||
								TofuDefUse.happensBefore(dMethod, dMcStmt, dFrmcStmt)) {
								TofuStmtRep tempStmt = TofuStmtRep.makeTempStmtRep(frMethod, frStmt, frmc);
								if (!TypeContextOne.containsStmt(tempStmt)) {
									TofuStmtRep newStmt = new TofuStmtRep(frMethod, frStmt, frmc);
									worklist.add(newStmt);
								}
							}
						}
					}
				}
			}
		}
	}

	private void UI_Return(TofuStmtRep stmtRep, Value ret, List<TofuStmtRep> worklist) {
		if (null == TypeContextOne.updateStmt(stmtRep)) {
			return;
		}
		SootMethod method = stmtRep.getMethod();
		Stmt stmt = stmtRep.getLabel();
		TofuCtxRep ctx = stmtRep.getCtx();
		TofuVarRep tempVar = null;
		if (ctx != null) {
			Pair<SootMethod, Stmt> callingCtx = ctx.topCtxElement();
			SootMethod caller = callingCtx.getO1();
			Stmt callsite = callingCtx.getO2();
			UI_ReturnToCaller(method, ctx, caller, callsite, worklist);
		} else {
			Iterator<Edge> iterator = CG.edgesInto(method);
			while (iterator.hasNext()) {
				Edge edge = iterator.next();
				SootMethod caller = edge.src();
				Stmt callsite = edge.srcStmt();
				UI_ReturnToCaller(method, ctx, caller, callsite, worklist);
			}
		}
	}

	private void UI_ReturnToCaller(SootMethod method, TofuCtxRep ctx, SootMethod caller, Stmt callsite,
								   List<TofuStmtRep> worklist) {
		if (FakeEntryMethod.equals(caller)) {
			if (!caller.getName().equals("<init>")) {
				TopMethodsOfUIReturn.add(method);
			}
		} else {
			TofuCtxRep callerCtx = TofuCGContext.getCorrespondingCtxForCaller(CG, ctx, method);
			TofuStmtRep tempStmt = TofuStmtRep.makeTempStmtRep(caller, callsite, callerCtx);
			if (!TypeContextOne.containsStmt(tempStmt)) {
				TypeContextOne.updateStmt(caller, callsite, callerCtx);
				List<ValueBox> callsiteDefs = callsite.getDefBoxes();
				if (!callsiteDefs.isEmpty()) {
					Value vdef = callsiteDefs.get(0).getValue();
					TofuVarRep tempVar = TofuVarRep.makeTempVarRep(vdef, caller, callsite, callerCtx);
					if (!TypeContextOne.containsVar(tempVar)) {
						TypeContextOne.updateVar(vdef, caller, callsite, callerCtx);
						propagate(caller, callsite, vdef, callerCtx, worklist);
					}
				}
			}
		}
	}

	private void propagate(SootMethod method, Stmt stmt, Value value, TofuCtxRep ctx, List<TofuStmtRep> worklist) {
		List<Stmt> varUses = TofuDefUse.getUses(method, stmt, value);
		for (Stmt use : varUses) {
			TofuStmtRep tempStmt = TofuStmtRep.makeTempStmtRep(method, use, ctx);
			if (!TypeContextOne.containsStmt(tempStmt)) {
				TofuStmtRep newStmt = new TofuStmtRep(method, use, ctx);
				worklist.add(newStmt);
			}
		}
	}
}
