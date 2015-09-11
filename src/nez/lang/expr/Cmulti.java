package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.GrammarTransducer;
import nez.lang.PossibleAcceptance;
import nez.util.StringUtils;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Cmulti extends Char {
	public byte[] byteSeq;

	Cmulti(SourcePosition s, boolean binary, byte[] byteSeq) {
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
	public Expression reshape(GrammarTransducer m) {
		return m.reshapeCmulti(this);
	}

	@Override
	public boolean isConsumed() {
		return true;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.acceptByteChar(byteSeq[0] & 0xff, ch);
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeCmulti(this, next, failjump);
	}
}
