package edu.purdue.cs.toydroid.soot.util;


import java.util.HashSet;
import java.util.Set;

public class TofuUISigConstants {
	public static final String Activity_findViewById = "<android.app.Activity: android.view.View findViewById(int)>";
	public static final String View_findViewById = "<android.app.View: android.view.View findViewById(int)>";
	// android.view.LayoutInflater.inflate(ILandroid/view/ViewGroup;)Landroid/view/View;
	// android.view.LayoutInflater.inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;
	public static final String Inflate_1 = "<android.view.LayoutInflater: android.view.View inflate(int,android.view.ViewGroup)>";
	public static final String Inflate_2 = "<android.view.LayoutInflater: android.view.View inflate(int,android.view.ViewGroup,boolean)>";

	public static final String FindViewById_Sub = "android.view.View findViewById(int)";
	public static final String Inflate_Sub_1 = "android.view.View inflate(int,android.view.ViewGroup)";
	public static final String Inflate_Sub_2 = "android.view.View inflate(int,android.view.ViewGroup,boolean)";

	public static final String UIDataPut_Start_1 = "set";
	public static final String UIDataPut_End_1 = "Text";
	public static final String UIDataPut_End_2 = "Drawable";
	public static final String UIDataPut_End_3 = "Image";

	private static Set<String> AllSig = new HashSet<>();
	private static Set<String> AllSubSig = new HashSet<>();

	private static Set<String> UIDataPutStarts = new HashSet<>();
	private static Set<String> UIDataPutEnds = new HashSet<>();

	static {
		AllSig.add(Activity_findViewById);
		AllSig.add(View_findViewById);
		AllSig.add(Inflate_1);
		AllSig.add(Inflate_2);
		AllSubSig.add(FindViewById_Sub);
		AllSubSig.add(Inflate_Sub_1);
		AllSubSig.add(Inflate_Sub_2);
		UIDataPutStarts.add(UIDataPut_Start_1);
		UIDataPutEnds.add(UIDataPut_End_1);
		UIDataPutEnds.add(UIDataPut_End_2);
		UIDataPutEnds.add(UIDataPut_End_3);
	}

	public static boolean isTargetUISig(String sig) {
		return AllSig.contains(sig);
	}

	public static boolean isTargetUISubSig(String subsig) {
		return AllSubSig.contains(subsig);
	}

	public static boolean isUIDataPutMethod(String mname) {
		int idx = 0;
		int leng = mname.length();
		int start0 = 0, start1 = -1, end0 = -1;
		for (idx = 0; idx < leng; idx++) {
			if (Character.isUpperCase(mname.charAt(idx))) {
				start1 = idx;
				break;
			}
		}
		for (idx = leng - 1; idx >= 0; idx--) {
			if (Character.isUpperCase(mname.charAt(idx))) {
				end0 = idx;
				break;
			}
		}
		if (start1 > start0 && end0 > 0) {
			String s = mname.substring(start0, start1);
			String e = mname.substring(end0);
			//System.out.println(s + "   " + e);
			if (UIDataPutStarts.contains(s) && UIDataPutEnds.contains(e)) {
				return true;
			}
		}
		return false;
	}
}
