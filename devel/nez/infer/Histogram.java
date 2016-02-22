package nez.infer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Histogram {

	private List<DataUnit> dataUnits;
	private Map<Integer, DataUnit> dataMap; // key: token frequency
	private List<Integer> orderIdList;
	private String label; // a label of the target token
	private int tmpTokenFrequency;
	private int totalNumOfChunks;

	public Histogram(String label, int totalNumOfChunks) {
		this.label = label;
		this.totalNumOfChunks = totalNumOfChunks;
		this.dataMap = new HashMap<Integer, DataUnit>();
		this.tmpTokenFrequency = 0;
		this.orderIdList = new ArrayList<Integer>();
	}

	public Histogram(String label, int totalNumOfChunks, List<DataUnit> dataUnits) {
		this.label = label;
		this.totalNumOfChunks = totalNumOfChunks;
		this.dataUnits = dataUnits;
	}

	public final String getLabel() {
		return this.label;
	}

	public final List<Integer> getOrderIdList() {
		return this.orderIdList;
	}

	public final void commit() {
		if (!this.dataMap.containsKey(tmpTokenFrequency)) {
			this.dataMap.put(tmpTokenFrequency, new DataUnit(tmpTokenFrequency));
		}
		this.dataMap.get(tmpTokenFrequency).updateChunkCount();
		this.tmpTokenFrequency = 0;

	}

	public void update() {
		this.tmpTokenFrequency++;
	}

	public void update(int id) {
		this.tmpTokenFrequency++;
		if (!orderIdList.contains(id)) {
			this.orderIdList.add(id);
		}
	}

	public final int width() {
		return this.dataUnits.size();
	}

	private final int wholeChunkSize() {
		return this.totalNumOfChunks;
	}

	public void normalize() {
		this.newDataUnits();
		this.orderByTokenFrequency();
	}

	private final void newDataUnits() {
		this.dataUnits = new ArrayList<DataUnit>();
		for (Entry<Integer, DataUnit> unit : dataMap.entrySet()) {
			dataUnits.add(unit.getValue());
		}
	}

	private final void orderByTokenFrequency() {
		this.dataUnits.sort((unit1, unit2) -> {
			int subOfTokenFrequency = unit2.getTokenFrequency() - unit1.getTokenFrequency();
			int subOfChunkCount = unit2.getChunkCount() - unit1.getChunkCount();
			if (subOfChunkCount == 0) {
				return subOfChunkCount;
			} else {
				return subOfTokenFrequency;
			}
		});
	}

	private final int getChunkCountI(int idx) {
		return idx < this.width() ? this.dataUnits.get(idx).getChunkCount() : 0;
	}

	private final double getChunkCountF(int idx) {
		return idx < this.width() ? this.dataUnits.get(idx).getChunkCount() : 0;
	}

	public final double residualMass(int idx) {
		int rm = 0;
		for (int i = idx + 1; i < this.width(); i++) {
			rm += this.getChunkCountI(i);
		}
		return (double) rm / this.wholeChunkSize();
	}

	public final double coverage() {
		double cov = 0.0;
		for (int i = 0; i < this.width(); i++) {
			cov += this.getChunkCountI(i);
		}
		// System.out.println(String.format("%s : %s,%s", this.getLabel(), cov,
		// this.wholeChunkSize()));
		return cov / this.wholeChunkSize();
	}

	protected static double calcRelativeEntropy(Histogram h1, Histogram h2) {
		double relativeEntropy = 0.0;
		double f1, f2;
		for (int i = 0; i < h1.width(); i++) {
			f1 = h1.getChunkCountF(i);
			f2 = h2.getChunkCountF(i);
			// System.out.println(String.format("%s : %s,%s,%s", h1.getLabel(),
			// h1.wholeChunkSize(), f1, f2));
			relativeEntropy += (f1 / h1.wholeChunkSize()) * Math.log(f1 / f2);
		}
		// System.out.println(relativeEntropy);
		return relativeEntropy;
	}

	public static double calcSimilarity(Histogram h1, Histogram h2) {
		double sim = 0.0;
		Histogram ave = Histogram.average(h1, h2);
		sim = (Histogram.calcRelativeEntropy(h1, ave) / 2) + (Histogram.calcRelativeEntropy(h2, ave) / 2);
		return sim;
	}

	public static Histogram average(Histogram h1, Histogram h2) {
		List<DataUnit> newBody = new ArrayList<>();
		int[] sums = new int[Math.max(h1.width(), h2.width())];
		for (int i = 0; i < sums.length; i++) {
			sums[i] += h1.getChunkCountI(i);
			sums[i] += h2.getChunkCountI(i);
			newBody.add(new DataUnit(0, sums[i] / 2));
		}
		String label = String.format("AVE_%s_%s", h1.getLabel(), h2.getLabel());
		return new Histogram(label, h1.totalNumOfChunks, newBody);
	}
}

class DataUnit {
	private final int tokenFrequency;
	private int chunkCount;
	private List<Integer> orderIdList;

	public DataUnit(int tokenFrequency, int chunkCount) {
		this.tokenFrequency = tokenFrequency;
		this.chunkCount = chunkCount;
	}

	public DataUnit(int tokenFrequency) {
		this.tokenFrequency = tokenFrequency;
		this.chunkCount = 0;
	}

	public int getTokenFrequency() {
		return tokenFrequency;
	}

	public int getChunkCount() {
		return chunkCount;
	}

	public void setOrderIdList(List<Integer> list) {
		this.orderIdList = list;
	}

	public List<Integer> getOrderIdList() {
		return this.orderIdList;
	}

	public void updateChunkCount() {
		this.chunkCount++;
	}
}
