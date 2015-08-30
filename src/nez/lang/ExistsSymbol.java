package nez.lang;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.util.UList;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class ExistsSymbol extends Expression implements Contextual {
	public final Tag tableName;
	final GrammarFile ns;
	String symbol;

	ExistsSymbol(SourcePosition s, GrammarFile ns, Tag tableName, String symbol) {
		super(s);
		this.ns = ns;
		this.tableName = tableName;
		this.symbol = symbol;
	}

	public final String getSymbol() {
		return this.symbol;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof ExistsSymbol) {
			ExistsSymbol s = (ExistsSymbol) o;
			return this.ns == s.ns && this.tableName == s.tableName && equals(this.symbol, s.symbol);
		}
		return false;
	}

	private boolean equals(String s, String s2) {
		if (s != null && s2 != null) {
			return s.equals(s2);
		}
		return s == s2;
	}

	public final GrammarFile getGrammarFile() {
		return ns;
	}

	public final Tag getTable() {
		return tableName;
	}

	public final String getTableName() {
		return tableName.getName();
	}

	public final Expression getSymbolExpression() {
		return ns.getSymbolExpresion(tableName.getName());
	}

	@Override
	public String getPredicate() {
		return "exists " + tableName.getName();
	}

	@Override
	public String key() {
		return this.getPredicate();
	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeExistsSymbol(this);
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
		if (this.getSymbolExpression() != null) {
			return this.getSymbolExpression().acceptByte(ch);
		}
		return PossibleAcceptance.Accept;
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeExistsSymbol(this, next, failjump);
	}
}
