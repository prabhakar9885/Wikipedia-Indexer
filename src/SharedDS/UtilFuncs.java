package SharedDS;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UtilFuncs {

	private static Stemmer stemmer = new Stemmer();
	private static StopWords sw = new StopWords();
	
	static Pattern p = Pattern.compile("[0-9]{4,}");

	public static ArrayList<String> getTokensAsList(String textStr, String delim) {
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
			Matcher m = p.matcher(temp);
			
			if(m.matches())
				continue;
			if (temp.length() > 0 && temp.length()<10)
				tokensList.add(temp.trim());
		}
		return tokensList;
	}

	public static int indexOf(String patternString, String string, int startIndex) {
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(string.substring(startIndex));
		return matcher.find() ? matcher.start() : -1;
	}

	public static int indexOf(String patternString, String string) {
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(string);
		return matcher.find() ? matcher.start() : -1;
	}

}
