import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import SharedDS.PageInfo;
import SharedDS.UtilFuncs;

class UserHandler extends DefaultHandler {

	boolean pageFound = false;
	boolean pageIdFound = false;
	boolean pageIdSet = false;
	boolean titleFound = false;
	boolean bodyFound = false;
	private int curlyBracesAfterInfoBox = 0;

	private int pageId = -1;

	HashSet<String> tokensInTitle = new HashSet<String>();
	String infoText;
	private HashMap<String, Integer> infoboxMap = new HashMap<String, Integer>();
	private HashMap<String, Integer> categoryMap = new HashMap<String, Integer>();
	private HashMap<String, Integer> refMap = new HashMap<String, Integer>();
	private HashMap<String, Integer> extLinksMap = new HashMap<String, Integer>();
	private StringBuilder str = new StringBuilder();
	private StringBuilder temp = new StringBuilder();
	public FileWriter outFile;
	public TreeMap<String, TreeMap<Integer, PageInfo>> tokenTree;

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		str.setLength(0);

		if (qName.equalsIgnoreCase("page")) {
			infoboxMap.clear();
			categoryMap.clear();
			refMap.clear();
			extLinksMap.clear();
			pageIdFound = pageIdSet = false;
			pageFound = true;
		} else if (qName.equalsIgnoreCase("id") && pageIdFound == false) {
			pageIdFound = true;
		} else if (qName.equalsIgnoreCase("title")) {
			tokensInTitle.clear();
			titleFound = true;
		} else if (qName.equalsIgnoreCase("text")) {
			bodyFound = true;
		}
	}

	@Override
	public void characters(char ch[], int start, int length) throws SAXException {

		if (titleFound)
			str.append(new String(ch, start, length));
		else if (pageFound && pageIdFound && !pageIdSet)
			str.append(new String(ch, start, length));
		else if (bodyFound)
			str.append(new String(ch, start, length));
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {

		// If the Current Page is parsed, reset all the Flags and Counters.
		if (qName.equalsIgnoreCase("page")) {
			pageFound = pageIdSet = pageIdFound = false;
			curlyBracesAfterInfoBox = 0;
			return;
		}

		if (pageIdFound && !pageIdSet && qName.equalsIgnoreCase("id")) {
			// If the current element is PageId, then map the tokens in the
			// Title, to the PageId and
			// Increment the title-tokens' titleFrequeny by 1
			pageId = Integer.parseInt(str.toString().trim());
			pageIdSet = true;
			PageInfo pi = null;
			TreeMap<Integer, PageInfo> piHastTable;
			for (String str : tokensInTitle) {
				if (tokenTree.containsKey(str.toString().trim())) {
					piHastTable = tokenTree.get(str.toString().trim());
					pi = piHastTable.get(pageId);
					if (pi == null) {
						pi = new PageInfo();
						pi.titleFrequeny = 1;
						piHastTable.put(pageId, pi);
					} else
						pi.titleFrequeny++;
				} else {
					piHastTable = new TreeMap<Integer, PageInfo>();
					pi = new PageInfo();
					pi.titleFrequeny = 1;
					piHastTable.put(pageId, pi);
					tokenTree.put(str.toString().trim(), piHastTable);
				}
			}
			return;

		}

		//
		String s1 = str.toString().toLowerCase();
		String s = s1;

		// Extract References, Categories and Infobox content
		if (qName.equalsIgnoreCase("text")) {
			// Extract refs.
			temp.setLength(0);
			s = s1.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&quot;", " ");

			int i = 0, n = s.length(), nextIndex = 0, endIndex = 0;
			while (i < n && (nextIndex = s.indexOf("<ref>", nextIndex)) > -1) {
				nextIndex += 5;
				endIndex = s.indexOf("</ref>", nextIndex);
				if (endIndex == -1 || nextIndex >= n)
					break;
				temp.append(s.substring(nextIndex, endIndex).replaceAll("(&lt;)|(&gt;)|[^a-z0-9]+", " "));
				nextIndex = endIndex;
			}

			ArrayList<String> refTokens = UtilFuncs
					.getTokensAsList(temp.toString().replaceAll("(&lt;)|(&gt;)|[^a-z0-9]+", " "), " ");

			temp.setLength(0);
			for (String item : refTokens)
				if (refMap.containsKey(item))
					refMap.put(item, refMap.get(item) + 1);
				else
					refMap.put(item, 1);

			if (pageId == 6201)
				pageId = 6201;

			// Extract External Links
			i = s.indexOf("==external links==") + 18;
			int categoryStartsAt = UtilFuncs.indexOf("\\[\\[category", s1.toString());
			if (i != 17 && i < categoryStartsAt) {

				temp.setLength(0);
				String link = null;
				while (i < categoryStartsAt) {
					while ( i < categoryStartsAt && s.charAt(i) != '[')
						i++;
					i++;
					while (i < categoryStartsAt && s.charAt(i) != ']')
						temp.append(s.charAt(i++));
					temp.append(" ");
				}

				ArrayList<String> extLinksTokens = UtilFuncs
						.getTokensAsList(temp.toString().replaceAll("[^a-z0-9]+|(www)|(http:s?)", " "), " ");

				temp.setLength(0);
				for (String item : extLinksTokens)
					if (extLinksMap.containsKey(item))
						extLinksMap.put(item, extLinksMap.get(item) + 1);
					else
						extLinksMap.put(item, 1);
			}

			// Extract Categories
			if (categoryStartsAt != -1) {
				for (i = categoryStartsAt; i < n; i++) {
					while (i < n && s1.charAt(i) != ':' && s1.charAt(i) != ']')
						i++;

					endIndex = i;
					while (endIndex < n && s1.charAt(endIndex) != ']')
						endIndex++;

					if (i >= n || s1.charAt(i) == ']')
						break;

					ArrayList<String> catTokens = UtilFuncs
							.getTokensAsList(s1.substring(i, endIndex).replaceAll("[^a-z0-9]+", " "), " ");
					for (String t : catTokens) {
						if (categoryMap.containsKey(t))
							categoryMap.put(t, categoryMap.get(t) + 1);
						else
							categoryMap.put(t, 1);
					}
					i = endIndex + 1;
				}
			}

			s = s.replaceAll("[^A-Za-z0-9]+", " ");
			infoText = null;

			// Extract Infobox content
			n = s1.length();
			int infoboxStartsAt = UtilFuncs.indexOf("\\{\\{infobox", s1.toString());
			int infoboxEndsAt = 0;
			if (infoboxStartsAt != -1) {
				for (i = infoboxStartsAt; i < n - 1; i++) {
					if (s1.charAt(i) == '{' && s1.charAt(i + 1) == '{')
						curlyBracesAfterInfoBox++;
					else if (s1.charAt(i) == '}' && s1.charAt(i + 1) == '}')
						curlyBracesAfterInfoBox--;
					if (curlyBracesAfterInfoBox == 0) {
						infoboxEndsAt = i;
						break;
					}
				}
				int len = (new String("{{infobox")).length();
				infoText = s1.substring(infoboxStartsAt + len, infoboxEndsAt).replaceAll("\\n|[a-z ]+=", " ")
						.replaceAll("[^a-z0-9]+", " ");

				ArrayList<String> infoBoxTokens = UtilFuncs.getTokensAsList(infoText.replaceAll("[^a-z0-9]+", " "),
						" ");
				for (String t : infoBoxTokens) {
					if (infoboxMap.containsKey(t))
						infoboxMap.put(t, infoboxMap.get(t) + 1);
					else
						infoboxMap.put(t, 1);
				}
				infoBoxTokens.clear();
			}

		}

		ArrayList<String> strList = UtilFuncs.getTokensAsList(s.replaceAll("[^a-z0-9]+", " "), " ");

		for (String str : strList) {
			str = str.trim();
			PageInfo pi = null;
			TreeMap<Integer, PageInfo> piHastTable;

			// Update the word frequency in the text/body.
			if (tokenTree.containsKey(str.toString().trim())) {
				piHastTable = tokenTree.get(str.toString().trim());
				pi = piHastTable.get(pageId);
				if (pi == null) {
					pi = new PageInfo();
					pi.frequency = 1;
					piHastTable.put(pageId, pi);
				} else
					pi.frequency++;
			} else {
				piHastTable = new TreeMap<Integer, PageInfo>();
				pi = new PageInfo();
				pi.frequency = 1;
				piHastTable.put(pageId, pi);
				if (qName.equalsIgnoreCase("title"))
					tokensInTitle.add(str);
				else
					tokenTree.put(str.toString().trim(), piHastTable);
			}

			if (qName.equalsIgnoreCase("text")) {
				if (pi.infoboxFrequeny == 0 && infoboxMap.containsKey(str)) {
					pi.infoboxFrequeny = infoboxMap.get(str);
				}
				if (pi.categoryFrequeny == 0 && categoryMap.containsKey(str)) {
					pi.categoryFrequeny = categoryMap.get(str);
				}
				if (pi.refFrequeny == 0 && refMap.containsKey(str)) {
					pi.refFrequeny = refMap.get(str);
				}
				if (pi.extLinkFrequeny == 0 && extLinksMap.containsKey(str)) {
					pi.extLinkFrequeny = extLinksMap.get(str);
				}
				piHastTable.put(pageId, pi);
			}
		}
	}

	public void endDocument() throws SAXException {

		for (Map.Entry<String, TreeMap<Integer, PageInfo>> entry : tokenTree.entrySet()) {
			String key = entry.getKey();

			// Stop Words elimination
			if (key.length() <= 2)
				continue;

			try {
				TreeMap<Integer, PageInfo> ht = entry.getValue();
				Set<Integer> keys = ht.keySet();

				// Eliminate words with frequency <=1
				if (keys.size() <= 1)
					continue;

				outFile.write("\n" + key + ":");

				for (Integer pageId : keys) {
					PageInfo pi = ht.get(pageId);
					outFile.write("|" + pageId + "-" + pi.frequency
							+ (pi.categoryFrequeny > 0 ? "C" + pi.categoryFrequeny : "")
							+ (pi.infoboxFrequeny > 0 ? "I" + pi.infoboxFrequeny : "")
							+ (pi.titleFrequeny > 0 ? "T" + pi.titleFrequeny : "")
							+ (pi.refFrequeny > 0 ? "R" + pi.refFrequeny : "")
							+ (pi.extLinkFrequeny > 0 ? "E" + pi.extLinkFrequeny : ""));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			outFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}