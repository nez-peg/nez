package nez.lang;

import nez.Parser;
import nez.ast.AbstractTree;
import nez.ast.SymbolId;
import nez.util.FileBuilder;

public class NezFileFormatter extends NezGrammarLoader {
	private FileBuilder f;

	public NezFileFormatter() {
		super(NezGrammar2.newGrammarFile());
		f = new FileBuilder(null);
	}

	@Override
	public void parse(AbstractTree<?> node) {
		visit("p", node);
	}

	public final static SymbolId _name = SymbolId.tag("name");
	public final static SymbolId _expr = SymbolId.tag("expr");
	public final static SymbolId _symbol = SymbolId.tag("symbol");

	public boolean pSource(AbstractTree<?> node) {
		for (AbstractTree<?> subnode : node) {
			parse(subnode);
		}
		return true;
	}

	// public final static Tag _String = Tag.tag("String");
	// public final static Tag _Integer = Tag.tag("Integer");
	public final static SymbolId _List = SymbolId.tag("List");

	// public final static Tag _Name = Tag.tag("Name");
	// public final static Tag _Format = Tag.tag("Format");
	public final static SymbolId _Class = SymbolId.tag("Class");

	//
	// public final static Tag _anno = Tag.tag("anno");

	public boolean pProduction(AbstractTree<?> node) {
		AbstractTree<?> nameNode = node.get(_name);
		AbstractTree<?> exprNode = node.get(_expr);
		f.writeIndent();
		pExpression(nameNode);
		String delim = "= ";
		f.incIndent();
		f.writeIndent(delim);
		pExpression(exprNode);
		f.decIndent();
		return true;
	}

	public boolean pExpression(AbstractTree<?> node) {
		return (Boolean) visit("p", node);
	}

	public boolean pNonTerminal(AbstractTree<?> node) {
		f.write(node.toText());
		return true;
	}

	public boolean pString(AbstractTree<?> node) {
		f.write("\"");
		f.write(node.toText());
		f.write("\"");
		return true;
	}

	public boolean pCharacter(AbstractTree<?> node) {
		f.write("'");
		f.write(node.toText());
		f.write("'");
		return true;
	}

	public boolean pClass(AbstractTree<?> node) {
		f.write("[");
		if (node.size() > 0) {
			for (int i = 0; i < node.size(); i++) {
				AbstractTree<?> o = node.get(i);
				if (o.is(_List)) { // range
					f.write(o.getText(0, ""));
					f.write("-");
					f.write(o.getText(1, ""));
				}
				if (o.is(_Class)) { // single
					f.write(o.toText());
				}
			}
		}
		f.write("]");
		return true;
	}

	public boolean pByte(AbstractTree<?> node) {
		String t = node.toText();
		f.write(t);
		return true;
	}

	public boolean pAnyChar(AbstractTree<?> node) {
		f.write(".");
		return true;
	}

	public boolean pChoice(AbstractTree<?> node) {
		boolean spacing = false;
		for (int i = 0; i < node.size(); i++) {
			if (spacing) {
				f.write(" / ");
			}
			spacing = pExpression(node.get(i));
		}
		return spacing;
	}

	public boolean pSequence(AbstractTree<?> node) {
		boolean spacing = false;
		for (int i = 0; i < node.size(); i++) {
			if (spacing) {
				f.write(" ");
			}
			spacing = pExpression(node.get(i));
		}
		return spacing;
	}

	private boolean needsParenthesis(AbstractTree<?> node) {
		return true;
	}

	private boolean pUnary(String prefix, AbstractTree<?> node, String suffix) {
		if (prefix != null) {
			f.write(prefix);
		}
		AbstractTree<?> exprNode = node.get(_expr);
		if (needsParenthesis(exprNode)) {
			f.write("(");
			pExpression(exprNode);
			f.write(")");
		} else {
			pExpression(exprNode);
		}
		if (suffix != null) {
			f.write(suffix);
		}
		return true;
	}

	public boolean pNot(AbstractTree<?> node) {
		return pUnary("!", node, null);
	}

	public boolean pAnd(AbstractTree<?> node) {
		return pUnary("&", node, null);
	}

	public boolean pMatch(AbstractTree<?> node) {
		return pUnary("~", node, null);
	}

	public boolean pOption(AbstractTree<?> node) {
		return pUnary("?", node, null);
	}

	public boolean pRepetition1(AbstractTree<?> node) {
		return pUnary("+", node, null);
	}

	public boolean pRepetition(AbstractTree<?> node) {
		return pUnary("*", node, null);
	}

	// PEG4d TransCapturing

	public boolean pNew(AbstractTree<?> node) {
		AbstractTree<?> exprNode = node.get(_expr, null);
		f.write("{ ");
		pExpression(exprNode);
		f.write(" }");
		return true;
	}

	private SymbolId parseLabelNode(AbstractTree<?> node) {
		SymbolId label = null;
		AbstractTree<?> labelNode = node.get(_name, null);
		if (labelNode != null) {
			label = SymbolId.tag(labelNode.toText());
		}
		return label;
	}

	public boolean pLeftFold(AbstractTree<?> node) {
		SymbolId tag = parseLabelNode(node);
		AbstractTree<?> exprNode = node.get(_expr, null);
		String label = tag == null ? "," : "," + tag.toString();
		if (exprNode != null) {
			return pUnary("{" + label + " ", node, " }");
		} else {
			f.write("{" + label + " }");
		}
		return true;
	}

