package edu.purdue.cs.toydroid.soot.tofu;


import edu.purdue.cs.toydroid.soot.tofu.type.TofuWholeRemoveFragment;
import edu.purdue.cs.toydroid.soot.tofu.type.TypeContextFour;
import edu.purdue.cs.toydroid.soot.util.TofuDefUse;
import edu.purdue.cs.toydroid.soot.util.TofuDexClasses;
import edu.purdue.cs.toydroid.soot.util.TofuEntryPointConstants;
import edu.purdue.cs.toydroid.soot.util.toDex.DexPrinter;
import soot.*;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.*;
import soot.options.Options;
import soot.toolkits.scalar.Pair;
import soot.util.Chain;

import java.io.File;
import java.util.*;

public class TofuRemove {

	private String ApkFile;
	private TofuReductionAnalysis ReductionAnalysis;
	private Set<String> DexOfModified;
	private Set<SootMethod> ModifiedTopMethods;

	public TofuRemove(String apk, TofuReductionAnalysis red) {
		ApkFile = apk;
		ReductionAnalysis = red;
		DexOfModified = new HashSet<>();
		ModifiedTopMethods = new HashSet<>();
	}

	private void initSoot() {
//		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_whole_program(true);
		Options.v().set_app(true);
		//Options.v().set_process_dir(Collections.singletonList(ApkFile));
		//Options.v().set_force_android_jar(AndroidJar);
		Options.v().set_src_prec(Options.src_prec_apk);
		Options.v().set_output_format(Options.output_format_dex);
		Options.v().set_keep_line_number(false);
		Options.v().set_keep_offset(false);
		Options.v().set_soot_classpath(ApkFile);
		Options.v().set_process_multiple_dex(true);
		Options.v().set_force_overwrite(true);
		Options.v().setPhaseOption("cg", "off");
		Main.v().autoSetOptions();
		Scene.v().loadNecessaryClasses();
	}

	private void removeOnTopMethods() {
		Set<SootMethod> topMethods = ReductionAnalysis.getTopMethodsOfUIReturn();
		if (topMethods != null && !topMethods.isEmpty()) {
			for (SootMethod method : topMethods) {
				if (TofuEntryPointConstants.V4FRAGMENT_ONCREATEVIEW.equals(method.getSubSignature())) {
					Set<SootMethod> modifiedMethods = TofuWholeRemoveFragment.doRemove(method.getDeclaringClass());
					ModifiedTopMethods.addAll(modifiedMethods);
				}
			}
		}
	}

	public void removeAndWriteback() {
		System.out.println("Reload APK and Do Removal...");
		Options.v().set_force_overwrite(true);
		Options.v().set_android_api_version(23);
		removeOnTopMethods();
		Set<Pair<SootMethod, Stmt>> typedStmtInGammeOne = TypeContextFour.getTypedStmts();
		final Map<SootMethod, Set<Stmt>> toRemove = new HashMap<>();
		for (Pair<SootMethod, Stmt> stmtPair : typedStmtInGammeOne) {
			if (TypeContextFour.isRemovable(stmtPair)) {
				SootMethod method = stmtPair.getO1();
				if (ModifiedTopMethods.contains(method)) {
					continue;
				}
				Stmt stmt = stmtPair.getO2();
				String clazz = method.getDeclaringClass().getName();
				String dexOfClazz = TofuDexClasses.getDex(clazz);
				if (dexOfClazz == null) {
					continue;
				}
				System.out.println("To Remove: " + stmt + " in method: " + method);
				DexOfModified.add(dexOfClazz);
				Set<Stmt> indices = toRemove.get(method);
				if (indices == null) {
					indices = new HashSet<>();
					toRemove.put(method, indices);
				}
				indices.add(stmt);
			}
		}
		Set<Map.Entry<SootMethod, Set<Stmt>>> set = toRemove.entrySet();
		for (Map.Entry<SootMethod, Set<Stmt>> rentry : set) {
			SootMethod method = rentry.getKey();
			Set<Stmt> indices = rentry.getValue();
			remove(method, indices);
		}
		System.out.println("Starting writing to APK...");
		DexPrinter dexPrinter = new DexPrinter();
		dexPrinter.setOriginalApk(ApkFile);
		DexOfModified.addAll(TofuDexClasses.allDexClasses().values());
		dexPrinter.partitionReachableClasses(DexOfModified);
		dexPrinter.print();
		System.out.println("Done writing to APK!!!");
	}

