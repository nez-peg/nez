package nez.lang.util;

import java.util.ArrayList;

import nez.ast.Symbol;
import nez.ast.Tree;
import nez.ast.TreeVisitor;
import nez.util.FileBuilder;

public class NezFileFormatter extends TreeVisitor {
	private FileBuilder f;

	public NezFileFormatter() {
		f = new FileBuilder(null);
	}

	boolean isBeforeComment = true;

	void writeIndent(String s) {
		if (s.startsWith("/*") || s.startsWith("//")) {
			if (!isBeforeComment) {
				f.writeNewLine();
			}
			isBeforeComment = true;
		} else {
			isBeforeComment = false;
		}
		f.writeIndent(s);
	}

	void write(String s) {
		f.write(s);
	}

	public void writeMultiLine(long prev, String sub) {
		int start = 0;
		boolean empty = true;
		for (int i = 0; i < sub.length(); i++) {
			char ch = sub.charAt(i);
			if (ch == ' ' || ch == '\t') {
				continue;
			}
			if (ch == '\n') {
				if (!empty) {
					if (prev == 0) {
						write(sub.substring(start, i));
						prev = 1;
					} else {
						writeIndent(sub.substring(start, i));
					}
				}
				start = i + 1;
				empty = true;
				continue;
			}
			empty = false;
		}
	}

	public void parse(Tree<?> node) {
		visit("p", node);
	}

	public final static Symbol _name = Symbol.tag("name");
	public final static Symbol _expr = Symbol.tag("expr");
	public final static Symbol _symbol = Symbol.tag("symbol");

	public final static Symbol _Production = Symbol.tag("Production");
	public final static Symbol _Example = Symbol.tag("Example");
	public final static Symbol _Format = Symbol.tag("Format");

	public boolean pSource(Tree<?> node) {
		ArrayList<Tree<?>> l = new ArrayList<>(node.size() * 2);
		for (Tree<?> subnode : node) {
			analyze(subnode, l);
		}

		long prev = 0;
		boolean hasFormat = false;
		boolean hasExample = false;
		for (Tree<?> subnode : l) {
			prev = checkComment(prev, subnode);
			if (subnode.is(_Format)) {
				hasFormat = true;
			}
			if (subnode.is(_Example)) {
				hasExample = true;
			}
			if (subnode.is(_Production)) {
				parse(subnode);
			}
		}
		if (hasExample) {
			writeIndent("");
			for (Tree<?> subnode : l) {
				if (subnode.is(_Example)) {
					parse(subnode);
				}
			}
		}
		if (hasFormat) {
			writeIndent("");
			for (Tree<?> subnode : l) {
				if (subnode.is(_Format)) {
					parse(subnode);
				}
			}
		}
		f.writeNewLine();
		f.writeIndent("// formatted by $ nez format");
		f.writeNewLine();
		return true;
	}

	private int prodLength = 8;

	private void analyze(Tree<?> node, ArrayList<Tree<?>> l) {
		l.add(node);
		if (node.is(_Production)) {
			Tree<?> name = node.get(_name);
			int len = name.toText().length() + 2;
			if (!name.is(_NonTerminal)) {
				len += 2;
			}
			if (len > prodLength) {
				prodLength = len;
			}
		}
	}

	private long checkComment(long prev, Tree<?> node) {
		long start = node.getSourcePosition();
		if (prev < start) {
			String sub = node.getSource().subString(prev, start);
			writeMultiLine(prev, sub);
		}
		return start + node.getLength();
	}

	public final static Symbol _NonTerminal = Symbol.tag("NonTerminal");

	public final static Symbol _Choice = Symbol.tag("Choice");
	public final static Symbol _Sequence = Symbol.tag("Sequence");
	public final static Symbol _List = Symbol.tag("List");
	public final static Symbol _Class = Symbol.tag("Class");

	//
	// public final static Tag _anno = Tag.tag("anno");

