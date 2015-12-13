package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.ast.Symbol;
import nez.lang.Contextual;
import nez.lang.Expression;
import nez.lang.ExpressionVisitor;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.parser.AbstractGenerator;
import nez.parser.moz.MozInst;

public class Xmatch extends Term implements Contextual {
	public final Symbol tableName;

	Xmatch(SourcePosition s, Symbol tableName) {
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

	public final Symbol getTable() {
		return tableName;
	}

	public final String getTableName() {
		return tableName.getSymbol();
	}

	@Override
	public Object visit(ExpressionVisitor v, Object a) {
		return v.visitXmatch(this, a);
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
	public MozInst encode(AbstractGenerator bc, MozInst next, MozInst failjump) {
		return bc.encodeXmatch(this, next, failjump);
	}
}
