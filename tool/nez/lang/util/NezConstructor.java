package nez.lang.util;

import java.util.ArrayList;

import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.Tree;
import nez.junks.Formatter;
import nez.junks.GrammarFile;
import nez.junks.GrammarFileLoader;
import nez.lang.Expression;
import nez.lang.Expressions;
import nez.lang.Grammar;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.lang.ast.GrammarExample;
import nez.lang.ast.NezGrammarCombinator;
import nez.parser.Parser;
import nez.parser.ParserStrategy;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UList;

public class NezConstructor extends GrammarFileLoader {

	public NezConstructor(Grammar g) {
		this.file = g;
		init(NezConstructor.class, new NezConstructorDefault());
	}

	public class NezConstructorDefault extends DefaultVisitor {
		@Override
		public void accept(Tree<?> node) {
			ConsoleUtils.println(node.formatSourceMessage("error", "unsupproted in NezConstructor #" + node));
		}

		@Override
		public Expression toExpression(Tree<?> node) {
			ConsoleUtils.println(node.formatSourceMessage("error", "unsupproted in NezConstructor #" + node));
			return null;
		}

		@Override
		public boolean parse(Tree<?> node) {
			ConsoleUtils.println(node.formatSourceMessage("error", "unsupproted in NezConstructor #" + node));
			return false;
		}
	}

	static Parser nezParser;

	@Override
	public Parser getLoaderParser(String start) {
		if (nezParser == null) {
			Grammar g = new Grammar("nez");
			nezParser = new NezGrammarCombinator().load(g, "File").newParser(start, ParserStrategy.newSafeStrategy());
			assert (nezParser != null);
		}
		return nezParser;
	}

	private long debugPosition = -1; // to detected infinite loop

	public void visit(Tree<?> node) {
		find(node.getTag().toString()).accept(node);
	}

	public boolean parseNode(Tree<?> node) {
		return find(node.getTag().toString()).parse(node);
	}

	public Expression newExpression(Tree<?> node) {
		return find(node.getTag().toString()).toExpression(node);
	}

	@Override
	public void parse(Tree<?> node) {
		if (node.getSourcePosition() == debugPosition) {
			ConsoleUtils.println(node.formatSourceMessage("panic", "parsed at the same position"));
			ConsoleUtils.println("node: " + node);
			throw new RuntimeException("");
		}
		// visit("parse", node);
		parseNode(node);
		debugPosition = node.getSourcePosition();
	}

	public class _Source extends NezConstructorDefault {
		@Override
		public boolean parse(Tree<?> node) {
			for (Tree<?> subnode : node) {
				parseNode(subnode);
			}
			return true;
		}
	}

	private boolean binary = false;
	public final static Symbol _String = Symbol.tag("String");
	public final static Symbol _Integer = Symbol.tag("Integer");
	public final static Symbol _List = Symbol.tag("List");
	public final static Symbol _Name = Symbol.tag("Name");
	public final static Symbol _Format = Symbol.tag("Format");
	public final static Symbol _Class = Symbol.tag("Class");

	public final static Symbol _anno = Symbol.tag("anno");

	public class _Production extends NezConstructorDefault {
		@Override
		public boolean parse(Tree<?> node) {
			Tree<?> nameNode = node.get(_name);
			String localName = nameNode.toText();
			int productionFlag = 0;
			if (nameNode.is(_String)) {
				localName = GrammarFile.nameTerminalProduction(localName);
			}

			Production rule = getGrammar().getProduction(localName);
			if (rule != null) {
				reportWarning(node, "duplicated rule name: " + localName);
				rule = null;
			}
			Expression e = newExpression(node.get(_expr));
			rule = getGrammar().newProduction(node.get(0), productionFlag, localName, e);
			return true;
		}
	}

