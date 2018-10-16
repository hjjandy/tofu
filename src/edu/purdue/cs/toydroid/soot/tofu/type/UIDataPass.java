package edu.purdue.cs.toydroid.soot.tofu.type;


import edu.purdue.cs.toydroid.soot.util.*;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.scalar.Pair;
import soot.util.Chain;

import java.util.*;

public class UIDataPass {
	private CallGraph CG;
	private SootMethod FakeEntryMethod;
	private List<TofuStmtRep> Worklist;
	private Set<TofuStmtRep> TopMethodsAsDataGen;
	private Set<TofuStmtRep> DataGenLocations;

	public UIDataPass(CallGraph cg, SootMethod fakeEntry, Collection<TofuStmtRep> seeds) {
		CG = cg;
		FakeEntryMethod = fakeEntry;
		Worklist = new LinkedList<>(seeds);
		DataGenLocations = new HashSet<>();
		TopMethodsAsDataGen = new HashSet<>();
	}

	public Set<TofuStmtRep> getDataGenLocations() {
		return DataGenLocations;
	}

	public Set<TofuStmtRep> getTopMethodsAsDataGen() {
		return TopMethodsAsDataGen;
	}

	public void solve() {
		List<TofuStmtRep> worklist = Worklist;
		while (!worklist.isEmpty()) {
			TofuStmtRep tsr = worklist.remove(0);
			solve(tsr, worklist);
		}
	}

	private void solve(TofuStmtRep tsr, List<TofuStmtRep> worklist) {
		Stmt stmt = tsr.getLabel();
		System.out.println("[UIDataPass] Visit: " + stmt + " in method " + tsr.getMethod());
		if (stmt.containsInvokeExpr()) {
			SootMethod callee = stmt.getInvokeExpr().getMethod();
			String mName = callee.getName();
			if (TofuDexClasses.isLibClass(callee.getDeclaringClass().getName())) {
				if (!"inflate".equals(mName) && !"findViewById".equals(mName)) {
					UIData_Call_API(tsr, worklist);
				}
			} else if (!TofuUISigConstants.isTargetUISubSig(callee.getSubSignature())) {
				UIData_Call_Normal(tsr, worklist);
			}
		} else if (stmt instanceof IdentityStmt) {
			UIData_Call_Param(tsr, ((IdentityStmt) stmt).getRightOp(), worklist);
		} else if (stmt instanceof ReturnStmt) {
			ReturnStmt ret = (ReturnStmt) stmt;
			UIData_Return(tsr, ret.getOp(), worklist);
		} else if (stmt instanceof AssignStmt) {
			AssignStmt assign = (AssignStmt) stmt;
			Value right = assign.getRightOp();
			UIData_Assign(tsr, right, worklist);
		}
	}

	private void UIData_Call_Normal(TofuStmtRep stmtRep, List<TofuStmtRep> worklist) {
		//System.err.println("Enter Call_Normal. Size of Gamma2 = " + TypeContextTwo.sizeStmtInGamma());
		if (null == TypeContextTwo.updateStmt(stmtRep)) {
			return;
		}
		Stmt stmt = stmtRep.getLabel();
		List<ValueBox> stmtDefs = stmt.getDefBoxes();
		if (stmtDefs.isEmpty()) {
			return;
		}
		//System.err.println("Has Return Variable...");
		SootMethod method = stmtRep.getMethod();
		TofuCtxRep ctx = stmtRep.getCtx();
		TofuVarRep tempVar = null;
		// only process retvar --> return(X)
		Value ret = stmtDefs.get(0).getValue();
		tempVar = TofuVarRep.makeTempVarRep(ret, method, stmt, ctx);
		if (!TypeContextTwo.containsVar(tempVar)) {
			return;
		}
		//System.err.println("Return Variable " + ret + "is Typed...");
		Iterator<Edge> iterator = CG.edgesOutOf(stmt);
		while (iterator.hasNext()) {
			Edge edge = iterator.next();
			SootMethod callee = edge.tgt();
			//System.err.println(" --> Callee: " + callee);
			Set<TofuCtxRep> ctx4Callee = TofuCGContext.getCorrespondingCtxForCallee(method, stmt, ctx, callee);
			if (ctx4Callee == null) {
				propagateToRetInCallee(callee, null, worklist);
			} else {
				for (TofuCtxRep calleeCtx : ctx4Callee) {
					propagateToRetInCallee(callee, calleeCtx, worklist);
				}
			}
		}
	}

