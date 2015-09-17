package nez.lang.util;

import java.util.ArrayList;

import nez.ast.AbstractTree;
import nez.ast.AbstractTreeVisitor;
import nez.ast.Symbol;
import nez.util.FileBuilder;

public class NezFileFormatter extends AbstractTreeVisitor {
	private FileBuilder f;

	public NezFileFormatter() {
		f = new FileBuilder(null);
	}

	public void parse(AbstractTree<?> node) {
		visit("p", node);
	}

	public final static Symbol _name = Symbol.tag("name");
	public final static Symbol _expr = Symbol.tag("expr");
	public final static Symbol _symbol = Symbol.tag("symbol");

	public boolean pSource(AbstractTree<?> node) {
		ArrayList<AbstractTree<?>> l = new ArrayList<>(node.size() * 2);
		for (AbstractTree<?> subnode : node) {
			analyze(subnode, l);
		}

		long prev = 0;
		for (AbstractTree<?> subnode : l) {
			prev = checkComment(prev, subnode);
			parse(subnode);
		}
		f.writeNewLine();
		return true;
	}

	private int prodLength = 8;

	private void analyze(AbstractTree<?> node, ArrayList<AbstractTree<?>> l) {
		l.add(node);
		if (node.is(_Production)) {
			AbstractTree<?> name = node.get(_name);
			int len = name.toText().length() + 2;
			if (!name.is(_NonTerminal)) {
				len += 2;
			}
			if (len > prodLength) {
				prodLength = len;
			}
		}
	}

	private long checkComment(long prev, AbstractTree<?> node) {
		long start = node.getSourcePosition();
		if (prev < start) {
			String sub = node.getSource().substring(prev, start);
			f.writeMultiLine(sub);
		}
		return start + node.getLength();
	}

	public final static Symbol _Production = Symbol.tag("Production");
	public final static Symbol _NonTerminal = Symbol.tag("NonTerminal");

	public final static Symbol _Choice = Symbol.tag("Choice");
	public final static Symbol _Sequence = Symbol.tag("Sequence");
	public final static Symbol _List = Symbol.tag("List");
	public final static Symbol _Class = Symbol.tag("Class");

	//
	// public final static Tag _anno = Tag.tag("anno");

	public boolean pProduction(AbstractTree<?> node) {
		AbstractTree<?> nameNode = node.get(_name);
		AbstractTree<?> exprNode = node.get(_expr);
		String name = nameNode.is(_NonTerminal) ? nameNode.toText() : "\"" + nameNode.toText() + "\"";
		String format = "%-" + this.prodLength + "s";
		f.writeIndent(String.format(format, name));
		String delim = "= ";
		if (exprNode.is(_Choice)) {
			for (AbstractTree<?> sub : exprNode) {
				if (!delim.startsWith("=")) {
					f.writeIndent(String.format(format, ""));
				}
				f.write(delim);
				pExpression(sub);
				delim = "/ ";
			}
		} else {
			f.write(delim);
			pExpression(exprNode);
		}
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
			AbstractTree<?> sub = node.get(i);
			if (sub.is(_Choice)) {
				f.write("(");
			}
			spacing = pExpression(sub);
			if (sub.is(_Choice)) {
				f.write(")");
			}
		}
		return spacing;
	}

	private boolean needsParenthesis(AbstractTree<?> node) {
		return node.is(_Choice) || node.is(_Sequence);
	}

	private boolean pUnary(String prefix, AbstractTree<?> node, String suffix) {
		if (prefix != null) {
			f.write(prefix);
		}
		AbstractTree<?> exprNode = node.get(_expr);
		if (needsParenthesis(exprNode)) {
			f.write("( ");
			pExpression(exprNode);
			f.write(" )");
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
		return pUnary(null, node, "?");
	}

	public boolean pRepetition1(AbstractTree<?> node) {
		return pUnary(null, node, "+");
	}

	public boolean pRepetition(AbstractTree<?> node) {
		return pUnary(null, node, "*");
	}

	// PEG4d TransCapturing

	public boolean pNew(AbstractTree<?> node) {
		AbstractTree<?> exprNode = node.get(_expr, null);
		f.write("{ ");
		pExpression(exprNode);
		f.write(" }");
		return true;
	}

	private Symbol parseLabelNode(AbstractTree<?> node) {
		Symbol label = null;
		AbstractTree<?> labelNode = node.get(_name, null);
		if (labelNode != null) {
			label = Symbol.tag(labelNode.toText());
		}
		return label;
	}

	public boolean pLeftFold(AbstractTree<?> node) {
		Symbol tag = parseLabelNode(node);
		AbstractTree<?> exprNode = node.get(_expr, null);
		String label = tag == null ? "$" : "$" + tag.toString();
		if (exprNode != null) {
			f.write("{" + label + " ");
			pExpression(exprNode);
			f.write(" }");
		} else {
			f.write("{" + label + " }");
		}
		return true;
	}

	public boolean pLink(AbstractTree<?> node) {
		Symbol tag = parseLabelNode(node);
		f.write("$");
		if (tag != null) {
			f.write(tag.toString());
		}
		f.write("(");
		pExpression(node.get(_expr));
		f.write(")");
		return true;
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
			f.write("<exists " + node.getText(_name, "") + " " + symbol + ">");
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

	public final static Symbol _hash = Symbol.tag("hash"); // example
	public final static Symbol _name2 = Symbol.tag("name2"); // example
	public final static Symbol _text = Symbol.tag("text"); // example

	public boolean pExample(AbstractTree<?> node) {
		AbstractTree<?> nameNode = node.get(_name);
		AbstractTree<?> name2Node = node.get(_name2, null);
		String hash = node.getText(_hash, null);
		AbstractTree<?> textNode = node.get(_text);

		f.writeIndent("example " + nameNode.toText());
		if (name2Node != null) {
			f.write("&" + name2Node.toText());
		}
		if (hash != null) {
			f.write(" ~" + hash);
		}
		String s = "'''";
		f.write(" " + s);
		f.writeIndent(textNode.toText());
		f.writeIndent(s);
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

}