	public class Instance extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			return newExpression(node);
		}
	}

	public class _NonTerminal extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			String symbol = node.toText();
			return Expressions.newNonTerminal(node, getGrammar(), symbol);
		}
	}

	public class _String extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			String name = GrammarFile.nameTerminalProduction(node.toText());
			return Expressions.newNonTerminal(node, getGrammar(), name);
		}
	}

	public class _Character extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			return Expressions.newExpression(node, StringUtils.unquoteString(node.toText()));
		}
	}

	public class _Class extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
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

	public class _ByteChar extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
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

	public class _AnyChar extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			return Expressions.newAny(node);
		}
	}

	public class _Choice extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			UList<Expression> l = new UList<Expression>(new Expression[node.size()]);
			for (int i = 0; i < node.size(); i++) {
				Expressions.addChoice(l, newExpression(node.get(i)));
			}
			return Expressions.newChoice(l);
		}
	}

	public class _Sequence extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			UList<Expression> l = new UList<Expression>(new Expression[node.size()]);
			for (int i = 0; i < node.size(); i++) {
				Expressions.addSequence(l, newExpression(node.get(i)));
			}
			return Expressions.newPair(l);
		}
	}

	public class _Not extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			return Expressions.newNot(node, newExpression(node.get(_expr)));
		}
	}

	// public Expression newNot(Tree<?> node) {
	// return ExpressionCommons.newPnot(node, newExpression(node.get(_expr)));
	// }

	public class _And extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			return Expressions.newAnd(node, newExpression(node.get(_expr)));
		}
	}

	public class _Option extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			return Expressions.newOption(node, newExpression(node.get(_expr)));
		}
	}

	public class _Repetition1 extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			return Expressions.newOneMore(node, newExpression(node.get(_expr)));
		}
	}

	public class _Repetition extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			if (node.size() == 2) {
				int ntimes = StringUtils.parseInt(node.getText(1, ""), -1);
				if (ntimes != 1) {
					UList<Expression> l = new UList<Expression>(new Expression[ntimes]);
					for (int i = 0; i < ntimes; i++) {
						Expressions.addSequence(l, newExpression(node.get(0)));
					}
					return Expressions.newPair(l);
				}
			}
			return Expressions.newZeroMore(node, newExpression(node.get(_expr)));
		}
	}

	// PEG4d TransCapturing

	public class _New extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			Tree<?> exprNode = node.get(_expr, null);
			Expression p = (exprNode == null) ? Expressions.newEmpty(node) : newExpression(exprNode);
			return Expressions.newTree(node, false, null, p);
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

	public class _LeftFold extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			Tree<?> exprNode = node.get(_expr, null);
			Expression p = (exprNode == null) ? Expressions.newEmpty(node) : newExpression(exprNode);
			return Expressions.newTree(node, true, parseLabelNode(node), p);
		}
	}

	public class _Link extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			return Expressions.newLinkTree(node, parseLabelNode(node), newExpression(node.get(_expr)));
		}
	}

	public class _Tagging extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			return Expressions.newTag(node, Symbol.tag(node.toText()));
		}
	}

	public class _Replace extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			return Expressions.newReplace(node, node.toText());
		}
	}

	public class _If extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			return Expressions.newIfCondition(node, node.getText(_name, ""));
		}
	}

	public class _On extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			return Expressions.newOnCondition(node, node.getText(_name, ""), newExpression(node.get(_expr)));
		}
	}

	public class _Block extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			return Expressions.newBlockScope(node, newExpression(node.get(_expr)));
		}
	}

	public class _Def extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			Grammar g = getGrammar();
			Tree<?> nameNode = node.get(_name);
			NonTerminal pat = g.newNonTerminal(node, nameNode.toText());
			Expression e = newExpression(node.get(_expr));
			Production p = g.newProduction(pat.getLocalName(), e);
			reportWarning(nameNode, "new production generated: " + p);
			return Expressions.newSymbol(node, pat);
		}
	}

	public class _Symbol extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			Grammar g = getGrammar();
			NonTerminal pat = g.newNonTerminal(node, node.getText(_name, ""));
			return Expressions.newSymbol(node, pat);
		}
	}

	public class _Match extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			Grammar g = getGrammar();
			NonTerminal pat = g.newNonTerminal(node, node.getText(_name, ""));
			return Expressions.newSymbolMatch(node, pat);
		}
	}

	public class _Is extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			Grammar g = getGrammar();
			NonTerminal pat = g.newNonTerminal(node, node.getText(_name, ""));
			return Expressions.newIsSymbol(node, pat);
		}
	}

	public class _Isa extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			Grammar g = getGrammar();
			NonTerminal pat = g.newNonTerminal(node, node.getText(_name, ""));
			return Expressions.newIsaSymbol(node, pat);
		}
	}

	public class _Exists extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			return Expressions.newSymbolExists(node, Symbol.tag(node.getText(_name, "")), node.getText(_symbol, null));
		}
	}

	public class _Local extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			return Expressions.newLocalScope(node, Symbol.tag(node.getText(_name, "")), newExpression(node.get(_expr)));
		}
	}

	public class Undefined extends NezConstructorDefault {
		@Override
		public Expression toExpression(Tree<?> node) {
			reportError(node, "undefined or deprecated notation");
			return Expressions.newEmpty(node);
		}
	}

	public class _Example extends NezConstructorDefault {
		@Override
		public boolean parse(Tree<?> node) {
			String hash = node.getText(_hash, null);
			Tree<?> textNode = node.get(_text);
			Tree<?> nameNode = node.get(_name2, null);
			if (nameNode != null) {
				GrammarExample.add(getGrammar(), true, nameNode, hash, textNode);
				nameNode = node.get(_name);
				GrammarExample.add(getGrammar(), false, nameNode, hash, textNode);
			} else {
				nameNode = node.get(_name);
				GrammarExample.add(getGrammar(), true, nameNode, hash, textNode);
			}
			return true;
		}
	}

	public class Format extends NezConstructorDefault {
		@Override
		public boolean parse(Tree<?> node) {
			String tag = node.getText(0, "token");
			int index = StringUtils.parseInt(node.getText(1, "*"), -1);
			Formatter fmt = toFormatter(node.get(2));
			// getGrammarFile().addFormatter(tag, index, fmt);
			return true;
		}
	}

	Formatter toFormatter(Tree<?> node) {
		if (node.is(_List)) {
			ArrayList<Formatter> l = new ArrayList<Formatter>(node.size());
			for (Tree<?> t : node) {
				l.add(toFormatter(t));
			}
			return Formatter.newFormatter(l);
		}
		if (node.is(_Integer)) {
			return Formatter.newFormatter(StringUtils.parseInt(node.toText(), 0));
		}
		if (node.is(_Format)) {
			int s = StringUtils.parseInt(node.getText(0, "*"), -1);
			int e = StringUtils.parseInt(node.getText(2, "*"), -1);
			Formatter fmt = toFormatter(node.get(1));
			return Formatter.newFormatter(s, fmt, e);
		}
		if (node.is(_Name)) {
			Formatter fmt = Formatter.newAction(node.toText());
			if (fmt == null) {
				this.reportWarning(node, "undefined formatter action");
				fmt = Formatter.newFormatter("${" + node.toText() + "}");
			}
			return fmt;
		}
		return Formatter.newFormatter(node.toText());
	}

	/**
	 * public class Import extends NezConstructorDefault {
	 * 
	 * @Override public boolean parse(Tree<?> node) { String ns = null; String
	 *           name = node.getText(0, "*"); int loc = name.indexOf('.'); if
	 *           (loc >= 0) { ns = name.substring(0, loc); name =
	 *           name.substring(loc + 1); } String urn =
	 *           path(node.getSource().getResourceName(), node.getText(1, ""));
	 *           try { GrammarFile source = (GrammarFile)
	 *           GrammarFileLoader.loadGrammar(urn, strategy); if
	 *           (name.equals("*")) { int c = 0; for (Production p : source) {
	 *           if (p.isPublic()) { checkDuplicatedName(node.get(0));
	 *           getGrammarFile().importProduction(ns, p); c++; } } if (c == 0)
	 *           { reportError(node.get(0),
	 *           "nothing imported (no public production exisits)"); } } else {
	 *           Production p = source.getProduction(name); if (p == null) {
	 *           reportError(node.get(0), "undefined production: " + name);
	 *           return false; } getGrammarFile().importProduction(ns, p); }
	 *           return true; } catch (IOException e) { reportError(node.get(1),
	 *           "unfound: " + urn); } catch (NullPointerException e) {
	 *           reportError(node.get(1), "unfound: " + urn); } return false; }
	 *           }
	 **/

	private void checkDuplicatedName(Tree<?> errorNode) {
		String name = errorNode.toText();
		if (this.getGrammar().hasProduction(name)) {
			this.reportWarning(errorNode, "duplicated production: " + name);
		}
	}

	private String path(String path, String path2) {
		if (path != null) {
			int loc = path.lastIndexOf('/');
			if (loc > 0) {
				return path.substring(0, loc + 1) + path2;
			}
		}
		return path2;
	}

	@Override
	public String parseGrammarDescription(Source sc) {
		StringBuilder sb = new StringBuilder();
		long pos = 0;
		boolean found = false;
		for (; pos < sc.length(); pos++) {
			int ch = sc.byteAt(pos);
			if (Character.isAlphabetic(ch)) {
				found = true;
				break;
			}
		}
		if (found) {
			for (; pos < sc.length(); pos++) {
				int ch = sc.byteAt(pos);
				if (ch == '\n' || ch == '\r' || ch == '-' || ch == '*') {
					break;
				}
				sb.append((char) ch);
			}
		}
		return sb.toString().trim();
	}

}
