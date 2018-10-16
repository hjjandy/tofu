package edu.purdue.cs.toydroid.soot.tofu;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AnalysisConfig {
	private static String CONFIG = "TOFU.config";
	private static boolean configParsed = false;
	private static String androidJar;
	private static String otherJar;

	private static void parseConfig() throws IOException {
		if (configParsed) {
			return;
		}
		InputStream is = new FileInputStream(CONFIG);
		Properties prop = new Properties();
		prop.load(is);
		androidJar = prop.getProperty("ANDROID_JAR");
		otherJar = prop.getProperty("OTHER_JARS");
		is.close();
		configParsed = true;
	}

	public static String getAndroidJar() throws IOException {
		parseConfig();
		return androidJar;
	}

	public static String getOtherJar() throws IOException {
		parseConfig();
		return otherJar;
	}
}