	private void propagateToRetInCallee(SootMethod callee, TofuCtxRep calleeCtx, List<TofuStmtRep> worklist) {
		TofuStmtRep tempStmt = null;
		List<Stmt> retStmts = returnOfMethod(callee);
		for (Stmt rs : retStmts) {
			//System.err.println("      [ret] " + rs);
			tempStmt = TofuStmtRep.makeTempStmtRep(callee, rs, calleeCtx);
			if (!TypeContextTwo.containsStmt(tempStmt)) {
				TofuStmtRep newStmt = new TofuStmtRep(callee, rs, calleeCtx);
				worklist.add(newStmt);
			}
		}
	}

	private List<Stmt> returnOfMethod(SootMethod method) {
		if (!method.hasActiveBody()) {
			return Collections.emptyList();
		}
		List<Stmt> list = new LinkedList<>();
		Chain<Unit> chain = method.getActiveBody().getUnits();
		for (Unit unit : chain) {
			Stmt stmt = (Stmt) unit;
			if (stmt instanceof ReturnStmt) {
				list.add(stmt);
			}
		}
		return list;
	}

	private void UIData_Call_Param(TofuStmtRep stmtRep, Value ref, List<TofuStmtRep> worklist) {
		if (null == TypeContextTwo.updateStmt(stmtRep)) {
			return;
		}
		SootMethod method = stmtRep.getMethod();
		Stmt stmt = stmtRep.getLabel();
		TofuCtxRep ctx = stmtRep.getCtx();
		TofuStmtRep tempStmt = null;
		TofuVarRep tempVar = TofuVarRep.makeTempVarRep(ref, method, null, ctx);
		if (TypeContextTwo.containsVar(tempVar)) {
			TypeContextTwo.updateVar(ref, method, null, ctx);
		}
		if (ctx != null && FakeEntryMethod.equals(ctx.topCtxElement().getO1())) {
			if (!ctx.topCtxElement().getO1().getName().equals("<init>")) {
				// cannot go furthur. treat as a data gen point for InputUIData analysis.
				// use current IdentifyStmt as data gen point.
//				Integer dgType = TypeContextThree.findOrCreateTypeOfDataGen(method, stmt);
//				if (!TypeContextThree.containsStmt(stmtRep, dgType)) {
//					TypeContextThree.updateStmt(stmtRep, dgType);
//				}
				if (ctx == null) {
					DataGenLocations.add(stmtRep);
				} else {
					DataGenLocations.add(new TofuStmtRep(method, stmt, null));//DG point should not be tagged with ctx
				}
				TopMethodsAsDataGen.add(stmtRep);
			}
		} else {
			TofuCtxRep callerCtx = TofuCGContext.getCorrespondingCtxForCaller(CG, ctx, method);
			if (callerCtx != null) {
				SootMethod caller = ctx.topCtxElement().getO1();
				Stmt callsite = ctx.topCtxElement().getO2();
				UIData_Call_Param_Callsite(caller, callsite, callerCtx, ref, worklist);
			} else {
				Iterator<Edge> iterator = CG.edgesInto(method);
				while (iterator.hasNext()) {
					Edge edge = iterator.next();
					SootMethod caller = edge.src();
					if (FakeEntryMethod.equals(caller)) {
						if (!caller.getName().equals("<init>")) {
//							Integer dgType = TypeContextThree.findOrCreateTypeOfDataGen(method, stmt);
//							if (!TypeContextThree.containsStmt(stmtRep, dgType)) {
//								TypeContextThree.updateStmt(stmtRep, dgType);
//							}
							if (ctx != null) {
								DataGenLocations.add(stmtRep);
							} else {
								DataGenLocations.add(new TofuStmtRep(method, stmt, null));
							}
							TopMethodsAsDataGen.add(stmtRep);
						}
					} else {
						Stmt callsite = edge.srcStmt();
						UIData_Call_Param_Callsite(caller, callsite, callerCtx, ref, worklist);
					}
				}
			}
		}
	}

