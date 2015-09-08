package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.GrammarReshaper;
import nez.lang.PossibleAcceptance;
import nez.util.StringUtils;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class ByteChar extends Char {
	public int byteChar;

	ByteChar(SourcePosition s, boolean binary, int ch) {
		super(s, binary);
		this.byteChar = ch;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof ByteChar) {
			return this.byteChar == ((ByteChar) o).byteChar && this.binary == ((ByteChar) o).isBinary();
		}
		return false;
	}

	@Override
	public
	final void format(StringBuilder sb) {
		sb.append(StringUtils.stringfyCharacter(this.byteChar));
	}

	@Override
	public String getPredicate() {
		return "byte " + byteChar;
	}

	@Override
	public String key() {
		return binary ? "b'" + byteChar : "'" + byteChar;
	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeByteChar(this);
	}

	@Override
	public boolean isConsumed() {
		return true;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.acceptByteChar(byteChar, ch);
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeByteChar(this, next, failjump);
	}

}
