import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.TreeMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;

import SharedDS.PageInfo;

public class Main {

	/***
	 * 
	 * @param args[0]
	 *            Wiki-dump.xml
	 * @param args[1]
	 *            Path-to-the-output-folder
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
			InputStream inputStream = new FileInputStream(inputFile);
			Reader reader = new InputStreamReader(inputStream, "UTF-8");
			InputSource is = new InputSource(reader);
			is.setEncoding("UTF-8");
			saxParser.parse(is, userhandler);
			System.out.println("Segments Created.");

			// Write-back N to File
			FileWriter nFileWriter = new FileWriter(args[1] + "/N");
			nFileWriter.append(userhandler.docsCount + "");
			nFileWriter.close();

			// Read N-Value
			FileReader nFileReader = new FileReader(args[1] + "/N");
			char docsCountAsChars[] = new char[20];
			int t = nFileReader.read(docsCountAsChars);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < t; i++)
				sb.append(docsCountAsChars[i]);
			long N = Long.parseLong(sb.toString());
			nFileReader.close();

			// Merge Segmented Index Files
			inputFile = new File(args[1] + "/tempIndex");
			dir = new File(args[1] + "/FinalIndex");
			dir.mkdir();
			FileMerger fileMerger = new FileMerger(inputFile.getAbsolutePath(), dir.getAbsolutePath(), N);
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
