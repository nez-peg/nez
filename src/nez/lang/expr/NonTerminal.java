package nez.lang.expr;

import nez.Grammar;
import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.GrammarTransducer;
import nez.lang.PossibleAcceptance;
import nez.lang.Production;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.main.Verbose;
import nez.parser.AbstractGenerator;
import nez.parser.Instruction;

public class NonTerminal extends ExpressionCommons {
	private Grammar g;
	private String localName;
	private String uniqueName;
	private Production deref = null;

	public NonTerminal(SourcePosition s, Grammar g, String ruleName) {
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

	public final Grammar getGrammar() {
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
	public Expression reshape(GrammarTransducer m) {
		return m.reshapeNonTerminal(this);
	}

	@Override
	public boolean isConsumed() {
		return this.getProduction().isConsumed();
	}

	@Override
	public int inferTypestate(Visa v) {
		Production p = this.getProduction();
		if (p == null) {
			if (!this.isTerminal()) {
				Verbose.debug("** unresolved name: " + this.getLocalName());
			}
			return Typestate.BooleanType;
		}
		return p.inferTypestate(v);
	}

	@Override
	public short acceptByte(int ch) {
		try {
			return this.deReference().acceptByte(ch);
		} catch (StackOverflowError e) {
			Verbose.debug(e + " at " + this.getLocalName());
			return PossibleAcceptance.Accept;
		}
	}

	@Override
	public Instruction encode(AbstractGenerator bc, Instruction next, Instruction failjump) {
		return bc.encodeNonTerminal(this, next, failjump);
	}

	public final NonTerminal newNonTerminal(String localName) {
		return ExpressionCommons.newNonTerminal(this.getSourcePosition(), this.getGrammar(), localName);
	}

}
