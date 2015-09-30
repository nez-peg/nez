package nez;

import nez.ast.CommonTree;
import nez.ast.Tree;
import nez.ast.TreeTransducer;
import nez.io.SourceContext;
import nez.main.NezProfier;
import nez.main.Verbose;
import nez.parser.GenerativeGrammar;
import nez.parser.Instruction;
import nez.parser.MemoTable;
import nez.parser.NezCode;
import nez.parser.NezCompiler;
import nez.parser.ParsingMachine;

public class Parser {
	private GenerativeGrammar gg;
	protected Strategy strategy;
	protected NezCode compiledCode = null;

	public Parser(GenerativeGrammar gg, Strategy option) {
		this.gg = gg;
		this.strategy = option;
	}

	public final GenerativeGrammar getGrammar() {
		return gg;
	}

	public final Strategy getStrategy() {
		return this.strategy;
	}

	public final NezCode getCompiledCode() {
		return compiledCode;
	}

	protected NezProfier prof = null;

	public void setProfiler(NezProfier prof) {
		this.prof = prof;
		if (prof != null) {
			this.compile();
			// prof.setFile("G.File", this.start.getGrammarFile().getURN());
			prof.setCount("G.Production", this.gg.size());
			prof.setCount("G.Instruction", this.compiledCode.getInstructionSize());
			prof.setCount("G.MemoPoint", this.compiledCode.getMemoPointSize());
		}
	}

	public NezProfier getProfiler() {
		return this.prof;
	}

	public void logProfiler() {
		if (prof != null) {
			prof.log();
		}
	}

	/* -------------------------------------------------------------------- */

	public Instruction compile() {
		if (compiledCode == null) {
			NezCompiler bc = NezCompiler.newCompiler(this.strategy);
			compiledCode = bc.compile(gg);
		}
		return compiledCode.getStartPoint();
	}

	public final boolean perform(ParsingMachine machine, SourceContext s, Tree<?> prototype) {
		Instruction pc = this.compile();
		s.init(newMemoTable(s), prototype);
		if (prof != null) {
			s.startProfiling(prof);
			boolean matched = machine.run(pc, s);
			s.doneProfiling(prof);
			if (Verbose.PackratParsing) {
				this.compiledCode.dumpMemoPoints();
			}
			return matched;
		}
		return machine.run(pc, s);
	}

	protected ParsingMachine newParsingMachine() {
		// return new TraceMachine(); // debug
		return new ParsingMachine();
	}

	protected final MemoTable newMemoTable(SourceContext sc) {
		return MemoTable.newTable(strategy, sc.length(), 32, this.compiledCode.getMemoPointSize());
	}

	/* --------------------------------------------------------------------- */

	public final boolean match(SourceContext s) {
		return perform(newParsingMachine(), s, null);
	}

	public final boolean match(String str) {
		SourceContext sc = SourceContext.newStringContext(str);
		if (perform(newParsingMachine(), sc, null)) {
			return (!sc.hasUnconsumed());
		}
		return false;
	}

	public Tree<?> parse(SourceContext sc, Tree<?> prototype) {
		long startPosition = sc.getPosition();
		if (!this.perform(newParsingMachine(), sc, prototype)) {
			return null;
		}
		return sc.getParseResult(startPosition, sc.getPosition());
	}

	@Deprecated
	public Tree<?> parse(SourceContext sc, TreeTransducer t) {
		throw new RuntimeException("FIXME");
		// return parseCommonTree(sc);
	}

	public final CommonTree parseCommonTree(SourceContext sc) {
		return (CommonTree) this.parse(sc, new CommonTree());
	}

	public final CommonTree parseCommonTree(String str) {
		SourceContext sc = SourceContext.newStringContext(str);
		return (CommonTree) this.parse(sc, new CommonTree());
	}

}
