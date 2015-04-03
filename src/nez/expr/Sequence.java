package nez.expr;

import java.util.TreeMap;

import nez.Production;
import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.UFlag;
import nez.util.UList;
import nez.util.UMap;

public class Sequence extends SequentialExpression {
	Sequence(SourcePosition s, UList<Expression> l) {
		super(s, l, l.size());
	}
	@Override
	public String getPredicate() {
		return "seq";
	}	
	@Override
	public String getInterningKey() {
		return " ";
	}
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		for(Expression e: this) {
			if(e.checkAlwaysConsumed(checker, startNonTerminal, stack)) {
				return true;
			}
		}
		return false;
	}
	@Override
	boolean setOuterLefted(Expression outer) { 
		for(Expression e: this) {
			if(e.setOuterLefted(outer)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Expression removeASTOperator(boolean newNonTerminal) {
		UList<Expression> l = new UList<Expression>(new Expression[this.size()]);
		for(Expression e : this) {
			Factory.addSequence(l, e.removeASTOperator(newNonTerminal));
		}
		return Factory.newSequence(s, l);
	}
	@Override
	public int inferTypestate(UMap<String> visited) {
		for(Expression e: this) {
			int t = e.inferTypestate(visited);
			if(t == Typestate.ObjectType || t == Typestate.OperationType) {
				return t;
			}
		}
		return Typestate.BooleanType;
	}
	@Override
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		UList<Expression> l = newList();
		for(Expression e : this) {
			Factory.addSequence(l, e.checkTypestate(checker, c));
		}
		return Factory.newSequence(s, l);
	}
	@Override
	public Expression removeFlag(TreeMap<String, String> undefedFlags) {
		UList<Expression> l = newList();
		for(int i = 0; i < this.size(); i++) {
			Expression e = get(i).removeFlag(undefedFlags);
			Factory.addSequence(l, e);
		}
		return Factory.newSequence(s, l);
	}

	@Override
	public short acceptByte(int ch, int option) {
		return Prediction.acceptSequence(this, ch, option);
	}

	public final boolean isMultiChar() {
		return this.isMultiChar(0, this.size());
	}

	public final boolean isMultiChar(int start, int end) {
		for(int i = start; i < end; i++) {
			Expression p = this.get(i);
			if(!(p instanceof ByteChar)) {
				return false;
			}
		}
		return true;
	}
	
	public final byte[] extractMultiChar(int start, int end) {
		for(int i = start; i < end; i++) {
			Expression p = this.get(i);
			if(!(p instanceof ByteChar)) {
				end = i;
				break;
			}
		}
		byte[] b = new byte[end - start];
		for(int i = start; i < end; i++) {
			Expression p = this.get(i);
			if(p instanceof ByteChar) {
				b[i - start] = (byte)((ByteChar) p).byteChar;
			}
		}
		return b;
	}
	
	@Override
	void optimizeImpl(int option) {
		if(UFlag.is(option, Production.Optimization) && this.get(this.size() - 1) instanceof AnyChar) {
			boolean byteMap[] = ByteMap.newMap(false);
			if(isByteMap(option, byteMap)) {
				this.optimized = Factory.newByteMap(s, byteMap);
				return;
			}
			// (!'ab' !'ac' .) => (^[a]) / (!'ab' !'ac' .)
			if(UFlag.is(option, Production.Prediction)) {
				ByteMap.clear(byteMap);
				if(isPredictedNotByteMap(0, this.size() - 1, byteMap, option)) {
					this.optimized = Factory.newChoice(s, Factory.newByteMap(s, byteMap), this);
					return;
				}
			}
		}
		if(UFlag.is(option, Production.DFA) && needsReplaceOperation(option)) {
			this.optimized = operationReplacedSequence(option);
			//System.out.println("replaced: " + this + "\n => " + this.optimized);
		}
		else {
			this.optimized = this;
		}
	}

	private boolean needsReplaceOperation(int option) {
		for(int i = 1; i < this.size(); i++) {
			Expression p = this.get(i-1).optimize(option);
			Expression e = this.get(i).optimize(option);
			if(Expression.isByteConsumed(e)) {
				if(Expression.isPositionIndependentOperation(p) || p instanceof New || p instanceof Capture) {
					return true;
				}
			}
		}
		return false;
	}

	private Expression operationReplacedSequence(int option) {
		UList<Expression> l = this.toList();
		for(int i = 1; i < l.size(); i++) {
			Expression p = l.ArrayValues[i-1].optimize(option);
			Expression e = l.ArrayValues[i].optimize(option);
			if(Expression.isByteConsumed(e)) {
				if(Expression.isPositionIndependentOperation(p)) {
					l.ArrayValues[i-1] = e;
					l.ArrayValues[i]   = p;
					continue;
				}
				if(p instanceof New) {
					New n = (New)p;
					l.ArrayValues[i-1] = e;
					if(n.isInterned()) {
						l.ArrayValues[i] =  Factory.newNew(n.s, n.lefted, n.shift - 1);
					}
					else {
						n.shift -= 1;
						l.ArrayValues[i]   = n;
					}
					continue;
				}
				if(p instanceof Capture) {
					Capture n = (Capture)p;
					l.ArrayValues[i-1] = e;
					if(n.isInterned()) {
						l.ArrayValues[i] =  Factory.newCapture(n.s, n.shift - 1);
					}
					else {
						n.shift -= 1;
						l.ArrayValues[i]   = n;
					}
					continue;
				}
			}
		}
		return Factory.newSequence(s, l);
	}

	
	boolean isByteMap(int option, boolean[] byteMap) {
		for(int i = 0; i < this.size() - 1; i++) {
			Expression p = this.get(i).optimize(option);
			if(p instanceof Not) {
				p = p.get(i).optimize(option);
				if(p instanceof ByteChar) {
					byteMap[((ByteChar) p).byteChar] = true;
					continue;
				}
				if(p instanceof ByteMap) {
					ByteMap.appendBitMap(byteMap, ((ByteMap) p).byteMap);
					continue;
				}
			}
			return false;
		}
		ByteMap.reverse(byteMap, option);
		return true;
	}

	
	boolean isPredictedNotByteMap(int start, int end, boolean[] byteMap, int option) {
		for(int i = start; i < end; i++) {
			Expression p = this.get(i); //.optimize(option);
			if(p instanceof Not) {
				p = p.get(i).optimize(option);
				predictByte(p, byteMap, option);
				continue;
			}
			return false;
		}
		ByteMap.reverse(byteMap, option);
		return true;
	}

	void predictByte(Expression e, boolean[] byteMap, int option) {
		for(int c = 0; c < 256; c++) {
			if(e.acceptByte(c, option) != Prediction.Reject) {
				byteMap[c] = true;
			}
		}
	}
	
	//			if(is(O_SpecString)) {
//				byte[] u = new byte[holder.size()];
//				for(int i = 0; i < holder.size(); i++) {
//					ParsingExpression inner = resolveNonTerminal(holder.get(i));				
//					if(inner instanceof ParsingByte) {
//						u[i] = (byte)((ParsingByte) inner).byteChar;
//						continue;
//					}
//					return;
//				}
//				holder.matcher = new StringMatcher(u);
//				CountSpecString += 1;
//				return;
//			}
//		}
//
//		return this;
//	}
//	@Override
//	public boolean match(SourceContext context) {
//		long pos = context.getPosition();
//		int mark = context.startConstruction();
//		for(int i = 0; i < this.size(); i++) {
//			if(!(this.get(i).optimized.match(context))) {
//				context.abortConstruction(mark);
//				context.rollback(pos);
//				return false;
//			}
//		}
//		return true;
//	}
	@Override
	public Instruction encode(RuntimeCompiler bc, Instruction next) {
		return bc.encodeSequence(this, next);
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
		for(Expression e: this) {
			e.examplfy(gep, sb, p);
		}
	}


}