	private void UIData_Call_Param_Callsite(SootMethod caller, Stmt callsite, TofuCtxRep callerCtx, Value param,
											List<TofuStmtRep> worklist) {
		TofuStmtRep tempStmt = null;
		TofuVarRep tempVar = null;
		InvokeExpr invoke = callsite.getInvokeExpr();
		Value correspondingArg = null;
		if (param instanceof ThisRef && invoke instanceof InstanceInvokeExpr) {
			correspondingArg = ((InstanceInvokeExpr) invoke).getBase();
		} else if (param instanceof ParameterRef) {
			correspondingArg = invoke.getArg(((ParameterRef) param).getIndex());
		}
		if (correspondingArg != null) {
			List<Stmt> argDefs = TofuDefUse.getDefs(caller, callsite, correspondingArg);
			for (Stmt adef : argDefs) {
				tempVar = TofuVarRep.makeTempVarRep(correspondingArg, caller, adef, callerCtx);
				if (!TypeContextTwo.containsVar(tempVar)) {
					TypeContextTwo.updateVar(correspondingArg, caller, adef, callerCtx);
				}
				tempStmt = TofuStmtRep.makeTempStmtRep(caller, adef, callerCtx);
				if (!TypeContextTwo.containsStmt(tempStmt)) {
					TofuStmtRep newStmt = new TofuStmtRep(caller, adef, callerCtx);
					worklist.add(newStmt);
				}
			}
		}
	}

	private void UIData_Call_API(TofuStmtRep stmtRep, List<TofuStmtRep> worklist) {
		if (null == TypeContextTwo.updateStmt(stmtRep)) {
			return;
		}
		SootMethod method = stmtRep.getMethod();
		TofuCtxRep ctx = stmtRep.getCtx();
		Stmt stmt = stmtRep.getLabel();
		TofuVarRep tempVar = null;
		TofuStmtRep tempStmt = null;
		// check if DataGen API
		List<ValueBox> stmtDefs = stmt.getDefBoxes();
		if (!stmtDefs.isEmpty()) {
			Value def = stmtDefs.get(0).getValue();
			tempVar = TofuVarRep.makeTempVarRep(def, method, stmt, ctx);
			if (TypeContextTwo.containsVar(tempVar) && TofuDataGenAPI.isDataGenAPI(method.getSignature())) {
//				Integer dgType = TypeContextThree.findOrCreateTypeOfDataGen(method, stmt);
//				if (!TypeContextThree.containsStmt(stmtRep, dgType)) {
//					// initialize Gamma3 with data gen points.
//					TypeContextThree.updateStmt(stmtRep, dgType);
//				}
				if (ctx == null) {
					DataGenLocations.add(stmtRep);
				} else {
					DataGenLocations.add(new TofuStmtRep(method, stmt, null));
				}
				return;//do not further propagate this/params.
			}
		}
		InvokeExpr invoke = stmt.getInvokeExpr();
		boolean bwdPropAllArgs = true;
		if (invoke instanceof InstanceInvokeExpr) {
			Value base = ((InstanceInvokeExpr) invoke).getBase();
			String mClass = method.getDeclaringClass().getName();
			String mName = method.getName();
			if (mClass.startsWith("java.util.") && (mName.startsWith("get") || mName.startsWith("remove") ||
													mName.startsWith("pop") || mName.startsWith("peek"))) {
				Set<Stmt> allUses = new HashSet<>();
				List<Stmt> useDefs = TofuDefUse.getDefs(method, stmt, base);
				for (Stmt udef : useDefs) {
					tempVar = TofuVarRep.makeTempVarRep(base, method, udef, ctx);
					if (!TypeContextTwo.containsVar(tempVar)) {
						TypeContextTwo.updateVar(base, method, udef, ctx);
					}
					List<Stmt> udefUses = TofuDefUse.getUses(method, udef, base);
					for (Stmt uu : udefUses) {
						if (uu.containsInvokeExpr() && uu.getInvokeExpr() instanceof InstanceInvokeExpr) {
							InvokeExpr uInvoke = uu.getInvokeExpr();
							Local uBase = (Local) ((InstanceInvokeExpr) uInvoke).getBase();
							if (!uBase.getName().equals(((Local) base).getName())) {
								continue;
							}
							String uClass = uInvoke.getMethod().getDeclaringClass().getName();
							String uName = uInvoke.getMethod().getName();
							if (uClass.startsWith("java.util.") &&
								(uName.startsWith("set") || uName.startsWith("put") || uName.startsWith("add") ||
								 uName.startsWith("insert") ||
								 uName.startsWith("push"))) {
								allUses.add(uu);
							}
						}
					}
				}
				for (Stmt ustmt : allUses) {
					tempStmt = TofuStmtRep.makeTempStmtRep(method, ustmt, ctx);
					if (!TypeContextTwo.containsStmt(tempStmt)) {
						TofuStmtRep newStmt = new TofuStmtRep(method, ustmt, ctx);
						worklist.add(newStmt);
					}
				}
				bwdPropAllArgs = false;//not prop args for collection GET method??
			}
		}
		if (bwdPropAllArgs) {
			List<Value> args = invoke.getArgs();
			for (Value arg : args) {
				if (arg instanceof Constant) {
					continue;
				}
				List<Stmt> argDefs = TofuDefUse.getDefs(method, stmt, arg);
				for (Stmt adef : argDefs) {
					tempVar = TofuVarRep.makeTempVarRep(arg, method, adef, ctx);
					if (!TypeContextTwo.containsVar(tempVar)) {
						TypeContextTwo.updateVar(arg, method, adef, ctx);
					}
					tempStmt = TofuStmtRep.makeTempStmtRep(method, adef, ctx);
					if (!TypeContextTwo.containsStmt(tempStmt)) {
						TofuStmtRep newStmt = new TofuStmtRep(method, adef, ctx);
						worklist.add(newStmt);
					}
				}
			}
		}

	}

