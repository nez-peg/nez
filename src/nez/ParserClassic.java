package nez;

import java.util.List;
import java.util.TreeMap;

import nez.lang.ConditionalAnalysis;
import nez.lang.Expression;
import nez.lang.Production;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Xif;
import nez.parser.Instruction;
import nez.parser.NezCompiler;
import nez.parser.PackratCompiler;
import nez.parser.ParsingMachine;
import nez.util.UList;
import nez.util.UMap;

public class ParserClassic extends Parser {
	Production start;
	UList<Production> productionList;
	UMap<Production> productionMap;
	TreeMap<String, Boolean> conditionMap;

	public ParserClassic(Production start, NezOption option) {
		super(null, option);
		this.start = start;
		this.productionList = new UList<Production>(new Production[4]);
		this.productionMap = new UMap<Production>();

		conditionMap = start.isConditional() ? new TreeMap<String, Boolean>() : null;
		analyze(start, conditionMap);
		if (conditionMap != null) {
			assert (conditionMap.size() > 0);
			// Verbose.debug("condition flow analysis: " +
			// conditionMap.keySet());
			this.start = new ConditionalAnalysis(conditionMap).newStart(start);
			this.productionList = new UList<Production>(new Production[4]);
			this.productionMap = new UMap<Production>();
			analyze(this.start, conditionMap);
		}
	}

	@Override
	public final Production getStartProduction() {
		return this.start;
	}

	@Override
	public final List<Production> getProductionList() {
		return this.productionList;
	}

	private void analyze(Production p, TreeMap<String, Boolean> conditionMap) {
		String uname = p.getUniqueName();
		if (productionMap.hasKey(uname)) {
			return;
		}
		productionList.add(p);
		productionMap.put(p.getUniqueName(), p);
		analyze(p.getExpression(), conditionMap);
	}

	private void analyze(Expression p, TreeMap<String, Boolean> conditionMap) {
		if (p instanceof NonTerminal) {
			analyze(((NonTerminal) p).getProduction(), conditionMap);
		}
		if (p instanceof Xif) {
			conditionMap.put(((Xif) p).getFlagName(), true);
		}
		for (Expression se : p) {
			analyze(se, conditionMap);
		}
	}

	@Override
	protected final ParsingMachine newParsingMachine() {
		return new ParsingMachine();
	}

	/* --------------------------------------------------------------------- */
	/* memoization configuration */

	// private NezOption option;
	// private NezCode compiledCode = null;
	//
	// public final NezOption getNezOption() {
	// return this.option;
	// }
	//
	// private void setOption(NezOption option) {
	// this.option = option;
	// this.compiledCode = null;
	// }
	//
	// private MemoTable getMemoTable(SourceContext sc) {
	// return MemoTable.newTable(option, sc.length(), 32,
	// this.compiledCode.getMemoPointSize());
	// }

	@Override
	public Instruction compile() {
		if (compiledCode == null) {
			// NezCompiler bc = Command.ReleasePreview ? new
			// PackratCompiler(this.option) : new PlainCompiler(this.option);
			NezCompiler bc = new PackratCompiler(this.option);
			compiledCode = bc.compile(this);
			// if(Verbose.VirtualMachine) {
			// bc.dump(this.productionList);
			// }
		}
		return compiledCode.getStartPoint();
	}

}