package edu.purdue.cs.toydroid.soot.tofu;


import edu.purdue.cs.toydroid.soot.util.TofuDefUse;
import soot.SootMethod;
import soot.Value;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.Iterator;
import java.util.List;

public class TofuIdDiscover {
	public TofuIdDiscover() {

	}

	public int discover(CallGraph cg, SootMethod method, Stmt stmt, int idx) {
		InvokeExpr invoke = stmt.getInvokeExpr();
		if (idx >= invoke.getArgCount()) {
			return 0;
		}
		Value value = invoke.getArg(idx);
		if (value instanceof IntConstant) {
			return ((IntConstant) value).value;
		}
		return discoverNonConstantId(cg, method, stmt, value);
	}

	private int discoverNonConstantId(CallGraph cg, SootMethod method, Stmt stmt, Value value) {
		List<Stmt> defs = TofuDefUse.getDefs(method, stmt, value);
		for (Stmt d : defs) {System.out.println(method + "\n     " + d);
			if (d instanceof IdentityStmt) {
				IdentityStmt is = (IdentityStmt) d;
				Value right = is.getRightOp();
				if (right instanceof ParameterRef) {
					int pindex = ((ParameterRef)right).getIndex();
					Iterator<Edge> iterator = cg.edgesInto(method);
					while (iterator.hasNext()) {
						Edge edge = iterator.next();
						Stmt call = edge.srcStmt();System.out.println(edge.src() + "\n   -> " + call);
						if (call.containsInvokeExpr()) {
							InvokeExpr invoke = call.getInvokeExpr();
							Value arg = invoke.getArg(pindex);
							if (arg instanceof IntConstant) {
								return ((IntConstant)arg).value;
							}
						}
					}
				}
			}
		}
		return 0;
	}
}
