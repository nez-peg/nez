package nez.lang;

import java.io.IOException;
import java.util.ArrayList;

import nez.NezOption;
import nez.ast.AbstractTree;
import nez.ast.Tag;
import nez.util.StringUtils;
import nez.util.UList;

public class NezGrammarLoader extends GrammarLoader {
	static Parser nezGrammar;

	public NezGrammarLoader(GrammarFile file) {
		super(file);
	}

	@Override
	public Parser getStartGrammar() {
		if (nezGrammar == null) {
			// if (this.getGrammarOption().classicMode) {
			// Verbose.println("Loading classic Nez grammar");
			nezGrammar = NezGrammarClassic.newGrammar("File", NezOption.newSafeOption());
			// } else {
			// nezGrammar = NezGrammar.newGrammar("File",
			// NezOption.newSafeOption());
			// }
		}
		return nezGrammar;
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
	public final static Tag _String = Tag.tag("String");
	public final static Tag _Integer = Tag.tag("Integer");
	public final static Tag _List = Tag.tag("List");
	public final static Tag _Name = Tag.tag("Name");
	public final static Tag _Format = Tag.tag("Format");
	public final static Tag _Class = Tag.tag("Class");

	public final static Tag _anno = Tag.tag("anno");

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
		rule = this.getGrammarFile().defineProduction(node.get(0), productionFlag, localName, e);
		return rule;
	}

	public Expression newExpression(AbstractTree<?> node) {
		return (Expression) visit("new", node);
	}

	public Expression newNonTerminal(AbstractTree<?> node) {
		String symbol = node.toText();
		return GrammarFactory.newNonTerminal(node, this.getGrammarFile(), symbol);
	}

	public Expression newString(AbstractTree<?> node) {
		String name = GrammarFile.nameTerminalProduction(node.toText());
		return GrammarFactory.newNonTerminal(node, this.getGrammarFile(), name);
	}

	public Expression newCharacter(AbstractTree<?> node) {
		return GrammarFactory.newString(node, StringUtils.unquoteString(node.toText()));
	}

