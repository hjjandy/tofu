package edu.purdue.cs.toydroid.soot.tofu.type;


import edu.purdue.cs.toydroid.soot.util.*;
import soot.Local;
import soot.SootMethod;
import soot.Value;
import soot.ValueBox;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.scalar.Pair;

import java.util.*;

public class InputUIDataPass {
	private CallGraph CG;
	private SootMethod FakeEntryMethod;
	// different worklist. TofuStmtRep,Set<Integer>,TofuStmtRep,Set<Integer>,...
	private List<TofuStmtRep> Worklist;
	private Set<Integer> TempTypes;

	public InputUIDataPass(CallGraph cg, SootMethod fakeEntry, Collection<TofuStmtRep> seeds) {
		CG = cg;
		FakeEntryMethod = fakeEntry;
		Worklist = new LinkedList<>();
		TempTypes = new TreeSet<>();
		initWorklist(seeds);
	}

	private void initWorklist(Collection<TofuStmtRep> seeds) {
		List<TofuStmtRep> worklist = Worklist;
		Integer type;
		for (TofuStmtRep stmtRep : seeds) {
			Stmt stmt = stmtRep.getLabel();
			SootMethod method = stmtRep.getMethod();
			TofuCtxRep ctx = stmtRep.getCtx();
			type = TypeContextThree.findOrCreateTypeOfDataGen(method, stmt);
			if (stmt instanceof IdentityStmt) {
				Value right = ((IdentityStmt) stmt).getRightOp();
				TypeContextThree.updateVar(right, method, null, ctx, type);
				worklist.add(stmtRep);
				// the stmt waits to be processed later (type stmt & type left)
			} else {
				// normal data gen APIs
				List<ValueBox> stmtDefs = stmt.getDefBoxes();
				if (!stmtDefs.isEmpty()) {
					Value left = stmtDefs.get(0).getValue();
					TofuVarRep varRep = TypeContextThree.updateVar(left, method, stmt, ctx, type);
					TypeContextThree.updateStmt(stmtRep, type);
					// type stmt here and propagate left to other stmts
					propagate(method, stmt, left, ctx, TypeContextThree.getType(varRep), worklist);
				}
			}
		}
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
		System.out.println("[InputUIDataPass] Visit: " + stmt + " in method " + tsr.getMethod());
		if (stmt.containsInvokeExpr()) {
			SootMethod callee = stmt.getInvokeExpr().getMethod();
			String mName = callee.getName();
			if (TofuDexClasses.isLibClass(callee.getDeclaringClass().getName())) {
				UI_Call_API(tsr, worklist);
			} else if (!TofuUISigConstants.isTargetUISubSig(callee.getSubSignature())) {
				UI_Call_Normal(tsr, worklist);
			}
		} else if (stmt instanceof IdentityStmt) {
			IdentityStmt idstmt = (IdentityStmt) stmt;
			InputUIData_Identity(tsr, idstmt.getLeftOp(), idstmt.getRightOp(), worklist);
		} else if (stmt instanceof ReturnStmt) {
			ReturnStmt ret = (ReturnStmt) stmt;
			InputUIData_Return(tsr, ret.getOp(), worklist);
		} else if (stmt instanceof AssignStmt) {
			AssignStmt assign = (AssignStmt) stmt;
			Value left = assign.getLeftOp();
			Value right = assign.getRightOp();
			if (left instanceof FieldRef) {//field write
				InputUIData_AssignToField(tsr, (FieldRef) left, right, worklist);
			} else if (left instanceof ArrayRef) {
				// skip
			} else {
				InputUIData_Assign(tsr, left, right, worklist);
			}
		}
	}

