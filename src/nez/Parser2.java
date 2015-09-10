package nez;

import nez.ast.TreeTransducer;
import nez.main.NezProfier;
import nez.main.Verbose;
import nez.vm.Instruction;
import nez.vm.Machine;
import nez.vm.MemoTable;
import nez.vm.NezCode;
import nez.vm.NezCompiler;
import nez.vm.PackratCompiler;
import nez.vm.ParserGrammar;

public class Parser2 {
	private ParserGrammar g;
	private NezOption option;
	private NezCode compiledCode = null;

	public Parser2(ParserGrammar g, NezOption option) {
		this.g = g;
		this.option = option;
	}

	private MemoTable getMemoTable(SourceContext sc) {
		return MemoTable.newTable(option, sc.length(), 32, this.compiledCode.getMemoPointSize());
	}

	private NezProfier prof = null;

	public void setProfiler(NezProfier prof) {
		this.prof = prof;
		if (prof != null) {
			this.compile();
			// prof.setFile("G.File", this.start.getGrammarFile().getURN());
			prof.setCount("G.Production", this.g.size());
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

	public final Instruction compile() {
		if (compiledCode == null) {
			// NezCompiler bc = Command.ReleasePreview ? new
			// PackratCompiler(this.option) : new PlainCompiler(this.option);
			NezCompiler bc = new PackratCompiler(this.option);
			compiledCode = bc.compile(g);
		}
		return compiledCode.getStartPoint();
	}

	public final boolean perform(Machine machine, SourceContext s, TreeTransducer treeTransducer) {
		Instruction pc = this.compile();
		s.init(getMemoTable(s), treeTransducer);
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

}
