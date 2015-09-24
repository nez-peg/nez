package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.GrammarTransducer;
import nez.lang.Visa;
import nez.parser.AbstractGenerator;
import nez.parser.Instruction;

public class Xsymbol extends Unary {
	public final Symbol tableName;

	Xsymbol(SourcePosition s, NonTerminal pat) {
		super(s, pat);
		this.tableName = Symbol.tag(pat.getLocalName());
	}

	Xsymbol(SourcePosition s, Symbol tableName, Expression pat) {
		super(s, pat);
		this.tableName = tableName;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Xsymbol) {
			Xsymbol e = (Xsymbol) o;
			if (this.tableName == e.tableName) {
				return this.get(0).equalsExpression(e.get(0));
			}
		}
		return false;
	}

	public final Symbol getTable() {
		return tableName;
	}

	public final String getTableName() {
		return tableName.getSymbol();
	}

	@Override
	public void format(StringBuilder sb) {
		sb.append("<symbol ");
		sb.append(getTableName());
		sb.append(">");
	}

	@Override
	public Expression reshape(GrammarTransducer m) {
		return m.reshapeXdef(this);
	}

	@Override
	public boolean isConsumed() {
		return this.inner.isConsumed();
	}

	@Override
	public int inferTypestate(Visa v) {
		return this.inner.inferTypestate(v);
	}

	@Override
	public short acceptByte(int ch) {
		return this.inner.acceptByte(ch);
	}

	@Override
	public Instruction encode(AbstractGenerator bc, Instruction next, Instruction failjump) {
		return bc.encodeXsymbol(this, next, failjump);
	}

}