	public boolean pLink(AbstractTree<?> node) {
		SymbolId tag = parseLabelNode(node);
		f.write(":");
		if (tag != null) {
			f.write(tag.toString());
		}
		return pUnary("(", node, ")");
	}

	public boolean pTagging(AbstractTree<?> node) {
		f.write("#");
		f.write(node.toText());
		return true;
	}

	public boolean pReplace(AbstractTree<?> node) {
		f.write("`");
		f.write(node.toText());
		f.write("`");
		return true;
	}

	public boolean pIf(AbstractTree<?> node) {
		f.write("<if " + node.getText(_name, "") + ">");
		return true;
	}

	public boolean pOn(AbstractTree<?> node) {
		String p = "<on " + node.getText(_name, "") + " ";
		return pUnary(p, node, ">");
	}

	public boolean pBlock(AbstractTree<?> node) {
		return pUnary("<block ", node, ">");
	}

	public boolean pDef(AbstractTree<?> node) {
		String p = "<on " + node.getText(_name, "") + " ";
		return pUnary(p, node, ">");
	}

	public boolean pIs(AbstractTree<?> node) {
		f.write("<is " + node.getText(_name, "") + ">");
		return true;
	}

	public boolean pIsa(AbstractTree<?> node) {
		f.write("<isa " + node.getText(_name, "") + ">");
		return true;
	}

	public boolean pExists(AbstractTree<?> node) {
		String symbol = node.getText(_symbol, null);
		if (symbol == null) {
			f.write("<exists " + node.getText(_name, "") + ">");
		} else {
			f.write("<exists " + node.getText(_name, "") + ">");
		}
		return true;
	}

	public boolean pLocal(AbstractTree<?> node) {
		String p = "<local " + node.getText(_name, "") + " ";
		return pUnary(p, node, ">");
	}

	public boolean pDefIndent(AbstractTree<?> node) {
		f.write("<match indent>");
		return true;
	}

	public boolean pIndent(AbstractTree<?> node) {
		return true;
	}

	public boolean pUndefined(AbstractTree<?> node) {
		return false;
	}

	public boolean pExample(AbstractTree<?> node) {
		// if (node.size() == 2) {
		// Example ex = new Example(node.get(0), node.get(1), true);
		// this.getGrammarFile().addExample(ex);
		// } else {
		// Example ex = new Example(node.get(0), node.get(2), true);
		// this.getGrammarFile().addExample(ex);
		// ex = new Example(node.get(1), node.get(2), true);
		// this.getGrammarFile().addExample(ex);
		// }
		return true;
	}

	public boolean pFormat(AbstractTree<?> node) {
		return true;
	}

	// @Override
	// Formatter toFormatter(AbstractTree<?> node) {
	// if (node.is(_List)) {
	// ArrayList<Formatter> l = new ArrayList<Formatter>(node.size());
	// for (AbstractTree<?> t : node) {
	// l.add(toFormatter(t));
	// }
	// return Formatter.newFormatter(l);
	// }
	// if (node.is(_Integer)) {
	// return Formatter.newFormatter(StringUtils.parseInt(node.toText(), 0));
	// }
	// if (node.is(_Format)) {
	// int s = StringUtils.parseInt(node.getText(0, "*"), -1);
	// int e = StringUtils.parseInt(node.getText(2, "*"), -1);
	// Formatter fmt = toFormatter(node.get(1));
	// return Formatter.newFormatter(s, fmt, e);
	// }
	// if (node.is(_Name)) {
	// Formatter fmt = Formatter.newAction(node.toText());
	// if (fmt == null) {
	// this.reportWarning(node, "undefined formatter action");
	// fmt = Formatter.newFormatter("${" + node.toText() + "}");
	// }
	// return fmt;
	// }
	// return Formatter.newFormatter(node.toText());
	// }

	/* import */
	public boolean pImport(AbstractTree<?> node) {
		// // System.out.println("DEBUG? parsed: " + node);
		// String ns = null;
		// String name = node.getText(0, "*");
		// int loc = name.indexOf('.');
		// if (loc >= 0) {
		// ns = name.substring(0, loc);
		// name = name.substring(loc + 1);
		// }
		// String urn = path(node.getSource().getResourceName(), node.getText(1,
		// ""));
		// try {
		// GrammarFile source = GrammarFile.loadNezFile(urn,
		// NezOption.newDefaultOption());
		// if (name.equals("*")) {
		// int c = 0;
		// for (String n : source.getNonterminalList()) {
		// Production p = source.getProduction(n);
		// if (p.isPublic()) {
		// checkDuplicatedName(node.get(0));
		// this.getGrammarFile().inportProduction(ns, p);
		// c++;
		// }
		// }
		// if (c == 0) {
		// this.reportError(node.get(0),
		// "nothing imported (no public production exisits)");
		// }
		// } else {
		// Production p = source.getProduction(name);
		// if (p == null) {
		// this.reportError(node.get(0), "undefined production: " + name);
		// return false;
		// }
		// this.getGrammarFile().inportProduction(ns, p);
		// }
		// return true;
		// } catch (IOException e) {
		// this.reportError(node.get(1), "unfound: " + urn);
		// } catch (NullPointerException e) {
		// /*
		// * This is for a bug unhandling IOException at
		// * java.io.Reader.<init>(Reader.java:78)
		// */
		// this.reportError(node.get(1), "unfound: " + urn);
		// }
		return true;
	}

	@Override
	public Parser getStartGrammar() {
		throw new RuntimeException("FIXME");
	}

}
