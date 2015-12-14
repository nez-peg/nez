package nez.lang;

import java.util.List;

import nez.ast.SourcePosition;
import nez.lang.Expression.Conditional;
import nez.lang.Expression.Contextual;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cset;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pand;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pnot;
import nez.lang.expr.Pone;
import nez.lang.expr.Poption;
import nez.lang.expr.Psequence;
import nez.lang.expr.Pzero;
import nez.util.ConsoleUtils;
import nez.util.UFlag;
import nez.util.UList;

public class Production /* extends Expression */{

	public final static int PublicProduction = 1 << 0;
	public final static int TerminalProduction = 1 << 1;
	public final static int SymbolTableProduction = 1 << 2;

	public final static int InlineProduction = 1 << 3;

	public final static int Declared = PublicProduction | TerminalProduction | SymbolTableProduction | InlineProduction;

	public final static int RecursiveChecked = 1 << 4;
	public final static int RecursiveProduction = 1 << 5;

	public final static int ConsumedChecked = 1 << 6;
	public final static int ConsumedProduction = 1 << 7;

	public final static int ASTChecked = 1 << 8;
	public final static int ObjectProduction = 1 << 9;
	public final static int OperationalProduction = 1 << 10;

	public final static int ConditionalChecked = 1 << 16;
	public final static int ConditionalProduction = 1 << 17;
	public final static int ContextualChecked = 1 << 18;
	public final static int ContextualProduction = 1 << 19;

	public final static int ResetFlag = 1 << 30;

	int flag;
	Grammar g;
	String name;
	String uname;
	Expression body;

	Production(SourcePosition s, int flag, Grammar g, String name, Expression body) {
		// super(s);
		this.flag = flag;
		this.g = g;
		this.name = name;
		this.uname = g.uniqueName(name);
		this.body = (body == null) ? ExpressionCommons.newEmpty(s) : body;
		Production.quickCheck(this);
	}

	void resetFlag() {
		this.flag = (this.flag & Declared) | ResetFlag;
	}

	void initFlag() {
		this.flag = this.flag | ConsumedChecked | ConditionalChecked | ContextualChecked | RecursiveChecked;
		this.flag = UFlag.unsetFlag(this.flag, ResetFlag);
		quickCheck(this, this.getExpression());
	}

	public final static void quickCheck(Production p) {
		p.flag = p.flag | ConsumedChecked | ConditionalChecked | ContextualChecked | RecursiveChecked;
		quickCheck(p, p.getExpression());
	}

