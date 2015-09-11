//package nez.lang;
//
//import java.util.List;
//
//import nez.lang.expr.ExpressionCommons;
//import nez.lang.expr.NonTerminal;
//import nez.lang.expr.Pchoice;
//import nez.lang.expr.Psequence;
//import nez.lang.expr.Pand;
//import nez.lang.expr.Pnot;
//import nez.lang.expr.Pone;
//import nez.lang.expr.Poption;
//import nez.lang.expr.Pzero;
//import nez.util.StringUtils;
//import nez.util.UFlag;
//
//public class NameAnalysis extends GrammarTransducer {
//
//	public void analyze(List<Production> l) {
//		for (Production p : l) {
//			if (p.isTerminal()) {
//				continue;
//			}
//			analyze(p);
//		}
//		for (Production p : l) {
//			if (p.isTerminal()) {
//				continue;
//			}
//			if (UFlag.is(p.flag, Production.ResetFlag)) {
//				p.initFlag();
//				if (p.isRecursive()) {
//					checkLeftRecursion(p.getExpression(), new ProductionStacker(p, null));
//				}
//				p.isNoNTreeConstruction();
//			}
//		}
//	}
//
//	boolean checkLeftRecursion(Expression e, ProductionStacker s) {
//		if (e instanceof NonTerminal) {
//			Production p = ((NonTerminal) e).getProduction();
//			if (s.isVisited(p)) {
//				((NonTerminal) e).getGrammar().reportError(e, "left recursion: " + p.getLocalName());
//				return true; // stop as consumed
//			}
//			return checkLeftRecursion(p.getExpression(), new ProductionStacker(p, s));
//		}
//		if (e.size() > 0) {
//			if (e instanceof Psequence) {
//				if (!checkLeftRecursion(e.get(0), s)) {
//					return checkLeftRecursion(e.get(1), s);
//				}
//			}
//			if (e instanceof Pchoice) {
//				boolean consumed = true;
//				for (Expression se : e) {
//					if (!checkLeftRecursion(e.get(1), s)) {
//						consumed = false;
//					}
//				}
//				return consumed;
//			}
//			boolean r = checkLeftRecursion(e.get(0), s);
//			if (e instanceof Pone) {
//				return r;
//			}
//			if (e instanceof Pnot || e instanceof Pzero || e instanceof Poption || e instanceof Pand) {
//				return false;
//			}
//			return r;
//		}
//		return e.isConsumed();
//	}
//
//	boolean sync;
//
//	public boolean analyze(Production p) {
//		sync = false;
//		p.setExpression(p.getExpression().reshape(this));
//		if (sync) {
//			p.resetFlag();
//		}
//		return sync;
//	}
//
//	@Override
//	public Expression reshapeNonTerminal(NonTerminal n) {
//		Production p = n.getGrammar().getProduction(n.getLocalName());
//		if (p == null) {
//			if (n.isTerminal()) {
//				n.getGrammar().reportNotice(n, "undefined terminal: " + n.getLocalName());
//				return ExpressionCommons.newString(n.getSourcePosition(), StringUtils.unquoteString(n.getLocalName()));
//			}
//			n.getGrammar().reportWarning(n, "undefined production: " + n.getLocalName());
//			return n.newEmpty();
//		}
//		if (n.isTerminal()) {
//			return p.getExpression().reshape(this); // inlining terminal
//		}
//		// if (n.syncProduction()) {
//		// this.sync = true;
//		// }
//		return n;
//	}
// }
