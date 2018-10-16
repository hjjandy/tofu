package edu.purdue.cs.toydroid.soot.tofu.type;


import edu.purdue.cs.toydroid.soot.util.TofuEntryPointConstants;
import soot.*;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.*;
import soot.util.Chain;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class TofuWholeRemoveFragment {
	private final static String GetActivity = "android.support.v4.app.FragmentActivity getActivity()";
	private static Set<SootMethod> ModifiedMethods = new HashSet<>();

	public static Set<SootMethod> doRemove(SootClass clazz) {
		ModifiedMethods.clear();
		List<String> methodSubSigs = TofuEntryPointConstants.getFragmentLifecycleMethods();
		for (String subsig : methodSubSigs) {
			if (clazz.declaresMethod(subsig)) {
				SootMethod method = clazz.getMethod(subsig);
				doRemove(method);
			}
		}
		return ModifiedMethods;
	}

	private static void doRemove(SootMethod method) {
		if (!method.hasActiveBody()) {
			return;
		}
		ModifiedMethods.add(method);
		Body body = method.getActiveBody();
		Local thisRef = body.getThisLocal();
		Chain<Unit> chain = body.getUnits();
		for (Iterator<Unit> iter = chain.snapshotIterator(); iter.hasNext(); ) {
			Unit unit = iter.next();
			Stmt stmt = (Stmt) unit;
			if (stmt instanceof IdentityStmt) {
				continue;
			} else if (stmt.containsInvokeExpr()) {
				InvokeExpr invoke = stmt.getInvokeExpr();
				if (invoke instanceof InstanceInvokeExpr) {
					Local base = (Local) ((InstanceInvokeExpr) invoke).getBase();
					if (thisRef.getName().equals(base.getName()) &&
						method.getSubSignature().equals(invoke.getMethod().getSubSignature()) &&
						Scene.v().getActiveHierarchy()
							 .isClassSubclassOf(method.getDeclaringClass(), invoke.getMethod().getDeclaringClass())) {
						// super call
						continue;
					}
				}
			}
			chain.remove(unit);
		}

		if (method.getName().equals("onCreateView")) {

			LocalGenerator gen = new LocalGenerator(body);
			Local localContext = gen.generateLocal(RefType.v("android.support.v4.app.FragmentActivity"));
			SootMethod mGetAct = findMethodGetActivity(method.getDeclaringClass());
			InvokeExpr getAct = Jimple.v().newVirtualInvokeExpr(thisRef, mGetAct.makeRef());
			Stmt assignAct = Jimple.v().newAssignStmt(localContext, getAct);
			body.getUnits().add(assignAct);
			Local localTV = gen.generateLocal(RefType.v("android.widget.TextView"));
			Expr newTV = Jimple.v().newNewExpr(RefType.v("android.widget.TextView"));
			Stmt assignTV = Jimple.v().newAssignStmt(localTV, newTV);
			body.getUnits().add(assignTV);
			SootMethod init = Scene.v().getMethod("<android.widget.TextView: void <init>(android.content.Context)>");
			Expr tvInit = Jimple.v().newSpecialInvokeExpr(localTV, init.makeRef(), localContext);
			body.getUnits().add(Jimple.v().newInvokeStmt(tvInit));
			body.getUnits().add(Jimple.v().newReturnStmt(localTV));
		} else {
			body.getUnits().add(Jimple.v().newReturnVoidStmt());
		}
		System.out.println("Remove Whole Method: " + method);
	}

	private static SootMethod findMethodGetActivity(SootClass clazz) {
		if (clazz.declaresMethod(GetActivity)) {
			return clazz.getMethod(GetActivity);
		}
		if (clazz.hasSuperclass()) {
			SootClass sp = clazz.getSuperclass();
			return findMethodGetActivity(sp);
		}
		return null;
	}
}
