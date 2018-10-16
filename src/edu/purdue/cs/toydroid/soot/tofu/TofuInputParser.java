package edu.purdue.cs.toydroid.soot.tofu;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TofuInputParser {
	public static List<Entry> getTofuInput(String inputFile) throws IOException {
		List<Entry> list = new LinkedList<>();
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		String line = null;
		Entry inputEntry = null;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.isEmpty() || line.startsWith("#")) {
				inputEntry = null;
				continue;
			}
			if (line.startsWith("Context")) {
				String ctx = getValueAfterEQ(line);
				if (!ctx.isEmpty()) {
					inputEntry = new Entry(ctx);
					list.add(inputEntry);
				}
			} else if (line.startsWith("UI") && inputEntry != null) {
				String ui = getValueAfterEQ(line);
				if (!ui.isEmpty()) {
					int idxArrow = ui.indexOf("->");
					if (idxArrow < 0) {// layout only
						inputEntry.put(ui, "");
					} else if (idxArrow == 0) {// widget only
						ui = ui.substring(2).trim();
						inputEntry.put("", ui);
					} else {
						String l = ui.substring(0, idxArrow).trim();
						String w = ui.substring(idxArrow + 2).trim();
						if (w.isEmpty()) {
							inputEntry.put(l, "");//layout only
						} else {
							inputEntry.put(l, w);
						}
					}
				}
			} else if (line.startsWith("Type") && inputEntry != null) {
				String type = getValueAfterEQ(line);
				if (!type.isEmpty()) {
					inputEntry.setType(type);
				}
			}
		}
		return list;
	}

	private static String getValueAfterEQ(String line) {
		int idxEQ = line.indexOf('=');
		if (idxEQ > 0 && idxEQ + 1 < line.length()) {
			return line.substring(idxEQ + 1).trim();
		} else {
			return "";
		}
	}

	public static class Entry {
		private String context;
		private int type;
		private Map<String, String> layout2widgets;

		Entry(String ctx) {
			context = ctx;
			type = 0;
			layout2widgets = new HashMap<>();
		}

		public void setType(String tp) {
			if ("Click".equalsIgnoreCase(tp)) {
				type = 1;
			} else if ("Banner".equalsIgnoreCase(tp)) {
				type = 2;
			}
		}

		public boolean isNormalType() {
			return type == 0;
		}

		public boolean isClickType() {
			return type == 1;
		}

		public boolean isBannerAdType() {
			return type == 2;
		}

		public void put(String layouts, String widgets) {
			String w = layout2widgets.get(layouts);
			if (w != null) {
				w = w + "," + widgets;
				layout2widgets.put(layouts, w);
			} else {
				layout2widgets.put(layouts, widgets);
			}
		}

		@Override
		public String toString() {
			String str = "Context = " + context + System.lineSeparator();
			if (!layout2widgets.isEmpty()) {
				Set<Map.Entry<String, String>> set = layout2widgets.entrySet();
				for (Map.Entry<String, String> e : set) {
					String l = e.getKey();
					String w = e.getValue();
					str += "UI = " + l + " -> " + w + System.lineSeparator();
				}
			}
			switch (type) {
				case 1:
					str += "Type = Click";
					break;
				case 2:
					str += "Type = Banner";
					break;
				default:
					str += "Type = Normal";
			}
			str += System.lineSeparator();
			return str;
		}

		public String getContext() {
			return context;
		}

		public Map<String, String> getLayout2widgets() {
			return layout2widgets;
		}
	}
}
