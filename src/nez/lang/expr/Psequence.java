package nez.lang.expr;

import nez.Strategy;
import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.GrammarTransducer;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.parser.AbstractGenerator;
import nez.parser.Instruction;
import nez.util.StringUtils;
import nez.util.UList;

public class Psequence extends ExpressionCommons {
	public Expression first;
	public Expression next;

	Psequence(SourcePosition s, Expression first, Expression next) {
		super(s);
		this.first = first;
		this.next = next;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Psequence) {
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
		if (index == 0) {
			this.first = e;
		} else {
			p = this.next;
			this.next = e;
		}
		return p;
	}

	@Override
	public Expression getFirst() {
		return this.first;
	}

	@Override
	public Expression getNext() {
		return this.next;
	}

	public final UList<Expression> toList() {
		UList<Expression> l = ExpressionCommons.newList(4);
		Psequence p = this;
		while (true) {
			l.add(p.getFirst());
			Expression e = p.getNext();
			if (!(e instanceof Psequence)) {
				break;
			}
			p = (Psequence) e;
		}
		l.add(p.getNext());
		return l;
	}

	@Override
	public final void format(StringBuilder sb) {
		if (this.first instanceof Cbyte && this.next.getFirst() instanceof Cbyte) {
			sb.append("'");
			formatString(sb, (Cbyte) this.first, this.next);
		} else {
			formatInner(sb, this.first);
			sb.append(" ");
			formatInner(sb, this.next);
		}
	}

	private void formatString(StringBuilder sb, Cbyte b, Expression next) {
		while (b != null) {
			StringUtils.appendByteChar(sb, b.byteChar, "'");
			if (next == null) {
				sb.append("'");
				return;
			}
			Expression first = next.getFirst();
			b = null;
			if (first instanceof Cbyte) {
				b = (Cbyte) first;
				next = next.getNext();
			}
		}
		sb.append("'");
		sb.append(" ");
		formatInner(sb, next);
	}

	private void formatInner(StringBuilder sb, Expression e) {
		if (e instanceof Pchoice) {
			sb.append("(");
			e.format(sb);
			sb.append(")");
		} else {
			e.format(sb);
		}
	}

	@Override
	public Expression reshape(GrammarTransducer m) {
		return m.reshapePsequence(this);
	}

	@Override
	public boolean isConsumed() {
		if (this.get(0).isConsumed()) {
			return true;
		}
		return this.get(1).isConsumed();
	}

	// @Override
	// boolean setOuterLefted(Expression outer) {
	// for (Expression e : this) {
	// if (e.setOuterLefted(outer)) {
	// return true;
	// }
	// }
	// return false;
	// }

	@Override
	public int inferTypestate(Visa v) {
		for (Expression e : this) {
			int t = e.inferTypestate(v);
			if (t == Typestate.ObjectType || t == Typestate.OperationType) {
				return t;
			}
		}
		return Typestate.BooleanType;
	}

	@Override
	public short acceptByte(int ch) {
		short r = this.first.acceptByte(ch);
		if (r == PossibleAcceptance.Unconsumed) {
			return this.next.acceptByte(ch);
		}
		return r;
	}

	@Override
	public Instruction encode(AbstractGenerator bc, Instruction next, Instruction failjump) {
		Strategy option = bc.getStrategy();
		if (option.isEnabled("Ostr", Strategy.Ostr)) {
			Expression e = this.toMultiCharSequence();
			if (e instanceof Cmulti) {
				// System.out.println("stringfy .. " + e);
				return bc.encodeCmulti((Cmulti) e, next, failjump);
			}
		}
		return bc.encodePsequence(this, next, failjump);
	}

	public final boolean isMultiChar() {
		return (this.getFirst() instanceof Cbyte && this.getNext() instanceof Cbyte);
	}

	public final Expression toMultiCharSequence() {
		Expression f = this.getFirst();
		Expression s = this.getNext().getFirst();
		if (f instanceof Cbyte && s instanceof Cbyte) {
			UList<Byte> l = new UList<Byte>(new Byte[16]);
			l.add(((byte) ((Cbyte) f).byteChar));
			Expression next = convertMultiByte(this, l);
			Expression mb = this.newMultiChar(((Cbyte) f).isBinary(), toByteSeq(l));
			if (next != null) {
				return this.newSequence(mb, next);
			}
			return mb;
		}
		return this;
	}

	private Expression convertMultiByte(Psequence seq, UList<Byte> l) {
		Expression s = seq.getNext().getFirst();
		while (s instanceof Cbyte) {
			l.add((byte) ((Cbyte) s).byteChar);
			Expression next = seq.getNext();
			if (next instanceof Psequence) {
				seq = (Psequence) next;
				s = seq.getNext().getFirst();
				continue;
			}
			return null;
		}
		return seq.getNext();
	}

	private byte[] toByteSeq(UList<Byte> l) {
		byte[] byteSeq = new byte[l.size()];
		for (int i = 0; i < l.size(); i++) {
			byteSeq[i] = l.ArrayValues[i];
		}
		return byteSeq;
	}

	public final Expression newMultiChar(boolean binary, byte[] byteSeq) {
		return ExpressionCommons.newCmulti(this.getSourcePosition(), binary, byteSeq);
	}

}
