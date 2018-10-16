package edu.purdue.cs.toydroid.soot.util.toDex;

import soot.*;
import soot.jimple.IdentityStmt;
import soot.jimple.ParameterRef;
import soot.jimple.ThisRef;

import java.util.Iterator;
import java.util.Map;

/**
 * Tries may not start or end at units which have no corresponding Dalvik
 * instructions such as IdentityStmts. We reduce the traps to start at the first
 * "real" instruction. We could also use a TrapTigthener, but that would be too
 * expensive for just producing working Dex code.
 *
 * @author Steven Arzt
 */
public class FastDexTrapTightener extends BodyTransformer {
	private static FastDexTrapTightener instance_soot_toDex_FastDexTrapTightener;

	public FastDexTrapTightener(Singletons.Global g) {
	}

	public FastDexTrapTightener() {
	}

	public static FastDexTrapTightener v() {
		if (instance_soot_toDex_FastDexTrapTightener == null) {
			instance_soot_toDex_FastDexTrapTightener = new FastDexTrapTightener();
		}
		return instance_soot_toDex_FastDexTrapTightener;
	}

	@Override
	protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
		for (Iterator<Trap> trapIt = b.getTraps().snapshotIterator(); trapIt.hasNext(); ) {
			Trap t = trapIt.next();

			Unit beginUnit;
			while (!isDexInstruction(beginUnit = t.getBeginUnit()) && t.getBeginUnit() != t.getEndUnit())
				t.setBeginUnit(b.getUnits().getSuccOf(beginUnit));

			// If the trap is empty, we remove it
			if (t.getBeginUnit() == t.getEndUnit())
				trapIt.remove();
		}
	}

	private boolean isDexInstruction(Unit unit) {
		if (unit instanceof IdentityStmt) {
			IdentityStmt is = (IdentityStmt) unit;
			return !(is.getRightOp() instanceof ThisRef || is.getRightOp() instanceof ParameterRef);
		}
		return true;
	}

}