	public final static void quickCheck(Production p, Expression e) {
		if (e instanceof Cbyte || e instanceof Cset || e instanceof Cany) {
			p.flag = p.flag | ConsumedProduction | ConsumedChecked;
			return;
		}
		if (e instanceof NonTerminal) {
			NonTerminal n = (NonTerminal) e;
			if (n.getUniqueName().equals(p.getUniqueName())) {
				p.flag = RecursiveProduction | RecursiveChecked;
			}
			Production np = n.getProduction();
			if (!UFlag.is(p.flag, ConsumedProduction)) {
				if (np != null && UFlag.is(np.flag, ConsumedChecked)) {
					if (UFlag.is(p.flag, ConsumedProduction)) {
						p.flag = p.flag | ConsumedProduction | ConsumedChecked;
					}
				} else {
					p.flag = UFlag.unsetFlag(p.flag, ConsumedChecked);
				}
			}
			if (!UFlag.is(p.flag, ConditionalProduction)) {
				if (np != null && UFlag.is(np.flag, ConditionalChecked)) {
					if (UFlag.is(p.flag, ConditionalProduction)) {
						p.flag = p.flag | ConditionalChecked | ConditionalProduction;
					}
				} else {
					p.flag = UFlag.unsetFlag(p.flag, ConditionalChecked);
				}
			}
			if (!UFlag.is(p.flag, ContextualProduction)) {
				if (np != null && UFlag.is(np.flag, ContextualChecked)) {
					if (UFlag.is(p.flag, ContextualProduction)) {
						p.flag = p.flag | ContextualChecked | ContextualProduction;
					}
				} else {
					p.flag = UFlag.unsetFlag(p.flag, ContextualChecked);
				}
			}
			if (!UFlag.is(p.flag, RecursiveProduction)) {
				p.flag = UFlag.unsetFlag(p.flag, RecursiveChecked);
			}
			return;
		}
		if (e instanceof Psequence) {
			quickCheck(p, e.get(0));
			quickCheck(p, e.get(1));
			return;
		}
		if (e instanceof Pone) {
			quickCheck(p, e.get(0));
			return;
		}
		if (e instanceof Pnot || e instanceof Poption || e instanceof Pzero || e instanceof Pand) {
			boolean consumed = UFlag.is(p.flag, ConsumedProduction);
			quickCheck(p, e.get(0));
			if (!consumed) {
				p.flag = UFlag.unsetFlag(p.flag, ConsumedProduction);
			}
			return;
		}
		if (e instanceof Pchoice) {
			boolean checkedConsumed = UFlag.is(p.flag, ConsumedProduction);
			if (checkedConsumed) {
				for (Expression sub : e) {
					quickCheck(p, sub);
					p.flag = p.flag | ConsumedProduction;
				}
			} else {
				boolean unconsumed = false;
				for (Expression sub : e) {
					p.flag = UFlag.unsetFlag(p.flag, ConsumedProduction);
					quickCheck(p, sub);
					if (!UFlag.is(p.flag, ConsumedProduction)) {
						unconsumed = true;
					}
				}
				if (unconsumed) {
					p.flag = UFlag.unsetFlag(p.flag, ConsumedProduction);
				} else {
					p.flag = p.flag | ConsumedProduction | ConsumedChecked;
				}
			}
			return;
		}
		if (e instanceof Expression.Conditional) {
			p.flag = p.flag | ConditionalChecked | ConditionalProduction;
		}
		if (e instanceof Expression.Contextual) {
			p.flag = p.flag | ContextualChecked | ContextualProduction;
		}
		for (Expression sub : e) {
			quickCheck(p, sub);
		}
	}

	public final List<String> flag() {
		UList<String> l = new UList<String>(new String[4]);
		if (UFlag.is(this.flag, RecursiveChecked)) {
			if (UFlag.is(this.flag, RecursiveProduction))
				l.add("recursive");
			else
				l.add("nonrecursive");
		}
		if (UFlag.is(this.flag, ConsumedChecked)) {
			if (UFlag.is(this.flag, ConsumedProduction))
				l.add("consumed");
			else
				l.add("unconsumed");
		}
		if (UFlag.is(this.flag, ConditionalChecked)) {
			if (UFlag.is(this.flag, ConditionalProduction))
				l.add("conditional");
			else
				l.add("unconditional");
		}
		if (UFlag.is(this.flag, ContextualChecked)) {
			if (UFlag.is(this.flag, ContextualProduction))
				l.add("contextual");
			else
				l.add("uncontextual");
		}
		return l;
	}

	public final Grammar getGrammar() {
		return this.g;
	}

	public final boolean isPublic() {
		return UFlag.is(this.flag, Production.PublicProduction);
	}

	public final boolean isInline() {
		return UFlag.is(this.flag, Production.InlineProduction);
	}

	public final boolean isTerminal() {
		return UFlag.is(this.flag, Production.TerminalProduction);
	}

	public boolean isSymbolTable() {
		return UFlag.is(this.flag, Production.SymbolTableProduction);
	}

	public final boolean isRecursive() {
		if (!UFlag.is(this.flag, Production.RecursiveChecked)) {
			checkRecursive(this.getExpression(), null);
			this.flag |= Production.RecursiveChecked;
		}
		return UFlag.is(this.flag, Production.RecursiveProduction);
	}

