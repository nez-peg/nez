package nez.lang;

import java.util.TreeMap;

import nez.ast.Source;
import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.UFlag;
import nez.util.UList;
import nez.util.UMap;

public class AnyChar extends Terminal {
	AnyChar(SourcePosition s) {
		super(s);
	}
	@Override
	public String getPredicate() {
		return "any";
	}
	@Override
	public String getInterningKey() { 
		return ".";
	}
	@Override
	public Expression reshape(Manipulator m) {
		return m.reshapeAnyChar(this);
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
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		return this;
	}
	@Override
	public Expression removeFlag(TreeMap<String, String> undefedFlags) {
		return this;
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
	public Instruction encode(RuntimeCompiler bc, Instruction next) {
		return bc.encodeMatchAny(this, next);
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