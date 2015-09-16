package nez.ext;

import java.io.IOException;
import java.util.ArrayList;

import nez.Parser;
import nez.Strategy;
import nez.ast.AbstractTree;
import nez.ast.SymbolId;
import nez.lang.Example;
import nez.lang.Expression;
import nez.lang.Formatter;
import nez.lang.GrammarFile;
import nez.lang.GrammarFileLoader;
import nez.lang.NezGrammar1;
import nez.lang.Production;
import nez.lang.expr.ExpressionCommons;
import nez.util.StringUtils;
import nez.util.UList;

public class Gnez extends GrammarFileLoader {

	public Gnez() {
	}

	static Parser nezParser;

	@Override
	public Parser getLoaderGrammar() {
		if (nezParser == null) {
			nezParser = new NezGrammar1().newParser(Strategy.newSafeStrategy(), null);
			assert (nezParser != null);
		}
		return nezParser;
	}

	@Override
	public void parse(AbstractTree<?> node) {
		// System.out.println("parsing " + node +
		// node.formatSourceMessage("debug", "looping"));
		visit("parse", node);
	}

	public boolean parseSource(AbstractTree<?> node) {
		for (AbstractTree<?> subnode : node) {
			parse(subnode);
		}
		return true;
	}

	private boolean binary = false;
	public final static SymbolId _String = SymbolId.tag("String");
	public final static SymbolId _Integer = SymbolId.tag("Integer");
	public final static SymbolId _List = SymbolId.tag("List");
	public final static SymbolId _Name = SymbolId.tag("Name");
	public final static SymbolId _Format = SymbolId.tag("Format");
	public final static SymbolId _Class = SymbolId.tag("Class");

	public final static SymbolId _anno = SymbolId.tag("anno");

	public Production parseProduction(AbstractTree<?> node) {
		AbstractTree<?> nameNode = node.get(_name);
		String localName = nameNode.toText();
		int productionFlag = 0;
		if (nameNode.is(_String)) {
			localName = GrammarFile.nameTerminalProduction(localName);
			productionFlag |= Production.TerminalProduction;
		}
		this.binary = false;
		AbstractTree<?> annoNode = node.get(_anno, null);
		if (annoNode != null) {
			if (annoNode.containsToken("binary")) {
				this.binary = true;
			}
			if (annoNode.containsToken("public")) {
				productionFlag |= Production.PublicProduction;
			}
			if (annoNode.containsToken("inline")) {
				productionFlag |= Production.InlineProduction;
			}
		}
		Production rule = this.getGrammarFile().getProduction(localName);
		if (rule != null) {
			this.reportWarning(node, "duplicated rule name: " + localName);
			rule = null;
		}
		Expression e = newExpression(node.get(_expr));
		rule = this.getGrammarFile().addProduction(node.get(0), productionFlag, localName, e);
		return rule;
	}

	public Expression newExpression(AbstractTree<?> node) {
		return (Expression) visit("new", node);
	}

	public Expression newNonTerminal(AbstractTree<?> node) {
		String symbol = node.toText();
		return ExpressionCommons.newNonTerminal(node, this.getGrammarFile(), symbol);
	}

	public Expression newString(AbstractTree<?> node) {
		String name = GrammarFile.nameTerminalProduction(node.toText());
		return ExpressionCommons.newNonTerminal(node, this.getGrammarFile(), name);
	}

	public Expression newCharacter(AbstractTree<?> node) {
		return ExpressionCommons.newString(node, StringUtils.unquoteString(node.toText()));
	}