	private void UI_Call_API(TofuStmtRep stmtRep, List<TofuStmtRep> worklist) {
		TofuVarRep tempVar = null;
		SootMethod method = stmtRep.getMethod();
		Stmt stmt = stmtRep.getLabel();
		TofuCtxRep ctx = stmtRep.getCtx();
		InvokeExpr invoke = stmt.getInvokeExpr();
		Set<Integer> types = null;
		Set<Integer> baseTypes = new TreeSet<>();
		Set<Integer> argTypes = new TreeSet<>();
		// collect all arg types first because UnexpectedDataUse requires arg types
		int idxArg = 0;
		int nArgs = invoke.getArgCount();
		for (; idxArg < nArgs; idxArg++) {
			Value arg = invoke.getArg(idxArg);
			List<Stmt> argDefs = TofuDefUse.getDefs(method, stmt, arg);
			for (Stmt adef : argDefs) {
				tempVar = TofuVarRep.makeTempVarRep(arg, method, adef, ctx);
				types = TypeContextThree.getType(tempVar);
				if (types != null) {
					argTypes.addAll(types);
				}
			}
		}
		// collect base types and check if Unexpected-Data-Use
		boolean mustTypeBaseFromArgs = false;
		if (invoke instanceof InstanceInvokeExpr) {
			Value base = ((InstanceInvokeExpr) invoke).getBase();
			List<Stmt> useDefs = TofuDefUse.getDefs(method, stmt, base);
			for (Stmt udef : useDefs) {
				String mClass = method.getDeclaringClass().getName();
				String mName = method.getName();
				// some other args might have been typed. only type 'base' for Collections
				if (mClass.startsWith("java.util.") &&
					(mName.startsWith("set") || mName.startsWith("add") || mName.startsWith("put") ||
					 mName.startsWith("insert") ||
					 mName.startsWith("push"))) {
					mustTypeBaseFromArgs = true;
				}
				tempVar = TofuVarRep.makeTempVarRep(base, method, udef, ctx);
				types = TypeContextThree.getType(tempVar);
				if (types != null) {
					baseTypes.addAll(types);
				}
				// check if Unexpected-Data-Use: at least an arg is typed with InputUIData but the base is NOT typed
				// with UIRelated (in Gamma-1)
				if (!argTypes.isEmpty() && !TypeContextOne.containsVar(tempVar)) {
					MustRetain.retain(argTypes);
				}
			}
		}
		// make sure we have new types coming from args
		if (mustTypeBaseFromArgs && !argTypes.isEmpty() && !baseTypes.containsAll(argTypes)) {
			Value base = ((InstanceInvokeExpr) invoke).getBase();
			List<Stmt> baseDefs = TofuDefUse.getDefs(method, stmt, base);
			for (Stmt bdef : baseDefs) {
				tempVar = TofuVarRep.makeTempVarRep(base, method, bdef, ctx);
				if (!TypeContextThree.containsVar(tempVar, argTypes)) {
					// not path-sensitive here
					TypeContextThree.updateVar(base, method, bdef, ctx, argTypes);
				}
			}
			propagate(method, stmt, base, ctx, argTypes, worklist);
		}
		// merge/type all base types and arg types to def & stmt
		baseTypes.addAll(argTypes);
		TofuStmtRep tempStmt = TofuStmtRep.makeTempStmtRep(method, stmt, ctx);
		if (!TypeContextThree.containsStmt(tempStmt, baseTypes)) {
			TypeContextThree.updateStmt(method, stmt, ctx, baseTypes);
		}
		List<ValueBox> defBoxes = stmt.getDefBoxes();
		if (!defBoxes.isEmpty()) {
			Value def = defBoxes.get(0).getValue();
			tempVar = TofuVarRep.makeTempVarRep(def, method, stmt, ctx);
			if (!TypeContextThree.containsVar(tempVar, baseTypes)) {
				tempVar = TypeContextThree.updateVar(def, method, stmt, ctx, baseTypes);
			}
			propagate(method, stmt, def, ctx, TypeContextThree.getType(tempVar), worklist);
		}
		baseTypes = null;
		argTypes = null;
	}

	private void UI_Call_Normal(TofuStmtRep stmtRep, List<TofuStmtRep> worklist) {
		SootMethod method = stmtRep.getMethod();
		Stmt stmt = stmtRep.getLabel();
		TofuCtxRep ctx = stmtRep.getCtx();
		InvokeExpr invoke = stmt.getInvokeExpr();
		if (invoke instanceof InstanceInvokeExpr) {
			Value base = ((InstanceInvokeExpr) invoke).getBase();
			InputUIData_Call_Normal_Arg(stmtRep, method, stmt, ctx, base, 0, true, worklist);
		}
		int nArgs = invoke.getArgCount();
		for (int i = 0; i < nArgs; i++) {
			Value arg = invoke.getArg(i);
			if (arg instanceof Constant) {
				continue;
			}
			InputUIData_Call_Normal_Arg(stmtRep, method, stmt, ctx, arg, i, false, worklist);
		}
	}