	public boolean pProduction(Tree<?> node) {
		Tree<?> nameNode = node.get(_name);
		Tree<?> exprNode = node.get(_expr);
		String name = nameNode.is(_NonTerminal) ? nameNode.toText() : "\"" + nameNode.toText() + "\"";
		String format = "%-" + this.prodLength + "s";
		writeIndent(String.format(format, name));
		String delim = "= ";
		if (exprNode.is(_Choice)) {
			for (Tree<?> sub : exprNode) {
				if (!delim.startsWith("=")) {
					writeIndent(String.format(format, ""));
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

	public boolean pExpression(Tree<?> node) {
		return (Boolean) visit("p", node);
	}

	public boolean pNonTerminal(Tree<?> node) {
		f.write(node.toText());
		return true;
	}

	public boolean pString(Tree<?> node) {
		f.write("\"");
		f.write(node.toText());
		f.write("\"");
		return true;
	}

	public boolean pCharacter(Tree<?> node) {
		f.write("'");
		f.write(node.toText());
		f.write("'");
		return true;
	}

	public boolean pClass(Tree<?> node) {
		f.write("[");
		if (node.size() > 0) {
			for (int i = 0; i < node.size(); i++) {
				Tree<?> o = node.get(i);
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

	public boolean pByteChar(Tree<?> node) {
		String t = node.toText();
		f.write(t);
		return true;
	}

	public boolean pAnyChar(Tree<?> node) {
		f.write(".");
		return true;
	}

	public boolean pChoice(Tree<?> node) {
		boolean spacing = false;
		for (int i = 0; i < node.size(); i++) {
			if (spacing) {
				f.write(" / ");
			}
			spacing = pExpression(node.get(i));
		}
		return spacing;
	}

	public boolean pSequence(Tree<?> node) {
		boolean spacing = false;
		for (int i = 0; i < node.size(); i++) {
			if (spacing) {
				f.write(" ");
			}
			Tree<?> sub = node.get(i);
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

	private boolean needsParenthesis(Tree<?> node) {
		return node.is(_Choice) || node.is(_Sequence);
	}

	private boolean pUnary(String prefix, Tree<?> node, String suffix) {
		if (prefix != null) {
			f.write(prefix);
		}
		Tree<?> exprNode = node.get(_expr);
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

	public boolean pNot(Tree<?> node) {
		return pUnary("!", node, null);
	}

	public boolean pAnd(Tree<?> node) {
		return pUnary("&", node, null);
	}

	public boolean pMatch(Tree<?> node) {
		return pUnary("~", node, null);
	}

	public boolean pOption(Tree<?> node) {
		return pUnary(null, node, "?");
	}

	public boolean pRepetition1(Tree<?> node) {
		return pUnary(null, node, "+");
	}

	public boolean pRepetition(Tree<?> node) {
		return pUnary(null, node, "*");
	}

	// PEG4d TransCapturing

	public boolean pNew(Tree<?> node) {
		Tree<?> exprNode = node.get(_expr, null);
		f.write("{ ");
		pExpression(exprNode);
		f.write(" }");
		return true;
	}

	private Symbol parseLabelNode(Tree<?> node) {
		Symbol label = null;
		Tree<?> labelNode = node.get(_name, null);
		if (labelNode != null) {
			label = Symbol.tag(labelNode.toText());
		}
		return label;
	}

	public boolean pLeftFold(Tree<?> node) {
		Symbol tag = parseLabelNode(node);
		Tree<?> exprNode = node.get(_expr, null);
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

	public boolean pLink(Tree<?> node) {
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

	public boolean pTagging(Tree<?> node) {
		f.write("#");
		f.write(node.toText());
		return true;
	}

	public boolean pReplace(Tree<?> node) {
		f.write("`");
		f.write(node.toText());
		f.write("`");
		return true;
	}

	public boolean pIf(Tree<?> node) {
		f.write("<if " + node.getText(_name, "") + ">");
		return true;
	}

	public boolean pOn(Tree<?> node) {
		String p = "<on " + node.getText(_name, "") + " ";
		return pUnary(p, node, ">");
	}

	public boolean pBlock(Tree<?> node) {
		return pUnary("<block ", node, ">");
	}

	public boolean pDef(Tree<?> node) {
		String p = "<def " + node.getText(_name, "") + " ";
		return pUnary(p, node, ">");
	}

	public boolean pIs(Tree<?> node) {
		f.write("<is " + node.getText(_name, "") + ">");
		return true;
	}

	public boolean pIsa(Tree<?> node) {
		f.write("<isa " + node.getText(_name, "") + ">");
		return true;
	}

	public boolean pExists(Tree<?> node) {
		String symbol = node.getText(_symbol, null);
		if (symbol == null) {
			f.write("<exists " + node.getText(_name, "") + ">");
		} else {
			f.write("<exists " + node.getText(_name, "") + " " + symbol + ">");
		}
		return true;
	}

	public boolean pLocal(Tree<?> node) {
		String p = "<local " + node.getText(_name, "") + " ";
		return pUnary(p, node, ">");
	}

	public boolean pDefIndent(Tree<?> node) {
		f.write("<match indent>");
		return true;
	}

	public boolean pIndent(Tree<?> node) {
		f.write("<match indent>");
		return true;
	}

	public boolean pUndefined(Tree<?> node) {
		throw new RuntimeException("undefined node");
	}

	public final static Symbol _hash = Symbol.tag("hash"); // example
	public final static Symbol _name2 = Symbol.tag("name2"); // example
	public final static Symbol _text = Symbol.tag("text"); // example

	public boolean pExample(Tree<?> node) {
		Tree<?> nameNode = node.get(_name);
		Tree<?> name2Node = node.get(_name2, null);
		String hash = node.getText(_hash, null);
		Tree<?> textNode = node.get(_text);

		writeIndent("example " + nameNode.toText());
		if (name2Node != null) {
			f.write("&" + name2Node.toText());
		}
		if (hash != null) {
			f.write(" ~" + hash);
		}
		String s = "'''";
		f.write(" " + s);
		writeIndent(textNode.toText());
		writeIndent(s);
		return true;
	}

	public final static Symbol _size = Symbol.tag("hash"); // format
	public final static Symbol _format = Symbol.tag("format"); // format

	public boolean pFormat(Tree<?> node) {
		// System.out.println("node:" + node);
		writeIndent("format #" + node.getText(_name, ""));
		f.write("[" + node.getText(_size, "*") + "] ");
		Tree<?> formatNode = node.get(_format);
		f.write("`" + formatNode.toText() + "`");
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
	public boolean pImport(Tree<?> node) {
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
