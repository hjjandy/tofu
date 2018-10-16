package edu.purdue.cs.toydroid.soot.util;


import pxb.android.axml.AxmlVisitor;
import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlHandler;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.axml.parsers.AXML20Parser;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class TofuResParser {
	private static ARSCFileParser ArscFileParser;
	private static Map<Integer, String> Id2Name = new HashMap<>();
	private static Map<String, Integer> Name2id = new HashMap<>();
	private static Map<String, Set<String>> Layout2CustomViews = new HashMap<>();
	private static Map<String, Set<String>> LayoutIncludes = new HashMap<>();
	private static Set<String> CustomViews = new HashSet<>();

	public static void parseARSC(InputStream input) throws IOException {
		ArscFileParser = new ARSCFileParser();
		ArscFileParser.parse(input);
		List<ARSCFileParser.ResPackage> pkgs = ArscFileParser.getPackages();
		if (pkgs != null) {
			for (ARSCFileParser.ResPackage pkg : pkgs) {
				//System.out.println("Package: " + pkg.getPackageName());
				for (ARSCFileParser.ResType type : pkg.getDeclaredTypes()) {
					//System.out.println("  Type : " + type.getTypeName());
					for (ARSCFileParser.ResConfig config : type.getConfigurations()) {
						//System.out.println("    Config: " + config.toString());
						for (ARSCFileParser.AbstractResource res : config.getResources()) {
							//System.out.println("      - Res : " + res.getResourceName() + "  0x" + Integer.toHexString(res.getResourceID()));
							int id = res.getResourceID();
							if (!Id2Name.containsKey(id)) {
								String name = String.format("res/%s/%s", type.getTypeName(), res.getResourceName());
								Id2Name.put(id, name);
								Name2id.put(name, id);
							}
						}
					}
				}
			}
		}
	}

	public static void parseLayout(String layoutFile, InputStream input) throws IOException {
		AXmlHandler handler = new AXmlHandler(input, new AXML20Parser());
		parseLayout(layoutFile, handler);
	}

	private static void parseLayout(String layoutFile, AXmlHandler handler) {
		// remove ".xml"
		int len = layoutFile.length();
		String layoutRes = layoutFile.substring(0, len - 4);
		System.out.println("Parse Layout: " + layoutRes);
		AXmlNode node = handler.getDocument().getRootNode();
		List<AXmlNode> worklist = new LinkedList<>();
		worklist.add(node);
		while (!worklist.isEmpty()) {
			node = worklist.remove(0);
			if (node.getTag() == null || node.getTag().isEmpty()) {
				continue;
			}
			String tname = node.getTag().trim();
			if (tname.equals("include")) {
				parseIncludeAttributes(layoutRes, node);
			} else if (tname.contains(".")) {
				if (tname.startsWith("android.support.")) {
					continue;
				}
				System.out.println("   [Custom] " + tname);
				Set<String> customViews = Layout2CustomViews.get(layoutRes);
				if (customViews == null) {
					customViews = new HashSet<>();
					Layout2CustomViews.put(layoutRes, customViews);
				}
				customViews.add(tname);
				CustomViews.add(tname);
			}
			for (AXmlNode childNode : node.getChildren()) {
				worklist.add(childNode);
			}
		}
	}

	private static void parseIncludeAttributes(String layoutRes, AXmlNode rootNode) {
		for (Map.Entry<String, AXmlAttribute<?>> entry : rootNode.getAttributes().entrySet()) {
			String attrName = entry.getKey().trim();
			AXmlAttribute<?> attr = entry.getValue();

			if (attrName.equals("layout")) {
				if ((attr.getType() == AxmlVisitor.TYPE_REFERENCE || attr.getType() == AxmlVisitor.TYPE_INT_HEX) &&
					attr.getValue() instanceof Integer) {
					// We need to get the target XML file from the binary manifest
					ARSCFileParser.AbstractResource targetRes = ArscFileParser.findResource((Integer) attr.getValue());
					if (targetRes == null) {
						//System.err.println("Target resource " + attr.getValue() + " for layout include not found");
						return;
					}
					if (!(targetRes instanceof ARSCFileParser.StringResource)) {
						//System.err.println("Invalid target node for include tag in layout XML, was " +
						//				   targetRes.getClass().getName());
						return;
					}
					String targetFile = ((ARSCFileParser.StringResource) targetRes).getValue();
					System.out.println("   <include> " + targetFile + "   0x" + Integer.toHexString(((Integer)attr.getValue()).intValue()));
					Set<String> includes = LayoutIncludes.get(layoutRes);
					if (includes == null) {
						includes = new HashSet<>();
						LayoutIncludes.put(layoutRes, includes);
					}
					includes.add(targetFile);
				}
			}
		}
	}

	public static boolean isCustomView(String clazz) {
		return CustomViews.contains(clazz);
	}

	public static Set<String> getCustomViewsIn(String layout) {
		Set<String> cvs = new HashSet<>();
		getCustomViewsIn(layout, cvs);
		return cvs;
	}

	public static Set<String> getCustomViewsIn(int id) {
		String name = Id2Name.get(id);
		if (name != null && name.startsWith("res/layout/")) {
			return getCustomViewsIn(name);
		}
		return Collections.EMPTY_SET;
	}

	private static void getCustomViewsIn(String layout, Set<String> customViews) {
		Set<String> set = Layout2CustomViews.get(layout);
		if (set != null && customViews.addAll(set)) {
			for (String str : set) {
				getCustomViewsIn(str, customViews);
			}
		}
	}

	public static String id2Name(int id) {
		return Id2Name.get(id);
	}

	public static Integer name2Id(String name) {
		return Name2id.get(name);
	}
}
