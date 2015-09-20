package nez.lang.expr;

import nez.Grammar;
import nez.ast.SourcePosition;
import nez.ast.Symbol;
import nez.lang.Contextual;
import nez.lang.Expression;
import nez.lang.GrammarTransducer;
import nez.lang.PossibleAcceptance;
import nez.lang.Production;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.main.Verbose;
import nez.parser.AbstractGenerator;
import nez.parser.Instruction;

public class Xis extends Term implements Contextual {
	public final Symbol tableName;
	final Grammar g;
	public final boolean is;

	Xis(SourcePosition s, Grammar g, Symbol tableName, boolean is) {
		super(s);
		this.g = g;
		this.tableName = tableName;
		this.is = is;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Xis) {
			Xis e = (Xis) o;
			return this.tableName == e.tableName && this.g == e.g && this.is == e.is;
		}
		return false;
	}

	public final Grammar getGrammar() {
		return g;
	}

	public final Symbol getTable() {
		return tableName;
	}

	public final String getTableName() {
		return tableName.getSymbol();
	}

	public final Expression getSymbolExpression() {
		Production p = g.getProduction(tableName.getSymbol());
		if (p == null) {
			Verbose.debug("no symbol expression: " + tableName.getSymbol());
		}
		return p.getExpression();
	}

	@Override
	public Expression reshape(GrammarTransducer m) {
		return m.reshapeXis(this);
	}

	@Override
	public boolean isConsumed() {
		// Expression inner = this.getSymbolExpression();
		// if (inner != null) {
		// return inner.isConsumed();
		// }
		return false;
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.BooleanType;
	}

	@Override
	public short acceptByte(int ch) {
		// if(this.getSymbolExpression() != null) {
		// return this.getSymbolExpression().acceptByte(ch);
		// }
		return PossibleAcceptance.Accept;
	}

	@Override
	public Instruction encode(AbstractGenerator bc, Instruction next, Instruction failjump) {
		return bc.encodeXis(this, next, failjump);
	}
}