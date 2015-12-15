package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;

import nez.lang.PossibleAcceptance;
import nez.util.StringUtils;

public class Cmulti extends Char {
	public byte[] byteSeq;

	Cmulti(SourceLocation s, boolean binary, byte[] byteSeq) {
		super(s, binary);
		this.byteSeq = byteSeq;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Cmulti) {
			Cmulti mb = (Cmulti) o;
			if (mb.byteSeq.length == this.byteSeq.length) {
				for (int i = 0; i < this.byteSeq.length; i++) {
					if (byteSeq[i] != mb.byteSeq[i]) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public final void format(StringBuilder sb) {
		sb.append("'");
		for (int i = 0; i < this.byteSeq.length; i++) {
			StringUtils.appendByteChar(sb, byteSeq[i] & 0xff, "\'");
		}
		sb.append("'");
	}

	@Override
	public Object visit(Expression.Visitor v, Object a) {
		return v.visitCmulti(this, a);
	}

	@Override
	public boolean isConsumed() {
		return true;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.acceptByteChar(byteSeq[0] & 0xff, ch);
	}

}
