package nez.lang.util;

import java.io.IOException;
import java.util.ArrayList;

import nez.Grammar;
import nez.Parser;
import nez.Strategy;
import nez.ast.Constructor;
import nez.ast.Symbol;
import nez.ast.Tree;
import nez.io.SourceContext;
import nez.lang.Example;
import nez.lang.Expression;
import nez.lang.Formatter;
import nez.lang.GrammarFile;
import nez.lang.GrammarFileLoader;
import nez.lang.NezGrammar1;
import nez.lang.Production;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.NonTerminal;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UList;

public class NezConstructor extends GrammarFileLoader implements Constructor {

	public NezConstructor(Grammar g) {
		this.file = g;
	}

	static Parser nezParser;

	@Override
	public Parser getLoaderParser(String start) {
		if (nezParser == null) {
			nezParser = new NezGrammar1().newParser(start, Strategy.newSafeStrategy());
			assert (nezParser != null);
		}
		return nezParser;
	}

	private long debugPosition = -1; // to detected infinite loop

	@Override
	public void parse(Tree<?> node) {
		if (node.getSourcePosition() == debugPosition) {
			ConsoleUtils.println(node.formatSourceMessage("panic", "parsed at the same position"));
			ConsoleUtils.println("node: " + node);
			throw new RuntimeException("");
		}
		visit("parse", node);
		debugPosition = node.getSourcePosition();
	}

	public boolean parseSource(Tree<?> node) {
		for (Tree<?> subnode : node) {
			parse(subnode);
		}
		return true;
	}

	private boolean binary = false;
	public final static Symbol _String = Symbol.tag("String");
	public final static Symbol _Integer = Symbol.tag("Integer");
	public final static Symbol _List = Symbol.tag("List");
	public final static Symbol _Name = Symbol.tag("Name");
	public final static Symbol _Format = Symbol.tag("Format");
	public final static Symbol _Class = Symbol.tag("Class");

	public final static Symbol _anno = Symbol.tag("anno");

	public Production parseProduction(Tree<?> node) {
		Tree<?> nameNode = node.get(_name);
		String localName = nameNode.toText();
		int productionFlag = 0;
		if (nameNode.is(_String)) {
			localName = GrammarFile.nameTerminalProduction(localName);
			productionFlag |= Production.TerminalProduction;
		}
		// this.binary = false;
		// AbstractTree<?> annoNode = node.get(_anno, null);
		// if (annoNode != null) {
		// if (annoNode.containsToken("binary")) {
		// this.binary = true;
		// }
		// if (annoNode.containsToken("public")) {
		// productionFlag |= Production.PublicProduction;
		// }
		// if (annoNode.containsToken("inline")) {
		// productionFlag |= Production.InlineProduction;
		// }
		// }
		Production rule = this.getGrammar().getProduction(localName);
		if (rule != null) {
			this.reportWarning(node, "duplicated rule name: " + localName);
			rule = null;
		}
		Expression e = newExpression(node.get(_expr));
		rule = this.getGrammar().newProduction(node.get(0), productionFlag, localName, e);
		return rule;
	}

	@Override
	public final Object newInstance(Tree<?> node) {
		return visit("new", node);
	}

	public Expression newExpression(Tree<?> node) {
		return (Expression) visit("new", node);
	}

	public Expression newNonTerminal(Tree<?> node) {
		String symbol = node.toText();
		return ExpressionCommons.newNonTerminal(node, this.getGrammar(), symbol);
	}

	public Expression newString(Tree<?> node) {
		String name = GrammarFile.nameTerminalProduction(node.toText());
		return ExpressionCommons.newNonTerminal(node, this.getGrammar(), name);
	}

	public Expression newCharacter(Tree<?> node) {
		return ExpressionCommons.newString(node, StringUtils.unquoteString(node.toText()));
	}

