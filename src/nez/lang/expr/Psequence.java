package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.Nez;
import nez.util.UList;

public class Psequence extends Nez.Pair {

	Psequence(SourceLocation s, Expression first, Expression next) {
		super(first, next);
		this.setSourceLocation(s);
	}

	@Override
	public Expression getFirst() {
		return this.first;
	}

	@Override
	public Expression getNext() {
		return this.next;
	}

	// @Override
	// public final void format(StringBuilder sb) {
	// if (this.first instanceof Cbyte && this.next.getFirst() instanceof Cbyte)
	// {
	// sb.append("'");
	// formatString(sb, (Cbyte) this.first, this.next);
	// } else {
	// formatInner(sb, this.first);
	// sb.append(" ");
	// formatInner(sb, this.next);
	// }
	// }
	//
	// private void formatString(StringBuilder sb, Cbyte b, Expression next) {
	// while (b != null) {
	// StringUtils.appendByteChar(sb, b.byteChar, "'");
	// if (next == null) {
	// sb.append("'");
	// return;
	// }
	// Expression first = next.getFirst();
	// b = null;
	// if (first instanceof Cbyte) {
	// b = (Cbyte) first;
	// next = next.getNext();
	// }
	// }
	// sb.append("'");
	// sb.append(" ");
	// formatInner(sb, next);
	// }
	//
	// private void formatInner(StringBuilder sb, Expression e) {
	// if (e instanceof Pchoice) {
	// sb.append("(");
	// e.format(sb);
	// sb.append(")");
	// } else {
	// e.format(sb);
	// }
	// }

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

	// @Override
	// public MozInst encode(AbstractGenerator bc, MozInst next, MozInst
	// failjump) {
	// ParserStrategy option = bc.getStrategy();
	// if (option.Ostring) {
	// Expression e = this.toMultiCharSequence();
	// if (e instanceof Cmulti) {
	// // System.out.println("stringfy .. " + e);
	// return bc.encodeCmulti((Cmulti) e, next, failjump);
	// }
	// }
	// return bc.encodePsequence(this, next, failjump);
	// }

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
			Expression mb = this.newMultiChar(false, toByteSeq(l));
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
		return ExpressionCommons.newCmulti(this.getSourceLocation(), binary, byteSeq);
	}

}
