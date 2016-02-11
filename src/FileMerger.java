import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class FileMerger {

	private String pathToParentDir;
	private String outputFile;

	ArrayList<StringBuilder> values = new ArrayList<StringBuilder>();
	ArrayList<StringBuilder> keys = new ArrayList<StringBuilder>();
	ArrayList<BufferedReader> buffReaderForFiles = new ArrayList<BufferedReader>();

	public FileMerger(String parentDir, String outputFile) {
		pathToParentDir = parentDir;
		this.outputFile = outputFile;
	}

	public void startMerging() throws IOException {

		File parentDir = new File(pathToParentDir);
		FileWriter writer = new FileWriter(outputFile);
		String files[] = parentDir.list();
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
		while (!buffReaderForFiles.isEmpty()) {

			int minIndex = getIndexForMinFile();
			if (previousKey.equals(keys.get(minIndex)))
				writer.append(values.get(minIndex));
			else
				writer.append("\n").append(keys.get(minIndex)).append(":").append(values.get(minIndex));
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
		writer.close();
	}

	private int getIndexForMinFile() {

		int pointerToMinIndex = 0;

		for (int i = 1; i < keys.size(); i++) {
			if (keys.get(pointerToMinIndex).toString().compareTo(keys.get(i).toString()) < 0)
				pointerToMinIndex = i;
		}

		return pointerToMinIndex;
	}

}
