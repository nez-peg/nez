package nez.lang;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class IsSymbol extends Expression implements Contextual {
	public final Tag tableName;
	final GrammarMap g;
	public final boolean is;

	IsSymbol(SourcePosition s, GrammarMap g, Tag tableName, boolean is) {
		super(s);
		this.g = g;
		this.tableName = tableName;
		this.is = is;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof IsSymbol) {
			IsSymbol e = (IsSymbol) o;
			return this.tableName == e.tableName && this.g == e.g && this.is == e.is;
		}
		return false;
	}

	public final GrammarMap getGrammarMap() {
		return g;
	}

	public final Tag getTable() {
		return tableName;
	}

	public final String getTableName() {
		return tableName.getName();
	}

	public final Expression getSymbolExpression() {
		return g.getSymbolExpresion(tableName.getName());
	}

	@Override
	public String getPredicate() {
		return (is ? "is " : "isa ") + tableName.getName();
	}

	@Override
	public String key() {
		return this.getPredicate();
	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeIsSymbol(this);
	}

	@Override
	public boolean isConsumed() {
		Expression inner = this.getSymbolExpression();
		if (inner != null) {
			return inner.isConsumed();
		}
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
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeIsSymbol(this, next, failjump);
	}
}