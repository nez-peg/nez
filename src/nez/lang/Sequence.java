package nez.lang;

import nez.ast.SourcePosition;
import nez.util.StringUtils;
import nez.util.UList;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Sequence extends Expression {
	Expression first;
	Expression next;
	Sequence(SourcePosition s, Expression first, Expression next) {
		super(s);
		this.first = first;
		this.next  = next;
	}
	@Override
	public final boolean equalsExpression(Expression o) {
		if(o instanceof Sequence) {
			return this.get(0).equalsExpression(o.get(0)) && this.get(1).equalsExpression(o.get(1));
		}
		return false;
	}
	@Override
	public final int size() {
		return 2;
	}
	@Override
	public final Expression get(int index) {
		return index == 0 ? this.first : this.next;
	}
	@Override
	public final Expression set(int index, Expression e) {
		Expression p = this.first;
		if(index == 0) {
			this.first = e;
		}
		else {
			p = this.next;
			this.next = e;
		}
		return p;
	}
	
	public Expression getFirst() {
		return this.first;
	}
	public Expression getNext() {
		return this.next;
	}
	@Override
	public String getPredicate() {
		return "seq";
	}	
	@Override
	public String key() {
		return " ";
	}
	@Override
	protected final void format(StringBuilder sb) {
		if(this.first instanceof ByteChar && this.next.getFirst() instanceof ByteChar) {
			sb.append("'");
			formatString(sb, (ByteChar)this.first, this.next);
		}
		else {
			formatInner(sb, this.first);
			sb.append(" ");
			formatInner(sb, this.next);
		}
	}
	
	private void formatString(StringBuilder sb,  ByteChar b, Expression next) {
		while(b != null) {
			StringUtils.appendByteChar(sb, b.byteChar, "'");
			if(next == null) {
				sb.append("'");
				return;
			}
			Expression first = next.getFirst();
			b = null;
			if(first instanceof ByteChar) {
				b = (ByteChar)first;
				next = next.getNext();
			}
		}
		sb.append("'");
		sb.append(" ");
		formatInner(sb, next);
	}

	private void formatInner(StringBuilder sb, Expression e) {
		if(e instanceof Choice || e instanceof Sequence) {
			sb.append("( ");
			e.format(sb);
			sb.append(" )");
		}
		else {	
			e.format(sb);
		}
	}
	
	public final Expression convertToMultiByte() {
		Expression f = this.getFirst();
		Expression s = this.getNext().getFirst();
		if(f instanceof ByteChar || s instanceof ByteChar) {
			UList<Byte> l = new UList<Byte>(new Byte[16]);
			l.add(((byte)((ByteChar)f).byteChar));
			Expression next = convertMultiByte(this, l);
			Expression mb = this.newCharMultiByte(((ByteChar)f).isBinary(), toByteSeq(l));
			if(next != null) {
				return this.newSequence(mb, next);
			}
			return mb;
		}
		return this;
	}

	private Expression newCharMultiByte(boolean binary, byte[] byteSeq) {
		// TODO Auto-generated method stub
		return null;
	}
	private Expression convertMultiByte(Sequence seq, UList<Byte> l) {
		Expression s = seq.getNext().getFirst();
		while(s instanceof ByteChar) {
			l.add((byte)((ByteChar)s).byteChar);
			Expression next = seq.getNext();
			if(next instanceof Sequence) {
				seq = (Sequence)next;
				s = seq.getNext().getFirst();
				continue;
			}
			return null;
		}
		return seq.getNext();
	}
	
	private byte[] toByteSeq(UList<Byte> l) {
		byte[] byteSeq = new byte[l.size()];
		for(int i = 0; i < l.size(); i++) {
			byteSeq[i] = l.ArrayValues[i];
		}
		return byteSeq;
	}
	
	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeSequence(this);
	}

	@Override
	public boolean isConsumed() {
		if(this.get(0).isConsumed()) {
			return true;
		}
		return this.get(1).isConsumed();
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
	public int inferTypestate(Visa v) {
		for(Expression e: this) {
			int t = e.inferTypestate(v);
			if(t == Typestate.ObjectType || t == Typestate.OperationType) {
				return t;
			}
		}
		return Typestate.BooleanType;
	}

	@Override
	public short acceptByte(int ch) {
		short r = this.first.acceptByte(ch);
		if(r == PossibleAcceptance.Unconsumed) {
			return this.next.acceptByte(ch);
		}
		return r;
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
	
	/**
	@Override
	void optimizeImpl(int option) {
		if(UFlag.is(option, Grammar.Optimization) && this.get(this.size() - 1) instanceof AnyChar) {
			boolean byteMap[] = ByteMap.newMap(false);
			if(isByteMap(option, byteMap)) {
				this.optimized = GrammarFactory.newByteMap(s, byteMap);
				return;
			}
			// (!'ab' !'ac' .) => (^[a]) / (!'ab' !'ac' .)
			if(UFlag.is(option, Grammar.Prediction)) {
				ByteMap.clear(byteMap);
				if(isPredictedNotByteMap(0, this.size() - 1, byteMap, option)) {
					this.optimized = GrammarFactory.newChoice(s, GrammarFactory.newByteMap(s, byteMap), this);
					return;
				}
			}
		}
		if(UFlag.is(option, Grammar.DFA) && needsReplaceOperation(option)) {
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
						l.ArrayValues[i] =  GrammarFactory.newNew(n.s, n.lefted, n.shift - 1);
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
						l.ArrayValues[i] =  GrammarFactory.newCapture(n.s, n.shift - 1);
					}
					else {
						n.shift -= 1;
						l.ArrayValues[i]   = n;
					}
					continue;
				}
			}
		}
		return GrammarFactory.newSequence(s, l);
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
			if(e.acceptByte(c, option) != Acceptance.Reject) {
				byteMap[c] = true;
			}
		}
	}
	**/
	
	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeSequence(this, next, failjump);
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
