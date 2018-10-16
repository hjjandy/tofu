package edu.purdue.cs.toydroid.soot.tofu;


import java.io.File;
import java.util.Arrays;
import java.util.List;

public class TofuMain {

	private static final String[] UITypes = {"normal", "banner", "click"};
	private String ApkFile = "D:\\TEST\\com.treemolabs.apps.cbsnews.apk";//"D:\\TEST\\com.aws.android.apk";//"E:\\Research\\TurningOffUIElement\\APKs\\com.singtaogroup.apk";//"D:\\TEST\\mnn.Android.apk";//
	private String InputFile = "tofu.input";
	private List<String> SetOfUITypes = Arrays.asList(UITypes);
	private String UIType = "Banner";

	public TofuMain() {

	}

	public static void main(String[] args) {
		TofuMain tofu = new TofuMain();
		tofu.parseArgs(args);
		tofu.startAnalysis();
	}

	private void parseArgs(String[] args) {
		String errorLoc = null;
		String errorMsg = null;
		boolean correctArgs = args.length == 0 ? true : false;
		if (args.length > 0 && args.length % 2 == 0) {
			for (int i = 0; i < args.length; i += 2) {
				if ("-apk".equalsIgnoreCase(args[i])) {
					ApkFile = args[i + 1];
					File file = new File(ApkFile);
					if (file.exists() && file.canRead() && !file.isDirectory() &&
						file.getName().toLowerCase().endsWith(".apk")) {
						correctArgs = true;
					} else {
						correctArgs = false;
						errorLoc = "-apk";
						errorMsg = "Invalid APK File";
						break;
					}
				} else if ("-input".equals(args[i])) {
					InputFile = args[i + 1];
					File file = new File(InputFile);
					if (file.exists() && file.canRead() && !file.isDirectory()) {
						correctArgs = true;
					} else {
						correctArgs = false;
						errorLoc = "-input";
						errorMsg = "Invalid Input File (default: tofu.input)";
						break;
					}
				} else if ("-type".equals(args[i])) {
					UIType = args[i + 1];
					String type = UIType.toLowerCase();
					if (!SetOfUITypes.contains(type)) {
						correctArgs = false;
						errorLoc = "-type";
						errorMsg = "Invalid UI Type";
					} else {
						correctArgs = true;
					}
				}
			}
		}

		if (!correctArgs) {
			printHelp(errorLoc, errorMsg);
			System.exit(0);
		}
	}

	private void printHelp(String loc, String msg) {
		System.out.println(msg + " for option [" + loc + "]");
		System.out.println("Usage:");
		System.out.println(
				"  " + this.getClass().getSimpleName() + " -apk <APK file> [-input <Input file>] [-type <UI type>]");
		System.out.println("    -input <Input file>: By default it is 'tofu.input'.");
		System.out.println("    -type <UI type>: Normal, Banner, Click. By default it is 'Normal'.");
	}

	private void startAnalysis() {
		TofuAnalysis tofuAnalysis = new TofuAnalysis(ApkFile, InputFile, UIType);
		try {
			tofuAnalysis.startAnalysis();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
