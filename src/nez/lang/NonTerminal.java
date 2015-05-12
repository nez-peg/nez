package nez.lang;

import java.util.TreeMap;

import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.UList;
import nez.util.UMap;

public class NonTerminal extends Expression {
	public NameSpace ns;
	public String  localName;
	String  uniqueName;

	public NonTerminal(SourcePosition s, NameSpace ns, String ruleName) {
		super(s);
		this.ns = ns;
		this.localName = ruleName;
		this.uniqueName = this.ns.uniqueName(this.localName);
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
	public String getInterningKey() {
		return getUniqueName();
	}

	@Override
	public String getPredicate() {
		return getUniqueName();
	}

	public final String getLocalName() {
		return localName;
	}

	public final String getUniqueName() {
		return this.uniqueName;
	}
	
	public final Production getRule() {
		return this.ns.getProduction(this.localName);
	}
	
	public final Expression deReference() {
		Production r = this.ns.getProduction(this.localName);
		return (r != null) ? r.getExpression() : null;
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
		Production r = this.getRule();
		if(r != null) {
			return r.checkAlwaysConsumed(checker, startNonTerminal, stack);
		}
		return false;
	}

	@Override void checkPhase1(GrammarChecker checker, String ruleName, UMap<String> visited, int depth) {
		Production r = this.getRule();
		if(r == null) {
			checker.reportWarning(s, "undefined rule: " + this.localName + " => created empty rule!!");
			r = this.ns.newRule(this.localName, Factory.newEmpty(s));
		}
		if(depth == 0) {
			r.refCount += 1;
		}
		if(!r.isRecursive) {
			String u = r.getUniqueName();
			if(u.equals(ruleName)) {
				r.isRecursive = true;
				if(r.isInline) {
					checker.reportError(s, "recursion disallows inlining " + r.getLocalName());
					r.isInline = false;
				}
			}
			if(!visited.hasKey(u)) {
				visited.put(u, ruleName);
				checker.checkPhase1(r.getExpression(), ruleName, visited, depth+1);
			}
		}
	}

	@Override
	public int inferTypestate(UMap<String> visited) {
		Production r = this.getRule();
		return r.inferTypestate(visited);
	}
	@Override
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		Production r = this.getRule();
		int t = r.inferTypestate();
		if(t == Typestate.BooleanType) {
			return this;
		}
		if(c.required == Typestate.ObjectType) {
			if(t == Typestate.OperationType) {
				checker.reportWarning(s, "unexpected AST operations => removed!!");
				return this.removeASTOperator(Expression.CreateNonTerminal);
			}
			c.required = Typestate.OperationType;
			return this;
		}
		if(c.required == Typestate.OperationType) {
			if(t == Typestate.ObjectType) {
				checker.reportWarning(s, "expected @ => inserted!!");
				return Factory.newLink(this.s, this, -1);
			}
		}
		return this;
	}
	@Override
	public Expression removeASTOperator(boolean newNonTerminal) {
		if(newNonTerminal) {
			Production r = (Production)this.getRule().removeASTOperator(newNonTerminal);
			if(!this.localName.equals(r.getLocalName())) {
				return Factory.newNonTerminal(this.s, ns, r.getLocalName());
			}
		}
		return this;
	}
	@Override
	public Expression removeFlag(TreeMap<String,String> undefedFlags) {
		Production r = (Production)this.getRule().removeFlag(undefedFlags);
		if(!this.localName.equals(r.getLocalName())) {
			return Factory.newNonTerminal(this.s, ns, r.getLocalName());
		}
		return this;
	}
	
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
	public Instruction encode(RuntimeCompiler bc, Instruction next) {
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
