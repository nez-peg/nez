package nez.parser.hachi6;

import java.util.HashMap;
import java.util.List;

import nez.lang.Grammar;
import nez.lang.Production;
import nez.parser.ByteCoder;
import nez.parser.MemoPoint;
import nez.parser.TerminationException;
import nez.util.UList;
import nez.util.Verbose;

public class Hachi6Code {

	final Grammar grammar;
	final HashMap<String, ParseFunc> funcMap;
	final UList<Hachi6Inst> codeList;
	final List<MemoPoint> memoPointList;

	public Hachi6Code(Grammar gg) {
		this.grammar = gg;
		this.funcMap = new HashMap<>();
		this.codeList = new UList<>(new Hachi6Inst[4096]);
		this.memoPointList = null;
	}

	protected int getParseFuncSize() {
		if (this.funcMap != null) {
			return funcMap.size();
		}
		return 0;
	}

	protected ParseFunc getParseFunc(Production p) {
		return funcMap.get(p.getUniqueName());
	}

	protected void setParseFunc(Production p, ParseFunc f) {
		funcMap.put(p.getUniqueName(), f);
	}

	void layoutCode(Hachi6Inst inst) {
		if (inst == null) {
			return;
		}
		if (inst.id == -1) {
			inst.id = codeList.size();
			codeList.add(inst);
			layoutCode(inst.next);
			if (inst instanceof Hachi6Branch) {
				layoutCode(((Hachi6Branch) inst).jump);
			}
			if (inst instanceof Hachi6BranchTable) {
				Hachi6BranchTable match = (Hachi6BranchTable) inst;
				for (int ch = 0; ch < match.jumpTable.length; ch++) {
					layoutCode(match.jumpTable[ch]);
				}
			}
		}
	}

	public final Hachi6Inst getStartPoint() {
		return codeList.get(0);
	}

	public final int getInstructionSize() {
		return codeList.size();
	}

	public final int getMemoPointSize() {
		return this.memoPointList != null ? this.memoPointList.size() : 0;
	}

	public final void dumpMemoPoints() {
		if (this.memoPointList != null) {
			Verbose.println("ID\tPEG\tCount\tHit\tFail\tMean");
			for (MemoPoint p : this.memoPointList) {
				String s = String.format("%d\t%s\t%d\t%f\t%f\t%f", p.id, p.label, p.count(), p.hitRatio(), p.failHitRatio(), p.meanLength());
				Verbose.println(s);
			}
			Verbose.println("");
		}
	}

	public final void encode(ByteCoder coder) {
		// if (coder != null) {
		// coder.setHeader(codeList.size(), this.gg.size(), memoPointList ==
		// null ? 0 : memoPointList.size());
		// coder.setInstructions(codeList.ArrayValues, codeList.size());
		// }
	}

	/**
	 * public final static void writeHachi6Code(Parser parser, String path) {
	 * Hachi6Compiler compile = new PackratCompiler(parser.getParserStrategy());
	 * Hachi6Code code = compile.compile(parser.getParserGrammar()); ByteCoder c
	 * = new ByteCoder(); code.encode(c); Verbose.println("generating " + path);
	 * c.writeTo(path); }
	 **/

	static class ParseFunc {
		Production p;
		// boolean state;
		// MemoPoint memoPoint = null;
		Hachi6Inst compiled;

		ParseFunc(Production p, Hachi6Inst inst) {
			this.p = p;
			this.compiled = inst;
		}

		// public final MemoPoint getMemoPoint() {
		// return this.memoPoint;
		// }
		//
		// public final boolean isStateful() {
		// return this.state;
		// }

		public final Hachi6Inst getCompiled() {
			return this.compiled;
		}
	}

	/* Coverage */

	class Coverage {
		int id;
		String key;
		int count;

		Coverage(String key, int id) {
			this.key = key;
			this.id = id;
			this.count = 0;
		}

		void count() {
			this.count++;
		}

		void reset() {
			this.count = 0;
		}

	}

