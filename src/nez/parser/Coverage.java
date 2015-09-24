package nez.parser;

import java.util.HashMap;
import java.util.List;

import nez.lang.Expression;
import nez.lang.Production;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Unary;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class Coverage {
	static UList<Coverage> covList = null;
	static HashMap<String, Coverage> covMap;

	public final static void init() {
		covList = new UList<Coverage>(new Coverage[128]);
		covMap = new HashMap<>();
	}

	public final static Coverage getCoverage(Production p) {
		Coverage cov = covMap.get(p.getUniqueName());
		if (cov == null) {
			cov = new Coverage(p, covList.size());
			covList.add(cov);
			covMap.put(p.getUniqueName(), cov);
		}
		return cov;
	}

	public final static Instruction encodeEnterCoverage(Production p, Instruction next) {
		if (covList != null) {
			Coverage cov = getCoverage(p);
			return new Icov(cov, next);
		}
		return next;
	}

	public final static Instruction encodeExitCoverage(Production p, Instruction next) {
		if (covList != null) {
			Coverage cov = getCoverage(p);
			return new Icovx(cov, next);
		}
		return next;
	}

	public final static void enter(int covPoint) {
		covList.ArrayValues[covPoint].countEnter();
	}

	public static void exit(int covPoint) {
		covList.ArrayValues[covPoint].countExit();
	}

	public final static Coverage getCoverage(String prefix, Expression e) {
		String key = prefix + e.getSourcePosition();
		Coverage cov = covMap.get(key);
		if (cov == null) {
			cov = new Coverage(e, covList.size());
			covList.add(cov);
			covMap.put(key, cov);
		}
		return cov;
	}

	public final static Instruction encodeEnterCoverage(Pchoice e, int index, Instruction next) {
		if (covList != null) {
			Coverage cov = getCoverage("c", e.get(index));
			return new Icov(cov, next);
		}
		return next;
	}

	public final static Instruction encodeExitCoverage(Pchoice e, int index, Instruction next) {
		if (covList != null) {
			Coverage cov = getCoverage("c", e.get(index));
			return new Icovx(cov, next);
		}
		return next;
	}

	public final static Instruction encodeEnterCoverage(Unary e, int index, Instruction next) {
		if (covList != null) {
			Coverage cov = getCoverage("u", e.get(0));
			return new Icov(cov, next);
		}
		return next;
	}

	public final static Instruction encodeExitCoverage(Unary e, int index, Instruction next) {
		if (covList != null) {
			Coverage cov = getCoverage("u", e.get(0));
			return new Icovx(cov, next);
		}
		return next;
	}

	public final static float calc() {
		int prodCount = 0;
		int prodEnter = 0;
		int prodExit = 0;
		// int exprCount = 0;
		// int exprEnter = 0;
		// int exprExit = 0;
		for (Coverage cov : covList) {
			if (cov.p != null) {
				prodCount++;
				if (cov.enterCount > 0) {
					prodEnter++;
				}
				if (cov.exitCount > 0) {
					prodExit++;
				}
			}
			// if (cov.e != null) {
			// exprCount++;
			// if (cov.enterCount > 0) {
			// exprEnter++;
			// }
			// if (cov.exitCount > 0) {
			// exprExit++;
			// }
			// }
		}
		ConsoleUtils.println("Production coverage: " + ((100.f * prodEnter) / prodCount) + "%, " + ((100.f * prodExit) / prodCount) + "%");
		// ConsoleUtils.println("Branch coverage: " + ((100.f * exprEnter) /
		// exprCount) + "%, " + ((100.f * exprExit) / exprCount) + "%");
		return 1.0f * prodExit / prodCount;
	}

	public final static List<String> getUntestedProductionList() {
		UList<String> l = new UList<String>(new String[10]);
		for (Coverage cov : covList) {
			if (cov.p != null) {
				if (cov.enterCount == 0) {
					l.add(cov.p.getLocalName());
				}
			}
		}
		return l;
	}

	public final static void dump() {
		ConsoleUtils.println("Coverage:");
		for (Coverage cov : covList) {
			if (cov.p != null) {
				ConsoleUtils.println(String.format("  %-40s: %d / %d", cov.p.getUniqueName(), cov.enterCount, cov.exitCount));
			}
			if (cov.e != null) {
				ConsoleUtils.println(cov.e + ": " + cov.enterCount + " / " + cov.exitCount);
			}
		}
	}

	// Fields
	Production p;
	Expression e;
	int covPoint;
	int enterCount;
	int exitCount;

	Coverage(Production p, int point) {
		this.p = p;
		this.covPoint = point;
	}

	Coverage(Expression e, int point) {
		this.e = e;
		this.covPoint = point;
	}

	private void countEnter() {
		this.enterCount++;

	}

	private void countExit() {
		this.exitCount++;
	}

}

class Icov extends Instruction {
	final int covPoint;

	public Icov(Coverage cov, Instruction next) {
		super(InstructionSet.Cov, null, next);
		this.covPoint = cov.covPoint;
	}

	@Override
	protected
	void encodeImpl(ByteCoder c) {
		// TODO Auto-generated method stub

	}

	@Override
	public
	Instruction exec(RuntimeContext sc) throws TerminationException {
		Coverage.enter(this.covPoint);
		return this.next;
	}

}

class Icovx extends Instruction {
	final int covPoint;

	public Icovx(Coverage cov, Instruction next) {
		super(InstructionSet.Cov, null, next);
		this.covPoint = cov.covPoint;
	}

	@Override
	protected
	void encodeImpl(ByteCoder c) {
		// TODO Auto-generated method stub

	}

	@Override
	public
	Instruction exec(RuntimeContext sc) throws TerminationException {
		Coverage.exit(this.covPoint);
		return this.next;
	}

}
