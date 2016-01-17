import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import SharedDS.*;

class UserHandler extends DefaultHandler {

	boolean pageFound = false;
	boolean pageIdFound = false;
	boolean pageIdSet = false;
	boolean titleFound = false;
	boolean bodyFound = false;
	boolean redirectTitleFound = false;

	private int pageId = -1;

	private StopWords sw = new StopWords();
	private Stemmer stemmer = new Stemmer();
	private StringBuilder str = new StringBuilder();
	public FileWriter outFile;
	public TreeMap<String, TreeMap<Integer, PageInfo>> tokenTree;

	private ArrayList<String> getTokensAsList(String textStr, String delim) {
		ArrayList<String> tokensList = new ArrayList<String>();
		StringTokenizer strTok = new StringTokenizer(textStr, delim);
		String temp;

		while (strTok.hasMoreTokens()) {
			temp = strTok.nextToken();

			if (sw.isStopWord(temp))
				continue;

			stemmer.add(temp);
			stemmer.stem();
			temp = stemmer.toString();
			if (temp.length() > 0)
				tokensList.add(temp.trim());
		}
		return tokensList;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		str.setLength(0);

		if (qName.equalsIgnoreCase("page")) {
			pageFound = true;
		} else if (qName.equalsIgnoreCase("id") && pageIdFound == false) {
			pageIdFound = true;
		} else if (qName.equalsIgnoreCase("title")) {
			titleFound = true;
		} else if (qName.equalsIgnoreCase("text")) {
			bodyFound = true;
		} else if (qName.equalsIgnoreCase("redirect")) {
			redirectTitleFound = true;
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
		else if (redirectTitleFound)
			str.append(new String(ch, start, length));
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {

		if (qName.equalsIgnoreCase("page")) {
			pageFound = pageIdSet = pageIdFound = false;
			return;
		}

		String s = str.toString().replaceAll("[\\W ]+", " ").toLowerCase();
		ArrayList<String> strList = getTokensAsList(s, " ");

		for (String str : strList) {
			str = str.trim();
			if (pageIdFound && qName.equalsIgnoreCase("id")) {
				pageId = Integer.parseInt(str.toString().trim());
				pageIdSet = true;
			}

			PageInfo pi = null;
			if (tokenTree.containsKey(str.toString().trim())) {
				TreeMap<Integer, PageInfo> piHastTable = tokenTree.get(str.toString().trim());
				pi = piHastTable.get(pageId);
				if (pi == null) {
					pi = new PageInfo();
					pi.frequency = 1;
					piHastTable.put(pageId, pi);
				} else
					pi.frequency++;
			} else {
				TreeMap<Integer, PageInfo> ht = new TreeMap<Integer, PageInfo>();
				pi = new PageInfo();
				pi.frequency = 1;
				ht.put(pageId, pi);
				tokenTree.put(str.toString().trim(), ht);
			}

			if (titleFound || redirectTitleFound) {
				pi.tokenType |= TokenType.isTitle;
				titleFound = redirectTitleFound = false;
			} else if (bodyFound) {
				pi.tokenType |= TokenType.isBodyText;
				bodyFound = false;
			}

			redirectTitleFound = titleFound = bodyFound = false;
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
					outFile.write("|" + pageId + "-" + ht.get(pageId).frequency);
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