package nez.parser;

import java.util.HashMap;

import nez.parser.vm.Moz;
import nez.parser.vm.MozInst;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class CoverageProfiler {
	private static class CoverageEntry {
		String label;
		public int covPoint;
		int enterCount;
		int exitCount;

		CoverageEntry(String label, int point) {
			this.label = label;
			this.covPoint = point;
		}

		private void count(boolean start) {
			if (start) {
				this.enterCount++;
			} else {
				this.exitCount++;
			}
		}
	}

	UList<CoverageEntry> covList = new UList<CoverageEntry>(new CoverageEntry[128]);
	HashMap<String, CoverageEntry> covMap = new HashMap<>();

	public final void initCoverage() {
		covList = new UList<CoverageEntry>(new CoverageEntry[128]);
		covMap = new HashMap<>();
	}

	private CoverageEntry getCoverage(String u) {
		CoverageEntry cov = covMap.get(u);
		if (cov == null) {
			cov = new CoverageEntry(u, covList.size());
			covList.add(cov);
			covMap.put(u, cov);
		}
		return cov;
	}

	public final MozInst compileCoverage(String label, boolean start, MozInst next) {
		if (covList != null) {
			CoverageEntry cov = getCoverage(label);
			return new Moz.Cov(this, cov.covPoint, start, next);
		}
		return next;
	}

	public void countCoverage(int id, boolean start) {
		covList.ArrayValues[id].count(start);
	}

	public final double getCoverage() {
		int prodCount = 0;
		int prodExit = 0;
		for (CoverageEntry cov : covList) {
			prodCount++;
			if (cov.exitCount > 0) {
				prodExit++;
			}
		}
		return 1.0 * prodExit / prodCount;
	}

	public final void dumpCoverage() {
		ConsoleUtils.println("Coverage:");
		if (covList != null) {
			for (CoverageEntry cov : covList) {
				ConsoleUtils.println(String.format("  %-40s: %d / %d", cov.label, cov.enterCount, cov.exitCount));
			}
		}
	}

}
