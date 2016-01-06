package nez.debugger;

import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Nez;
import nez.lang.Nez.Unary;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.lang.expr.Poption;
import nez.util.ConsoleUtils;

public class GrammarAnalyzer {
	Grammar peg;

	public GrammarAnalyzer(Grammar peg) {
		this.peg = peg;
	}

	public void analyze() {
		for (Production p : this.peg) {
			this.analizeConsumption(p.getExpression());
		}
	}

	private boolean analizeConsumption(Expression p) {
		if (p instanceof Nez.ZeroMore || p instanceof Nez.OneMore) {
			if (!this.analizeInnerOfRepetition(p.get(0))) {
				ConsoleUtils.println(p.getSourceLocation().formatSourceMessage("warning", "unconsumed Repetition"));
				return false;
			}
		}
		if (p instanceof Nez.Unary) {
			return this.analizeConsumption(p.get(0));
		}
		if (p instanceof Nez.Sequence || p instanceof Nez.Choice) {
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
		if (p instanceof Nez.OneMore) {
			return true;
		}
		if (p instanceof Nez.ZeroMore || p instanceof Poption) {
			return false;
		}
		if (p instanceof Nez.Fail) {
			return false;
		}
		if (p instanceof Nez.Not) {
			if (p.get(0) instanceof Nez.Any) {
				return false;
			}
			return this.analizeInnerOfRepetition(p.get(0));
		}
		if (p instanceof Unary) {
			return this.analizeInnerOfRepetition(p.get(0));
		}
		if (p instanceof Nez.Sequence) {
			for (int i = 0; i < p.size(); i++) {
				if (!isUnconsumedASTConstruction(p.get(i))) {
					if (this.analizeInnerOfRepetition(p.get(i))) {
						return true;
					}
				}
			}
			return false;
		}
		if (p instanceof Nez.Choice) {
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
		if (p instanceof Nez.BeginTree || p instanceof Nez.EndTree || p instanceof Nez.Tag || p instanceof Nez.Replace) {
			return true;
		}
		return false;
	}

}