	private void remove(SootMethod method, Set<Stmt> indices) {
		Body body = method.getActiveBody();
		Chain<Unit> chain = body.getUnits();
		for (Stmt stmt : indices) {
			if (stmt instanceof AssignStmt) {
				Value left = ((AssignStmt) stmt).getLeftOp();
				Value right = ((AssignStmt) stmt).getRightOp();
				//if (right instanceof FieldRef) {
				if (((left instanceof Local) && TofuDefUse.usedInCondOrSwitch(method, stmt, left)) ||
					(left instanceof FieldRef)) {
					//continue;
					Type retType = left.getType();
					Stmt newStmt = null;
					if (retType instanceof RefType) {
						newStmt = Jimple.v().newAssignStmt(left, NullConstant.v());
					} else if ("float".equals(retType.getEscapedName())) {
						newStmt = Jimple.v().newAssignStmt(left, FloatConstant.v(0));
					} else if ("double".equals(retType.getEscapedName())) {
						newStmt = Jimple.v().newAssignStmt(left, DoubleConstant.v(0));
					} else {
						newStmt = Jimple.v().newAssignStmt(left, IntConstant.v(0));
					}
					chain.insertAfter(newStmt, stmt);
					System.out.println("Insert: " + newStmt + " in method: " + method);
				}
				//}
				//chain.remove(stmt);
				chain.swapWith(stmt, Jimple.v().newNopStmt());
				System.out.println("Remove: " + stmt + " in method: " + method);
			} else if (stmt instanceof IdentityStmt) {
				// nothing to do
			} else {
				Stmt newStmt = null;
				if (stmt instanceof ReturnStmt) {
					LocalGenerator gen = new LocalGenerator(body);
					Type retType = method.getReturnType();
					Local local = gen.generateLocal(retType);
					if (retType instanceof RefType) {
						newStmt = Jimple.v().newReturnStmt(NullConstant.v());
					} else if ("float".equals(retType.getEscapedName())) {
						newStmt = Jimple.v().newReturnStmt(FloatConstant.v(0));
					} else if ("double".equals(retType.getEscapedName())) {
						newStmt = Jimple.v().newReturnStmt(DoubleConstant.v(0));
					} else {
						newStmt = Jimple.v().newReturnStmt(IntConstant.v(0));
					}
					chain.swapWith(stmt, newStmt);
					System.out.println("Replace: " + stmt + " in method: " + method + " \n  with: " + newStmt);
				} else {
					//chain.remove(stmt);
					chain.swapWith(stmt, Jimple.v().newNopStmt());
					System.out.println("Remove: " + stmt + " in method: " + method);
				}
			}
		}
		body.validate();
	}

	private void remove(SootMethod method, Body body, Set<Integer> indices) {
		Chain<Unit> chain = body.getUnits();
		int idx = 0;
		//important to use snapshotIterator here
		for (Iterator<Unit> iter = chain.snapshotIterator(); iter.hasNext(); ) {
			Stmt stmt = (Stmt) iter.next();
			if (!indices.contains(idx++)) {
				continue;
			}
			if (stmt instanceof AssignStmt) {
				Value left = ((AssignStmt) stmt).getLeftOp();
				Value right = ((AssignStmt) stmt).getRightOp();
				if (right instanceof FieldRef) {
					if (TofuDefUse.usedInCondOrSwitch(method, stmt, left)) {
						continue;
					}
				}
				chain.remove(stmt);
				System.out.println("Remove: " + stmt + " in method: " + method);
			} else if (stmt instanceof IdentityStmt) {
				// nothing to do
			} else {
				Stmt newStmt = null;
				if (stmt instanceof ReturnStmt) {
					LocalGenerator gen = new LocalGenerator(body);
					Type retType = method.getReturnType();
					Local local = gen.generateLocal(retType);
					if (retType instanceof RefType) {
						newStmt = Jimple.v().newAssignStmt(local, NullConstant.v());
					} else if ("float".equals(retType.getEscapedName())) {
						newStmt = Jimple.v().newAssignStmt(local, FloatConstant.v(0));
					} else if ("double".equals(retType.getEscapedName())) {
						newStmt = Jimple.v().newAssignStmt(local, DoubleConstant.v(0));
					} else {
						newStmt = Jimple.v().newAssignStmt(local, IntConstant.v(0));
					}
					chain.swapWith(stmt, newStmt);
				} else {
					chain.remove(stmt);
				}
				System.out.println("Remove: " + stmt + " in method: " + method);
			}
		}
		body.validate();
	}

}
