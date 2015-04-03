package nez.expr;

import java.util.TreeMap;

import nez.Production;
import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.UFlag;
import nez.util.UList;
import nez.util.UMap;

public class Choice extends SequentialExpression {
	Choice(SourcePosition s, UList<Expression> l, int size) {
		super(s, l, size);
	}
	@Override
	public String getPredicate() {
		return "/";
	}
	@Override
	public String getInterningKey() {
		return "/";
	}
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		boolean afterAll = true;
		for(Expression e: this) {
			if(!e.checkAlwaysConsumed(checker, startNonTerminal, stack)) {
				if(stack == null) {  // reconfirm 
					return false;
				}
				afterAll = false;
			}
		}
		return afterAll;
	}
	@Override
	public int inferTypestate(UMap<String> visited) {
		if(this.size() > 0) {
			return this.get(0).inferTypestate(visited);
		}
		return Typestate.BooleanType;
	}
	@Override
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		int required = c.required;
		UList<Expression> l = newList();
		for(Expression e : this) {
			c.required = required;
			Factory.addChoice(l, e.checkTypestate(checker, c));
		}
		return Factory.newChoice(this.s, l);
	}
	@Override
	public Expression removeASTOperator(boolean newNonTerminal) {
		UList<Expression> l = newList();
		for(Expression e : this) {
			Factory.addChoice(l, e.removeASTOperator(newNonTerminal));
		}
		return Factory.newChoice(this.s, l);
	}
	@Override
	public Expression removeFlag(TreeMap<String, String> undefedFlags) {
		UList<Expression> l = new UList<Expression>(new Expression[this.size()]);
		for(Expression e : this) {
			Factory.addChoice(l, e.removeFlag(undefedFlags));
		}
		return Factory.newChoice(this.s, l);
	}
	@Override
	public short acceptByte(int ch, int option) {
		boolean hasUnconsumed = false;
		for(int i = 0; i < this.size(); i++) {
			short r = this.get(i).acceptByte(ch, option);
			if(r == Prediction.Accept) {
				return r;
			}
			if(r == Prediction.Unconsumed) {
				hasUnconsumed = true;
			}
		}
		return hasUnconsumed ? Prediction.Unconsumed : Prediction.Reject;
	}

	@Override
	public Instruction encode(RuntimeCompiler bc, Instruction next) {
		return bc.encodeChoice(this, next);
	}
	
	// optimize
	public Expression[] matchCase = null;
//	boolean selfChoice = false;
//	int startIndex = -1;
//	int endIndex = 257;
	
	@Override
	void optimizeImpl(int option) {
		this.optimized = flatten();
		if(UFlag.is(option, Production.Optimization) && this.optimized instanceof Choice) {
			Expression p = ((Choice)this.optimized).toByteMap(option);
			if(p != null) {
				this.optimized = p;
				return;
			}
		}
		if(UFlag.is(option, Production.Prediction) && !UFlag.is(option, Production.DFA)) {
			Expression fails = Factory.newFailure(s);
			this.matchCase = new Expression[257];
			for(int ch = 0; ch <= 256; ch++) {
				Expression selected = selectChoice(ch, fails, option);
				matchCase[ch] = selected;
			}
		}
	}
	
	public final Choice flatten() {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		flatten(this, l);
		Expression e = Factory.newChoice(s, l);
		//System.out.println("Flatten: " + this + "\n => " + e);
		if(e instanceof Choice) {
			return (Choice)e;
		}
		return this;
	}

	private void flatten(Choice p, UList<Expression> l) {
		for(Expression e: p) {
			e = Factory.resolveNonTerminal(e);
			if(e instanceof Choice) {
				flatten((Choice)e, l);
			}
			else {
				l.add(e);
			}
		}
	}
	
	public ByteMap toByteMap(int option) {
		boolean byteMap[] = ByteMap.newMap(false);
		for(Expression e : this) {
			e = e.optimize(option);
			if(e instanceof ByteChar) {
				byteMap[((ByteChar) e).byteChar] = true;
				continue;
			}
			if(e instanceof ByteMap) {
				ByteMap.appendBitMap(byteMap, ((ByteMap)e).byteMap);
				continue;
			}
			return null;
		}
		return (ByteMap)Factory.newByteMap(s, byteMap);
	}
	
	private Expression selectChoice(int ch, Expression failed, int option) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		selectChoice(ch, failed, l, option);
		if(l.size() == 0) {
			return failed;
		}
		return Factory.newChoice(s, l);
	}

	private void selectChoice(int ch, Expression failed, UList<Expression> l, int option) {
		for(Expression e : this) {
			e = Factory.resolveNonTerminal(e);
			if(e instanceof Choice) {
				((Choice)e).selectChoice(ch, failed, l, option);
			}
			else {
				short r = e.acceptByte(ch, option);
				//System.out.println("~ " + GrammarFormatter.stringfyByte(ch) + ": r=" + r + " in " + e);
				if(r != Prediction.Reject) {
					l.add(e);
				}
			}
		}
	}
	
	@Override
	protected int pattern(GEP gep) {
		return this.size();
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		this.get(p % size()).examplfy(gep, sb, p);
	}

}
