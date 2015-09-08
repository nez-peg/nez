package nez.lang;

import java.util.List;

import nez.ast.SourcePosition;
import nez.lang.expr.And;
import nez.lang.expr.AnyChar;
import nez.lang.expr.ByteChar;
import nez.lang.expr.ByteMap;
import nez.lang.expr.Choice;
import nez.lang.expr.GrammarFactory;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Not;
import nez.lang.expr.Option;
import nez.lang.expr.Repetition;
import nez.lang.expr.Repetition1;
import nez.lang.expr.Sequence;
import nez.util.ConsoleUtils;
import nez.util.UFlag;
import nez.util.UList;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Production extends Expression {
	public final static int PublicProduction = 1 << 0;
	public final static int TerminalProduction = 1 << 1;
	public final static int InlineProduction = 1 << 2;
	public final static int Declared = PublicProduction | TerminalProduction | InlineProduction;

	public final static int RecursiveChecked = 1 << 3;
	public final static int RecursiveProduction = 1 << 4;

	public final static int ConsumedChecked = 1 << 5;
	public final static int ConsumedProduction = 1 << 6;

	public final static int ASTChecked = 1 << 8;
	public final static int ObjectProduction = 1 << 9;
	public final static int OperationalProduction = 1 << 10;

	public final static int ConditionalChecked = 1 << 16;
	public final static int ConditionalProduction = 1 << 17;
	public final static int ContextualChecked = 1 << 18;
	public final static int ContextualProduction = 1 << 19;

	public final static int ResetFlag = 1 << 30;

	int flag;
	GrammarFile file;
	String name;
	String uname;
	Expression body;

	public Production(SourcePosition s, int flag, GrammarFile file, String name, Expression body) {
		super(s);
		this.file = file;
		this.name = name;
		this.uname = file.uniqueName(name);
		this.body = (body == null) ? GrammarFactory.newEmpty(s) : body;
		this.flag = flag;
		Production.quickCheck(this);
	}

	private Production(String name, Production orig, Expression body) {
		super(orig.s);
		this.file = orig.getGrammarFile();
		this.name = name;
		this.uname = file.uniqueName(name);
		this.body = (body == null) ? GrammarFactory.newEmpty(s) : body;
		Production.quickCheck(this);
	}

	Production newProduction(String localName) {
		return new Production(localName, this, this.getExpression());
	}

	void resetFlag() {
		this.flag = (this.flag & Declared) | ResetFlag;
	}

	void initFlag() {
		this.flag = this.flag | ConsumedChecked | ConditionalChecked | ContextualChecked | RecursiveChecked;
		this.flag = UFlag.unsetFlag(this.flag, ResetFlag);
		quickCheck(this, this.getExpression());
		// System.out.println(this.getLocalName() + ":: " + flag());
	}

	public final static void quickCheck(Production p) {
		p.flag = p.flag | ConsumedChecked | ConditionalChecked | ContextualChecked | RecursiveChecked;
		quickCheck(p, p.getExpression());
	}

	public final static void quickCheck(Production p, Expression e) {
		if (e instanceof ByteChar || e instanceof ByteMap || e instanceof AnyChar) {
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
		if (e instanceof Sequence) {
			quickCheck(p, e.get(0));
			quickCheck(p, e.get(1));
			return;
		}
		if (e instanceof Repetition1) {
			quickCheck(p, e.get(0));
			return;
		}
		if (e instanceof Not || e instanceof Option || e instanceof Repetition || e instanceof And) {
			boolean consumed = UFlag.is(p.flag, ConsumedProduction);
			quickCheck(p, e.get(0));
			if (!consumed) {
				p.flag = UFlag.unsetFlag(p.flag, ConsumedProduction);
			}
			return;
		}
		if (e instanceof Choice) {
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
		if (e instanceof Conditional) {
			p.flag = p.flag | ConditionalChecked | ConditionalProduction;
		}
		if (e instanceof Contextual) {
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

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Production) {
			return this.getExpression().equalsExpression(((Production) o).getExpression());
		}
		return false;
	}

	public final GrammarFile getGrammarFile() {
		return this.file;
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
			checkContextual(this.getExpression(), null);
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

	@Override
	public Expression get(int index) {
		return body;
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public
	final void format(StringBuilder sb) {
		sb.append(this.getLocalName());
		sb.append(" = ");
		this.getExpression().format(sb);
	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeProduction(this);
	}

	@Override
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
		if (e instanceof Sequence) {
			if (checkConsumed(e.get(0), s)) {
				return true;
			}
			return checkConsumed(e.get(1), s);
		}
		if (e instanceof Choice) {
			boolean consumed = true;
			for (Expression se : e) {
				if (!checkConsumed(se, s)) {
					consumed = false;
				}
			}
			return consumed;
		}
		if (e.size() > 0) {
			if (e instanceof Repetition1) {
				return checkConsumed(e.get(0), s);
			}
			if (e instanceof Not || e instanceof Option || e instanceof Repetition || e instanceof And) {
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
		if (t == Typestate.ObjectType) {
			this.flag |= ObjectProduction;
		}
		if (t == Typestate.OperationType) {
			this.flag |= OperationalProduction;
		}
		this.flag |= ASTChecked;
	}

	@Override
	public int inferTypestate(Visa v) {
		if (UFlag.is(this.flag, ASTChecked)) {
			if (UFlag.is(this.flag, ObjectProduction)) {
				return Typestate.ObjectType;
			}
			if (UFlag.is(this.flag, OperationalProduction)) {
				return Typestate.OperationType;
			}
			return Typestate.BooleanType;
		}
		if (Visa.isVisited(v, this)) {
			return Typestate.Undefined;
		}
		v = Visa.visited(v, this);
		return this.getExpression().inferTypestate(v);
	}

	@Override
	public String key() {
		return this.getUniqueName() + "=";
	}

	public final void internRule() {
		this.body = this.body.intern();
	}

	@Override
	public String getPredicate() {
		return this.getUniqueName() + "=";
	}

	@Override
	public short acceptByte(int ch) {
		return this.getExpression().acceptByte(ch);
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return this.getExpression().encode(bc, next, failjump);
	}

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

class ProductionStacker {
	int n;
	ProductionStacker prev;
	Production p;

	ProductionStacker(Production p, ProductionStacker prev) {
		this.prev = prev;
		this.p = p;
		this.n = (prev == null) ? 0 : prev.n + 1;
	}

	boolean isVisited(Production p) {
		ProductionStacker d = this;
		while (d != null) {
			if (d.p == p) {
				return true;
			}
			d = d.prev;
		}
		return false;
	}
}