	UList<Coverage> covList = null;
	HashMap<String, Coverage> covMap = null;

	public final void initCoverage() {
		if (covList == null) {
			covList = new UList<Coverage>(new Coverage[128]);
			covMap = new HashMap<>();
		}
	}

	public final void resetCoverage() {
		if (covList != null) {
			for (Coverage cov : covList) {
				cov.reset();
			}
		}
	}

	public int getCoverageCount(String key) {
		Coverage cov = covMap.get(key);
		if (cov == null) {
			return 0;
		}
		return cov.count;
	}

	private Coverage getCoverage(String key) {
		Coverage cov = covMap.get(key);
		if (cov == null) {
			cov = new Coverage(key, covList.size());
			covList.add(cov);
			covMap.put(key, cov);
		}
		return cov;
	}

	private class Cov extends Hachi6.Cov {
		public Cov(int uid, Hachi6Inst next) {
			super(uid, next);
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			covList.ArrayValues[uid].count();
			return next;
		}
	}

	public final Hachi6Inst visitEnterCoverage(Production p, Hachi6Inst next) {
		if (covList != null) {
			String key = p.getLocalName() + "^";
			Coverage cov = getCoverage(key);
			return new Cov(cov.id, next);
		}
		return next;
	}

	public final Hachi6Inst visitExitCoverage(Production p, Hachi6Inst next) {
		if (covList != null) {
			String key = p.getLocalName();
			Coverage cov = getCoverage(key);
			return new Cov(cov.id, next);
		}
		return next;
	}

	// public final float calc() {
	// int prodCount = 0;
	// int prodEnter = 0;
	// int prodExit = 0;
	// // int exprCount = 0;
	// // int exprEnter = 0;
	// // int exprExit = 0;
	// for (Coverage cov : covList) {
	// if (cov.p != null) {
	// prodCount++;
	// if (cov.enterCount > 0) {
	// prodEnter++;
	// }
	// if (cov.exitCount > 0) {
	// prodExit++;
	// }
	// }
	// // if (cov.e != null) {
	// // exprCount++;
	// // if (cov.enterCount > 0) {
	// // exprEnter++;
	// // }
	// // if (cov.exitCount > 0) {
	// // exprExit++;
	// // }
	// // }
	// }
	// ConsoleUtils.println("Production coverage: " + ((100.f * prodEnter) /
	// prodCount) + "%, " + ((100.f * prodExit) / prodCount) + "%");
	// // ConsoleUtils.println("Branch coverage: " + ((100.f * exprEnter) /
	// // exprCount) + "%, " + ((100.f * exprExit) / exprCount) + "%");
	// return 1.0f * prodExit / prodCount;
	// }
	//
	// public final static List<String> getUntestedProductionList() {
	// UList<String> l = new UList<String>(new String[10]);
	// for (Coverage cov : covList) {
	// if (cov.p != null) {
	// if (cov.enterCount == 0) {
	// l.add(cov.p.getLocalName());
	// }
	// }
	// }
	// return l;
	// }
	//
	// public final static void dump() {
	// ConsoleUtils.println("Coverage:");
	// for (Coverage cov : covList) {
	// if (cov.p != null) {
	// ConsoleUtils.println(String.format("  %-40s: %d / %d",
	// cov.p.getUniqueName(), cov.enterCount, cov.exitCount));
	// }
	// if (cov.e != null) {
	// ConsoleUtils.println(cov.e + ": " + cov.enterCount + " / " +
	// cov.exitCount);
	// }
	// }
	// }

	// public Object exec(ParserContext context) {
	// long startPosition = context.getPosition();
	// Hachi6Machine machine = (Hachi6Machine) context.getRuntime();
	// Hachi6Inst inst = this.getStartPoint();
	// boolean result = false;
	// try {
	// while (true) {
	// inst = inst.exec(machine);
	// }
	// } catch (TerminationException e) {
	// result = e.status;
	// }
	// return result ? machine.getReftgetParseResult(startPosition,
	// context.getPosition()) : null;
	// }

}
