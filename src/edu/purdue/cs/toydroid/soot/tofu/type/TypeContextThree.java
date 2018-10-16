package edu.purdue.cs.toydroid.soot.tofu.type;


import edu.purdue.cs.toydroid.soot.util.TofuCtxRep;
import edu.purdue.cs.toydroid.soot.util.TofuStmtRep;
import edu.purdue.cs.toydroid.soot.util.TofuVarRep;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class TypeContextThree {
	private static final Pair<SootMethod, Stmt> TempPair = new Pair<>();
	// maintain the records of types for datagen api
	private static Map<Pair, Integer> DataGen2Type = new HashMap<>();
	private static Map<Integer, Pair> Type2DataGen = new HashMap<>();
	// in Gamma3, var and stmt can be typed with more than one types.
	private static Map<TofuStmtRep, Set<Integer>> GammaStmt = new HashMap<>();
	private static Map<TofuVarRep, Set<Integer>> GammaVar = new HashMap<>();

	public static Integer findOrCreateTypeOfDataGen(SootMethod method, Stmt stmt) {
		TempPair.setPair(method, stmt);
		Integer type = DataGen2Type.get(TempPair);
		if (type == null) {
			type = TypeSystem.newInputUIDataType();
			Pair<SootMethod, Stmt> pair = new Pair<>(method, stmt);
			DataGen2Type.put(pair, type);
			Type2DataGen.put(type, pair);
		}
		return type;
	}

	public static Integer typeOfDataGen(SootMethod method, Stmt stmt) {
		TempPair.setPair(method, stmt);
		Integer type = DataGen2Type.get(TempPair);
		return type;
	}

	public static Pair<SootMethod, Stmt> locationsOfDataGenForType(Integer type) {
		return Type2DataGen.get(type);
	}

	public static Set<Integer> getType(TofuStmtRep stmtRep) {
		return GammaStmt.get(stmtRep);
	}

	public static Set<Integer> getType(TofuVarRep varRep) {
		return GammaVar.get(varRep);
	}

	public static boolean containsStmt(TofuStmtRep stmt, Integer type) {
		Set<Integer> types = GammaStmt.get(stmt);
		if (types != null) {
			return types.contains(type);
		}
		return false;
	}

	public static boolean containsStmt(TofuStmtRep stmt, Set<Integer> types) {
		Set<Integer> existingTypes = GammaStmt.get(stmt);
		if (existingTypes != null) {
			return existingTypes.containsAll(types);
		}
		return false;
	}

	public static boolean containsVar(TofuVarRep varRep, Integer type) {
		Set<Integer> types = GammaVar.get(varRep);
		if (types != null) {
			return types.contains(type);
		}
		return false;
	}

	public static boolean containsVar(TofuVarRep varRep, Set<Integer> types) {
		Set<Integer> existingTypes = GammaVar.get(varRep);
		if (existingTypes != null) {
			return existingTypes.containsAll(types);
		}
		return false;
	}

	public static TofuVarRep updateVar(TofuVarRep varRep, Integer type) {
		Set<Integer> existingTypes = GammaVar.get(varRep);
		if (existingTypes == null) {
			existingTypes = TypeCache.getTypes(type);
			GammaVar.put(varRep, existingTypes);
			return varRep;
		}
		if (!existingTypes.contains(type)) {
			existingTypes = TypeCache.getTypes(existingTypes, type);
			GammaVar.put(varRep, existingTypes);
			return varRep;
		}
		return null;
	}

	public static TofuVarRep updateVar(TofuVarRep varRep, Set<Integer> types) {
		Set<Integer> existingTypes = GammaVar.get(varRep);
		if (existingTypes == null) {
			existingTypes = TypeCache.getTypes(types);
			GammaVar.put(varRep, existingTypes);
			return varRep;
		}
		if (!existingTypes.containsAll(types)) {
			existingTypes = TypeCache.getTypes(existingTypes, types);
			GammaVar.put(varRep, existingTypes);
			return varRep;
		}
		return null;
	}

	public static TofuVarRep updateVar(Value var, SootMethod method, Stmt def, TofuCtxRep ctx, Integer type) {
		TofuVarRep varRep = TofuVarRep.makeTempVarRep(var, method, def, ctx);
		if (!GammaVar.containsKey(varRep)) {
			varRep = new TofuVarRep(var, method, def, ctx);
		}
		return updateVar(varRep, type);
	}

	/**
	 * Update the type context for corresponding variable. The return value can be either a new object or a temp object.
	 * Thus do NOT use the return value for insertion operations. Only use it to retrieve information from the context.
	 *
	 * @param var
	 * @param method
	 * @param def
	 * @param ctx
	 * @param types
	 * @return A representation of TofuVarRep.
	 */
	public static TofuVarRep updateVar(Value var, SootMethod method, Stmt def, TofuCtxRep ctx, Set<Integer> types) {
		TofuVarRep varRep = TofuVarRep.makeTempVarRep(var, method, def, ctx);
		if (!GammaVar.containsKey(varRep)) {
			varRep = new TofuVarRep(var, method, def, ctx);
		}
		return updateVar(varRep, types);
	}

	public static TofuStmtRep updateStmt(TofuStmtRep stmtRep, Integer type) {
		Set<Integer> existingTypes = GammaVar.get(stmtRep);
		if (existingTypes == null) {
			existingTypes = TypeCache.getTypes(type);
			GammaStmt.put(stmtRep, existingTypes);
			return stmtRep;
		}
		if (!existingTypes.contains(type)) {
			existingTypes = TypeCache.getTypes(existingTypes, type);
			GammaStmt.put(stmtRep, existingTypes);
			return stmtRep;
		}
		return null;
	}

	public static TofuStmtRep updateStmt(TofuStmtRep stmtRep, Set<Integer> types) {
		Set<Integer> existingTypes = GammaVar.get(stmtRep);
		if (existingTypes == null) {
			existingTypes = TypeCache.getTypes(types);
			GammaStmt.put(stmtRep, existingTypes);
			return stmtRep;
		}
		if (!existingTypes.containsAll(types)) {
			existingTypes = TypeCache.getTypes(existingTypes, types);
			GammaStmt.put(stmtRep, existingTypes);
			return stmtRep;
		}
		return null;
	}

	public static TofuStmtRep updateStmt(SootMethod method, Stmt stmt, TofuCtxRep ctx, Integer type) {
		TofuStmtRep stmtRep = TofuStmtRep.makeTempStmtRep(method, stmt, ctx);
		if (!GammaStmt.containsKey(stmtRep)) {
			stmtRep = new TofuStmtRep(method, stmt, ctx);
		}
		return updateStmt(stmtRep, type);
	}

	/**
	 * Update the type context for corresponding stmt. The return value can be either a new object or a temp object.
	 * Thus do NOT use the return value for insertion operations. Only use it to retrieve information from the context.
	 *
	 * @param method
	 * @param stmt
	 * @param ctx
	 * @param types
	 * @return A representation of TofuStmtRep..
	 */
	public static TofuStmtRep updateStmt(SootMethod method, Stmt stmt, TofuCtxRep ctx, Set<Integer> types) {
		TofuStmtRep stmtRep = TofuStmtRep.makeTempStmtRep(method, stmt, ctx);
		if (!GammaStmt.containsKey(stmtRep)) {
			stmtRep = new TofuStmtRep(method, stmt, ctx);
		}
		return updateStmt(stmtRep, types);
	}

	public static Map<TofuStmtRep, Set<Integer>> getAllStmts() {
		return GammaStmt;
	}

	private static class TypeCache {
		private static Map<String, Set<Integer>> CachedTypes = new HashMap<>();
		private static StringBuilder Builder = new StringBuilder();
		private static Set<Integer> TempSet = new TreeSet<>();
		private static Set<Integer> SingTempSet = new TreeSet<>();

		static String getRep(Set<Integer> types) {
			int size = types.size();
			int idx = 1;
			Builder.setLength(0);
			for (Integer type : types) {
				Builder.append(type.toString());
				if (idx < size) {
					Builder.append(',');
				}
				idx++;
			}
			return Builder.toString();
		}

		static String getRep(Set<Integer> types1, Set<Integer> types2) {
			TempSet.clear();
			TempSet.addAll(types1);
			TempSet.addAll(types2);
			return getRep(TempSet);
		}

		static Set<Integer> getTypes(Integer type) {
			String rep = type.toString();
			Set<Integer> existingTypes = CachedTypes.get(rep);
			if (existingTypes == null) {
				existingTypes = new TreeSet<>();
				existingTypes.add(type);
				CachedTypes.put(rep, existingTypes);
			}
			return existingTypes;
		}

		static Set<Integer> getTypes(Set<Integer> types) {
			String rep = getRep(types);
			Set<Integer> existingTypes = CachedTypes.get(rep);
			if (existingTypes == null) {
				existingTypes = new TreeSet<>();
				existingTypes.addAll(types);
				CachedTypes.put(rep, existingTypes);
			}
			return existingTypes;
		}

		static Set<Integer> getTypes(Set<Integer> types, Integer type) {
			SingTempSet.clear();
			SingTempSet.add(type);
			String rep = getRep(types, SingTempSet);
			Set<Integer> existingTypes = CachedTypes.get(rep);
			if (existingTypes == null) {
				existingTypes = new TreeSet<>();
				existingTypes.addAll(types);
				existingTypes.add(type);
				CachedTypes.put(rep, existingTypes);
			}
			return existingTypes;
		}

		static Set<Integer> getTypes(Set<Integer> types1, Set<Integer> types2) {
			String rep = getRep(types1, types2);
			Set<Integer> existingTypes = CachedTypes.get(rep);
			if (existingTypes == null) {
				existingTypes = new TreeSet<>();
				existingTypes.addAll(types1);
				existingTypes.addAll(types2);
				CachedTypes.put(rep, existingTypes);
			}
			return existingTypes;
		}
	}

}