	private void UIData_Assign(TofuStmtRep stmtRep, Value right, List<TofuStmtRep> worklist) {
		if (null == TypeContextTwo.updateStmt(stmtRep)) {
			return;
		}
		SootMethod method = stmtRep.getMethod();
		Stmt stmt = stmtRep.getLabel();
		TofuCtxRep ctx = stmtRep.getCtx();
		TofuStmtRep tempStmt = null;
		TofuVarRep tempVar = null;
		if (right instanceof FieldRef) {
			UIData_AssignFromField(stmtRep, (FieldRef) right, worklist);
		} else if (right instanceof ArrayRef) {
			// skip
		} else if (right instanceof NewExpr || right instanceof Constant) {
			// skip
		} else {
			List<ValueBox> useBoxes = right.getUseBoxes();
			for (ValueBox useb : useBoxes) {
				Value vuse = useb.getValue();
				if (vuse instanceof Constant) {
					continue;
				}
				List<Stmt> useDefs = TofuDefUse.getDefs(method, stmt, vuse);
				for (Stmt udef : useDefs) {
					tempVar = TofuVarRep.makeTempVarRep(vuse, method, udef, ctx);
					if (!TypeContextTwo.containsVar(tempVar)) {
						TypeContextTwo.updateVar(vuse, method, udef, ctx);
					}
					tempStmt = TofuStmtRep.makeTempStmtRep(method, udef, ctx);
					if (!TypeContextTwo.containsStmt(tempStmt)) {
						TofuStmtRep newStmt = new TofuStmtRep(method, udef, ctx);
						worklist.add(newStmt);
					}
				}
			}
		}
	}