	private void InputUIData_Call_Normal_Arg(TofuStmtRep stmtRep, SootMethod method, Stmt stmt, TofuCtxRep ctx,
											 Value value, int idxParam, boolean isBase, List<TofuStmtRep> worklist) {
		TofuVarRep tempVar;
		Set<Integer> types;
		List<Stmt> valDefs = TofuDefUse.getDefs(method, stmt, value);
		for (Stmt vdef : valDefs) {
			tempVar = TofuVarRep.makeTempVarRep(value, method, vdef, ctx);
			types = TypeContextThree.getType(tempVar);
			if (types != null) {
				TempTypes.addAll(types);
			}
		}
		// propagate all base types to [def/stmt/]param_of_callee
		if (!TempTypes.isEmpty()) {
			if (isBase) {
				// type stmt
				if (!TypeContextThree.containsStmt(stmtRep, TempTypes)) {
					TypeContextThree.updateStmt(stmtRep, TempTypes);
				}
				// type definition
				List<ValueBox> stmtDefs = stmt.getDefBoxes();
				if (!stmtDefs.isEmpty()) {
					Value vdef = stmtDefs.get(0).getValue();
					tempVar = TofuVarRep.makeTempVarRep(vdef, method, stmt, ctx);
					if (!TypeContextThree.containsVar(tempVar, TempTypes)) {
						tempVar = TypeContextThree.updateVar(vdef, method, stmt, ctx, TempTypes);
					}
					propagate(method, stmt, vdef, ctx, TypeContextThree.getType(tempVar), worklist);
				}
			}
			// type 'this'/'param' of callee
			List<Pair<SootMethod, IdentityStmt>> thisInCallee = TofuDefUse.getParam(CG, stmt, idxParam, isBase);
			for (Pair<SootMethod, IdentityStmt> thisDef : thisInCallee) {
				SootMethod callee = thisDef.getO1();
				IdentityStmt istmt = thisDef.getO2();
				InputUIData_Call_Normal_ArgToParam(method, stmt, ctx, callee, istmt, TempTypes, worklist);
			}
		}
		TempTypes.clear();
	}

	private void InputUIData_Call_Normal_ArgToParam(SootMethod caller, Stmt callsite, TofuCtxRep callerCtx,
													SootMethod callee, IdentityStmt paramStmt, Set<Integer> types,
													List<TofuStmtRep> worklist) {
		TofuStmtRep tempStmt;
		TofuVarRep tempVar;
		Value pval = paramStmt.getRightOp();
		Set<TofuCtxRep> ctx4Calee = TofuCGContext.getCorrespondingCtxForCallee(caller, callsite, callerCtx, callee);
		TofuCtxRep calleeCtx = null;
		if (ctx4Calee == null || ctx4Calee.isEmpty()) {
			tempVar = TofuVarRep.makeTempVarRep(pval, callee, null, calleeCtx);
			if (!TypeContextThree.containsVar(tempVar, types)) {
				TypeContextThree.updateVar(pval, callee, null, calleeCtx, types);
			}
		} else {
			for (TofuCtxRep cctx : ctx4Calee) {
				tempVar = TofuVarRep.makeTempVarRep(pval, callee, null, cctx);
				if (!TypeContextThree.containsVar(tempVar, types)) {
					TypeContextThree.updateVar(pval, callee, null, cctx, types);
				}
				tempStmt = TofuStmtRep.makeTempStmtRep(callee, paramStmt, cctx);
				if (TypeContextThree.containsStmt(tempStmt, types)) {
					TofuStmtRep newStmt = new TofuStmtRep(callee, paramStmt, cctx);
					worklist.add(newStmt);
				}
			}
		}
	}

	private void InputUIData_Identity(TofuStmtRep stmtRep, Value left, Value right, List<TofuStmtRep> worklist) {
		TofuVarRep tempVar;
		SootMethod method = stmtRep.getMethod();
		Stmt stmt = stmtRep.getLabel();
		TofuCtxRep ctx = stmtRep.getCtx();
		tempVar = TofuVarRep.makeTempVarRep(right, method, null, ctx);
		Set<Integer> rightVarTypes = TypeContextThree.getType(tempVar);
		if (rightVarTypes != null && !rightVarTypes.isEmpty()) {
			if (!TypeContextThree.containsStmt(stmtRep, rightVarTypes)) {
				TypeContextThree.updateStmt(stmtRep, rightVarTypes);
			}
			tempVar = TofuVarRep.makeTempVarRep(left, method, stmt, ctx);
			if (!TypeContextThree.containsVar(tempVar, rightVarTypes)) {
				tempVar = TypeContextThree.updateVar(left, method, stmt, ctx, rightVarTypes);
			}
			propagate(method, stmt, left, ctx, TypeContextThree.getType(tempVar), worklist);
		}
	}