	public Expression newClass(AbstractTree<?> node) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		if (node.size() > 0) {
			for (int i = 0; i < node.size(); i++) {
				AbstractTree<?> o = node.get(i);
				if (o.is(_List)) { // range
					l.add(GrammarFactory.newCharSet(node, o.getText(0, ""), o.getText(1, "")));
				}
				if (o.is(_Class)) { // single
					l.add(GrammarFactory.newCharSet(node, o.toText(), o.toText()));
				}
			}
		}
		return GrammarFactory.newChoice(node, l);
	}

	public Expression newByte(AbstractTree<?> node) {
		String t = node.toText();
		if (t.startsWith("U+")) {
			int c = StringUtils.hex(t.charAt(2));
			c = (c * 16) + StringUtils.hex(t.charAt(3));
			c = (c * 16) + StringUtils.hex(t.charAt(4));
			c = (c * 16) + StringUtils.hex(t.charAt(5));
			if (c < 128) {
				return GrammarFactory.newByteChar(node, this.binary, c);
			}
			String t2 = java.lang.String.valueOf((char) c);
			return GrammarFactory.newString(node, t2);
		}
		int c = StringUtils.hex(t.charAt(t.length() - 2)) * 16 + StringUtils.hex(t.charAt(t.length() - 1));
		return GrammarFactory.newByteChar(node, this.binary, c);
	}

	public Expression newAnyChar(AbstractTree<?> node) {
		return GrammarFactory.newAnyChar(node, this.binary);
	}

	public Expression newChoice(AbstractTree<?> node) {
		UList<Expression> l = new UList<Expression>(new Expression[node.size()]);
		for (int i = 0; i < node.size(); i++) {
			GrammarFactory.addChoice(l, newExpression(node.get(i)));
		}
		return GrammarFactory.newChoice(node, l);
	}

	public Expression newSequence(AbstractTree<?> node) {
		UList<Expression> l = new UList<Expression>(new Expression[node.size()]);
		for (int i = 0; i < node.size(); i++) {
			GrammarFactory.addSequence(l, newExpression(node.get(i)));
		}
		return GrammarFactory.newSequence(node, l);
	}

	public Expression newNot(AbstractTree<?> node) {
		return GrammarFactory.newNot(node, newExpression(node.get(_expr)));
	}

	public Expression newAnd(AbstractTree<?> node) {
		return GrammarFactory.newAnd(node, newExpression(node.get(_expr)));
	}

	public Expression newOption(AbstractTree<?> node) {
		return GrammarFactory.newOption(node, newExpression(node.get(_expr)));
	}

	public Expression newRepetition1(AbstractTree<?> node) {
		return GrammarFactory.newRepetition1(node, newExpression(node.get(_expr)));
	}

	public Expression newRepetition(AbstractTree<?> node) {
		if (node.size() == 2) {
			int ntimes = StringUtils.parseInt(node.getText(1, ""), -1);
			if (ntimes != 1) {
				UList<Expression> l = new UList<Expression>(new Expression[ntimes]);
				for (int i = 0; i < ntimes; i++) {
					GrammarFactory.addSequence(l, newExpression(node.get(0)));
				}
				return GrammarFactory.newSequence(node, l);
			}
		}
		return GrammarFactory.newRepetition(node, newExpression(node.get(_expr)));
	}

	// PEG4d TransCapturing

	public Expression newNew(AbstractTree<?> node) {
		AbstractTree<?> exprNode = node.get(_expr, null);
		Expression p = (exprNode == null) ? GrammarFactory.newEmpty(node) : newExpression(exprNode);
		return GrammarFactory.newNew(node, false, null, p);
	}

	public final static Tag _name = Tag.tag("name");
	public final static Tag _expr = Tag.tag("expr");
	public final static Tag _symbol = Tag.tag("symbol");

	private Tag parseLabelNode(AbstractTree<?> node) {
		Tag label = null;
		AbstractTree<?> labelNode = node.get(_name, null);
		if (labelNode != null) {
			label = Tag.tag(labelNode.toText());
		}
		return label;
	}

	public Expression newLeftFold(AbstractTree<?> node) {
		AbstractTree<?> exprNode = node.get(_expr, null);
		Expression p = (exprNode == null) ? GrammarFactory.newEmpty(node) : newExpression(exprNode);
		return GrammarFactory.newNew(node, true, parseLabelNode(node), p);
	}

	public Expression newLink(AbstractTree<?> node) {
		return GrammarFactory.newLink(node, parseLabelNode(node), newExpression(node.get(_expr)));
	}

	public Expression newTagging(AbstractTree<?> node) {
		return GrammarFactory.newTagging(node, Tag.tag(node.toText()));
	}

	public Expression newReplace(AbstractTree<?> node) {
		return GrammarFactory.newReplace(node, node.toText());
	}

	public Expression newMatch(AbstractTree<?> node) {
		AbstractTree<?> exprNode = node.get(_expr, null);
		if (exprNode != null) {
			return GrammarFactory.newMatch(node, newExpression(exprNode));
		}
		return GrammarFactory.newMatchSymbol(node, parseLabelNode(node));
	}

	public Expression newIf(AbstractTree<?> node) {
		return GrammarFactory.newIfFlag(node, node.getText(_name, ""));
	}

	public Expression newOn(AbstractTree<?> node) {
		return GrammarFactory.newOnFlag(node, true, node.getText(_name, ""), newExpression(node.get(_expr)));
	}

	public Expression newBlock(AbstractTree<?> node) {
		return GrammarFactory.newBlock(node, newExpression(node.get(_expr)));
	}

	public Expression newDef(AbstractTree<?> node) {
		return GrammarFactory.newDefSymbol(node, this.getGrammarFile(), Tag.tag(node.getText(_name, "")), newExpression(node.get(_expr)));
	}

	public Expression newIs(AbstractTree<?> node) {
		return GrammarFactory.newIsSymbol(node, this.getGrammarFile(), Tag.tag(node.getText(_name, "")));
	}

	public Expression newIsa(AbstractTree<?> node) {
		return GrammarFactory.newIsaSymbol(node, this.getGrammarFile(), Tag.tag(node.getText(_name, "")));
	}

	public Expression newExists(AbstractTree<?> node) {
		return GrammarFactory.newExists(node, this.getGrammarFile(), Tag.tag(node.getText(_name, "")), node.getText(_symbol, ""));
	}

	public Expression newLocal(AbstractTree<?> node) {
		return GrammarFactory.newLocal(node, this.getGrammarFile(), Tag.tag(node.getText(_name, "")), newExpression(node.get(_expr)));
	}

	public Expression newDefIndent(AbstractTree<?> node) {
		return GrammarFactory.newDefIndent(node);
	}

	public Expression newIndent(AbstractTree<?> node) {
		return GrammarFactory.newIndent(node);
	}

	public Expression newUndefined(AbstractTree<?> node) {
		this.reportError(node, "undefined or deprecated notation");
		return GrammarFactory.newEmpty(node);
	}

	public boolean parseExample(AbstractTree<?> node) {
		if (node.size() == 2) {
			Example ex = new Example(node.get(0), node.get(1), true);
			this.getGrammarFile().addExample(ex);
		} else {
			Example ex = new Example(node.get(0), node.get(2), true);
			this.getGrammarFile().addExample(ex);
			ex = new Example(node.get(1), node.get(2), true);
			this.getGrammarFile().addExample(ex);
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
			GrammarFile source = GrammarFile.loadNezFile(urn, NezOption.newDefaultOption());
			if (name.equals("*")) {
				int c = 0;
				for (String n : source.getNonterminalList()) {
					Production p = source.getProduction(n);
					if (p.isPublic()) {
						checkDuplicatedName(node.get(0));
						this.getGrammarFile().inportProduction(ns, p);
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
				this.getGrammarFile().inportProduction(ns, p);
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