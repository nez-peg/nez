package nez.lang.ast;

import nez.ast.Symbol;
import nez.ast.Tree;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.GrammarFile;
import nez.lang.Production;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.NonTerminal;
import nez.parser.ParserStrategy;
import nez.util.StringUtils;
import nez.util.UList;

public class NezExpressionTransducer extends GrammarVisitorMap<ExpressionTransducerVisitor> implements ExpressionTransducer {
	private boolean binary = false;
	public final static Symbol _String = Symbol.tag("String");
	public final static Symbol _Integer = Symbol.tag("Integer");
	public final static Symbol _List = Symbol.tag("List");
	public final static Symbol _Name = Symbol.tag("Name");
	public final static Symbol _Format = Symbol.tag("Format");
	public final static Symbol _Class = Symbol.tag("Class");

	public final static Symbol _anno = Symbol.tag("anno");

	public NezExpressionTransducer(Grammar grammar, ParserStrategy strategy) {
		super(grammar, strategy);
		init(NezExpressionTransducer.class, new Undefined());
	}

	@Override
	public Expression newInstance(Tree<?> node) {
		return newInstance(node, null);
	}

	public Expression newInstance(Tree<?> node, Expression next) {
		this.find(key(node)).accept(node, next);
		return null;
	}

	public class Undefined implements ExpressionTransducerVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			throw new TransducerException(node, "NezExpressionTransducer: undefined " + node);
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
			return ExpressionCommons.newNonTerminal(node, getGrammar(), symbol);
		}
	}

	public class _String extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression es) {
			String name = GrammarFile.nameTerminalProduction(node.toText());
			return ExpressionCommons.newNonTerminal(node, getGrammar(), name);
		}
	}

	public class _Character extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return ExpressionCommons.newString(node, StringUtils.unquoteString(node.toText()));
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
						l.add(ExpressionCommons.newCharSet(node, o.getText(0, ""), o.getText(1, "")));
					}
					if (o.is(_Class)) { // single
						l.add(ExpressionCommons.newCharSet(node, o.toText(), o.toText()));
					}
				}
			}
			return ExpressionCommons.newPchoice(node, l);
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
					return ExpressionCommons.newCbyte(node, binary, c);
				}
				String t2 = String.valueOf((char) c);
				return ExpressionCommons.newString(node, t2);
			}
			int c = StringUtils.hex(t.charAt(t.length() - 2)) * 16 + StringUtils.hex(t.charAt(t.length() - 1));
			return ExpressionCommons.newCbyte(node, binary, c);
		}
	}

	public class _AnyChar extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return ExpressionCommons.newCany(node, binary);
		}
	}

	public class _Choice extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			UList<Expression> l = new UList<Expression>(new Expression[node.size()]);
			for (int i = 0; i < node.size(); i++) {
				ExpressionCommons.addChoice(l, newInstance(node.get(i)));
			}
			return ExpressionCommons.newPchoice(node, l);
		}
	}

	public class _Sequence extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			UList<Expression> l = new UList<Expression>(new Expression[node.size()]);
			for (int i = 0; i < node.size(); i++) {
				ExpressionCommons.addSequence(l, newInstance(node.get(i)));
			}
			return ExpressionCommons.newPsequence(node, l);
		}
	}

	public class _Not extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return ExpressionCommons.newPnot(node, newInstance(node.get(_expr)));
		}
	}

	public class _And extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return ExpressionCommons.newPand(node, newInstance(node.get(_expr)));
		}
	}

	public class _Option extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return ExpressionCommons.newPoption(node, newInstance(node.get(_expr)));
		}
	}

	public class _Repetition1 extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return ExpressionCommons.newPone(node, newInstance(node.get(_expr)));
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
						ExpressionCommons.addSequence(l, newInstance(node.get(0)));
					}
					return ExpressionCommons.newPsequence(node, l);
				}
			}
			return ExpressionCommons.newPzero(node, newInstance(node.get(_expr)));
		}
	}

	// PEG4d TransCapturing

	public class _New extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			Tree<?> exprNode = node.get(_expr, null);
			Expression p = (exprNode == null) ? ExpressionCommons.newEmpty(node) : newInstance(exprNode);
			return ExpressionCommons.newNewCapture(node, false, null, p);
		}
	}

	public final static Symbol _name = Symbol.tag("name");
	public final static Symbol _expr = Symbol.tag("expr");
	public final static Symbol _symbol = Symbol.tag("symbol");
	public final static Symbol _hash = Symbol.tag("hash"); // example
	public final static Symbol _name2 = Symbol.tag("name2"); // example
	public final static Symbol _text = Symbol.tag("text"); // example

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
			Expression p = (exprNode == null) ? ExpressionCommons.newEmpty(node) : newInstance(exprNode);
			return ExpressionCommons.newNewCapture(node, true, parseLabelNode(node), p);
		}
	}

	public class _Link extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return ExpressionCommons.newTlink(node, parseLabelNode(node), newInstance(node.get(_expr)));
		}
	}

	public class _Tagging extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return ExpressionCommons.newTtag(node, Symbol.tag(node.toText()));
		}
	}

	public class _Replace extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return ExpressionCommons.newTreplace(node, node.toText());
		}
	}

	public class _Match extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			Tree<?> exprNode = node.get(_expr, null);
			if (exprNode != null) {
				return ExpressionCommons.newTdetree(node, newInstance(exprNode));
			}
			return ExpressionCommons.newXmatch(node, parseLabelNode(node));
		}
	}

	public class _If extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return ExpressionCommons.newXif(node, node.getText(_name, ""));
		}
	}

	public class _On extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return ExpressionCommons.newXon(node, true, node.getText(_name, ""), newInstance(node.get(_expr)));
		}
	}

	public class _Block extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return ExpressionCommons.newXblock(node, newInstance(node.get(_expr)));
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
			return ExpressionCommons.newXsymbol(node, pat);
		}
	}

	public class _Symbol extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			Grammar g = getGrammar();
			NonTerminal pat = g.newNonTerminal(node, node.getText(_name, ""));
			return ExpressionCommons.newXsymbol(node, pat);
		}
	}

	public class _Is extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			Grammar g = getGrammar();
			NonTerminal pat = g.newNonTerminal(node, node.getText(_name, ""));
			return ExpressionCommons.newXis(node, pat);
		}
	}

	public class _Isa extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			Grammar g = getGrammar();
			NonTerminal pat = g.newNonTerminal(node, node.getText(_name, ""));
			return ExpressionCommons.newXisa(node, pat);
		}
	}

	public class _Exists extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return ExpressionCommons.newXexists(node, Symbol.tag(node.getText(_name, "")), node.getText(_symbol, null));
		}
	}

	public class _Local extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return ExpressionCommons.newXlocal(node, Symbol.tag(node.getText(_name, "")), newInstance(node.get(_expr)));
		}
	}

	public class _DefIndent extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return ExpressionCommons.newDefIndent(node);
		}
	}

	public class _Indent extends Undefined {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			return ExpressionCommons.newIndent(node);
		}
	}

}
