package edu.purdue.cs.toydroid.soot.util;


import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class TofuCGReduction {
	private static Set<Edge> edgesToRemove = new HashSet<>();
	private static Set<SootMethod> visitedMethods = new HashSet<>();

	public static void reduceCallGraph(CallGraph cg, SootMethod fakeEntryMethod) {
		System.out.println("CG Reduction: Size = " + cg.size() + " [before reduction].");
		while (true) {
			int nEdges = cg.size();
			visitedMethods.clear();
			edgesToRemove.clear();
			Iterator<Edge> iterator = cg.iterator();
			while (iterator.hasNext()) {
				Edge edge = iterator.next();
				reduceCallGraph(cg, fakeEntryMethod, edge);
			}
			for (Edge edge : edgesToRemove) {
				cg.removeEdge(edge);
			}
			if (nEdges == cg.size()) {
				break;
			}
		}
		System.out.println("CG Reduction: Size = " + cg.size() + " [after reduction].");
		/*try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("cg.tofu.txt"));
			Iterator<Edge> iterator = cg.iterator();
			while (iterator.hasNext()) {
				Edge edge = iterator.next();
				String str = (edge.src() + "  ==>>  " + edge.tgt());
				writer.write(str);
				writer.newLine();
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}*/

	}

	private static void reduceCallGraph(CallGraph cg, SootMethod fakeEntryMethod, Edge edge) {
		SootMethod src = edge.src();
		if (src.equals(fakeEntryMethod) || visitedMethods.contains(src)) {
			return;
		}
		visitedMethods.add(src);
		if (TofuDexClasses.isLibClass(src.getDeclaringClass().getName()) || cg.isEntryMethod(src)) {
			Iterator<Edge> iterator = cg.edgesOutOf(src);
			while (iterator.hasNext()) {
				Edge out = iterator.next();
				//cg.removeEdge(out);
				edgesToRemove.add(out);
			}
		}
	}
}
