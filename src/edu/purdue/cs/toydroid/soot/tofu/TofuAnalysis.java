package edu.purdue.cs.toydroid.soot.tofu;


import edu.purdue.cs.toydroid.soot.util.*;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;
import soot.util.Chain;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class TofuAnalysis {
	private String ApkFile;
	private String InputFile;
	private String UIType;
	private String AndroidJar;
	private String OtherJar;
	private List<TofuInputParser.Entry> InputEntries;
	private Set<String> InitialEntries;
	private List<String> Entrypoints;
	private Set<String> CachedEntrypoints;
	private SootMethod FakeEntryMethod;
	private CallGraph Callgraph;
	private Set<Stmt> MeaningfullCallsInFakeEntry;


	public TofuAnalysis(String apk, String input, String type) {
		ApkFile = apk;
		InputFile = input;
		UIType = type;
	}

	public void startAnalysis() throws Exception {
		AndroidJar = AnalysisConfig.getAndroidJar();
		OtherJar = AnalysisConfig.getOtherJar();
		InputEntries = TofuInputParser.getTofuInput(InputFile);
		System.out.println("Android Jar: " + AndroidJar);
		System.out.println("Other Platform Jar: " + OtherJar);
		Entrypoints = new LinkedList<>();
		InitialEntries = new HashSet<>();
		for (TofuInputParser.Entry e : InputEntries) {
			System.out.println(e);
			Entrypoints.add(e.getContext());
			InitialEntries.add(e.getContext());
		}
		CachedEntrypoints = new HashSet<>(Entrypoints);
		preLoadDexAndARSC();
		discoverEntrypoints();
		makeCallGraph();
		collectMeaningfullCallsInFakeEntry();
		TofuFieldCollector.collectFields(Callgraph, FakeEntryMethod, MeaningfullCallsInFakeEntry);
//		//TofuCGContext.buildContextForCGNodes(Callgraph, FakeEntryMethod, MeaningfullCallsInFakeEntry);
		TofuReductionAnalysis reductionAnalysis = new TofuReductionAnalysis(Callgraph, Scene.v().getActiveHierarchy(),
																			FakeEntryMethod, InputEntries,
																			MeaningfullCallsInFakeEntry, UIType);
		reductionAnalysis.doAnalysis();
		TofuRemove remove = new TofuRemove(ApkFile, reductionAnalysis);
		remove.removeAndWriteback();
	}

	private void makeCallGraph() {
		G.reset();
		initSoot(true);
		createMainMethod();
		if (!Scene.v().hasCallGraph()) {
			PackManager.v().getPack("wjpp").apply();
			PackManager.v().getPack("cg").apply();
		}
		CallGraph cg = Scene.v().getCallGraph();
		TofuCGReduction.reduceCallGraph(cg, FakeEntryMethod);
		Callgraph = cg;
	}

	private void collectMeaningfullCallsInFakeEntry() {
		SootMethod fakeEntry = FakeEntryMethod;
		Set<String> initialEntries = InitialEntries;
		Body body = fakeEntry.getActiveBody();
		Chain<Unit> chain = body.getUnits();
		Set<Stmt> startingLabels = new HashSet<>();
		for (Unit unit : chain) {
			Stmt stmt = (Stmt) unit;
			if (stmt.containsInvokeExpr()) {
				InvokeExpr expr = stmt.getInvokeExpr();
				SootMethod method = expr.getMethod();
				if (!"<init>".equals(method.getName())) {
					startingLabels.add(stmt);
				}
				if ("<init>".equals(method.getName()) &&
					initialEntries.contains(method.getDeclaringClass().getName())) {
					Value base = ((InstanceInvokeExpr) expr).getBase();
					Unit next = chain.getSuccOf(unit);
					if (next != null) {
						Iterator<Unit> iterator = chain.iterator(next);
						while (iterator.hasNext()) {
							next = iterator.next();
							Stmt nextStmt = (Stmt) next;
							if (nextStmt.containsInvokeExpr()) {
								expr = nextStmt.getInvokeExpr();
								if (expr instanceof InstanceInvokeExpr) {
									Value v = ((InstanceInvokeExpr) expr).getBase();
									if (v.equals(base) && !"<init>".equals(expr.getMethod().getName())) {
										startingLabels.add(stmt);
										break;
									}
								}
							}
						}
					}
				}
			}
		}
		MeaningfullCallsInFakeEntry = startingLabels;
	}

	private void createMainMethod() {
		TofuEntryPointCreator entryPointCreator = new TofuEntryPointCreator(Entrypoints);
		SootMethod entryPoint = entryPointCreator.createDummyMain();
		Scene.v().setEntryPoints(Collections.singletonList(entryPoint));
		if (Scene.v().containsClass(entryPoint.getDeclaringClass().getName()))
			Scene.v().removeClass(entryPoint.getDeclaringClass());
		Scene.v().addClass(entryPoint.getDeclaringClass());
		entryPoint.getDeclaringClass().setApplicationClass();
		FakeEntryMethod = entryPoint;
	}

	private String getClasspath() {
		String cp = AndroidJar;
		if (OtherJar != null) {
			cp += File.pathSeparator + OtherJar;
		}
		return cp;
	}

	private void discoverEntrypoints() {
		boolean hasChanged = true;
		int pass = 1;
		while (hasChanged) {
			int nEP = CachedEntrypoints.size();
			hasChanged = false;
			System.out.println("Collect Entrypoint [PASS-" + (pass++) + "]");
			G.reset();
			initSoot(true);
			createMainMethod();
			PackManager.v().getPack("wjpp").apply();
			PackManager.v().getPack("cg").apply();
			PackManager.v().getPack("wjtp").apply();
			CallGraph cg = Scene.v().getCallGraph();
			collectNewComponent(cg);
			System.out.println(
					"  CG Size: " + cg.size() + "; Newly Discovered Entrypoints: " + (CachedEntrypoints.size() - nEP));
			if (CachedEntrypoints.size() > nEP) {
				hasChanged = true;
			}
		}

	}

	private void collectNewComponent(CallGraph cg) {
		List<SootMethod> worklist = new LinkedList<>();
		Set<SootMethod> visited = new HashSet<>();
		Iterator<Edge> iterator = cg.iterator();
		while (iterator.hasNext()) {
			Edge edge = iterator.next();
			SootMethod src = edge.src();
			SootMethod tgt = edge.tgt();
			if (!src.equals(FakeEntryMethod) && !TofuDexClasses.isLibClass(src.getDeclaringClass().getName()) &&
				visited.add(src)) {
				worklist.add(src);
			}
			if (!TofuDexClasses.isLibClass(tgt.getDeclaringClass().getName()) && visited.add(tgt)) {
				worklist.add(tgt);
			}
		}
		while (!worklist.isEmpty()) {
			SootMethod method = worklist.remove(0);
			if (method.hasActiveBody()) {
				Body body = method.getActiveBody();
				Chain<Unit> chain = body.getUnits();
				for (Unit unit : chain) {
					Stmt stmt = (Stmt) unit;
					if (stmt.containsInvokeExpr()) {
						InvokeExpr invoke = stmt.getInvokeExpr();
						int nArgs = invoke.getArgCount();
						for (int i = 0; i < nArgs; i++) {
							Value arg = invoke.getArg(i);
							if (arg instanceof IntConstant) {
								int varg = ((IntConstant) arg).value;
								if (Integer.toHexString(varg).startsWith("7f")) {
									//System.out.println("AAAAAAA 0x" + Integer.toHexString(varg));
									Set<String> customViews = TofuResParser.getCustomViewsIn(varg);
									for (String cv : customViews) {
										if (!TofuDexClasses.isLibClass(cv) && CachedEntrypoints.add(cv)) {
											Entrypoints.add(cv);
											System.out.println(
													" Entrypoint found custom view: [0x" + Integer.toHexString(varg) +
													"]  " + cv);
										}
									}
								}
							}
						}
					} else if (unit instanceof AssignStmt) {
						AssignStmt assign = (AssignStmt) unit;
						Value right = assign.getRightOp();
						if (right instanceof NewExpr) {
							//System.out.println(unit);
							NewExpr newExpr = (NewExpr) right;
							RefType type = newExpr.getBaseType();
							if (type.hasSootClass()) {
								SootClass clazz = type.getSootClass();
								if (!TofuDexClasses.isLibClass(clazz.getName()) && isComponentClass(clazz) &&
									!CachedEntrypoints.contains(clazz.getName())) {
									CachedEntrypoints.add(clazz.getName());
									Entrypoints.add(clazz.getName());
								}
							}
						}
					}
				}
			}
		}
	}

	private boolean isComponentClass(SootClass sc) {
		Set<SootClass> interfaces = new HashSet<>();
		List<SootClass> extendedClasses = Scene.v().getActiveHierarchy().getSuperclassesOf(sc);
		interfaces.addAll(sc.getInterfaces());
		for (SootClass sp : extendedClasses) {
			if (TofuEntryPointConstants.isLifecycleClass(sp.getName())) {
				return true;
			}
			interfaces.addAll(sp.getInterfaces());
		}
		Set<SootClass> superInterfaces = new HashSet<>();
		Set<SootClass> temp = new HashSet<>(interfaces);
		while (true) {
			for (SootClass interf : temp) {
				superInterfaces.addAll(Scene.v().getActiveHierarchy().getSuperinterfacesOf(interf));
			}
			temp.addAll(superInterfaces);
			temp.removeAll(interfaces);
			if (temp.isEmpty()) {
				break;
			}
			interfaces.addAll(superInterfaces);
		}
		superInterfaces = null;
		temp = null;
		for (SootClass sp : interfaces) {
			if (TofuEntryPointConstants.isLifecycleClass(sp.getName())) {
				return true;
			}
		}
		return false;
	}

	private void preLoadDexAndARSC() {
		ZipFile archive = null;
		try {
			archive = new ZipFile(ApkFile);
			for (Enumeration<? extends ZipEntry> entries = archive.entries(); entries.hasMoreElements(); ) {
				ZipEntry entry = entries.nextElement();
				String entryName = entry.getName();
				if (entryName.endsWith(".dex") && entryName.startsWith("classes")) {
					Set<String> classes = DexClassProvider.classesOfDex(new File(ApkFile), entryName);
					if (classes != null && !classes.isEmpty()) {
						TofuDexClasses.addClasses(classes, entryName);
					}
				} else if (entryName.equals("resources.arsc")) {
					TofuResParser.parseARSC(archive.getInputStream(entry));
				}
			}
			for (Enumeration<? extends ZipEntry> entries = archive.entries(); entries.hasMoreElements(); ) {
				ZipEntry entry = entries.nextElement();
				String entryName = entry.getName();
				if (entryName.startsWith("res/layout/") && entryName.endsWith(".xml")) {
					TofuResParser.parseLayout(entryName, archive.getInputStream(entry));
				}
			}
			System.out.println("#Dex Files: " + TofuDexClasses.numberOfDexFiles());
		} catch (IOException e) {
			throw new CompilationDeathException("Error reasing archive '" + ApkFile + "'", e);
		} finally {
			try {
				if (archive != null)
					archive.close();
			} catch (Throwable t) {
			}
		}
	}

	public void initSoot(boolean constructCallgraph) {
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_whole_program(true);
		Options.v().set_process_dir(Collections.singletonList(ApkFile));
		Options.v().set_force_android_jar(AndroidJar);
		Options.v().set_src_prec(Options.src_prec_apk_class_jimple);
		Options.v().set_output_format(Options.output_format_dex);
		Options.v().set_keep_line_number(false);
		Options.v().set_keep_offset(false);
		Options.v().set_soot_classpath(getClasspath());
		Options.v().set_process_multiple_dex(true);
		Main.v().autoSetOptions();
		if (constructCallgraph) {
			Options.v().setPhaseOption("cg.spark", "on");
		}
		Scene.v().loadNecessaryClasses();
//		Chain<SootClass> allClasses = Scene.v().getClasses();
//		for (SootClass sc : allClasses) {
//			String scName = sc.getName();
//			if (scName.startsWith("android.support.") || !TofuDexClasses.isAppClass(scName)) {
//				sc.setLibraryClass();
//			}
//		}
	}
}
