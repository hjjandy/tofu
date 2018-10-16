package edu.purdue.cs.toydroid.soot.util;

import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.TrapUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.Pair;
import soot.util.Chain;

import java.util.*;

public class TofuDefUse {
	private static Map<SootMethod, UnitGraph> Method2CFG = new HashMap<>();

	public static boolean usedInCondOrSwitch(SootMethod method, Stmt stmt, Value value) {
		Local valLocal = (Local) value;
		UnitGraph cfg = Method2CFG.get(method);
		if (cfg == null) {
			cfg = new TrapUnitGraph(method.getActiveBody());
			Method2CFG.put(method, cfg);
		}
		Set<Unit> visited = new HashSet<>();
		List<Unit> worklist = new LinkedList<>();
		visited.add(stmt);
		worklist.add(stmt);
		boolean first = true;
		boolean hitted = false;
		while (!worklist.isEmpty()) {
			Stmt next = (Stmt) worklist.remove(0);
			boolean redefined = false;
			if (!first) {
				if (next instanceof AssignStmt) {
					Value left = ((AssignStmt) next).getLeftOp();
					if (left instanceof Local && (((Local) left).getName().equals(valLocal.getName()))) {
						redefined = true;
					}
				} else if (next instanceof IfStmt) {
					IfStmt ifs = (IfStmt) next;
					Value cond = ifs.getCondition();
					if (cond instanceof BinopExpr) {
						Value bin1 = ((BinopExpr) cond).getOp1();
						Value bin2 = ((BinopExpr) cond).getOp2();
						if (bin1 instanceof Local && (((Local) bin1).getName().equals(valLocal.getName()))) {
							hitted = true;
							break;
						} else if (bin2 instanceof Local && (((Local) bin2).getName().equals(valLocal.getName()))) {
							hitted = true;
							break;
						}
					}
				} else if (next instanceof SwitchStmt) {
					SwitchStmt sst = (SwitchStmt) next;
					Value key = sst.getKey();
					if (key instanceof Local && (((Local) key).getName().equals(valLocal.getName()))) {
						hitted = true;
						break;
					}
				}
			}
			first = false;
			if (!redefined) {
				List<Unit> succs = cfg.getSuccsOf(next);
				for (Unit succ : succs) {
					if (visited.add(succ)) {
						worklist.add(succ);
					}
				}
			}
		}
		return hitted;
	}

	public static boolean happensBefore(SootMethod method, Stmt fst, Stmt snd) {
		UnitGraph cfg = Method2CFG.get(method);
		if (cfg == null) {
			cfg = new TrapUnitGraph(method.getActiveBody());
			Method2CFG.put(method, cfg);
		}
		Set<Unit> visited = new HashSet<>();
		List<Unit> worklist = new LinkedList<>();
		worklist.add(fst);
		visited.add(fst);
		boolean happensBefore = false;
		while (!worklist.isEmpty()) {
			Unit unit = worklist.remove(0);
			if (snd.equals(unit)) {
				happensBefore = true;
				break;
			}
			List<Unit> succs = cfg.getSuccsOf(unit);
			for (Unit succ : succs) {
				if (visited.add(succ)) {
					worklist.add(succ);
				}
			}
		}
		return happensBefore;
	}

	public static List<Stmt> getDefs(SootMethod method, Stmt stmt, Value value) {
		List<Stmt> localDefs = new LinkedList<>();
		Local valLocal = (Local) value;
		UnitGraph cfg = Method2CFG.get(method);
		if (cfg == null) {
			cfg = new TrapUnitGraph(method.getActiveBody());
			Method2CFG.put(method, cfg);
		}
		Set<Unit> visited = new HashSet<>();
		List<Unit> worklist = new LinkedList<>();
		worklist.add(stmt);
		visited.add(stmt);
		boolean first = true;
		while (!worklist.isEmpty()) {
			Unit unit = worklist.remove(0);
			boolean defFound = false;
			if (!first) {
				Value left = null;
				if (unit instanceof AssignStmt) {
					AssignStmt assign = (AssignStmt) unit;
					left = assign.getLeftOp();
				} else if (unit instanceof IdentityStmt) {
					left = ((IdentityStmt) unit).getLeftOp();
				}
				if (null != left) {
					if (left instanceof Local && ((Local) left).getName().equals(valLocal.getName())) {
						localDefs.add((Stmt) unit);
						defFound = true;
					}
				}
			}
			first = false;
			if (!defFound) {
				List<Unit> preds = cfg.getPredsOf(unit);
				for (Unit pred : preds) {
					if (visited.add(pred)) {
						worklist.add(pred);
					}
				}
			}
		}
		visited = null;
		return localDefs;
	}

