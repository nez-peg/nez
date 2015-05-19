package nez.lang;

import nez.ast.Source;
import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.NezCompiler;
import nez.util.UFlag;
import nez.util.UList;
import nez.util.UMap;

public class Not extends Unary {
	Not(SourcePosition s, Expression e) {
		super(s, e);
	}
	@Override
	public String getPredicate() { 
		return "!";
	}
	@Override
	public String key() { 
		return "!";
	}
	@Override
	public Expression reshape(Manipulator m) {
		return m.reshapeNot(this);
	}

	@Override
	public boolean isConsumed(Stacker stacker) {
		return false;
	}

	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
//		if(checker != null) {
//			this.inner.checkAlwaysConsumed(checker, startNonTerminal, stack);
//		}
		return false;
	}
	@Override
	public int inferTypestate(UMap<String> visited) {
		return Typestate.BooleanType;
	}
	@Override
	public short acceptByte(int ch, int option) {
		/* The code below works only if a single character in !(e) */
		/* we must accept 'i' for !'int' 'i' */
		Expression p = this.inner.optimize(option);
		if(p instanceof Choice) {
			for(Expression pp : p) {
				short r = acceptByte(pp, ch, option);
				if(r != Prediction.Unconsumed) {
					return r;
				}
			}
			return Prediction.Unconsumed;
		}
		else {
			return acceptByte(p, ch, option);
		}
	}
	private short acceptByte(Expression p, int ch, int option) {
		if(p instanceof ByteChar) {
			return ((ByteChar) p).byteChar == ch ? Prediction.Reject : Prediction.Unconsumed;
		}
		if(p instanceof ByteMap) {
			return ((ByteMap) p).byteMap[ch] ? Prediction.Reject : Prediction.Unconsumed;
		}
		if(p instanceof AnyChar) {
			if(ch == Source.BinaryEOF) return Prediction.Accept;
			if(ch == 0 && !UFlag.is(option, Grammar.Binary)) return Prediction.Accept;
			return Prediction.Reject;
		}
		return Prediction.Unconsumed;
	}

	@Override
	public Instruction encode(NezCompiler bc, Instruction next) {
		return bc.encodeNot(this, next);
	}
	@Override
	protected int pattern(GEP gep) {
		int max = 0;
		for(Expression p: this) {
			int c = p.pattern(gep);
			if(c > max) {
				max = c;
			}
		}
		return max;
	}

	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
	}



}