	private void InputUIData_Assign(TofuStmtRep stmtRep, Value left, Value right, List<TofuStmtRep> worklist) {
		SootMethod method = stmtRep.getMethod();
		Stmt stmt = stmtRep.getLabel();
		TofuCtxRep ctx = stmtRep.getCtx();
		TofuVarRep tempVar;
		if (right instanceof FieldRef) {
			// read from field. we need the stmt being typed before. we do not type field directly.
			Set<Integer> stmtType = TypeContextThree.getType(stmtRep);
			if (stmtType == null || stmtType.isEmpty()) {
				return;
			} else {
				tempVar = TofuVarRep.makeTempVarRep(left, method, stmt, ctx);
				if (!TypeContextThree.containsVar(tempVar, stmtType)) {
					tempVar = TypeContextThree.updateVar(left, method, stmt, ctx, stmtType);
				}
				propagate(method, stmt, left, ctx, TypeContextThree.getType(tempVar), worklist);
				return;
			}
		}
		// type stmt with all rhs types
		Set<Integer> types;
		List<ValueBox> rightUseBoxes = right.getUseBoxes();
		for (ValueBox rvb : rightUseBoxes) {
			Value rval = rvb.getValue();
			if (rval instanceof Constant) {
				continue;
			}
			List<Stmt> rvalDefs = TofuDefUse.getDefs(method, stmt, rval);
			for (Stmt rdef : rvalDefs) {
				tempVar = TofuVarRep.makeTempVarRep(rval, method, rdef, ctx);
				types = TypeContextThree.getType(tempVar);
				if (types != null && !types.isEmpty()) {
					// we find new types for rhs vars. type to stmt.
					if (!TypeContextThree.containsStmt(stmtRep, types)) {
						TypeContextThree.updateStmt(stmtRep, types);
					}
				}
			}
		}
		// after typing stmt, propagate all types of stmt to def (left)
		tempVar = TofuVarRep.makeTempVarRep(left, method, stmt, ctx);
		types = TypeContextThree.getType(stmtRep);
		if (!TypeContextThree.containsVar(tempVar, types)) {
			tempVar = TypeContextThree.updateVar(left, method, stmt, ctx, types);
		}
		propagate(method, stmt, left, ctx, TypeContextThree.getType(tempVar), worklist);
	}

