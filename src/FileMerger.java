import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class FileMerger {

	/**
	 * Constants
	 */
	private int PRIMARY_INDEX_SEGMENT_SIZE = 5000;

	private String pathToParentDir;
	private String outputFile;

	private int countOfTerms = PRIMARY_INDEX_SEGMENT_SIZE;

	ArrayList<StringBuilder> values = new ArrayList<StringBuilder>();
	ArrayList<StringBuilder> keys = new ArrayList<StringBuilder>();
	ArrayList<BufferedReader> buffReaderForFiles = new ArrayList<BufferedReader>();


	public FileMerger(String parentDir, String outputFile) {
		pathToParentDir = parentDir;
		this.outputFile = outputFile;
	}

	public void startMerging() throws IOException {

		System.out.println(pathToParentDir);
		File parentDir = new File(pathToParentDir);
		FileWriter primaryIndexWriter = null;
		FileWriter secondaryIndexWriter = new FileWriter(outputFile + "/index.SecondaryIndex");

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
			values.add(new StringBuilder(str.substring(postingListStartsAt + 1)));
		}

		StringBuilder previousKey = new StringBuilder();
		countOfFiles = 0;
		while (!buffReaderForFiles.isEmpty()) {

			int minIndex = getIndexForMinFile();
			if (previousKey.toString().equals(keys.get(minIndex).toString()))
				primaryIndexWriter.append(values.get(minIndex));
			else {
				if (countOfTerms == PRIMARY_INDEX_SEGMENT_SIZE) {
					try {
						primaryIndexWriter.close();
					} catch (Exception ex) {
						System.out.println("File is not open ");
					}
					primaryIndexWriter = new FileWriter(outputFile + "/" + countOfFiles);
					secondaryIndexWriter.append(keys.get(minIndex)).append(":").append(countOfFiles + "").append(",");
					System.out.println("Indexing from: " + keys.get(minIndex) + " : " + countOfFiles);
					countOfFiles++;
					countOfTerms = 0;
				}
				primaryIndexWriter.append("\n").append(keys.get(minIndex)).append(":").append(values.get(minIndex));
				countOfTerms++;
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
			values.get(minIndex).append(str.substring(postingListStartsAt + 1));
		}
		if (countOfTerms > 0 && countOfTerms != PRIMARY_INDEX_SEGMENT_SIZE)
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
