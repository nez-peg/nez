package nez.debugger;

import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Nez;
import nez.lang.Nez.Unary;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.lang.expr.Cany;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pfail;
import nez.lang.expr.Pnot;
import nez.lang.expr.Pone;
import nez.lang.expr.Poption;
import nez.lang.expr.Psequence;
import nez.lang.expr.Pzero;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Tnew;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.util.ConsoleUtils;

public class GrammarAnalyzer {
	Grammar peg;

	public GrammarAnalyzer(Grammar peg) {
		this.peg = peg;
	}

	public void analyze() {
		for (Production p : this.peg.getProductionList()) {
			this.analizeConsumption(p.getExpression());
		}
	}

	private boolean analizeConsumption(Expression p) {
		if (p instanceof Pzero || p instanceof Pone) {
			if (!this.analizeInnerOfRepetition(p.get(0))) {
				ConsoleUtils.println(p.getSourceLocation().formatSourceMessage("warning", "unconsumed Repetition"));
				return false;
			}
		}
		if (p instanceof Nez.Unary) {
			return this.analizeConsumption(p.get(0));
		}
		if (p instanceof Psequence || p instanceof Pchoice) {
			for (int i = 0; i < p.size(); i++) {
				if (!this.analizeConsumption(p.get(i))) {
					return false;
				}
			}
			return true;
		}
		return true;
	}

	Expression inlineNonTerminal(Expression e) {
		while (e instanceof NonTerminal) {
			NonTerminal n = (NonTerminal) e;
			e = n.getProduction().getExpression();
		}
		return e;
	}

	private boolean analizeInnerOfRepetition(Expression p) {
		p = this.inlineNonTerminal(p);
		if (p instanceof Pone) {
			return true;
		}
		if (p instanceof Pzero || p instanceof Poption) {
			return false;
		}
		if (p instanceof Pfail) {
			return false;
		}
		if (p instanceof Pnot) {
			if (p.get(0) instanceof Cany) {
				return false;
			}
			return this.analizeInnerOfRepetition(p.get(0));
		}
		if (p instanceof Unary) {
			return this.analizeInnerOfRepetition(p.get(0));
		}
		if (p instanceof Psequence) {
			for (int i = 0; i < p.size(); i++) {
				if (!isUnconsumedASTConstruction(p.get(i))) {
					if (this.analizeInnerOfRepetition(p.get(i))) {
						return true;
					}
				}
			}
			return false;
		}
		if (p instanceof Pchoice) {
			for (int i = 0; i < p.size(); i++) {
				if (!this.analizeInnerOfRepetition(p.get(i))) {
					return false;
				}
			}
			return true;
		}
		return true;
	}

	public boolean isUnconsumedASTConstruction(Expression p) {
		if (p instanceof Tnew || p instanceof Tcapture || p instanceof Ttag || p instanceof Treplace) {
			return true;
		}
		return false;
	}

}
