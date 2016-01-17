import java.io.File;
import java.io.FileWriter;
import java.util.TreeMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import SharedDS.PageInfo;

public class Main {

	public static void main(String[] args) {

		long lStartTime = System.currentTimeMillis();
		try {
			// File inputFile = new File("../SaxExample2/res/xml.xml");
			File inputFile = new File("/home/prabhakar/IIIT-H_current/Sem 2/IRE/Mini-Project/wiki-search-small_.xml");
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			UserHandler userhandler = new UserHandler();

			userhandler.outFile = new FileWriter("/home/prabhakar/IIIT-H_current/Sem 2/IRE/Mini-Project/temp.xml");
			userhandler.tokenTree = new TreeMap<String, TreeMap<Integer, PageInfo>>();

			saxParser.parse(inputFile, userhandler);

			System.out.println("Done.");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		long lEndTime = System.currentTimeMillis();
		System.out.println("Elapsed milliseconds: " + (lEndTime-lStartTime)/1000 );		
	}

}
