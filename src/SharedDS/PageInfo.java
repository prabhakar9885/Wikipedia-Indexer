package SharedDS;

public class PageInfo {

	public int frequency;
	public int infoboxFrequeny = 0;
	public int titleFrequeny = 0;
	public int categoryFrequeny = 0;
	public int refFrequeny = 0;
	public int extLinkFrequeny = 0;

	public int rank = 0;

	public PageInfo() {
		frequency = 1;
	}

	public void calculateRank() {
		rank = 1000 * titleFrequeny + 300 * infoboxFrequeny + 100 * categoryFrequeny
				+ 20 * (refFrequeny + extLinkFrequeny);
	}

}