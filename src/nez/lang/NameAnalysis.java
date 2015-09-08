package nez.lang;

import java.util.List;

import nez.lang.expr.Uand;
import nez.lang.expr.Choice;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Unot;
import nez.lang.expr.Uoption;
import nez.lang.expr.Uzero;
import nez.lang.expr.Uone;
import nez.lang.expr.Sequence;
import nez.util.StringUtils;
import nez.util.UFlag;

public class NameAnalysis extends ExpressionTransducer {

	public void analyze(List<Production> l) {
		for (Production p : l) {
			if (p.isTerminal()) {
				continue;
			}
			analyze(p);
		}
		for (Production p : l) {
			if (p.isTerminal()) {
				continue;
			}
			if (UFlag.is(p.flag, Production.ResetFlag)) {
				p.initFlag();
				if (p.isRecursive()) {
					checkLeftRecursion(p.getExpression(), new ProductionStacker(p, null));
				}
				p.isNoNTreeConstruction();
			}
		}
	}

	boolean checkLeftRecursion(Expression e, ProductionStacker s) {
		if (e instanceof NonTerminal) {
			Production p = ((NonTerminal) e).getProduction();
			if (s.isVisited(p)) {
				((NonTerminal) e).getGrammar().reportError(e, "left recursion: " + p.getLocalName());
				return true; // stop as consumed
			}
			return checkLeftRecursion(p.getExpression(), new ProductionStacker(p, s));
		}
		if (e.size() > 0) {
			if (e instanceof Sequence) {
				if (!checkLeftRecursion(e.get(0), s)) {
					return checkLeftRecursion(e.get(1), s);
				}
			}
			if (e instanceof Choice) {
				boolean consumed = true;
				for (Expression se : e) {
					if (!checkLeftRecursion(e.get(1), s)) {
						consumed = false;
					}
				}
				return consumed;
			}
			boolean r = checkLeftRecursion(e.get(0), s);
			if (e instanceof Uone) {
				return r;
			}
			if (e instanceof Unot || e instanceof Uzero || e instanceof Uoption || e instanceof Uand) {
				return false;
			}
			return r;
		}
		return e.isConsumed();
	}

	boolean sync;

	public boolean analyze(Production p) {
		sync = false;
		p.setExpression(p.getExpression().reshape(this));
		if (sync) {
			p.resetFlag();
		}
		return sync;
	}

	@Override
	public Expression reshapeNonTerminal(NonTerminal n) {
		Production p = n.getGrammar().getProduction(n.getLocalName());
		if (p == null) {
			if (n.isTerminal()) {
				n.getGrammar().reportNotice(n, "undefined terminal: " + n.getLocalName());
				return ExpressionCommons.newString(n.getSourcePosition(), StringUtils.unquoteString(n.getLocalName()));
			}
			n.getGrammar().reportWarning(n, "undefined production: " + n.getLocalName());
			return n.newEmpty();
		}
		if (n.isTerminal()) {
			return p.getExpression().reshape(this); // inlining terminal
		}
		if (n.syncProduction()) {
			this.sync = true;
		}
		return n;
	}
}

class StructualAnalysis extends ExpressionTransducer {
}
