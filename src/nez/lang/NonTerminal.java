package nez.lang;

import java.util.TreeMap;

import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.NezCompiler;
import nez.util.UList;
import nez.util.UMap;

public class NonTerminal extends Expression {
	private NameSpace ns;
	private String  localName;
	private String  uniqueName;

	public NonTerminal(SourcePosition s, NameSpace ns, String ruleName) {
		super(s);
		this.ns = ns;
		this.localName = ruleName;
		this.uniqueName = this.ns.uniqueName(this.localName);
	}
	
	public final NameSpace getNameSpace() {
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
	
	public final Production getProduction() {
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
	public String key() {
		return getUniqueName();
	}

	@Override
	public String getPredicate() {
		return getUniqueName();
	}
	@Override
	public Expression reshape(Manipulator m) {
		return m.reshapeNonTerminal(this);
	}
	
	@Override
	public boolean isConsumed(Stacker stacker) {
		Production p = this.getProduction();
		if(stacker != null) {
			if(stacker.isVisited(p)) {
				this.ns.reportError(this, "left recursion: " + this.localName);
				return false;
			}
		}
		if(p.minlen == -1) {
			return p.isConsumed(new Stacker(p, stacker));
		}
		return p.isConsumed(stacker);
	}

	
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		if(checker != null) {
//			checkPhase1(checker, ruleName, visited);
			if(startNonTerminal != null && startNonTerminal.equals(this.uniqueName)) {
				checker.reportError(s, "left recursion: " + this.localName);
				checker.foundFatalError();
				return false;
			}
		}
		Production r = this.getProduction();
		if(r != null) {
			return r.checkAlwaysConsumed(checker, startNonTerminal, stack);
		}
		return false;
	}

	@Override
	public int inferTypestate(UMap<String> visited) {
		Production r = this.getProduction();
		return r.inferTypestate(visited);
	}
//	@Override
//	public Expression removeASTOperator(boolean newNonTerminal) {
//		if(newNonTerminal) {
//			Production r = (Production)this.getProduction().removeASTOperator(newNonTerminal);
//			if(!this.localName.equals(r.getLocalName())) {
//				return Factory.newNonTerminal(this.s, ns, r.getLocalName());
//			}
//		}
//		return this;
//	}
//	@Override
//	public Expression removeFlag(TreeMap<String,String> undefedFlags) {
//		Production r = (Production)this.getProduction().removeFlag(undefedFlags);
//		if(!this.localName.equals(r.getLocalName())) {
//			return Factory.newNonTerminal(this.s, ns, r.getLocalName());
//		}
//		return this;
//	}
	
	@Override
	public short acceptByte(int ch, int option) {
		try {
			return this.deReference().acceptByte(ch, option);
		}
		catch(StackOverflowError e) {
			System.out.println(e + " at " + this.getLocalName());
			return Prediction.Accept;
		}
	}
	@Override
	void optimizeImpl(int option) {
		Expression e = this;
		while(e instanceof NonTerminal) {
			NonTerminal nterm = (NonTerminal) e;
			e = nterm.deReference().optimize(option);
		}
		this.optimized = e;
	}
	@Override
	public Instruction encode(NezCompiler bc, Instruction next) {
		return bc.encodeNonTerminal(this, next);
	}
	@Override
	protected int pattern(GEP gep) {
		return this.deReference().pattern(gep);
	}

	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		this.deReference().examplfy(gep, sb, p);
	}



}
