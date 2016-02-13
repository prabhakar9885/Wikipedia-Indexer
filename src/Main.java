import java.io.File;
import java.util.TreeMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import SharedDS.PageInfo;

public class Main {

	/***
	 * 
	 * @param args[0] Wiki-dump.xml
	 * @param args[1] Path-to-the-output-folder
	 */
	public static void main(String[] args) {

		long lStartTime = System.currentTimeMillis();
		File inputFile;

		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			UserHandler userhandler = new UserHandler();

			// Create Segmented Index Files
			File dir = new File(args[1] + "/tempIndex");
			dir.mkdir();
			userhandler.OutFileName = dir.getAbsolutePath();
			inputFile = new File(args[0]);
			userhandler.tokenTree = new TreeMap<String, TreeMap<Integer, PageInfo>>();
			saxParser.parse(inputFile, userhandler);
			System.out.println("Segments Created.");

			// Merge Segmented Index Files
			inputFile = new File(args[1] + "/tempIndex");
			dir = new File(args[1] + "/FinalIndex");
			dir.mkdir();
			FileMerger fileMerger = new FileMerger(inputFile.getAbsolutePath(), dir.getAbsolutePath());
			System.out.println("Building Primary & Secondary Index from Segments.");
			fileMerger.startMerging();
			System.out.println("Merged The Segmented Index files.");

		} catch (Exception e) {
			e.printStackTrace();
		}

		long lEndTime = System.currentTimeMillis();
		System.out.println("Elapsed milliseconds: " + (lEndTime - lStartTime) / 1000);
	}

}
