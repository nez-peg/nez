package nez.dfa;

import java.util.ArrayList;
import java.util.HashSet;

import nez.ast.TreeVisitorMap;
import nez.dfa.ProductionConverterOfPrioritizedChoice.DefaultVisitor;
import nez.lang.Expression;
import nez.lang.Expressions;
import nez.lang.Grammar;
import nez.lang.Production;
import nez.util.UList;

public class ProductionConverterOfPrioritizedChoice extends TreeVisitorMap<DefaultVisitor> {
	boolean showTrace = false;
	private HashSet<String> visited;

	public ProductionConverterOfPrioritizedChoice() {
		visited = new HashSet<String>();
		init(ProductionConverterOfPrioritizedChoice.class, new DefaultVisitor());
	}

	public Expression convert(Production p) {
		return visit(p.getExpression(), false);
	}

	public Expression visit(Expression e, boolean inZeroOrMore) {
		return find(e.getClass().getSimpleName()).accept(e, inZeroOrMore);
	}

	public class DefaultVisitor {
		public Expression accept(Expression e, boolean inZeroOrMore) {
			System.out.println("ERROR");
			return null;
		}
	}

	public class Empty extends DefaultVisitor {
		@Override
		public Expression accept(Expression e, boolean inZeroOrMore) {
			if (showTrace) {
				System.out.println("here is Pempty : " + e);
			}
			return e.newEmpty();
		}
	}

	public class Fail extends DefaultVisitor {
		@Override
		public Expression accept(Expression e, boolean inZeroOrMore) {
			if (showTrace) {
				System.out.println("here is Pfail : " + e);
			}
			return e.newFailure();
		}
	}

	public class Any extends DefaultVisitor {
		@Override
		public Expression accept(Expression e, boolean inZeroOrMore) {
			if (showTrace) {
				System.out.println("here is Cany : " + e);
			}
			// return ExpressionCommons.newCany(e.getSourcePosition(), false);
			return Expressions.newAny();
		}
	}

	public class Byte extends DefaultVisitor {
		@Override
		public Expression accept(Expression e, boolean inZeroOrMore) {
			if (showTrace) {
				System.out.println("here is Cbyte : " + e);
			}
			// return ExpressionCommons.newCbyte(e.getSourcePosition(), false,
			// ((nez.lang.expr.Cbyte) e).byteChar);
			return Expressions.newByte(e.getSourceLocation(), ((nez.lang.Nez.Byte) e).byteChar);
		}
	}

	public class ByteSet extends DefaultVisitor {
		@Override
		public Expression accept(Expression e, boolean inZeroOrMore) {
			if (showTrace) {
				System.out.println("here is Cset : " + e);
			}
			// return ExpressionCommons.newCset(e.getSourcePosition(), false,
			// ((nez.lang.expr.Cset) e).byteMap);
			return Expressions.newByteSet(e.getSourceLocation(), ((nez.lang.Nez.ByteSet) e).byteMap);
		}
	}

	public class Option extends DefaultVisitor {
		@Override
		public Expression accept(Expression e, boolean inZeroOrMore) {
			if (showTrace) {
				System.out.println("here is Poption : " + e);
			}
			if (inZeroOrMore) {
				return visit(e.get(0), false);
			} else {
				// return ExpressionCommons.newPoption(e.getSourcePosition(),
				// visit(e.get(0), false));
				return Expressions.newOption(e.getSourceLocation(), visit(e.get(0), false));
			}
		}
	}

	public class ZeroMore extends DefaultVisitor {
		@Override
		public Expression accept(Expression e, boolean inZeroOrMore) {
			if (showTrace) {
				System.out.println("here is Pzero : " + e);
			}
			if (inZeroOrMore) {
				return visit(e.get(0), true);
			} else {
				// return ExpressionCommons.newPzero(e.getSourcePosition(),
				// visit(e.get(0), true));
				return Expressions.newZeroMore(e.getSourceLocation(), visit(e.get(0), true));
			}
		}
	}

	public class OneMore extends DefaultVisitor {
		@Override
		public Expression accept(Expression e, boolean inZeroOrMore) {
			if (showTrace) {
				System.out.println("here is Pone : " + e);
			}
			// return ExpressionCommons.newPone(e.getSourcePosition(),
			// visit(e.get(0), false));
			return Expressions.newOneMore(e.getSourceLocation(), visit(e.get(0), false));
		}
	}

	public class And extends DefaultVisitor {
		@Override
		public Expression accept(Expression e, boolean inZeroOrMore) {
			if (showTrace) {
				System.out.println("here is Pand : " + e);
			}
			// return ExpressionCommons.newPand(e.getSourcePosition(),
			// visit(e.get(0), false));
			return Expressions.newAnd(e.getSourceLocation(), visit(e.get(0), false));
		}
	}

	public class Not extends DefaultVisitor {
		@Override
		public Expression accept(Expression e, boolean inZeroOrMore) {
			if (showTrace) {
				System.out.println("here is Pnot : " + e);
			}
			// return ExpressionCommons.newPnot(e.getSourcePosition(),
			// visit(e.get(0), false));
			return Expressions.newNot(e.getSourceLocation(), visit(e.get(0), false));
		}
	}