	public static List<Stmt> getUses(SootMethod method, Stmt stmt, Value value) {
		List<Stmt> localUses = new LinkedList<>();
		Local valLocal = (Local) value;
		UnitGraph cfg = Method2CFG.get(method);
		if (cfg == null) {
			cfg = new TrapUnitGraph(method.getActiveBody());
			Method2CFG.put(method, cfg);
		}
		Set<Unit> visited = new HashSet<>();
		List<Unit> worklist = new LinkedList<>();
		worklist.add(stmt);
		visited.add(stmt);
		boolean first = true;
		while (!worklist.isEmpty()) {
			Unit unit = worklist.remove(0);
			stmt = (Stmt) unit;
			boolean reDefFound = false;
			if (!first) {
				if (stmt.containsInvokeExpr()) {
					boolean thisMatches = false;
					InvokeExpr invoke = stmt.getInvokeExpr();
					if (invoke instanceof InstanceInvokeExpr) {
						Value base = ((InstanceInvokeExpr) invoke).getBase();
						if (((Local) base).getName().equals(valLocal.getName())) {
							thisMatches = true;
							localUses.add(stmt);
						}
					}
					if (!thisMatches) {
						List<Value> args = invoke.getArgs();
						for (Value arg : args) {
							if (arg instanceof Local) {
								if (((Local) arg).getName().equals(valLocal.getName())) {
									localUses.add(stmt);
									break;
								}
							}
						}
					}
				} else if (stmt instanceof AssignStmt) {
					AssignStmt assign = (AssignStmt) stmt;
					Value right = assign.getRightOp();
					if (right instanceof CastExpr) {
						CastExpr cast = (CastExpr) right;
						Value castOp = cast.getOp();
						if (castOp instanceof Local && (((Local) castOp).getName().equals(valLocal.getName()))) {
							localUses.add(assign);
						}
					} else if (right instanceof BinopExpr) {
						BinopExpr binop = (BinopExpr) right;
						Value bin1 = binop.getOp1();
						Value bin2 = binop.getOp2();
						if (bin1 instanceof Local && (((Local) bin1).getName().equals(valLocal.getName()))) {
							localUses.add(assign);
						} else if (bin2 instanceof Local && (((Local) bin2).getName().equals(valLocal.getName()))) {
							localUses.add(assign);
						}
					} else if (right instanceof UnopExpr) {
						UnopExpr unop = (UnopExpr) right;
						Value unv = unop.getOp();
						if (unv instanceof Local && (((Local) unv).getName().equals(valLocal.getName()))) {
							localUses.add(assign);
						}
					} else if (right instanceof InstanceOfExpr) {
						InstanceOfExpr insof = (InstanceOfExpr) right;
						Value insv = insof.getOp();
						if (insv instanceof Local && (((Local) insv).getName().equals(valLocal.getName()))) {
							localUses.add(assign);
						}
					} else if (right instanceof InstanceFieldRef) {
						InstanceFieldRef ifr = (InstanceFieldRef) right;
						Local base = (Local) ifr.getBase();
						if (base.getName().equals(valLocal.getName())) {
							localUses.add(assign);
						}
					} else if (right instanceof Local) {
						if (((Local) right).getName().equals(valLocal.getName())) {
							localUses.add(assign);
						}
					}
				} else if (stmt instanceof ReturnStmt) {
					ReturnStmt ret = (ReturnStmt) stmt;
					Value rv = ret.getOp();
					if (rv instanceof Local && (((Local) rv).getName().equals(valLocal.getName()))) {
						localUses.add(ret);
					}
				}
				if (stmt instanceof AssignStmt) {
					Value left = ((AssignStmt) stmt).getLeftOp();
					if (left instanceof Local && (((Local) left).getName().equals(valLocal.getName()))) {
						reDefFound = true;
					}
				}
			}
			first = false;
			if (!reDefFound) {
				List<Unit> succs = cfg.getSuccsOf(unit);
				for (Unit succ : succs) {
					if (visited.add(succ)) {
						worklist.add(succ);
					}
				}
			}
		}
		visited = null;
		return localUses;
	}

	public static List<Pair<SootMethod, IdentityStmt>> getParam(CallGraph cg, Stmt callsite, int idx,
																boolean isThisRef) {
		List<Pair<SootMethod, IdentityStmt>> list = new LinkedList<>();
		Iterator<Edge> iterator = cg.edgesOutOf(callsite);
		while (iterator.hasNext()) {
			SootMethod callee = iterator.next().tgt();
			if (!callee.hasActiveBody()) {
				continue;
			}
			Chain<Unit> chain = callee.getActiveBody().getUnits();
			for (Unit unit : chain) {
				if (unit instanceof IdentityStmt) {
					IdentityStmt idstmt = (IdentityStmt) unit;
					Value right = idstmt.getRightOp();
					if (isThisRef && right instanceof ThisRef) {
						list.add(new Pair<>(callee, idstmt));
						break;
					} else if (!isThisRef && right instanceof ParameterRef) {
						ParameterRef param = (ParameterRef) right;
						if (idx == param.getIndex()) {
							list.add(new Pair<>(callee, idstmt));
							break;
						}
					}
				}
			}
		}
		return list;
	}
}
