package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.GrammarMap;
import nez.lang.GrammarReshaper;
import nez.lang.PossibleAcceptance;
import nez.lang.Production;
import nez.lang.Visa;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class NonTerminal extends Expression {
	private GrammarMap g;
	private String localName;
	private String uniqueName;
	private Production deref = null;

	public NonTerminal(SourcePosition s, GrammarMap g, String ruleName) {
		super(s);
		this.g = g;
		this.localName = ruleName;
		this.uniqueName = this.g.uniqueName(this.localName);
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof NonTerminal) {
			return this.localName.equals(((NonTerminal) o).getLocalName());
		}
		return false;
	}

	public final GrammarMap getGrammar() {
		return g;
	}

	public final String getLocalName() {
		return localName;
	}

	public final boolean isTerminal() {
		return localName.startsWith("\"");
	}

	public final String getUniqueName() {
		return this.uniqueName;
	}

	public final boolean syncProduction() {
		Production p = this.g.getProduction(this.localName);
		boolean sync = (deref != p);
		this.deref = p;
		return sync;
	}

	public final Production getProduction() {
		if (deref != null) {
			return deref;
		}
		return this.g.getProduction(this.localName);
	}

	public final Expression deReference() {
		Production r = this.g.getProduction(this.localName);
		return (r != null) ? r.getExpression() : null;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public Expression get(int index) {
		return null;
	}

	@Override
	public final void format(StringBuilder sb) {
		sb.append(this.getLocalName());
	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeNonTerminal(this);
	}

	@Override
	public boolean isConsumed() {
		return this.getProduction().isConsumed();
	}

	@Override
	public int inferTypestate(Visa v) {
		return this.getProduction().inferTypestate(v);
	}

	@Override
	public short acceptByte(int ch) {
		try {
			return this.deReference().acceptByte(ch);
		} catch (StackOverflowError e) {
			System.out.println(e + " at " + this.getLocalName());
			return PossibleAcceptance.Accept;
		}
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeNonTerminal(this, next, failjump);
	}

	public final Expression newNonTerminal(String localName) {
		return ExpressionCommons.newNonTerminal(this.getSourcePosition(), this.getGrammar(), localName);
	}

}
