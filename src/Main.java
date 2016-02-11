import java.io.File;
import java.util.TreeMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import SharedDS.PageInfo;

public class Main {

	public static void main(String[] args) {

		long lStartTime = System.currentTimeMillis();
		try {
			// File inputFile = new File("../SaxExample2/res/xml.xml");
			File inputFile = new File(args[0]);
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			UserHandler userhandler = new UserHandler();

			userhandler.OutFileName = args[1];
			//userhandler.OutFileName = "/home/prabhakar/IIIT-H_current/Sem 2/IRE/Mini-Project/merge/m";"
			userhandler.tokenTree = new TreeMap<String, TreeMap<Integer, PageInfo>>();

			 saxParser.parse(inputFile, userhandler);

//			FileMerger fileMerger = new FileMerger(inputFile.getParent(), userhandler.OutFileName);
//			fileMerger.startMerging();

			System.out.println("Done.");
		} catch (Exception e) {
			e.printStackTrace();
		}

		long lEndTime = System.currentTimeMillis();
		System.out.println("Elapsed milliseconds: " + (lEndTime - lStartTime) / 1000);
	}

}