	// public class Psequence extends DefaultVisitor {
	public class Pair extends DefaultVisitor {
		@Override
		public Expression accept(Expression e, boolean inZeroOrMore) {
			if (showTrace) {
				System.out.println("here is Psequence : " + e);
			}
			inZeroOrMore = false;
			boolean allPzero = true;
			for (int i = 0; i < e.size(); i++) {
				// if (!(e.get(i) instanceof nez.lang.expr.Pzero)) {
				if (!(e.get(i) instanceof nez.lang.Nez.ZeroMore)) {
					allPzero = false;
					break;
				}
			}
			if (allPzero) {
				inZeroOrMore = true;
			}
			inZeroOrMore = false;// fopejwapoejwap
			// return ExpressionCommons.newPsequence(e.getSourcePosition(),
			// visit(e.getFirst(), inZeroOrMore), visit(e.getNext(),
			// inZeroOrMore));
			// return Expressions.newSequence(e.getSourcePosition(),
			// visit(e.getFirst(), inZeroOrMore), visit(e.getNext(),
			// inZeroOrMore));
			return Expressions.newPair(e.getSourceLocation(), visit(((nez.lang.Nez.Pair) e).first, inZeroOrMore), visit(((nez.lang.Nez.Pair) e).next, inZeroOrMore));
		}
	}

	public HashSet<Character> computeFirstCharacterSet(Expression e) {
		String exp = e.toString();
		HashSet<Character> S = new HashSet<Character>();
		// System.out.println("exp = " + exp);
		if (exp.charAt(0) == '\'') {
			S.add(new Character(exp.charAt(1)));
		} else if (exp.charAt(0) == '[' && exp.length() == 5 && exp.charAt(2) == '-' && exp.charAt(4) == ']') {
			for (char c = exp.charAt(1); c <= exp.charAt(3); c++) {
				S.add(new Character(c));
			}
		} else {
			return null;
		}
		for (int i = 0; i < exp.length(); i++) {
			if (exp.charAt(i) == '?') {
				return null;
			}
		}
		return S;
	}

	// e1はe2のprefixか？(簡易版)
	public boolean isPrefix(Expression e1, Expression e2) {
		/*
		 * 簡易版 先頭の１文字だけってやつ
		 */
		if (e2.toString().equals("''")) {
			return true;
		}
		HashSet<Character> LL1_e1 = computeFirstCharacterSet(e1);
		HashSet<Character> LL1_e2 = computeFirstCharacterSet(e2);
		if (LL1_e1 == null || LL1_e2 == null) {
			return true;
		}
		boolean allDistinct = true;
		for (Character character : LL1_e2) {
			if (LL1_e1.contains(character)) {
				allDistinct = false;
				break;
			}
		}
		System.out.println(e1.toString() + " <-- " + e2.toString() + " : " + allDistinct);
		return !allDistinct;
	}

	// 被っているものがあったら否定をつける.
	public class Choice extends DefaultVisitor {
		@Override
		public Expression accept(Expression e, boolean inZeroOrMore) {
			if (showTrace) {
				System.out.println("here is Pchoice : " + e);
			}
			ArrayList<Expression> notPredicates = new ArrayList<Expression>();
			ArrayList<Expression> prefixBuffer = new ArrayList<>();
			int size = e.size();
			Expression[] l = new Expression[size];
			for (int i = 0; i < e.size(); i++) {
				Expression tmp = e.get(i);
				l[i] = visit(e.get(i), inZeroOrMore);
				for (int j = 0; j < notPredicates.size(); j++) {
					if (isPrefix(prefixBuffer.get(j), tmp)) {
						// l[i] = ExpressionCommons.newPsequence(null,
						// notPredicates.get(j), l[i]);
						// l[i] = Expressions.newSequence(null,
						// notPredicates.get(j), l[i]);
						l[i] = Expressions.newPair(null, notPredicates.get(j), l[i]);
					}
				}
				// notPredicates.add(ExpressionCommons.newPnot(null, tmp));
				notPredicates.add(Expressions.newNot(null, tmp));
				prefixBuffer.add(tmp);
			}
			UList<Expression> ul = new UList<Expression>(l);
			for (int i = 0; i < l.length; i++) {
				ul.add(l[i]);
			}
			// return ExpressionCommons.newPchoice(null, ul);
			return Expressions.newChoice(ul);
		}
	}

	public class NonTerminal extends DefaultVisitor {
		@Override
		public Expression accept(Expression e, boolean inZeroOrMore) {
			if (showTrace) {
				// System.out.println("here is NonTerminal : " +
				// ((nez.lang.expr.NonTerminal) e).getLocalName() + " = " +
				// ((nez.lang.expr.NonTerminal)
				// e).getProduction().getExpression());
				System.out.println("here is NonTerminal : " + ((nez.lang.NonTerminal) e).getLocalName() + " = " + ((nez.lang.NonTerminal) e).getProduction().getExpression());
			}
			// String name = ((nez.lang.expr.NonTerminal) e).getLocalName();
			String name = ((nez.lang.NonTerminal) e).getLocalName();
			if (!visited.contains(name)) {
				visited.add(name);
				Grammar grammar = new Grammar();
				// ((nez.lang.expr.NonTerminal)
				// e).getProduction().setExpression(visit(((nez.lang.expr.NonTerminal)
				// e).getProduction().getExpression(), inZeroOrMore));
				((nez.lang.NonTerminal) e).getProduction().setExpression(visit(((nez.lang.NonTerminal) e).getProduction().getExpression(), inZeroOrMore));
				// grammar.addProduction(((nez.lang.expr.NonTerminal)
				// e).getProduction());
				grammar.addProduction((((nez.lang.NonTerminal) e).getSourceLocation()), (((nez.lang.NonTerminal) e).getProduction().getLocalName()), ((nez.lang.NonTerminal) e).getProduction().getExpression());
				// return
				// ExpressionCommons.newNonTerminal(e.getSourcePosition(),
				// grammar, name);
				return Expressions.newNonTerminal(e.getSourceLocation(), grammar, name);
			}
			return e;
		}
	}
}
