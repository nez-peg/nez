package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.ast.SymbolId;
import nez.lang.Contextual;
import nez.lang.Expression;
import nez.lang.ExpressionTransducer;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Xmatch extends Term implements Contextual {
	public final SymbolId tableName;

	Xmatch(SourcePosition s, SymbolId tableName) {
		super(s);
		this.tableName = tableName;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Xmatch) {
			Xmatch e = (Xmatch) o;
			return this.tableName == e.tableName;
		}
		return false;
	}

	public final SymbolId getTable() {
		return tableName;
	}

	public final String getTableName() {
		return tableName.getSymbol();
	}

	@Override
	public Expression reshape(ExpressionTransducer m) {
		return m.reshapeXmatch(this);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.BooleanType;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.Accept;
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeXmatch(this, next, failjump);
	}
}
