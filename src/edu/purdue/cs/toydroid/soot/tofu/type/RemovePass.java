package edu.purdue.cs.toydroid.soot.tofu.type;


import edu.purdue.cs.toydroid.soot.util.TofuCGContext;
import edu.purdue.cs.toydroid.soot.util.TofuCtxRep;
import edu.purdue.cs.toydroid.soot.util.TofuStmtRep;
import soot.Body;
import soot.PatchingChain;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RemovePass {
	private CallGraph CG;
	private SootMethod FakeEntryMethod;

	public RemovePass(CallGraph cg, SootMethod fakeEntry) {
		CG = cg;
		FakeEntryMethod = fakeEntry;
	}

	public void solve() {
		boolean hasChanged = Remove_Stmt_1();
		if (!hasChanged) {
			return;
		}
		Map<TofuStmtRep, Set<Integer>> stmtsInGamma3 = new HashMap<>();
		stmtsInGamma3.putAll(TypeContextThree.getAllStmts());
		Set<TofuStmtRep> mustUnremovable = new HashSet<>();
		// Unremove-Stmt && Remove-Stmt-2
		while (hasChanged) {
			hasChanged = false;
			mustUnremovable.clear();
			Set<Map.Entry<TofuStmtRep, Set<Integer>>> set = stmtsInGamma3.entrySet();
			for (Map.Entry<TofuStmtRep, Set<Integer>> entry : set) {
				TofuStmtRep stmtRep = entry.getKey();
				Set<Integer> types = entry.getValue();
				hasChanged = Unremove_Stmt(stmtRep, types, mustUnremovable) | hasChanged;
				hasChanged = Remove_Stmt_2(stmtRep, types) | hasChanged;
			}
			// for those Unremovable stmts, we do not pay more attention on them.
			if (hasChanged) {
				for (TofuStmtRep toremove : mustUnremovable) {
					stmtsInGamma3.remove(toremove);
				}
			}
		}
		stmtsInGamma3 = null;
		mustUnremovable = null;
	}

	// return: hasChanged in Gamma-4
	private boolean Unremove_Stmt(TofuStmtRep stmtRep, Set<Integer> gamma3Types, Set<TofuStmtRep> mustUnremovable) {
		SootMethod method = stmtRep.getMethod();
		Stmt stmt = stmtRep.getLabel();
		Integer gamma4Type = TypeContextFour.getTypeOf(method, stmt);
		if (TypeSystem.isUnremovable(gamma4Type)) {
			mustUnremovable.add(stmtRep);
			return false;
		}
		for (Integer gamma3Type : gamma3Types) {
			if (!MustRetain.mustRetain(gamma3Type)) {
				return false;
			}
		}
		// check if the stmt is UIRelated
		TofuStmtRep tempStmt = TofuStmtRep.makeTempStmtRep(method, stmt, null);// all ctx
		if (TypeContextOne.containsStmt(tempStmt)) {
			return false;
		}
		// all calling ctx of method/stmt
		Set<TofuCtxRep> allCtx = TofuCGContext.getCtxForMethod(method);
		if (allCtx != null && !allCtx.isEmpty()) {
			for (TofuCtxRep ctx : allCtx) {
				tempStmt = TofuStmtRep.makeTempStmtRep(method, stmt, ctx);
				if (TypeContextOne.containsStmt(tempStmt)) {
					return false;
				}
			}
		}
		mustUnremovable.add(stmtRep);
		return (null != TypeContextFour.updateStmt(method, stmt, TypeSystem.UnRemovable));
	}

	// return: hasChanged in Gamma-4
	private boolean Remove_Stmt_2(TofuStmtRep stmtRep, Set<Integer> gamma3Types) {
		SootMethod method = stmtRep.getMethod();
		Stmt stmt = stmtRep.getLabel();
		Integer gamma4Type = TypeContextFour.getTypeOf(method, stmt);
		// if Unremovable, do nothing; if Removable, do not update.
		if (null != gamma4Type) {
			return false;
		}
		for (Integer gamma3Type : gamma3Types) {
			if (MustRetain.mustRetain(gamma3Type)) {
				return false;// at least one datagen must be retained, current stmt cannot be removed.
			}
		}
		// Removable stmt may be later typed with Unremovable.
		return (null != TypeContextFour.updateStmt(method, stmt, TypeSystem.Removable));
	}

	private int idxOfStmt(SootMethod method, Stmt stmt) {
		Body body = method.getActiveBody();
		PatchingChain<Unit> chain = body.getUnits();
		int idx = 0;
		for (Unit unit : chain) {
			if (unit.equals(stmt)) {
				return idx;
			}
			idx++;
		}
		if (idx == chain.size()) {
			idx = -1;
		}
		return idx;
	}

	private boolean Remove_Stmt_1() {
		boolean hasChanged = false;
		Set<TofuStmtRep> stmtsInGammaOne = TypeContextOne.getAllStmts();
		for (TofuStmtRep stmtRep : stmtsInGammaOne) {
			TofuCtxRep ctx = stmtRep.getCtx();
			SootMethod method = stmtRep.getMethod();
			Stmt stmt = stmtRep.getLabel();
			if (ctx == null) {
				if (!TypeContextFour.containsStmt(method, stmt)) {
					TypeContextFour.updateStmt(method, stmt, TypeSystem.Removable);
					hasChanged = true;
				}
			} else {
				// make sure all ctx of 'stmt' are typed with UIRelated (GammaOne)
				Set<TofuCtxRep> allCtx = TofuCGContext.getCtxForMethod(method);
				if (allCtx != null && !allCtx.isEmpty()) {
					boolean stmtInAllCtxTyped = true;
					for (TofuCtxRep pctx : allCtx) {
						TofuStmtRep tempStmt = TofuStmtRep.makeTempStmtRep(method, stmt, pctx);
						if (!TypeContextOne.containsStmt(tempStmt)) {
							stmtInAllCtxTyped = false;
							break;
						}
					}
					if (stmtInAllCtxTyped) {
						if (TypeContextFour.containsStmt(method, stmt)) {
							TypeContextFour.updateStmt(method, stmt, TypeSystem.Removable);
							hasChanged = true;
						}
					}
				} else {
					if (TypeContextFour.containsStmt(method, stmt)) {
						TypeContextFour.updateStmt(method, stmt, TypeSystem.Removable);
						hasChanged = true;
					}
				}
			}
		}
		return hasChanged;
	}
}