	private void checkRecursive(Expression e, Visa v) {
		if (e instanceof NonTerminal) {
			Production p = ((NonTerminal) e).getProduction();
			if (p == this) {
				this.flag |= Production.RecursiveChecked | Production.RecursiveProduction;
				return;
			}
			if (Visa.isVisited(v, p)) {
				// p.flag |= Production.RecursiveChecked |
				// Production.RecursiveProduction;
				return;
			}
			v = Visa.visited(v, p);
			this.checkRecursive(p.getExpression(), v);
			return;
		}
		for (Expression sub : e) {
			this.checkRecursive(sub, v);
			if (UFlag.is(this.flag, Production.RecursiveProduction)) {
				break;
			}
		}
	}

	public final boolean isConditional() {
		if (!UFlag.is(this.flag, Production.ConditionalChecked)) {
			checkConditional(this.getExpression(), null);
			this.flag |= Production.ConditionalChecked;
			// Verbose.debug("conditional? " + this.getLocalName() + " ? " +
			// this.isConditional());
		}
		return UFlag.is(this.flag, Production.ConditionalProduction);
	}

	private void checkConditional(Expression e, Visa v) {
		if (e instanceof Conditional) {
			this.flag |= Production.ConditionalChecked | Production.ConditionalProduction;
			return;
		}
		if (e instanceof NonTerminal) {
			Production p = ((NonTerminal) e).getProduction();
			if (UFlag.is(p.flag, Production.ConditionalProduction)) {
				this.flag |= Production.ConditionalChecked | Production.ConditionalProduction;
				return;
			}
			if (!UFlag.is(p.flag, Production.ConditionalChecked)) {
				if (Visa.isVisited(v, p)) {
					return;
				}
				v = Visa.visited(v, p);
				p.checkConditional(p.getExpression(), v);
				if (UFlag.is(p.flag, Production.ConditionalProduction)) {
					this.flag |= Production.ConditionalChecked | Production.ConditionalProduction;
					p.flag |= Production.ConditionalChecked;
				}
			}
			return;
		}
		for (Expression sub : e) {
			checkConditional(sub, v);
			if (UFlag.is(this.flag, Production.ConditionalProduction)) {
				break;
			}
		}
	}

	boolean testCondition(Expression e, Visa v) {
		if (e instanceof NonTerminal) {
			Production p = ((NonTerminal) e).getProduction();
			if (!Visa.isVisited(v, p)) {
				v = Visa.visited(v, p);
				boolean r = testCondition(p.getExpression(), v);
				return r;
			}
			return false;
		}
		for (Expression se : e) {
			if (testCondition(se, v)) {
				return true;
			}
		}
		return (e instanceof Conditional);
	}

	public final boolean isContextual() {
		if (!UFlag.is(this.flag, Production.ContextualChecked)) {
			Expression e = this.getExpression();
			if (e == null) {
				return false;
			}
			checkContextual(e, null);
			this.flag |= Production.ContextualChecked;
		}
		return UFlag.is(this.flag, Production.ContextualProduction);
	}

	private void checkContextual(Expression e, Visa v) {
		if (e instanceof Contextual) {
			this.flag |= Production.ContextualChecked | Production.ContextualProduction;
			return;
		}
		if (e instanceof NonTerminal) {
			Production p = ((NonTerminal) e).getProduction();
			if (p == null) {
				return;
			}
			if (UFlag.is(p.flag, Production.ContextualProduction)) {
				this.flag |= Production.ContextualChecked | Production.ContextualProduction;
				return;
			}
			if (!UFlag.is(p.flag, Production.ContextualChecked)) {
				if (Visa.isVisited(v, p)) {
					return;
				}
				v = Visa.visited(v, p);
				p.checkContextual(p.getExpression(), v);
				if (UFlag.is(p.flag, Production.ContextualProduction)) {
					this.flag |= Production.ContextualChecked | Production.ContextualProduction;
					p.flag |= Production.ContextualChecked;
				}
			}
			return;
		}
		for (Expression sub : e) {
			checkContextual(sub, v);
			if (UFlag.is(this.flag, Production.ContextualProduction)) {
				break;
			}
		}
	}

