package nez.generator;

import nez.Parser;
import nez.lang.Expression;
import nez.lang.Production;
import nez.lang.expr.Uand;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cset;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pempty;
import nez.lang.expr.Pfail;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Unot;
import nez.lang.expr.Uoption;
import nez.lang.expr.Uzero;
import nez.lang.expr.Uone;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.lang.expr.Unary;

public class MouseGrammarGenerator extends GrammarGenerator {

	@Override
	public String getDesc() {
		return "a PEG-style grammar for Mouse";
	}

	@Override
	public void makeHeader(Parser g) {
		file.write("// Parsing Expression Grammars for Mouse");
		file.writeIndent("// Translated from Nez");
	}

	String stringfyName(String s) {
		if (s.equals("_")) {
			return "SPACING";
		}
		return s;
	}

	@Override
	public void visitProduction(Production rule) {
		Expression e = rule.getExpression();
		file.writeIndent(stringfyName(rule.getLocalName().replaceAll("_", "under")));
		file.incIndent();
		file.writeIndent("= ");
		if (e instanceof Pchoice) {
			for (int i = 0; i < e.size(); i++) {
				if (i > 0) {
					file.writeIndent("/ ");
				}
				visitExpression(e.get(i));
			}
		} else {
			visitExpression(e);
		}
		file.writeIndent(";");
		file.decIndent();
	}

	public void visitEmpty(Pempty e) {
		file.write("\"\"");
	}

	public void visitFailure(Pfail e) {
		file.write("!_");
	}

	public void visitNonTerminal(NonTerminal e) {
		file.write(stringfyName(e.getLocalName().replaceAll("_", "under")));
	}

	public void visitByteChar(Cbyte e) {
		file.write(stringfy("\"", e.byteChar, "\""));
	}

	public void visitByteMap(Cset e) {
		file.write(stringfy(e.byteMap));
	}

	public void visitAnyChar(Cany e) {
		file.write("_");
	}

	private final String stringfy(String s, int ch, String e) {
		char c = (char) ch;
		switch (c) {
		case '\n':
			return s + "\\n" + e;
		case '\t':
			return s + "\\t" + e;
		case '\r':
			return s + "\\r" + e;
		case '"':
			return s + "\\\"" + e;
		case '\\':
			return s + "\\\\" + e;
		}
		if (Character.isISOControl(c) || c > 127) {
			return s + String.format("0x%02x", (int) c) + e;
		}
		return (s + c + e);
	}

	private final String stringfy(boolean[] b) {
		StringBuilder sb = new StringBuilder();
		String delim = "";
		for (int s = 0; s < 256; s++) {
			if (b[s]) {
				int e = searchEndChar(b, s + 1);
				if (s == e) {
					sb.append(delim);
					sb.append(stringfy("\"", s, "\""));
					delim = " / ";
				} else {
					sb.append(delim);
					sb.append("[");
					sb.append(stringfy("", s, ""));
					sb.append("-");
					sb.append(stringfy("", e, ""));
					sb.append("]");
					delim = " / ";
					s = e;
				}
			}
		}
		return sb.toString();
	}

	private final static int searchEndChar(boolean[] b, int s) {
		for (; s < 256; s++) {
			if (!b[s]) {
				return s - 1;
			}
		}
		return 255;
	}

	protected void visit(String prefix, Unary e, String suffix) {
		if (prefix != null) {
			file.write(prefix);
		}
		if (/* e.get(0) instanceof String || */e.get(0) instanceof NonTerminal/*
																			 * ||
																			 * e
																			 * .
																			 * get
																			 * (
																			 * 0
																			 * )
																			 * instanceof
																			 * NewClosure
																			 */) {
			this.visitExpression(e.get(0));
		} else {
			file.write("(");
			this.visitExpression(e.get(0));
			file.write(")");
		}
		if (suffix != null) {
			file.write(suffix);
		}
	}

	public void visitOption(Uoption e) {
		this.visit(null, e, "?");
	}

	public void visitRepetition(Uzero e) {
		this.visit(null, e, "*");
	}

	public void visitRepetition1(Uone e) {
		this.visit(null, e, "+");
	}

	public void visitAnd(Uand e) {
		this.visit("&", e, null);
	}

	public void visitNot(Unot e) {
		this.visit("!", e, null);
	}

	// protected void visitSequenceImpl(Multinary l) {
	// for(int i = 0; i < l.size(); i++) {
	// if(i > 0) {
	// file.write(" ");
	// }
	// int n = appendAsString(l, i);
	// if(n > i) {
	// i = n;
	// continue;
	// }
	// Expression e = l.get(i);
	// if(e instanceof Choice || e instanceof Sequence) {
	// file.write("( ");
	// visitExpression(e);
	// file.write(" )");
	// continue;
	// }
	// visitExpression(e);
	// }
	// }
	//
	// private int appendAsString(Multinary l, int start) {
	// int end = l.size();
	// String s = "";
	// for(int i = start; i < end; i++) {
	// Expression e = l.get(i);
	// if(e instanceof ByteChar) {
	// char c = (char)(((ByteChar) e).byteChar);
	// if(c >= ' ' && c < 127) {
	// s += c;
	// continue;
	// }
	// }
	// end = i;
	// break;
	// }
	// if(s.length() > 1) {
	// file.write(StringUtils.quoteString('"', s, '"'));
	// }
	// return end - 1;
	// }

	public void visitChoice(Pchoice e) {
		for (int i = 0; i < e.size(); i++) {
			if (i > 0) {
				file.write(" / ");
			}
			visitExpression(e.get(i));
		}
	}

	public void visitNew(Tnew e) {

	}

	public void visitCapture(Tcapture e) {

	}

	public void visitTagging(Ttag e) {
		// file.write("{");
		// file.write(e.tag.toString().toLowerCase());
		// file.write("}");
	}

	public void visitValue(Treplace e) {
		// file.write(StringUtils.quoteString('`', e.value, '`'));
	}

	public void visitLink(Tlink e) {
		// String predicate = "@";
		// if(e.index != -1) {
		// predicate += "[" + e.index + "]";
		// }
		// this.visit(predicate, e, null);
		this.visitExpression(e.get(0));
	}

	@Override
	public void visitUndefined(Expression e) {
		file.write("/* Mouse Unsupported <");
		file.write(e.getPredicate());
		for (Expression se : e) {
			file.write(" ");
			visitExpression(se);
		}
		file.write("> */");
	}

}