	public Expression newClass(AbstractTree<?> node) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		if (node.size() > 0) {
			for (int i = 0; i < node.size(); i++) {
				AbstractTree<?> o = node.get(i);
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

	public Expression newByte(AbstractTree<?> node) {
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

	public Expression newAnyChar(AbstractTree<?> node) {
		return ExpressionCommons.newCany(node, this.binary);
	}

	public Expression newChoice(AbstractTree<?> node) {
		UList<Expression> l = new UList<Expression>(new Expression[node.size()]);
		for (int i = 0; i < node.size(); i++) {
			ExpressionCommons.addChoice(l, newExpression(node.get(i)));
		}
		return ExpressionCommons.newPchoice(node, l);
	}

	public Expression newSequence(AbstractTree<?> node) {
		UList<Expression> l = new UList<Expression>(new Expression[node.size()]);
		for (int i = 0; i < node.size(); i++) {
			ExpressionCommons.addSequence(l, newExpression(node.get(i)));
		}
		return ExpressionCommons.newPsequence(node, l);
	}

	public Expression newNot(AbstractTree<?> node) {
		return ExpressionCommons.newPnot(node, newExpression(node.get(_expr)));
	}

	public Expression newAnd(AbstractTree<?> node) {
		return ExpressionCommons.newPand(node, newExpression(node.get(_expr)));
	}

	public Expression newOption(AbstractTree<?> node) {
		return ExpressionCommons.newPoption(node, newExpression(node.get(_expr)));
	}

	public Expression newRepetition1(AbstractTree<?> node) {
		return ExpressionCommons.newPone(node, newExpression(node.get(_expr)));
	}

	public Expression newRepetition(AbstractTree<?> node) {
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

	public Expression newNew(AbstractTree<?> node) {
		AbstractTree<?> exprNode = node.get(_expr, null);
		Expression p = (exprNode == null) ? ExpressionCommons.newEmpty(node) : newExpression(exprNode);
		return ExpressionCommons.newNewCapture(node, false, null, p);
	}

	public final static SymbolId _name = SymbolId.tag("name");
	public final static SymbolId _expr = SymbolId.tag("expr");
	public final static SymbolId _symbol = SymbolId.tag("symbol");
	public final static SymbolId _hash = SymbolId.tag("hash"); // example
	public final static SymbolId _name2 = SymbolId.tag("name2"); // example
	public final static SymbolId _text = SymbolId.tag("text"); // example

	private SymbolId parseLabelNode(AbstractTree<?> node) {
		SymbolId label = null;
		AbstractTree<?> labelNode = node.get(_name, null);
		if (labelNode != null) {
			label = SymbolId.tag(labelNode.toText());
		}
		return label;
	}

	public Expression newLeftFold(AbstractTree<?> node) {
		AbstractTree<?> exprNode = node.get(_expr, null);
		Expression p = (exprNode == null) ? ExpressionCommons.newEmpty(node) : newExpression(exprNode);
		return ExpressionCommons.newNewCapture(node, true, parseLabelNode(node), p);
	}

	public Expression newLink(AbstractTree<?> node) {
		return ExpressionCommons.newTlink(node, parseLabelNode(node), newExpression(node.get(_expr)));
	}

	public Expression newTagging(AbstractTree<?> node) {
		return ExpressionCommons.newTtag(node, SymbolId.tag(node.toText()));
	}

	public Expression newReplace(AbstractTree<?> node) {
		return ExpressionCommons.newTreplace(node, node.toText());
	}

	public Expression newMatch(AbstractTree<?> node) {
		AbstractTree<?> exprNode = node.get(_expr, null);
		if (exprNode != null) {
			return ExpressionCommons.newTdetree(node, newExpression(exprNode));
		}
		return ExpressionCommons.newXmatch(node, parseLabelNode(node));
	}

	public Expression newIf(AbstractTree<?> node) {
		return ExpressionCommons.newXif(node, node.getText(_name, ""));
	}

	public Expression newOn(AbstractTree<?> node) {
		return ExpressionCommons.newXon(node, true, node.getText(_name, ""), newExpression(node.get(_expr)));
	}

	public Expression newBlock(AbstractTree<?> node) {
		return ExpressionCommons.newXblock(node, newExpression(node.get(_expr)));
	}

	public Expression newDef(AbstractTree<?> node) {
		return ExpressionCommons.newXdef(node, this.getGrammarFile(), SymbolId.tag(node.getText(_name, "")), newExpression(node.get(_expr)));
	}

	public Expression newIs(AbstractTree<?> node) {
		return ExpressionCommons.newXis(node, this.getGrammarFile(), SymbolId.tag(node.getText(_name, "")));
	}

	public Expression newIsa(AbstractTree<?> node) {
		return ExpressionCommons.newXisa(node, this.getGrammarFile(), SymbolId.tag(node.getText(_name, "")));
	}

	public Expression newExists(AbstractTree<?> node) {
		return ExpressionCommons.newXexists(node, SymbolId.tag(node.getText(_name, "")), node.getText(_symbol, null));
	}

	public Expression newLocal(AbstractTree<?> node) {
		return ExpressionCommons.newXlocal(node, SymbolId.tag(node.getText(_name, "")), newExpression(node.get(_expr)));
	}

	public Expression newDefIndent(AbstractTree<?> node) {
		return ExpressionCommons.newDefIndent(node);
	}

	public Expression newIndent(AbstractTree<?> node) {
		return ExpressionCommons.newIndent(node);
	}

	public Expression newUndefined(AbstractTree<?> node) {
		this.reportError(node, "undefined or deprecated notation");
		return ExpressionCommons.newEmpty(node);
	}

	public boolean parseExample(AbstractTree<?> node) {
		String hash = node.getText(_hash, null);
		AbstractTree<?> textNode = node.get(_text);
		AbstractTree<?> nameNode = node.get(_name2, null);
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

	public boolean parseFormat(AbstractTree<?> node) {
		// System.out.println("node: " + node);
		String tag = node.getText(0, "token");
		int index = StringUtils.parseInt(node.getText(1, "*"), -1);
		Formatter fmt = toFormatter(node.get(2));
		this.getGrammarFile().addFormatter(tag, index, fmt);
		return true;
	}

	Formatter toFormatter(AbstractTree<?> node) {
		if (node.is(_List)) {
			ArrayList<Formatter> l = new ArrayList<Formatter>(node.size());
			for (AbstractTree<?> t : node) {
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
	public boolean parseImport(AbstractTree<?> node) {
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
			GrammarFile source = (GrammarFile) GrammarFileLoader.loadGrammar(urn, this.option, this.repo);
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

	private void checkDuplicatedName(AbstractTree<?> errorNode) {
		String name = errorNode.toText();
		if (this.getGrammarFile().hasProduction(name)) {
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

}