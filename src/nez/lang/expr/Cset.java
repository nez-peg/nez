package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.GrammarTransducer;
import nez.lang.PossibleAcceptance;
import nez.parser.Instruction;
import nez.parser.AbstractGenerator;
import nez.util.StringUtils;

public class Cset extends Char {
	public boolean[] byteMap; // Immutable

	Cset(SourcePosition s, boolean binary, int beginChar, int endChar) {
		super(s, binary);
		this.byteMap = newMap(false);
		appendRange(this.byteMap, beginChar, endChar);
	}

	Cset(SourcePosition s, boolean binary, boolean[] b) {
		super(s, binary);
		this.byteMap = b;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Cset && this.binary == ((Cset) o).isBinary()) {
			Cset e = (Cset) o;
			for (int i = 0; i < this.byteMap.length; i++) {
				if (this.byteMap[i] != e.byteMap[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public final void format(StringBuilder sb) {
		sb.append(StringUtils.stringfyCharacterClass(this.byteMap));
	}

	@Override
	public Expression reshape(GrammarTransducer m) {
		return m.reshapeCset(this);
	}

	@Override
	public boolean isConsumed() {
		return true;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.acceptByteMap(byteMap, ch);
	}

	@Override
	public Instruction encode(AbstractGenerator bc, Instruction next, Instruction failjump) {
		return bc.encodeCset(this, next, failjump);
	}

	// Utils
	public final static boolean[] newMap(boolean initValue) {
		boolean[] b = new boolean[257];
		if (initValue) {
			for (int i = 0; i < b.length; i++) {
				b[i] = initValue;
			}
		}
		return b;
	}

	public final static void clear(boolean[] byteMap) {
		for (int c = 0; c < byteMap.length; c++) {
			byteMap[c] = false;
		}
	}

	public final static void appendRange(boolean[] b, int beginChar, int endChar) {
		for (int c = beginChar; c <= endChar; c++) {
			b[c] = true;
		}
	}

	public final static void appendBitMap(boolean[] dst, boolean[] src) {
		for (int i = 0; i < 256; i++) {
			if (src[i]) {
				dst[i] = true;
			}
		}
	}

	public final static void reverse(boolean[] byteMap, boolean isBinary) {
		for (int i = 0; i < 256; i++) {
			byteMap[i] = !byteMap[i];
		}
		if (!isBinary) {
			byteMap[0] = false;
		}
	}
}