	private void UIData_AssignFromField(TofuStmtRep stmtRep, FieldRef field, List<TofuStmtRep> worklist) {
		if (null == TypeContextOne.updateStmt(stmtRep)) {
			return;
		}
		SootMethod method = stmtRep.getMethod();
		Stmt stmt = stmtRep.getLabel();
		TofuCtxRep ctx = stmtRep.getCtx();
		String fSig = field.getField().getSignature();
		Set<Pair<SootMethod, Stmt>> fieldsWrites = TofuFieldCollector.getLocationsOfFieldWrite(fSig);
		if (fieldsWrites != null && !fieldsWrites.isEmpty()) {
			Set<Pair<SootMethod, Stmt>> tempFW = new HashSet<>();
			for (Pair<SootMethod, Stmt> fw : fieldsWrites) {
				SootMethod fwMethod = fw.getO1();
				Stmt fwStmt = fw.getO2();
				if (method.equals(fwMethod)) {
					if (TofuDefUse.happensBefore(method, fwStmt, stmt)) {
						TofuStmtRep tempStmt = TofuStmtRep.makeTempStmtRep(fwMethod, fwStmt, ctx);
						if (!TypeContextOne.containsStmt(tempStmt)) {
							TofuStmtRep newStmt = new TofuStmtRep(fwMethod, fwStmt, ctx);
							worklist.add(newStmt);
						}
					}
				} else {
					tempFW.add(fw);
				}
			}
			if (ctx == null) {
				Set<TofuCtxRep> mCtx = TofuCGContext.getCtxForMethod(method);
				if (mCtx != null && !mCtx.isEmpty()) {
					Set<Pair<SootMethod, Stmt>> hitted = new HashSet<>();
					for (TofuCtxRep mc : mCtx) {
						for (Pair<SootMethod, Stmt> fw : tempFW) {
							SootMethod fwMethod = fw.getO1();
							Stmt fwStmt = fw.getO2();
							Set<TofuCtxRep> fwmCtx = TofuCGContext.getCtxForMethod(fwMethod);
							if (fwmCtx != null && !fwmCtx.isEmpty()) {
								for (TofuCtxRep fwmc : fwmCtx) {
									TofuTriple<SootMethod, Stmt, Stmt> ctxDispatch = TofuCGContext
											.getInfoOfFirstUnsameCtx(mc, fwmc);
									SootMethod dMethod = ctxDispatch.get01();
									Stmt dMcStmt = ctxDispatch.get02();
									Stmt dFwmcStmt = ctxDispatch.get03();
									if (FakeEntryMethod.equals(dMethod) ||
										TofuDefUse.happensBefore(dMethod, dFwmcStmt, dMcStmt)) {
										TofuStmtRep tempStmt = TofuStmtRep.makeTempStmtRep(fwMethod, fwStmt, fwmc);
										if (!TypeContextOne.containsStmt(tempStmt)) {
											TofuStmtRep newStmt = new TofuStmtRep(fwMethod, fwStmt, fwmc);
											worklist.add(newStmt);
										}
										hitted.add(fw);
									}
								}
							}
						}
						tempFW.removeAll(hitted);
						hitted.clear();
						if (tempFW.isEmpty()) {
							break;
						}
					}
				} else {
					// analysis is not ctx-sensitive. then all field reads match to all field writes.
					for (Pair<SootMethod, Stmt> fw : tempFW) {
						SootMethod fwMethod = fw.getO1();
						Stmt fwStmt = fw.getO2();
						TofuStmtRep tempStmt = TofuStmtRep.makeTempStmtRep(fwMethod, fwStmt, null);
						if (!TypeContextOne.containsStmt(tempStmt)) {
							TofuStmtRep newStmt = new TofuStmtRep(fwMethod, fwStmt, null);
							worklist.add(newStmt);
						}
					}
				}
			} else {
				for (Pair<SootMethod, Stmt> fw : tempFW) {
					SootMethod fwMethod = fw.getO1();
					Stmt fwStmt = fw.getO2();
					Set<TofuCtxRep> fwmCtx = TofuCGContext.getCtxForMethod(fwMethod);
					if (fwmCtx != null && !fwmCtx.isEmpty()) {
						for (TofuCtxRep fwmc : fwmCtx) {
							TofuTriple<SootMethod, Stmt, Stmt> ctxDispatch = TofuCGContext
									.getInfoOfFirstUnsameCtx(ctx, fwmc);
							SootMethod dMethod = ctxDispatch.get01();
							Stmt dMcStmt = ctxDispatch.get02();
							Stmt dFwmcStmt = ctxDispatch.get03();
							if (FakeEntryMethod.equals(dMethod) ||
								TofuDefUse.happensBefore(dMethod, dFwmcStmt, dMcStmt)) {
								TofuStmtRep tempStmt = TofuStmtRep.makeTempStmtRep(fwMethod, fwStmt, fwmc);
								if (!TypeContextOne.containsStmt(tempStmt)) {
									TofuStmtRep newStmt = new TofuStmtRep(fwMethod, fwStmt, fwmc);
									worklist.add(newStmt);
								}
							}
						}
					}
				}
			}
		}
	}

	private void UIData_Return(TofuStmtRep stmtRep, Value ret, List<TofuStmtRep> worklist) {
		if (null == TypeContextTwo.updateStmt(stmtRep)) {
			return;
		}
		SootMethod method = stmtRep.getMethod();
		Stmt stmt = stmtRep.getLabel();
		TofuCtxRep ctx = stmtRep.getCtx();
		TofuVarRep tempVar = null;
		TofuStmtRep tempStmt = null;
		List<Stmt> retDefs = TofuDefUse.getDefs(method, stmt, ret);
		for (Stmt rdef : retDefs) {
			tempVar = TofuVarRep.makeTempVarRep(ret, method, rdef, ctx);
			if (!TypeContextTwo.containsVar(tempVar)) {
				TypeContextTwo.updateVar(ret, method, rdef, ctx);
			}
			tempStmt = TofuStmtRep.makeTempStmtRep(method, rdef, ctx);
			if (!TypeContextTwo.containsStmt(tempStmt)) {
				TofuStmtRep newStmt = new TofuStmtRep(method, rdef, ctx);
				worklist.add(newStmt);
			}
		}
	}
}
