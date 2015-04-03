package nez.expr;

import java.util.TreeMap;

import nez.Grammar;
import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.UList;
import nez.util.UMap;

public class NonTerminal extends Expression {
	public Grammar peg;
	public String  ruleName;
	String  uniqueName;
	public NonTerminal(SourcePosition s, Grammar peg, String ruleName) {
		super(s);
		this.peg = peg;
		this.ruleName = ruleName;
		this.uniqueName = this.peg.uniqueName(this.ruleName);
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
		return ruleName;
	}

	public final String getUniqueName() {
		return this.uniqueName;
	}
	
	public final Rule getRule() {
		return this.peg.getRule(this.ruleName);
	}
	
	public final Expression deReference() {
		Rule r = this.peg.getRule(this.ruleName);
		return (r != null) ? r.getExpression() : null;
	}
	
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		if(checker != null) {
//			checkPhase1(checker, ruleName, visited);
			if(startNonTerminal != null && startNonTerminal.equals(this.uniqueName)) {
				checker.reportError(s, "left recursion: " + this.ruleName);
				checker.foundFatalError();
				return false;
			}
		}
		Rule r = this.getRule();
		if(r != null) {
			return r.checkAlwaysConsumed(checker, startNonTerminal, stack);
		}
		return false;
	}

	@Override void checkPhase1(GrammarChecker checker, String ruleName, UMap<String> visited, int depth) {
		Rule r = this.getRule();
		if(r == null) {
			checker.reportWarning(s, "undefined rule: " + this.ruleName + " => created empty rule!!");
			r = this.peg.newRule(this.ruleName, Factory.newEmpty(s));
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
		Rule r = this.getRule();
		return r.inferTypestate(visited);
	}
	@Override
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		Rule r = this.getRule();
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
			Rule r = (Rule)this.getRule().removeASTOperator(newNonTerminal);
			if(!this.ruleName.equals(r.getLocalName())) {
				return Factory.newNonTerminal(this.s, peg, r.getLocalName());
			}
		}
		return this;
	}
	@Override
	public Expression removeFlag(TreeMap<String,String> undefedFlags) {
		Rule r = (Rule)this.getRule().removeFlag(undefedFlags);
		if(!this.ruleName.equals(r.getLocalName())) {
			return Factory.newNonTerminal(this.s, peg, r.getLocalName());
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
