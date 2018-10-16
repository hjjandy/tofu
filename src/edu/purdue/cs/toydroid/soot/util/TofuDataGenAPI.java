package edu.purdue.cs.toydroid.soot.util;


import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

public class TofuDataGenAPI {
	private static Set<String> APIs = new HashSet<>();
	private static boolean ApiLoaded = false;
	private static String ApiFile = "tofu.datagen.api";

	private static void loadAPI() {
		if (ApiLoaded) {
			return;
		}
		ApiLoaded = true;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(ApiFile));
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				if (line.startsWith("<") && line.endsWith(">")) {
					APIs.add(line);
				} else {
					System.out.println("[DataGenAPI] Incorrect API: " + line);
				}
			}
		} catch (Exception e) {

		}
	}

	public static boolean isDataGenAPI(String sig) {
		return APIs.contains(sig);
	}
}
