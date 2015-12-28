package nez.lang.ast;

import nez.ast.Symbol;
import nez.ast.Tree;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.lang.expr.Expressions;
import nez.parser.ParserStrategy;
import nez.util.StringUtils;
import nez.util.UList;

public class NezExpressionConstructor extends GrammarVisitorMap<ExpressionTransducer> implements ExpressionConstructor, NezSymbols {
	private boolean binary = false;

	public NezExpressionConstructor(Grammar grammar, ParserStrategy strategy) {
		super(grammar, strategy);
		init(NezExpressionConstructor.class, new Undefined());
	}

	@Override
	public Expression newInstance(Tree<?> node) {
		return this.find(key(node)).accept(node, null);
	}

	public Expression newInstance(Tree<?> node, Expression next) {
		return this.find(key(node)).accept(node, next);
	}

	public class Undefined implements ExpressionTransducer {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			undefined(node);
			return null;
		}
	}

	public class Instance extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return newInstance(node);
		}
	}

	public class _NonTerminal extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			String symbol = node.toText();
			return Expressions.newNonTerminal(node, getGrammar(), symbol);
		}
	}

	public class _String extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression es) {
			String name = Grammar.nameTerminalProduction(node.toText());
			return Expressions.newNonTerminal(node, getGrammar(), name);
		}
	}

	public class _Character extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newString(node, StringUtils.unquoteString(node.toText()));
		}
	}

	public class _Class extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			UList<Expression> l = new UList<Expression>(new Expression[2]);
			if (node.size() > 0) {
				for (int i = 0; i < node.size(); i++) {
					Tree<?> o = node.get(i);
					if (o.is(_List)) { // range
						l.add(Expressions.newCharSet(node, o.getText(0, ""), o.getText(1, "")));
					}
					if (o.is(_Class)) { // single
						l.add(Expressions.newCharSet(node, o.toText(), o.toText()));
					}
				}
			}
			return Expressions.newPchoice(node, l);
		}
	}

	public class _ByteChar extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			String t = node.toText();
			if (t.startsWith("U+")) {
				int c = StringUtils.hex(t.charAt(2));
				c = (c * 16) + StringUtils.hex(t.charAt(3));
				c = (c * 16) + StringUtils.hex(t.charAt(4));
				c = (c * 16) + StringUtils.hex(t.charAt(5));
				if (c < 128) {
					return Expressions.newCbyte(node, binary, c);
				}
				String t2 = String.valueOf((char) c);
				return Expressions.newString(node, t2);
			}
			int c = StringUtils.hex(t.charAt(t.length() - 2)) * 16 + StringUtils.hex(t.charAt(t.length() - 1));
			return Expressions.newCbyte(node, binary, c);
		}
	}

	public class _AnyChar extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newCany(node, binary);
		}
	}

	public class _Choice extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			UList<Expression> l = new UList<Expression>(new Expression[node.size()]);
			for (int i = 0; i < node.size(); i++) {
				Expressions.addChoice(l, newInstance(node.get(i)));
			}
			return Expressions.newPchoice(node, l);
		}
	}

	public class _Sequence extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			UList<Expression> l = new UList<Expression>(new Expression[node.size()]);
			for (int i = 0; i < node.size(); i++) {
				Expressions.addSequence(l, newInstance(node.get(i)));
			}
			return Expressions.newPair(node, l);
		}
	}

	public class _Not extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newPnot(node, newInstance(node.get(_expr)));
		}
	}

	public class _And extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newPand(node, newInstance(node.get(_expr)));
		}
	}

	public class _Option extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newPoption(node, newInstance(node.get(_expr)));
		}
	}

	public class _Repetition1 extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newPone(node, newInstance(node.get(_expr)));
		}
	}

	public class _Repetition extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			if (node.size() == 2) {
				int ntimes = StringUtils.parseInt(node.getText(1, ""), -1);
				if (ntimes != 1) {
					UList<Expression> l = new UList<Expression>(new Expression[ntimes]);
					for (int i = 0; i < ntimes; i++) {
						Expressions.addSequence(l, newInstance(node.get(0)));
					}
					return Expressions.newPair(node, l);
				}
			}
			return Expressions.newPzero(node, newInstance(node.get(_expr)));
		}
	}

	// PEG4d TransCapturing

	public class _New extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			Tree<?> exprNode = node.get(_expr, null);
			Expression p = (exprNode == null) ? Expressions.newEmpty(node) : newInstance(exprNode);
			return Expressions.newNewCapture(node, false, null, p);
		}
	}

	private Symbol parseLabelNode(Tree<?> node) {
		Symbol label = null;
		Tree<?> labelNode = node.get(_name, null);
		if (labelNode != null) {
			label = Symbol.tag(labelNode.toText());
		}
		return label;
	}

	public class _LeftFold extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			Tree<?> exprNode = node.get(_expr, null);
			Expression p = (exprNode == null) ? Expressions.newEmpty(node) : newInstance(exprNode);
			return Expressions.newNewCapture(node, true, parseLabelNode(node), p);
		}
	}

	public class _Link extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newTlink(node, parseLabelNode(node), newInstance(node.get(_expr)));
		}
	}

	public class _Tagging extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newTtag(node, Symbol.tag(node.toText()));
		}
	}

	public class _Replace extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newTreplace(node, node.toText());
		}
	}

	public class _Match extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			Tree<?> exprNode = node.get(_expr, null);
			if (exprNode != null) {
				return Expressions.newTdetree(node, newInstance(exprNode));
			}
			return Expressions.newXmatch(node, parseLabelNode(node));
		}
	}

	public class _If extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newXif(node, node.getText(_name, ""));
		}
	}

	public class _On extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newXon(node, true, node.getText(_name, ""), newInstance(node.get(_expr)));
		}
	}

	public class _Block extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newXblock(node, newInstance(node.get(_expr)));
		}
	}

	public class _Def extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			Grammar g = getGrammar();
			Tree<?> nameNode = node.get(_name);
			NonTerminal pat = g.newNonTerminal(node, nameNode.toText());
			Expression expr = newInstance(node.get(_expr));
			Production p = g.newProduction(pat.getLocalName(), expr);
			reportWarning(nameNode, "new production generated: " + p);
			return Expressions.newXsymbol(node, pat);
		}
	}

	public class _Symbol extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			Grammar g = getGrammar();
			NonTerminal pat = g.newNonTerminal(node, node.getText(_name, ""));
			return Expressions.newXsymbol(node, pat);
		}
	}

	public class _Is extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			Grammar g = getGrammar();
			NonTerminal pat = g.newNonTerminal(node, node.getText(_name, ""));
			return Expressions.newXis(node, pat);
		}
	}

	public class _Isa extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			Grammar g = getGrammar();
			NonTerminal pat = g.newNonTerminal(node, node.getText(_name, ""));
			return Expressions.newXisa(node, pat);
		}
	}

	public class _Exists extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newXexists(node, Symbol.tag(node.getText(_name, "")), node.getText(_symbol, null));
		}
	}

	public class _Local extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newXlocal(node, Symbol.tag(node.getText(_name, "")), newInstance(node.get(_expr)));
		}
	}

}
