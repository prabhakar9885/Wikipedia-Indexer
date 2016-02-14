import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import SharedDS.PageInfo;
import SharedDS.UtilFuncs;

class UserHandler extends DefaultHandler {

	private static final int BLOCK_SIZE = 10000;
	boolean pageFound = false;
	boolean pageIdFound = false;
	boolean pageIdSet = false;
	boolean titleFound = false;
	boolean bodyFound = false;
	private int curlyBracesAfterInfoBox = 0;

	private int pageId = -1, countOfDocsForBlock = 0, fileIndex = 0;

	public int docsCount = 0;

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
	public String OutFileName;

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		str.setLength(0);

		if (qName.equalsIgnoreCase("page")) {
			countOfDocsForBlock++;
			docsCount++;
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
			addTokensInTitleToPageInfoTreeMap(tokensInTitle);
			return;
		}

		String s1 = str.toString().toLowerCase();
		String s = s1;

		/*
		 * Extract References, Categories and Infobox content
		 */
		if (qName.equalsIgnoreCase("text")) {

			/*
			 * Extract refs.
			 */
			temp.setLength(0);
			int i = 0, n = s.length(), nextIndex = 0, endIndex = 0;
			s = s1;
			try {
				while (i < n && (nextIndex = s.indexOf("<ref>", nextIndex)) > -1) {
					nextIndex += 5;
					endIndex = s.indexOf("</ref>", nextIndex);
					if (endIndex == -1 || nextIndex >= n)
						break;
					temp.append(s.substring(nextIndex, endIndex).replaceAll("[^a-z0-9]+", " "));
					nextIndex = endIndex;
				}
				fillMapWithTokens(refMap, temp.toString(), "[^a-z0-9]+");
				temp.setLength(0);
			} catch (Exception ex) {
				System.out.println("Eternal Ref Exception: " + ex.getMessage());
				System.out.println("DocId: " + pageId);
				System.out.println("Stack trace" + ex.getStackTrace());
			}

			/*
			 * Extract External Links
			 */
			i = s.indexOf("==external links==") + 18;
			int categoryStartsAt = UtilFuncs.indexOf("\\[\\[category", s1.toString());
			try {
				if (i != 17 && i < categoryStartsAt) {
					temp.setLength(0);
					while (i < categoryStartsAt) {
						while (i < categoryStartsAt && s.charAt(i) != '[')
							i++;
						i++;
						while (i < categoryStartsAt && s.charAt(i) != ']')
							temp.append(s.charAt(i++));
						temp.append(" ");
					}
					fillMapWithTokens(extLinksMap, temp.toString(), "[^a-z0-9]+|(www)|(http:s?)");
					temp.setLength(0);
				}
			} catch (Exception ex) {
				System.out.println("Eternal Ref Exception: " + ex.getMessage());
				System.out.println("DocId: " + pageId);
				System.out.println("Stack trace" + ex.getStackTrace());
			}

			/*
			 * Extract Categories
			 */
			try {
				if (categoryStartsAt != -1) {
					for (i = categoryStartsAt; i < n; i++) {
						while (i < n && s1.charAt(i) != ':' && s1.charAt(i) != ']')
							i++;
						endIndex = i;
						while (endIndex < n && s1.charAt(endIndex) != ']')
							endIndex++;
						if (i >= n || s1.charAt(i) == ']')
							break;
						fillMapWithTokens(categoryMap, s1.substring(i, endIndex), "[^a-z0-9]+");
						i = endIndex + 1;
					}
				}
			} catch (Exception ex) {
				System.out.println("Eternal Ref Exception: " + ex.getMessage());
				System.out.println("DocId: " + pageId);
				System.out.println("Stack trace" + ex.getStackTrace());
			}
			s = s.replaceAll("[^A-Za-z0-9]+", " ");
			infoText = null;

			/*
			 * Extract Infobox content
			 */
			n = s1.length();
			try {
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
					if (infoboxStartsAt + len < infoboxEndsAt) {
						infoText = s1.substring(infoboxStartsAt + len, infoboxEndsAt).replaceAll("\\n|[a-z ]+=", " ")
								.replaceAll("[^a-z0-9]+", " ");

						fillMapWithTokens(infoboxMap, infoText, "[^a-z0-9]+");
					}
				}
			} catch (Exception ex) {
				System.out.println("Eternal Ref Exception: " + ex.getMessage());
				System.out.println("DocId: " + pageId);
				System.out.println("Stack trace" + ex.getStackTrace());
			}
		}

		ArrayList<String> strList = UtilFuncs.getTokensAsList(s.replaceAll("[^a-z0-9]+", " "), " ");

		PageInfo pi = null;
		TreeMap<Integer, PageInfo> piTreeMap;
		for (String token : strList) {
			token = token.trim();

			if (token.length() < 3 || token.length() > 10)
				continue;

			/*
			 * Update the word frequency in the in-memory object piHastTable
			 */
			if (tokenTree.containsKey(token)) {
				piTreeMap = tokenTree.get(token);
				pi = piTreeMap.get(pageId);
				if (pi == null) {
					pi = new PageInfo();
					pi.frequency = 1;
					piTreeMap.put(pageId, pi);
				} else
					pi.frequency++;
				pi.noOfTerms++;
			} else {
				piTreeMap = new TreeMap<Integer, PageInfo>();
				pi = new PageInfo();
				pi.frequency = 1;
				piTreeMap.put(pageId, pi);
				if (qName.equalsIgnoreCase("title"))
					tokensInTitle.add(token);
				else
					tokenTree.put(token, piTreeMap);
				pi.noOfTerms++;
			}

			if (qName.equalsIgnoreCase("text")) {
				updateFrequeniesInPagInfoObj(token, pi, piTreeMap);
			}
		}

		if (countOfDocsForBlock == BLOCK_SIZE) {
			try {
				saveTodisk();
				tokenTree.clear();
				countOfDocsForBlock = 0;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Fills the <i>map</i> by tokenizing the <i>string</i> obtained by removing
	 * the <i>regEx</i> pattern from the string.
	 */
	private void fillMapWithTokens(HashMap<String, Integer> map, String string, String regEx) {

		ArrayList<String> tokensList = UtilFuncs.getTokensAsList(string.toString().replaceAll(regEx, " "), " ");

		for (String item : tokensList)
			if (map.containsKey(item))
				map.put(item, map.get(item) + 1);
			else
				map.put(item, 1);
	}

	/**
	 * Adds the tokens present in the set <i>tokensInTitle</i>
	 */
	private void addTokensInTitleToPageInfoTreeMap(HashSet<String> tokensInTitle) {
		PageInfo pi;
		TreeMap<Integer, PageInfo> piTreeMap;
		for (String str : tokensInTitle) {
			if (tokenTree.containsKey(str.toString().trim())) {
				piTreeMap = tokenTree.get(str.toString().trim());
				pi = piTreeMap.get(pageId);
				if (pi == null) {
					pi = new PageInfo();
					pi.titleFrequeny = 1;
					pi.noOfTermsInTitle = tokensInTitle.size();
					piTreeMap.put(pageId, pi);
				} else
					pi.titleFrequeny++;
			} else {
				piTreeMap = new TreeMap<Integer, PageInfo>();
				pi = new PageInfo();
				pi.titleFrequeny = 1;
				piTreeMap.put(pageId, pi);
				tokenTree.put(str.toString().trim(), piTreeMap);
			}
		}
	}

	/**
	 * Updates the map <i>piTreeMap</i> with the info present in the PageInfo
	 * object <i>pi</i>
	 */
	private void updateFrequeniesInPagInfoObj(String token, PageInfo pi, TreeMap<Integer, PageInfo> piTreeMap) {
		if (pi.infoboxFrequeny == 0 && infoboxMap.containsKey(token)) {
			pi.infoboxFrequeny = infoboxMap.get(token);
			pi.noOfTermsInCategory = infoboxMap.size();
		}
		if (pi.categoryFrequeny == 0 && categoryMap.containsKey(token)) {
			pi.categoryFrequeny = categoryMap.get(token);
			pi.noOfTermsInCategory = categoryMap.size();
		}
		if (pi.refFrequeny == 0 && refMap.containsKey(token)) {
			pi.refFrequeny = refMap.get(token);
		}
		if (pi.extLinkFrequeny == 0 && extLinksMap.containsKey(token)) {
			pi.extLinkFrequeny = extLinksMap.get(token);
		}
		piTreeMap.put(pageId, pi);
	}

	public void endDocument() throws SAXException {

		try {
			if (countOfDocsForBlock != 0)
				saveTodisk();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void saveTodisk() throws IOException {

		System.out.println("Last Page ID: " + pageId);
		System.out.print("Saving the file: " + OutFileName + fileIndex + "... ");
		outFile = new FileWriter(OutFileName + "/" + fileIndex);
		fileIndex++;
		boolean isFirstLine = true;

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

				if (!isFirstLine)
					outFile.write("\n");
				isFirstLine = false;
				outFile.write(key + ":");

				for (Integer pageId : keys) {
					PageInfo pi = ht.get(pageId);
					pi.computeWightedSum();
					outFile.write("|" + pageId + "-" + pi.frequency + (pi.categoryFrequeny > 0 ? "C" : "")
							+ (pi.infoboxFrequeny > 0 ? "I" : "") + (pi.titleFrequeny > 0 ? "T" : "")
							+ (pi.refFrequeny > 0 ? "R" : "") + (pi.extLinkFrequeny > 0 ? "E" : ""));
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

		System.out.println("Done");
	}

}