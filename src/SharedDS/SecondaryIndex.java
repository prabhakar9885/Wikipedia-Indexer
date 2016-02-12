package SharedDS;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

public class SecondaryIndex {

	public String inputFile;
	public String outputFile;

	public void build() throws IOException {
		File inpFile = new File(inputFile);
		File outFile = new File(outputFile);

		FileReader fileReader = new FileReader(inpFile);
	}

}
