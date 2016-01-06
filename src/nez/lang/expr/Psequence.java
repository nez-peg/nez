package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.Nez;

class Psequence extends Nez.Pair {

	Psequence(SourceLocation s, Expression first, Expression next) {
		super(first, next);
		this.setSourceLocation(s);
	}

	// @Override
	// public final void format(StringBuilder sb) {
	// if (this.first instanceof Nez.Byte && this.next.get(0) instanceof
	// Nez.Byte)
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
	// Expression first = next.get(0);
	// b = null;
	// if (first instanceof Nez.Byte) {
	// b = (Cbyte) first;
	// next = next.get(1);
	// }
	// }
	// sb.append("'");
	// sb.append(" ");
	// formatInner(sb, next);
	// }
	//
	// private void formatInner(StringBuilder sb, Expression e) {
	// if (e instanceof Nez.Choice) {
	// sb.append("(");
	// e.format(sb);
	// sb.append(")");
	// } else {
	// e.format(sb);
	// }
	// }

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
	// if (e instanceof Nez.MultiByte) {
	// // System.out.println("stringfy .. " + e);
	// return bc.encodeCmulti((Cmulti) e, next, failjump);
	// }
	// }
	// return bc.encodePsequence(this, next, failjump);
	// }

}
