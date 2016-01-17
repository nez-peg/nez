package nez.lang.ast;

import nez.ast.Symbol;
import nez.ast.Tree;
import nez.lang.Bytes;
import nez.lang.Expression;
import nez.lang.Expressions;
import nez.lang.Grammar;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.parser.ParserStrategy;
import nez.util.StringUtils;
import nez.util.UList;

public class NezExpressionConstructor extends GrammarVisitorMap<ExpressionTransducer> implements ExpressionConstructor, NezSymbols {

	public NezExpressionConstructor(Grammar grammar, ParserStrategy strategy) {
		super(grammar, strategy);
		init(NezExpressionConstructor.class, new TreeVisitor());
	}

	@Override
	public Expression newInstance(Tree<?> node) {
		return this.find(key(node)).accept(node, null);
	}

	public Expression newInstance(Tree<?> node, Expression next) {
		return this.find(key(node)).accept(node, next);
	}

	public class TreeVisitor implements ExpressionTransducer {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			undefined(node);
			return null;
		}
	}

	public class Instance extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return newInstance(node);
		}
	}

	public class _NonTerminal extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			String symbol = node.toText();
			return Expressions.newNonTerminal(node, getGrammar(), symbol);
		}
	}

	public class _String extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression es) {
			String name = Grammar.nameTerminalProduction(node.toText());
			return Expressions.newNonTerminal(node, getGrammar(), name);
		}
	}

	public class _Character extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newExpression(node, StringUtils.unquoteString(node.toText()));
		}
	}

	public class _Class extends TreeVisitor {
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
			return Expressions.newChoice(l);
		}
	}

	public class _ByteChar extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			String t = node.toText();
			if (t.startsWith("U+")) {
				int c = StringUtils.hex(t.charAt(2));
				c = (c * 16) + StringUtils.hex(t.charAt(3));
				c = (c * 16) + StringUtils.hex(t.charAt(4));
				c = (c * 16) + StringUtils.hex(t.charAt(5));
				if (c < 128) {
					return Expressions.newByte(node, c);
				}
				String t2 = String.valueOf((char) c);
				return Expressions.newExpression(node, t2);
			}
			int c = StringUtils.hex(t.charAt(t.length() - 2)) * 16 + StringUtils.hex(t.charAt(t.length() - 1));
			return Expressions.newByte(node, c);
		}
	}

	public class _ByteClass extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			String t = node.toText();
			return Expressions.newByteSet(node, Bytes.parseOctet(t));
		}
	}

	public class _AnyChar extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newAny(node);
		}
	}

	public class _Choice extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			UList<Expression> l = new UList<Expression>(new Expression[node.size()]);
			for (int i = 0; i < node.size(); i++) {
				Expressions.addChoice(l, newInstance(node.get(i)));
			}
			return Expressions.newChoice(l);
		}
	}

	public class _Sequence extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			UList<Expression> l = new UList<Expression>(new Expression[node.size()]);
			for (int i = 0; i < node.size(); i++) {
				Expressions.addSequence(l, newInstance(node.get(i)));
			}
			return Expressions.newPair(l);
		}
	}

	public class _Not extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newNot(node, newInstance(node.get(_expr)));
		}
	}

	public class _And extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newAnd(node, newInstance(node.get(_expr)));
		}
	}

	public class _Option extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newOption(node, newInstance(node.get(_expr)));
		}
	}

	public class _Repetition1 extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newOneMore(node, newInstance(node.get(_expr)));
		}
	}

	public class _Repetition extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			if (node.size() == 2) {
				int ntimes = StringUtils.parseInt(node.getText(1, ""), -1);
				if (ntimes != 1) {
					UList<Expression> l = new UList<Expression>(new Expression[ntimes]);
					for (int i = 0; i < ntimes; i++) {
						Expressions.addSequence(l, newInstance(node.get(0)));
					}
					return Expressions.newPair(l);
				}
			}
			return Expressions.newZeroMore(node, newInstance(node.get(_expr)));
		}
	}

	// PEG4d TransCapturing

	public class _New extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			Tree<?> exprNode = node.get(_expr, null);
			Expression p = (exprNode == null) ? Expressions.newEmpty(node) : newInstance(exprNode);
			return Expressions.newTree(node, false, null, p);
		}
	}

	private Symbol parseLabelNode(Tree<?> node) {
		Symbol label = null;
		Tree<?> labelNode = node.get(_name, null);
		if (labelNode != null) {
			label = Symbol.unique(labelNode.toText());
		}
		return label;
	}

	public class _LeftFold extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			Tree<?> exprNode = node.get(_expr, null);
			Expression p = (exprNode == null) ? Expressions.newEmpty(node) : newInstance(exprNode);
			return Expressions.newTree(node, true, parseLabelNode(node), p);
		}
	}

	public class _Link extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newLinkTree(node, parseLabelNode(node), newInstance(node.get(_expr)));
		}
	}

	public class _Tagging extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newTag(node, Symbol.unique(node.toText()));
		}
	}

	public class _Replace extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newReplace(node, node.toText());
		}
	}

	public class _If extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newIfCondition(node, node.getText(_name, ""));
		}
	}

	public class _On extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newOnCondition(node, node.getText(_name, ""), newInstance(node.get(_expr)));
		}
	}

	public class _Block extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newBlockScope(node, newInstance(node.get(_expr)));
		}
	}

	public class _Def extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			Grammar g = getGrammar();
			Tree<?> nameNode = node.get(_name);
			NonTerminal pat = Expressions.newNonTerminal(node, g, nameNode.toText());
			Expression expr = newInstance(node.get(_expr));
			Production p = g.addProduction(pat.getLocalName(), expr);
			reportWarning(nameNode, "new production generated: " + p);
			return Expressions.newSymbol(node, pat);
		}
	}

	public class _Symbol extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			Grammar g = getGrammar();
			NonTerminal pat = Expressions.newNonTerminal(node, g, node.getText(_name, ""));
			return Expressions.newSymbol(node, pat);
		}
	}

	public class _Is extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			Grammar g = getGrammar();
			NonTerminal pat = Expressions.newNonTerminal(node, g, node.getText(_name, ""));
			return Expressions.newIsSymbol(node, pat);
		}
	}

	public class _Isa extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			Grammar g = getGrammar();
			NonTerminal pat = Expressions.newNonTerminal(node, g, node.getText(_name, ""));
			return Expressions.newIsaSymbol(node, pat);
		}
	}

	public class _Match extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			Grammar g = getGrammar();
			NonTerminal pat = Expressions.newNonTerminal(node, g, node.getText(_name, ""));
			return Expressions.newSymbolMatch(node, pat);
		}
	}

	public class _Exists extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newSymbolExists(node, Symbol.unique(node.getText(_name, "")), node.getText(_symbol, null));
		}
	}

	public class _Local extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newLocalScope(node, Symbol.unique(node.getText(_name, "")), newInstance(node.get(_expr)));
		}
	}

	public class _Scanf extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			String mask = node.getText(_name, null);
			return Expressions.newScanf(node, node.getText(_name, null), newInstance(node.get(_expr)));
		}
	}

	public class _Repeat extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return Expressions.newRepeat(node, newInstance(node.get(_expr)));
		}
	}

}
