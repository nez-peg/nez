package nez.lang;

import nez.ast.Source;
import nez.ast.SourcePosition;
import nez.util.UFlag;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Instruction;
import nez.vm.NezCompiler;

public class AnyChar extends Terminal {
	AnyChar(SourcePosition s) {
		super(s);
	}

	@Override
	public String getPredicate() {
		return "any";
	}
	
	@Override
	public String key() { 
		return ".";
	}
	
	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeAnyChar(this);
	}

	@Override
	public boolean isConsumed(Stacker stacker) {
		return true;
	}

	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return true;
	}
	@Override
	public int inferTypestate(UMap<String> visited) {
		return Typestate.BooleanType;
	}
	
	@Override
	public short acceptByte(int ch, int option) {
		if(UFlag.is(option, Grammar.Binary)) {
			return (ch == Source.BinaryEOF) ? Prediction.Reject : Prediction.Accept;
		}
		else {
			return (ch == Source.BinaryEOF || ch == 0) ? Prediction.Reject : Prediction.Accept;
		}
	}
	
	@Override
	public Instruction encode(NezCompiler bc, Instruction next, Instruction failjump) {
		return bc.encodeMatchAny(this, next, failjump);
	}
	@Override
	protected int pattern(GEP gep) {
		return 1;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		sb.append(".");
	}
}