	boolean testContextSensitive(Expression e, Visa v) {
		if (e instanceof NonTerminal) {
			Production p = ((NonTerminal) e).getProduction();
			if (!Visa.isVisited(v, p)) {
				v = Visa.visited(v, p);
				boolean r = testContextSensitive(p.getExpression(), v);
				return r;
			}
			return false;
		}
		for (Expression se : e) {
			if (testContextSensitive(se, v)) {
				return true;
			}
		}
		return (e instanceof Contextual);
	}

	public final String getUniqueName() {
		return this.uname;
	}

	public final String getLocalName() {
		return this.name;
	}

	public final Expression getExpression() {
		return this.body;
	}

	public final void setExpression(Expression e) {
		this.body = e;
	}

	public boolean isConsumed() {
		if (!UFlag.is(this.flag, ConsumedChecked)) {
			checkConsumed(this.getExpression(), new ProductionStacker(this, null));
		}
		return UFlag.is(this.flag, ConsumedProduction);
	}

	private boolean checkConsumed(Expression e, ProductionStacker s) {
		if (e instanceof NonTerminal) {
			NonTerminal n = (NonTerminal) e;
			Production p = n.getProduction();
			if (UFlag.is(p.flag, ConsumedChecked)) {
				return p.isConsumed();
			}
			if (s.isVisited(p)) {
				// left recursion
				return false;
			}
			if (p.isRecursive()) {
				return checkConsumed(p.getExpression(), new ProductionStacker(p, s));
			}
			return p.isConsumed();
		}
		if (e instanceof Psequence) {
			if (checkConsumed(e.get(0), s)) {
				return true;
			}
			return checkConsumed(e.get(1), s);
		}
		if (e instanceof Pchoice) {
			boolean consumed = true;
			for (Expression se : e) {
				if (!checkConsumed(se, s)) {
					consumed = false;
				}
			}
			return consumed;
		}
		if (e.size() > 0) {
			if (e instanceof Pone) {
				return checkConsumed(e.get(0), s);
			}
			if (e instanceof Pnot || e instanceof Poption || e instanceof Pzero || e instanceof Pand) {
				return false;
			}
			return checkConsumed(e.get(0), s);
		}
		return e.isConsumed();
	}

	public final boolean isNoNTreeConstruction() {
		if (!UFlag.is(this.flag, ASTChecked)) {
			checkTypestate();
		}
		return !UFlag.is(this.flag, ObjectProduction) && !UFlag.is(this.flag, OperationalProduction);
	}

	private void checkTypestate() {
		int t = inferTypestate(null);
		if (t == Typestate.Tree) {
			this.flag |= ObjectProduction;
		}
		if (t == Typestate.TreeMutation) {
			this.flag |= OperationalProduction;
		}
		this.flag |= ASTChecked;
	}

	// @Override
	public int inferTypestate(Visa v) {
		if (UFlag.is(this.flag, ASTChecked)) {
			if (UFlag.is(this.flag, ObjectProduction)) {
				return Typestate.Tree;
			}
			if (UFlag.is(this.flag, OperationalProduction)) {
				return Typestate.TreeMutation;
			}
			return Typestate.Unit;
		}
		if (Visa.isVisited(v, this)) {
			return Typestate.Undecided;
		}
		v = Visa.visited(v, this);
		return this.getExpression().inferTypestate(v);
	}

	public final void internRule() {
		// this.body = this.body.intern();
	}

	// @Override
	public short acceptByte(int ch) {
		return this.getExpression().acceptByte(ch);
	}

	// // @Override
	// public MozInst encode(AbstractGenerator bc, MozInst next, MozInst
	// failjump) {
	// return this.getExpression().encode(bc, next, failjump);
	// }

	public final void dump() {
		UList<String> l = new UList<String>(new String[4]);
		if (this.isPublic()) {
			l.add("public");
		}
		if (this.isInline()) {
			l.add("inline");
		}
		if (this.isRecursive()) {
			l.add("recursive");
		}
		if (this.isConditional()) {
			l.add("conditional");
		}
		if (this.isContextual()) {
			l.add("contextual");
		}
		ConsoleUtils.println(l + "\n" + this.getLocalName() + " = " + this.getExpression());
	}

}
