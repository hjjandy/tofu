package edu.purdue.cs.toydroid.soot.util;


import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.scalar.Pair;
import soot.util.Chain;

import java.util.*;

public class TofuFieldCollector {
	private static Map<String, Set<Pair<SootMethod, Stmt>>> FieldRead2Locations = new HashMap<>();
	private static Map<String, Set<Pair<SootMethod, Stmt>>> FieldWrite2Locations = new HashMap<>();
	private static Pair<SootMethod, Stmt> TempPair = new Pair<>();

	public static Set<Pair<SootMethod, Stmt>> getLocationsOfFieldRead(String field) {
		return FieldRead2Locations.get(field);
	}

	public static Set<Pair<SootMethod, Stmt>> getLocationsOfFieldWrite(String field) {
		return FieldWrite2Locations.get(field);
	}

	/**
	 * @param field Signatore of a Field.
	 * @return All appearances of the field.
	 */
	public static Set<Pair<SootMethod, Stmt>> getLocationsOfField(String field) {
		Set<Pair<SootMethod, Stmt>> set = new HashSet<>();
		Set<Pair<SootMethod, Stmt>> reads = FieldRead2Locations.get(field);
		Set<Pair<SootMethod, Stmt>> writes = FieldWrite2Locations.get(field);
		if (reads != null) {
			set.addAll(reads);
		}
		if (writes != null) {
			set.addAll(writes);
		}
		return set;
	}

	public static void collectFields(CallGraph cg, SootMethod fakeEntry, Set<Stmt> startingLabels) {
		List<SootMethod> worklist = new LinkedList<>();
		Set<SootMethod> visited = new HashSet<>();
		for (Stmt stmt : startingLabels) {
			Iterator<Edge> iterator = cg.edgesOutOf(stmt);
			while (iterator.hasNext()) {
				Edge edge = iterator.next();
				SootMethod tgt = edge.tgt();
				if (!visited.contains(tgt)) {
					visited.add(tgt);
					worklist.add(tgt);
				}
			}
		}
		while (!worklist.isEmpty()) {
			SootMethod method = worklist.remove(0);
			collectFields(cg, method, worklist, visited);
			Iterator<Edge> iterator = cg.edgesOutOf(method);
			while (iterator.hasNext()) {
				Edge edge = iterator.next();
				SootMethod tgt = edge.tgt();
				if (TofuDexClasses.isLibClass(tgt.getDeclaringClass().getName())) {
					continue;
				}
				if (!visited.contains(tgt)) {
					visited.add(tgt);
					worklist.add(tgt);
				}
			}
		}
		visited = null;
	}

	private static void collectFields(CallGraph cg, SootMethod method, List<SootMethod> worklist,
									  Set<SootMethod> visited) {
		if (!method.hasActiveBody()) {
			return;
		}
		Body body = method.getActiveBody();
		Chain<Unit> chain = body.getUnits();
		for (Unit unit : chain) {
			Stmt stmt = (Stmt) unit;
			if (stmt instanceof AssignStmt && stmt.containsFieldRef()) {
				AssignStmt assign = (AssignStmt) stmt;
				Value left = assign.getLeftOp();
				FieldRef fieldRef = stmt.getFieldRef();
				SootFieldRef sfr = fieldRef.getFieldRef();
				SootClass clazz = sfr.declaringClass();
				if (TofuDexClasses.isLibClass(clazz.getName())) {
					continue;
				}
				String field = sfr.getSignature();
				if (left instanceof FieldRef) {
					Set<Pair<SootMethod, Stmt>> set = FieldWrite2Locations.get(field);
					if (set == null) {
						set = new HashSet<>();
						FieldWrite2Locations.put(field, set);
					}
					TempPair.setPair(method, stmt);
					if (!set.contains(TempPair)) {
						set.add(new Pair<>(method, stmt));
					}
				} else {
					Set<Pair<SootMethod, Stmt>> set = FieldRead2Locations.get(field);
					if (set == null) {
						set = new HashSet<>();
						FieldRead2Locations.put(field, set);
					}
					TempPair.setPair(method, stmt);
					if (!set.contains(TempPair)) {
						set.add(new Pair<>(method, stmt));
					}
				}
			}
		}

	}
}
