package nez.lang;

import nez.ast.SourcePosition;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class NonTerminal extends Expression {
	private GrammarFile ns;
	private String localName;
	private String uniqueName;
	private Production deref = null;

	public NonTerminal(SourcePosition s, GrammarFile ns, String ruleName) {
		super(s);
		this.ns = ns;
		this.localName = ruleName;
		this.uniqueName = this.ns.uniqueName(this.localName);
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if(o instanceof NonTerminal) {
			return this.localName.equals(((NonTerminal)o).getLocalName());
		}
		return false;
	}

	public final GrammarFile getGrammarFile() {
		return ns;
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
		Production p = this.ns.getProduction(this.localName);
		boolean sync = (deref != p);
		this.deref = p;
		return sync;
	}

	public final Production getProduction() {
		if(deref != null) {
			return deref;
		}
		return this.ns.getProduction(this.localName);
	}

	public final Expression deReference() {
		Production r = this.ns.getProduction(this.localName);
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
	protected final void format(StringBuilder sb) {
		sb.append(this.getLocalName());
	}

	@Override
	public String key() {
		return getUniqueName();
	}

	@Override
	public String getPredicate() {
		return getUniqueName();
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
		}
		catch(StackOverflowError e) {
			System.out.println(e + " at " + this.getLocalName());
			return PossibleAcceptance.Accept;
		}
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeNonTerminal(this, next, failjump);
	}

	@Override
	protected int pattern(GEP gep) {
		return this.deReference().pattern(gep);
	}

	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		this.deReference().examplfy(gep, sb, p);
	}

	public final Expression newNonTerminal(String localName) {
		return GrammarFactory.newNonTerminal(this.getSourcePosition(), this.getGrammarFile(), localName);
	}

}
