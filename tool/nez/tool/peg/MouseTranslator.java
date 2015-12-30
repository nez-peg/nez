package nez.tool.peg;

import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Nez;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pempty;
import nez.lang.expr.Pfail;
import nez.lang.expr.Treplace;

public class MouseTranslator extends PEGTranslator {

	@Override
	public void makeHeader(Grammar gg) {
		file.write("// Parsing Expression Grammars for Mouse");
		file.writeIndent("// Translated from Nez");
	}

	@Override
	protected String name(String s) {
		if (s.equals("_")) {
			return "SPACING";
		}
		return s;
	}

	@Override
	public void visitProduction(Grammar gg, Production rule) {
		Expression e = rule.getExpression();
		file.writeIndent(name(rule.getLocalName().replaceAll("_", "under")));
		file.incIndent();
		file.writeIndent("= ");
		if (e instanceof Nez.Choice) {
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

	@Override
	public void visitNonTerminal(NonTerminal e) {
		file.write(name(e.getLocalName().replaceAll("_", "under")));
	}

	@Override
	public void visitByte(Nez.Byte e) {
		file.write(stringfy("\"", e.byteChar, "\""));
	}

	@Override
	public void visitByteSet(Nez.ByteSet e) {
		file.write(stringfy(e.byteMap));
	}

	@Override
	public void visitAny(Nez.Any e) {
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

	protected void visit(String prefix, Nez.Unary e, String suffix) {
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

	@Override
	public void visitOption(Nez.Option e) {
		this.visit(null, e, "?");
	}

	@Override
	public void visitZeroMore(Nez.ZeroMore e) {
		this.visit(null, e, "*");
	}

	@Override
	public void visitOneMore(Nez.OneMore e) {
		this.visit(null, e, "+");
	}

	@Override
	public void visitAnd(Nez.And e) {
		this.visit("&", e, null);
	}

	@Override
	public void visitNot(Nez.Not e) {
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

	@Override
	public void visitChoice(Nez.Choice e) {
		for (int i = 0; i < e.size(); i++) {
			if (i > 0) {
				file.write(" / ");
			}
			visitExpression(e.get(i));
		}
	}

	@Override
	public void visitPreNew(Nez.PreNew e) {

	}

	@Override
	public void visitNew(Nez.New e) {

	}

	@Override
	public void visitTag(Nez.Tag e) {
		// file.write("{");
		// file.write(e.tag.toString().toLowerCase());
		// file.write("}");
	}

	public void visitValue(Treplace e) {
		// file.write(StringUtils.quoteString('`', e.value, '`'));
	}

	@Override
	public void visitLink(Nez.Link e) {
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
