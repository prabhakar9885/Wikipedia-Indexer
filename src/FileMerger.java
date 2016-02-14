import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;

public class FileMerger {

	/**
	 * Constants
	 */
	private int SECONDARY_INDEX_SKIPS = 5000;

	private String pathToParentDir;
	private String outputFile;
	private long docsCount;
	StringBuilder post = new StringBuilder();
	StringBuilder resSb = new StringBuilder();

	private int countOfTerms = SECONDARY_INDEX_SKIPS;

	ArrayList<StringBuilder> values = new ArrayList<StringBuilder>();
	ArrayList<StringBuilder> keys = new ArrayList<StringBuilder>();
	ArrayList<BufferedReader> buffReaderForFiles = new ArrayList<BufferedReader>();

	public FileMerger(String parentDir, String outputFile, long docsCount) {
		pathToParentDir = parentDir;
		this.outputFile = outputFile;
		this.docsCount = docsCount;
	}

	private StringBuilder getPostingsWithRank(String postingsList) {

		StringTokenizer stk = new StringTokenizer(postingsList, "|");
		resSb.setLength(0);
		while (stk.hasMoreElements()) {
			String postStr = stk.nextToken();
			int i = 0;

			// Append DocID to res
			while (i < postStr.length() && postStr.charAt(i) != '-')
				resSb.append(postStr.charAt(i++));
			resSb.append("-");
			i++;

			// Append rank to res
			post.setLength(0);
			while (i < postStr.length() && postStr.charAt(i) >= '0' && postStr.charAt(i) <= '9')
				post.append(postStr.charAt(i++));
			double rank = 10 * Double.parseDouble(post.toString())
					* Math.log10(docsCount / (StringUtils.countMatches(postingsList, '|') + 1));
			resSb.append((long) rank);

			// Append Metada-ta about the term in DocID ( T->Title, C->Category,
			// R->References, I->InfoBox etc)
			if (i < postStr.length())
				resSb.append("-");
			while (i < postStr.length())
				resSb.append(postStr.charAt(i++));
			resSb.append("|");
		}

		return resSb;
	}

	public void startMerging() throws IOException {

		System.out.println(pathToParentDir);
		File parentDir = new File(pathToParentDir);
		FileWriter mergedIndexWriter = new FileWriter(outputFile + "/postings");
		FileWriter primaryIndexWriter = new FileWriter(outputFile + "/index.PrimaryIndex");
		FileWriter secondaryIndexWriter = new FileWriter(outputFile + "/index.SecondaryIndex");

		// Sort the file-names in the Alphabetically order.
		String files[] = parentDir.list();
		Arrays.sort(files, new Comparator<String>() {
			public int compare(String f1, String f2) {
				try {
					int i1 = Integer.parseInt(f1.substring(f1.indexOf('-') + 1));
					int i2 = Integer.parseInt(f2.substring(f2.indexOf('-') + 1));
					return i1 - i2;
				} catch (NumberFormatException e) {
					throw new AssertionError(e);
				}
			}
		});

		int countOfFiles = parentDir.list().length;
		StringBuilder tempSB = new StringBuilder();

		for (int fileIndex = 0; fileIndex < countOfFiles; fileIndex++) {
			buffReaderForFiles.add(new BufferedReader(new FileReader(pathToParentDir + "/" + files[fileIndex])));

			tempSB.setLength(0);
			while (tempSB.length() == 0)
				tempSB.append(buffReaderForFiles.get(fileIndex).readLine());
			String str = tempSB.toString();

			int postingListStartsAt = 0;
			tempSB.setLength(0);
			for (; postingListStartsAt < 30 && str.charAt(postingListStartsAt) != ':'; postingListStartsAt++)
				tempSB.append(str.charAt(postingListStartsAt));
			keys.add(new StringBuilder(tempSB.toString()));
			// values.add(new StringBuilder(str.substring(postingListStartsAt +
			// 1)));;
			values.add(new StringBuilder(getPostingsWithRank(str.substring(postingListStartsAt + 2))));
		}

		StringBuilder previousKey = new StringBuilder();
		StringBuilder tempSBForMergedIndex = new StringBuilder();
		StringBuilder tempSBForPrimIndex = new StringBuilder();
		long byteOffsetInPrimaryIndexFile = 0, byteOffsetInMergedIndex = 1;

		while (!buffReaderForFiles.isEmpty()) {
			int minIndex = getIndexForMinFile();
			if (previousKey.toString().equals(keys.get(minIndex).toString())) {
				// If the Key is already added to the MergedIndexFile, append
				// the corresponding value to the MergedIndexFile and update the
				// byteOffsetInMergedIndex
				mergedIndexWriter.append(values.get(minIndex));
				byteOffsetInMergedIndex += values.get(minIndex).length();
			} else {
				// else, append the <key,value> pair to the MergedIndexFile and,
				// 1. Add the entry <key,byteOffsetInMergedIndex> to the
				// PrimaryIndexFile and update the byteOffsetInMergedIndex
				// 2. If countOfTerms == SECONDARY_INDEX_SKIPS, update
				tempSBForMergedIndex.setLength(0);
				tempSBForMergedIndex.append("\n").append(keys.get(minIndex)).append(":").append(values.get(minIndex));
				mergedIndexWriter.append(tempSBForMergedIndex);
				tempSBForPrimIndex.setLength(0);
				tempSBForPrimIndex.append(keys.get(minIndex)).append(":").append(byteOffsetInMergedIndex).append("\n");
				byteOffsetInMergedIndex += tempSBForMergedIndex.length();
				primaryIndexWriter.append(tempSBForPrimIndex);
				if (countOfTerms == SECONDARY_INDEX_SKIPS) {
					secondaryIndexWriter.append(keys.get(minIndex)).append(":")
							.append(byteOffsetInPrimaryIndexFile + ",");
					countOfTerms = 0;
				} else
					countOfTerms++;
				byteOffsetInPrimaryIndexFile += tempSBForPrimIndex.length();
			}

			previousKey.setLength(0);
			previousKey.append(keys.get(minIndex));

			String str = buffReaderForFiles.get(minIndex).readLine();
			if (str == null) {
				keys.remove(minIndex);
				values.remove(minIndex);
				buffReaderForFiles.remove(minIndex);
				continue;
			}

			tempSB.setLength(0);
			int postingListStartsAt = 0;
			for (; postingListStartsAt < 30 && str.charAt(postingListStartsAt) != ':'; postingListStartsAt++)
				tempSB.append(str.charAt(postingListStartsAt));

			keys.get(minIndex).setLength(0);
			keys.get(minIndex).append(tempSB.toString());
			values.get(minIndex).setLength(0);
			// values.get(minIndex).append(str.substring(postingListStartsAt +
			// 1));
			values.get(minIndex).append(getPostingsWithRank(str.substring(postingListStartsAt + 2)));
		}

		mergedIndexWriter.close();
		primaryIndexWriter.close();
		secondaryIndexWriter.close();
	}

	private int getIndexForMinFile() {

		int pointerToMinIndex = 0;

		for (int i = 1; i < keys.size(); i++) {
			if (keys.get(pointerToMinIndex).toString().compareTo(keys.get(i).toString()) > 0)
				pointerToMinIndex = i;
		}

		return pointerToMinIndex;
	}

}
