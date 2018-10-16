package edu.purdue.cs.toydroid.soot.util;


import java.util.*;

public class TofuDexClasses {
	private static Map<String, String> class2dex = new HashMap<>();
	private static Map<String, Set<String>> dex2class = new HashMap<>();

	public static Map<String, String> allDexClasses() {
		return class2dex;
	}

	public static int numberOfClasses() {
		return class2dex.size();
	}

	public static int numberOfDexFiles() {
		Set<String> values = new HashSet<>(class2dex.values());
		return values.size();
	}

	public static void addClasses(Collection<String> classes, String dex) {
		for (String k : classes) {
			addClass(k, dex);
		}
	}

	public static void addClass(String clazz, String dex) {
		class2dex.put(clazz, dex);
		Set<String> classes = dex2class.get(dex);
		if (classes == null) {
			classes = new HashSet<>();
			dex2class.put(dex, classes);
		}
		classes.add(clazz);
	}

	public static boolean isAppClass(String clazz) {
		return class2dex.containsKey(clazz);
	}

	public static String getDex(String clazz) {
		return class2dex.get(clazz);
	}

	public static Set<String> getClasses(String dex) {
		return dex2class.get(dex);
	}

	public static boolean isLibClass(String clazz) {
		return clazz.startsWith("android.support.") || !isAppClass(clazz);
	}
}
