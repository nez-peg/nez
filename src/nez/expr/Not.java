package nez.expr;

import nez.Production;
import nez.ast.Source;
import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
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
	public String getInterningKey() { 
		return "!";
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
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		int t = this.inner.inferTypestate(null);
		if(t == Typestate.ObjectType || t == Typestate.OperationType) {
			this.inner = this.inner.removeASTOperator(Expression.CreateNonTerminal);
		}
		return this;
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
			if(ch == 0 && !UFlag.is(option, Production.Binary)) return Prediction.Accept;
			return Prediction.Reject;
		}
		return Prediction.Unconsumed;
	}

	@Override
	Expression dupUnary(Expression e) {
		return (this.inner != e) ? Factory.newNot(this.s, e) : this;
	}

	@Override
	public Instruction encode(RuntimeCompiler bc, Instruction next) {
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