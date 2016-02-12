import java.io.File;
import java.io.FileWriter;
import java.nio.file.DirectoryIteratorException;
import java.util.TreeMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import SharedDS.PageInfo;
import SharedDS.SecondaryIndex;

public class Main {

	public static void main(String[] args) {

		long lStartTime = System.currentTimeMillis();
		File inputFile;

		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			UserHandler userhandler = new UserHandler();

			// Create Segmented Index Files
			userhandler.OutFileName = args[1];
			inputFile = new File(args[0]);
			userhandler.tokenTree = new TreeMap<String, TreeMap<Integer, PageInfo>>();
			saxParser.parse(inputFile, userhandler);
			System.out.println("Segments Created.");

			// Merge Segmented Index Files
			inputFile = new File(args[1]);
			File dir = new File(inputFile.getParent() + "/FinalIndex");
			dir.mkdir();
			userhandler.OutFileName = dir.getAbsolutePath();
			FileMerger fileMerger = new FileMerger(inputFile.getAbsolutePath(), userhandler.OutFileName);
			System.out.println("Building Primary & Secondary Index from Segments.");
			fileMerger.startMerging();
			System.out.println("Merged The Segmented Index files.");

			// Creating the Secondary Index for the Merged-Index file
			// SecondaryIndex secondaryIndex = new SecondaryIndex();
			// secondaryIndex.inputFile = userhandler.OutFileName;
			// secondaryIndex.outputFile = "/home/prabhakar/IIIT-H_current/Sem
			// 2/IRE/Mini-Project/merge/SecondaryIndex";
			// secondaryIndex.build();

		} catch (Exception e) {
			e.printStackTrace();
		}

		long lEndTime = System.currentTimeMillis();
		System.out.println("Elapsed milliseconds: " + (lEndTime - lStartTime) / 1000);
	}

}
