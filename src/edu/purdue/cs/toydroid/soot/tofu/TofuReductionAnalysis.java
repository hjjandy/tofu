package edu.purdue.cs.toydroid.soot.tofu;


import edu.purdue.cs.toydroid.soot.tofu.type.*;
import edu.purdue.cs.toydroid.soot.util.TofuDexClasses;
import edu.purdue.cs.toydroid.soot.util.TofuStmtRep;
import edu.purdue.cs.toydroid.soot.util.TofuUISigConstants;
import edu.purdue.cs.toydroid.soot.util.TofuVarRep;
import soot.Hierarchy;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.scalar.Pair;
import soot.util.Chain;

import java.util.*;

public class TofuReductionAnalysis {
	private static final Integer ZERO = Integer.valueOf(0);
	private CallGraph CG;
	private Hierarchy CHA;
	private SootMethod FakeEntryMethod;
	private List<TofuInputParser.Entry> InputEntries;
	private Set<Stmt> StartingLabelsInFakeEntry;
	private List<Map<Integer, Set<Integer>>> Ids4Entries;
	private Set<TofuStmtRep> InitialUIIntroStmts;
	private TofuIdDiscover IdDiscover;
	private boolean IsBanner;
	private boolean IsClick;
	private Set<SootMethod> TopMethodsOfUIReturn;
	private Set<TofuStmtRep> TopMethodsAsDataGen;

	public TofuReductionAnalysis(CallGraph cg, Hierarchy cha, SootMethod fakeEntry,
								 List<TofuInputParser.Entry> inputEntries, Set<Stmt> startingLabelsInEntry,
								 String uiType) {
		CG = cg;
		CHA = cha;
		FakeEntryMethod = fakeEntry;
		InputEntries = inputEntries;
		StartingLabelsInFakeEntry = startingLabelsInEntry;
		InitialUIIntroStmts = new HashSet<>();
		IdDiscover = new TofuIdDiscover();
		parseUIType(uiType);
		parseInputEntries();
	}

	public boolean isBanner() {
		return IsBanner;
	}

	public boolean isClick() {
		return IsClick;
	}

	public Set<SootMethod> getTopMethodsOfUIReturn() {
		return TopMethodsOfUIReturn;
	}

	public Set<TofuStmtRep> getTopMethodsAsDataGen() {
		return TopMethodsAsDataGen;
	}

	private void parseUIType(String type) {
		if ("Banner".equalsIgnoreCase(type)) {
			IsBanner = true;
		} else if ("Click".equalsIgnoreCase(type)) {
			IsClick = true;
		}
	}

	private void parseInputEntries() {
		Ids4Entries = new LinkedList<>();
		for (TofuInputParser.Entry entry : InputEntries) {
			Map<Integer, Set<Integer>> ids4Entry = new HashMap<>();
			Map<String, String> l2w = entry.getLayout2widgets();
			Set<Map.Entry<String, String>> l2wSet = l2w.entrySet();
			for (Map.Entry<String, String> l2wEntry : l2wSet) {
				String layouts = l2wEntry.getKey();
				String widgets = l2wEntry.getValue();
				List<Integer> lIDs = parseIds(layouts);
				List<Integer> wIDs = parseIds(widgets);
				if (!lIDs.isEmpty() && !wIDs.isEmpty()) {
					for (Integer obj : lIDs) {
						Set<Integer> set = ids4Entry.get(obj);
						if (set == null) {
							set = new TreeSet<>();
							ids4Entry.put(obj, set);
						}
						set.addAll(wIDs);
					}
				}
			}
			Ids4Entries.add(ids4Entry);
		}
	}

	private List<Integer> parseIds(String str) {
		if (str.isEmpty()) {
			return Collections.singletonList(ZERO);
		} else {
			List<Integer> list = new ArrayList<>();
			String[] IDS = str.split(",");
			for (String id : IDS) {
				id = id.trim();
				int vID = 0;
				try {
					if (id.startsWith("0x")) {
						vID = Integer.parseInt(id.substring(2), 16);
					} else {
						vID = Integer.parseInt(id);
					}
				} catch (Exception e) {
					System.err.println("Invalid Resource ID: " + id);
				}
				if (vID != 0) {
					list.add(vID);
				}
			}
			return list;
		}
	}