	private void InputUIData_AssignToField(TofuStmtRep stmtRep, FieldRef field, Value right,
										   List<TofuStmtRep> worklist) {
		if (!(right instanceof Local)) {
			System.out.println("[InputUIDataPass - Error] Wrong Assignment to field with RHS = " + right);
			return;
		}
		SootMethod method = stmtRep.getMethod();
		Stmt stmt = stmtRep.getLabel();
		TofuCtxRep ctx = stmtRep.getCtx();
		TofuVarRep tempVar = null;
		Set<Integer> types;
		List<Stmt> rightDefs = TofuDefUse.getDefs(method, stmt, right);
		for (Stmt rdef : rightDefs) {
			tempVar = TofuVarRep.makeTempVarRep(right, method, rdef, ctx);
			types = TypeContextThree.getType(tempVar);
			if (types != null) {
				TempTypes.addAll(types);
			}
		}
		if (TempTypes.isEmpty()) {
			return;
		}
		// type stmt
		if (!TypeContextThree.containsStmt(stmtRep, TempTypes)) {
			TypeContextThree.updateStmt(stmtRep, TempTypes);
		}
		// find field reads and type corresponding stmts (not the field variables).
		String fSig = field.getField().getSignature();
		Set<Pair<SootMethod, Stmt>> fieldReads = TofuFieldCollector.getLocationsOfFieldRead(fSig);
		if (fieldReads != null && !fieldReads.isEmpty()) {
			Set<Pair<SootMethod, Stmt>> tempFR = new HashSet<>();
			for (Pair<SootMethod, Stmt> fr : fieldReads) {
				SootMethod frMethod = fr.getO1();
				Stmt frStmt = fr.getO2();
				if (method.equals(frMethod)) {
					if (TofuDefUse.happensBefore(method, stmt, frStmt)) {
						TofuStmtRep tempStmt = TofuStmtRep.makeTempStmtRep(frMethod, frStmt, ctx);
						if (!TypeContextThree.containsStmt(tempStmt, TempTypes)) {
							TypeContextThree.updateStmt(frMethod, frStmt, ctx, TempTypes);
							// return of updateStmt may return the tempStmt so we cannot use it in worklist
							TofuStmtRep newStmt = new TofuStmtRep(frMethod, frStmt, ctx);
							worklist.add(newStmt);
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
										if (!TypeContextThree.containsStmt(tempStmt, TempTypes)) {
											TypeContextThree.updateStmt(frMethod, frStmt, frmc, TempTypes);
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
						if (!TypeContextThree.containsStmt(tempStmt, TempTypes)) {
							TypeContextThree.updateStmt(frMethod, frStmt, null, TempTypes);
							// return of updateStmt may return the tempStmt so we cannot use it in worklist
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
								if (!TypeContextThree.containsStmt(tempStmt, TempTypes)) {
									TypeContextThree.updateStmt(frMethod, frStmt, frmc, TempTypes);
									// return of updateStmt may return the tempStmt so we cannot use it in worklist
									TofuStmtRep newStmt = new TofuStmtRep(frMethod, frStmt, frmc);
									worklist.add(newStmt);
								}
							}
						}
					}
				}
			}
		}
		TempTypes.clear();
	}

	private void InputUIData_Return(TofuStmtRep stmtRep, Value ret, List<TofuStmtRep> worklist) {
		SootMethod method = stmtRep.getMethod();
		Stmt stmt = stmtRep.getLabel();
		TofuCtxRep ctx = stmtRep.getCtx();
		TofuVarRep tempVar = null;
		Set<Integer> types;
		List<Stmt> retDefs = TofuDefUse.getDefs(method, stmt, ret);
		for (Stmt rdef : retDefs) {
			tempVar = TofuVarRep.makeTempVarRep(ret, method, rdef, ctx);
			types = TypeContextThree.getType(tempVar);
			if (types != null) {
				TempTypes.addAll(types);
			}
		}
		if (TempTypes.isEmpty()) {
			return;
		}
		// type stmt
		if (!TypeContextThree.containsStmt(stmtRep, TempTypes)) {
			TypeContextThree.updateStmt(stmtRep, TempTypes);
		}
		// jump to callsite
		if (ctx != null) {
			Pair<SootMethod, Stmt> callingCtx = ctx.topCtxElement();
			SootMethod caller = callingCtx.getO1();
			Stmt callsite = callingCtx.getO2();
			UI_ReturnToCaller(method, ctx, caller, callsite, TempTypes, worklist);
		} else {
			Iterator<Edge> iterator = CG.edgesInto(method);
			while (iterator.hasNext()) {
				Edge edge = iterator.next();
				SootMethod caller = edge.src();
				Stmt callsite = edge.srcStmt();
				UI_ReturnToCaller(method, ctx, caller, callsite, TempTypes, worklist);
			}
		}
		TempTypes.clear();
	}

	private void UI_ReturnToCaller(SootMethod method, TofuCtxRep ctx, SootMethod caller, Stmt callsite,
								   Set<Integer> types, List<TofuStmtRep> worklist) {
		if (!FakeEntryMethod.equals(caller)) {
			TofuCtxRep callerCtx = TofuCGContext.getCorrespondingCtxForCaller(CG, ctx, method);
			TofuStmtRep tempStmt = TofuStmtRep.makeTempStmtRep(caller, callsite, callerCtx);
			// type callsite stmt
			if (!TypeContextThree.containsStmt(tempStmt, types)) {
				TypeContextThree.updateStmt(caller, callsite, callerCtx, types);
			}
			// type callsite ret var
			List<ValueBox> callsiteDefs = callsite.getDefBoxes();
			if (!callsiteDefs.isEmpty()) {
				Value vdef = callsiteDefs.get(0).getValue();
				TofuVarRep tempVar = TofuVarRep.makeTempVarRep(vdef, caller, callsite, callerCtx);
				if (!TypeContextThree.containsVar(tempVar, types)) {
					tempVar = TypeContextThree.updateVar(vdef, caller, callsite, callerCtx, types);
				}
				propagate(caller, callsite, vdef, callerCtx, TypeContextThree.getType(tempVar), worklist);
			}
		}
	}

	private void propagate(SootMethod method, Stmt stmt, Value value, TofuCtxRep ctx, Set<Integer> types,
						   List<TofuStmtRep> worklist) {
		List<Stmt> varUses = TofuDefUse.getUses(method, stmt, value);
		for (Stmt use : varUses) {
			TofuStmtRep tempStmt = TofuStmtRep.makeTempStmtRep(method, use, ctx);
			if (!TypeContextThree.containsStmt(tempStmt, types)) {
				TofuStmtRep newStmt = new TofuStmtRep(method, use, ctx);
				worklist.add(newStmt);
			}
		}
	}
}