	public Expression newClass(Tree<?> node) {
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

	public Expression newByteChar(Tree<?> node) {
		String t = node.toText();
		if (t.startsWith("U+")) {
			int c = StringUtils.hex(t.charAt(2));
			c = (c * 16) + StringUtils.hex(t.charAt(3));
			c = (c * 16) + StringUtils.hex(t.charAt(4));
			c = (c * 16) + StringUtils.hex(t.charAt(5));
			if (c < 128) {
				return ExpressionCommons.newCbyte(node, this.binary, c);
			}
			String t2 = java.lang.String.valueOf((char) c);
			return ExpressionCommons.newString(node, t2);
		}
		int c = StringUtils.hex(t.charAt(t.length() - 2)) * 16 + StringUtils.hex(t.charAt(t.length() - 1));
		return ExpressionCommons.newCbyte(node, this.binary, c);
	}

	public Expression newAnyChar(Tree<?> node) {
		return ExpressionCommons.newCany(node, this.binary);
	}

	public Expression newChoice(Tree<?> node) {
		UList<Expression> l = new UList<Expression>(new Expression[node.size()]);
		for (int i = 0; i < node.size(); i++) {
			ExpressionCommons.addChoice(l, newExpression(node.get(i)));
		}
		return ExpressionCommons.newPchoice(node, l);
	}

	public Expression newSequence(Tree<?> node) {
		UList<Expression> l = new UList<Expression>(new Expression[node.size()]);
		for (int i = 0; i < node.size(); i++) {
			ExpressionCommons.addSequence(l, newExpression(node.get(i)));
		}
		return ExpressionCommons.newPsequence(node, l);
	}

	public Expression newNot(Tree<?> node) {
		return ExpressionCommons.newPnot(node, newExpression(node.get(_expr)));
	}

	public Expression newAnd(Tree<?> node) {
		return ExpressionCommons.newPand(node, newExpression(node.get(_expr)));
	}

	public Expression newOption(Tree<?> node) {
		return ExpressionCommons.newPoption(node, newExpression(node.get(_expr)));
	}

	public Expression newRepetition1(Tree<?> node) {
		return ExpressionCommons.newPone(node, newExpression(node.get(_expr)));
	}

	public Expression newRepetition(Tree<?> node) {
		if (node.size() == 2) {
			int ntimes = StringUtils.parseInt(node.getText(1, ""), -1);
			if (ntimes != 1) {
				UList<Expression> l = new UList<Expression>(new Expression[ntimes]);
				for (int i = 0; i < ntimes; i++) {
					ExpressionCommons.addSequence(l, newExpression(node.get(0)));
				}
				return ExpressionCommons.newPsequence(node, l);
			}
		}
		return ExpressionCommons.newPzero(node, newExpression(node.get(_expr)));
	}

	// PEG4d TransCapturing

	public Expression newNew(Tree<?> node) {
		Tree<?> exprNode = node.get(_expr, null);
		Expression p = (exprNode == null) ? ExpressionCommons.newEmpty(node) : newExpression(exprNode);
		return ExpressionCommons.newNewCapture(node, false, null, p);
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

	public Expression newLeftFold(Tree<?> node) {
		Tree<?> exprNode = node.get(_expr, null);
		Expression p = (exprNode == null) ? ExpressionCommons.newEmpty(node) : newExpression(exprNode);
		return ExpressionCommons.newNewCapture(node, true, parseLabelNode(node), p);
	}

	public Expression newLink(Tree<?> node) {
		return ExpressionCommons.newTlink(node, parseLabelNode(node), newExpression(node.get(_expr)));
	}

	public Expression newTagging(Tree<?> node) {
		return ExpressionCommons.newTtag(node, Symbol.tag(node.toText()));
	}

	public Expression newReplace(Tree<?> node) {
		return ExpressionCommons.newTreplace(node, node.toText());
	}

	public Expression newMatch(Tree<?> node) {
		Tree<?> exprNode = node.get(_expr, null);
		if (exprNode != null) {
			return ExpressionCommons.newTdetree(node, newExpression(exprNode));
		}
		return ExpressionCommons.newXmatch(node, parseLabelNode(node));
	}

	public Expression newIf(Tree<?> node) {
		return ExpressionCommons.newXif(node, node.getText(_name, ""));
	}

	public Expression newOn(Tree<?> node) {
		return ExpressionCommons.newXon(node, true, node.getText(_name, ""), newExpression(node.get(_expr)));
	}

	public Expression newBlock(Tree<?> node) {
		return ExpressionCommons.newXblock(node, newExpression(node.get(_expr)));
	}

	public Expression newDef(Tree<?> node) {
		Grammar g = this.getGrammar();
		NonTerminal pat = g.newNonTerminal(node, node.getText(_name, ""));
		return ExpressionCommons.newXsymbol(node, pat);
	}

	public Expression newSymbol(Tree<?> node) {
		Grammar g = this.getGrammar();
		NonTerminal pat = g.newNonTerminal(node, node.getText(_name, ""));
		return ExpressionCommons.newXsymbol(node, pat);
	}

	public Expression newIs(Tree<?> node) {
		Grammar g = this.getGrammar();
		NonTerminal pat = g.newNonTerminal(node, node.getText(_name, ""));
		return ExpressionCommons.newXis(node, pat);
	}

	public Expression newIsa(Tree<?> node) {
		Grammar g = this.getGrammar();
		NonTerminal pat = g.newNonTerminal(node, node.getText(_name, ""));
		return ExpressionCommons.newXisa(node, pat);
	}

	public Expression newExists(Tree<?> node) {
		return ExpressionCommons.newXexists(node, Symbol.tag(node.getText(_name, "")), node.getText(_symbol, null));
	}

	public Expression newLocal(Tree<?> node) {
		return ExpressionCommons.newXlocal(node, Symbol.tag(node.getText(_name, "")), newExpression(node.get(_expr)));
	}

	public Expression newDefIndent(Tree<?> node) {
		return ExpressionCommons.newDefIndent(node);
	}

	public Expression newIndent(Tree<?> node) {
		return ExpressionCommons.newIndent(node);
	}

	public Expression newUndefined(Tree<?> node) {
		this.reportError(node, "undefined or deprecated notation");
		return ExpressionCommons.newEmpty(node);
	}

	public boolean parseExample(Tree<?> node) {

		String hash = node.getText(_hash, null);
		Tree<?> textNode = node.get(_text);
		Tree<?> nameNode = node.get(_name2, null);
		if (nameNode != null) {
			this.getGrammarFile().addExample(new Example(true, nameNode, hash, textNode));
			nameNode = node.get(_name);
			this.getGrammarFile().addExample(new Example(false, nameNode, hash, textNode));
		} else {
			nameNode = node.get(_name);
			this.getGrammarFile().addExample(new Example(true, nameNode, hash, textNode));
		}
		return true;
	}

	public boolean parseFormat(Tree<?> node) {
		// System.out.println("node: " + node);
		String tag = node.getText(0, "token");
		int index = StringUtils.parseInt(node.getText(1, "*"), -1);
		Formatter fmt = toFormatter(node.get(2));
		this.getGrammarFile().addFormatter(tag, index, fmt);
		return true;
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

	/* import */
	public boolean parseImport(Tree<?> node) {
		// System.out.println("DEBUG? parsed: " + node);
		String ns = null;
		String name = node.getText(0, "*");
		int loc = name.indexOf('.');
		if (loc >= 0) {
			ns = name.substring(0, loc);
			name = name.substring(loc + 1);
		}
		String urn = path(node.getSource().getResourceName(), node.getText(1, ""));
		try {
			GrammarFile source = (GrammarFile) GrammarFileLoader.loadGrammar(urn, this.strategy);
			if (name.equals("*")) {
				int c = 0;
				for (Production p : source) {
					if (p.isPublic()) {
						checkDuplicatedName(node.get(0));
						this.getGrammarFile().importProduction(ns, p);
						c++;
					}
				}
				if (c == 0) {
					this.reportError(node.get(0), "nothing imported (no public production exisits)");
				}
			} else {
				Production p = source.getProduction(name);
				if (p == null) {
					this.reportError(node.get(0), "undefined production: " + name);
					return false;
				}
				this.getGrammarFile().importProduction(ns, p);
			}
			return true;
		} catch (IOException e) {
			this.reportError(node.get(1), "unfound: " + urn);
		} catch (NullPointerException e) {
			/*
			 * This is for a bug unhandling IOException at
			 * java.io.Reader.<init>(Reader.java:78)
			 */
			this.reportError(node.get(1), "unfound: " + urn);
		}
		return false;
	}

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
	public String parseGrammarDescription(SourceContext sc) {
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
