package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.ast.SymbolId;
import nez.lang.Expression;
import nez.lang.GrammarTransducer;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.parser.AbstractGenerator;
import nez.parser.Instruction;

public class Ttag extends Term {
	public SymbolId tag;

	Ttag(SourcePosition s, SymbolId tag) {
		super(s);
		this.tag = tag;
	}

	Ttag(SourcePosition s, String name) {
		this(s, SymbolId.tag(name));
	}

	public final String getTagName() {
		return tag.getSymbol();
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Ttag) {
			return this.tag == ((Ttag) o).tag;
		}
		return false;
	}

	@Override
	public final void format(StringBuilder sb) {
		sb.append("#" + tag.getSymbol());
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.OperationType;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.Unconsumed;
	}

	@Override
	public Expression reshape(GrammarTransducer m) {
		return m.reshapeTtag(this);
	}

	@Override
	public Instruction encode(AbstractGenerator bc, Instruction next, Instruction failjump) {
		return bc.encodeTtag(this, next);
	}
}