	public void doAnalysis() {
		collectUIStmts();
		UIRelatedPass pass1 = new UIRelatedPass(CG, FakeEntryMethod, InitialUIIntroStmts, IsBanner || IsClick);
		pass1.solve();
		TopMethodsOfUIReturn = pass1.getTopMethodsOfUIReturn();
		if (!IsBanner && !IsClick) {
			// pass 2
			UIDataPass pass2 = new UIDataPass(CG, FakeEntryMethod, pass1.getDataPutLocations());
			pass2.solve();
			TopMethodsAsDataGen = pass2.getTopMethodsAsDataGen();
			// pass 3
			InputUIDataPass pass3 = new InputUIDataPass(CG, FakeEntryMethod, pass2.getDataGenLocations());
			pass3.solve();
		}
		RemovePass pass4 = new RemovePass(CG, FakeEntryMethod);
		pass4.solve();
	}


	private void collectUIStmts() {
		List<SootMethod> worklist = new LinkedList<>();
		Set<SootMethod> visited = new HashSet<>();
		for (Stmt stmt : StartingLabelsInFakeEntry) {
			Iterator<Edge> iterator = CG.edgesOutOf(stmt);
			while (iterator.hasNext()) {
				Edge edge = iterator.next();
				SootMethod tgt = edge.tgt();
				if (!visited.contains(tgt)) {
					visited.add(tgt);
					worklist.add(tgt);
				}
			}
		}
		Set<Pair<SootMethod, Stmt>> uiStmts = new HashSet<>();
		while (!worklist.isEmpty()) {
			SootMethod method = worklist.remove(0);
			collectUIStmts(method, worklist, visited, uiStmts);
		}
		visited = null;
		refineUIStmts(uiStmts);
	}

	private void collectUIStmts(SootMethod method, List<SootMethod> worklist, Set<SootMethod> visited,
								Set<Pair<SootMethod, Stmt>> uiStmts) {
		if (!method.hasActiveBody()) {
			return;
		}
		Chain<Unit> chain = method.getActiveBody().getUnits();
		for (Unit unit : chain) {
			Stmt stmt = (Stmt) unit;
			if (stmt instanceof AssignStmt && stmt.containsInvokeExpr()) {
				InvokeExpr invoke = stmt.getInvokeExpr();
				if (invoke instanceof StaticInvokeExpr) {
					continue;
				}
				SootMethod callee = invoke.getMethod();
				if (TofuUISigConstants.isTargetUISig(callee.getSignature()) ||
					TofuUISigConstants.isTargetUISubSig(callee.getSubSignature())) {
					Pair<SootMethod, Stmt> pair = new Pair<>(method, stmt);
					uiStmts.add(pair);
				}
			}
		}
		Iterator<Edge> iterator = CG.edgesOutOf(method);
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

	private void refineUIStmts(Set<Pair<SootMethod, Stmt>> uiStmts) {
		for (Pair<SootMethod, Stmt> pair : uiStmts) {
			int id = IdDiscover.discover(CG, pair.getO1(), pair.getO2(), 0);
			if (id != 0) {
				System.out.println("Found: 0x" + Integer.toHexString(id) + "  " + pair.getO2());
				inspectUIId(pair.getO1(), pair.getO2(), id);
			}
		}
	}

	private void inspectUIId(SootMethod method, Stmt stmt, int id) {
		boolean isInflate = "inflate".equals(stmt.getInvokeExpr().getMethod().getName());
		int idx = 0;
		Set<Integer> temp = new TreeSet<>();
		for (Map<Integer, Set<Integer>> ids : Ids4Entries) {
			boolean found = false;
			if (isInflate) {
				if (ids.containsKey(id)) {
					found = true;
				}
			} else {
				Collection<Set<Integer>> colls = ids.values();
				for (Set<Integer> c : colls) {
					temp.addAll(c);
				}
				if (temp.contains(id)) {
					found = true;
				}
			}
			if (found) {
				Value defValue = stmt.getDefBoxes().get(0).getValue();
				TofuStmtRep tsr = TofuStmtRep.makeTempStmtRep(method, stmt, null);
				TofuVarRep tvr = TofuVarRep.makeTempVarRep(defValue, method, stmt, null);
				if (!TypeContextOne.containsStmt(tsr)) {
//					tsr = TypeContextOne.updateStmt(method, stmt, null);
					tsr = new TofuStmtRep(method, stmt, null);
					if (tsr != null) {
						InitialUIIntroStmts.add(tsr);
					}
				}
//				if (!TypeContextOne.containsVar(tvr)) {
//					TypeContextOne.updateVar(defValue, method, stmt, null);
//				}
			}
			temp.clear();
			idx++;
		}
	}

}
