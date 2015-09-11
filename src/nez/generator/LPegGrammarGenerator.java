package nez.generator;

import nez.Parser;
import nez.NezOption;
import nez.lang.Expression;
import nez.lang.Production;
import nez.lang.expr.Pand;
import nez.lang.expr.Cany;
import nez.lang.expr.Xblock;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cset;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Xdefindent;
import nez.lang.expr.Xdef;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Tlink;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Tnew;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pnot;
import nez.lang.expr.Poption;
import nez.lang.expr.Pzero;
import nez.lang.expr.Pone;
import nez.lang.expr.Treplace;
import nez.lang.expr.Psequence;
import nez.lang.expr.Ttag;
import nez.lang.expr.Unary;
import nez.util.StringUtils;

public class LPegGrammarGenerator extends NezGenerator {

	@Override
	public String getDesc() {
		return "Lua script (required LPEG)";
	}

	@Override
	public void generate(Parser grammar, NezOption option, String fileName) {
		this.setOption(option);
		this.setOutputFile(fileName);
		file.writeIndent("local lpeg = require \"lpeg\"");
		for (Production r : grammar.getProductionList()) {
			if (!r.getLocalName().startsWith("\"")) {
				String localName = r.getLocalName();
				file.writeIndent("local " + localName + " = lpeg.V\"" + localName + "\"");
			}
		}
		file.writeIndent("G = lpeg.P{ File,");
		file.incIndent();
		for (Production r : grammar.getProductionList()) {
			if (!r.getLocalName().startsWith("\"")) {
				visitProduction(r);
			}
		}
		file.decIndent();
		file.writeIndent("}");
		file.writeIndent();
		makeFooter(grammar);
		file.writeNewLine();
		file.flush();
	}

	@Override
	public void makeHeader(Parser g) {
	}

	@Override
	public void visitProduction(Production rule) {
		Expression e = rule.getExpression();
		file.incIndent();
		file.writeIndent(rule.getLocalName() + " = ");
		if (e instanceof Pchoice) {
			for (int i = 0; i < e.size(); i++) {
				if (i > 0) {
					file.write(" + ");
				}
				visitExpression(e.get(i));
			}
		} else {
			visitExpression(e);
		}
		file.write(";");
		file.decIndent();
	}

	@Override
	public void makeFooter(Parser g) {
		file.writeIndent("function evalExp (s)");
		file.incIndent();
		file.writeIndent("for i = 0, 5 do");
		file.incIndent();
		file.writeIndent("local t1 = os.clock()");
		file.writeIndent("local t = lpeg.match(G, s)");
		file.writeIndent("local e1 = os.clock() - t1");
		file.writeIndent("print(\"elapsedTime1 : \", e1)");
		file.writeIndent("if not t then error(\"syntax error\", 2) end");
		file.decIndent();
		file.writeIndent("end");
		file.decIndent();
		file.writeIndent("end");
		file.writeIndent();
		file.writeIndent("fileName = arg[1]");
		file.writeIndent("fh, msg = io.open(fileName, \"r\")");
		file.writeIndent("if fh then");
		file.incIndent();
		file.writeIndent("data = fh:read(\"*a\")");
		file.decIndent();
		file.writeIndent("else");
		file.incIndent();
		file.writeIndent("print(msg)");
		file.decIndent();
		file.writeIndent("end");
		file.writeIndent("evalExp(data)");
	}

	public void visitEmpty(Expression e) {
		file.write("lpeg.P\"\"");
	}

	public void visitFailure(Expression e) {
		file.write("- lpeg.P(1) ");
	}

	public void visitNonTerminal(NonTerminal e) {
		file.write(e.getLocalName() + " ");
	}

	public String stringfyByte(int byteChar) {
		char c = (char) byteChar;
		switch (c) {
		case '\n':
			return ("'\\n'");
		case '\t':
			return ("'\\t'");
		case '\r':
			return ("'\\r'");
		case '\"':
			return ("\"\\\"\"");
		case '\\':
			return ("'\\\\'");
		}
		return "\"" + c + "\"";
	}

	public void visitByteChar(Cbyte e) {
		file.write("lpeg.P" + this.stringfyByte(e.byteChar) + " ");
	}

	private int searchEndChar(boolean[] b, int s) {
		for (; s < 256; s++) {
			if (!b[s]) {
				return s - 1;
			}
		}
		return 255;
	}

	private void getRangeChar(int ch, StringBuilder sb) {
		char c = (char) ch;
		switch (c) {
		case '\n':
			sb.append("\\n");
		case '\t':
			sb.append("'\\t'");
		case '\r':
			sb.append("'\\r'");
		case '\'':
			sb.append("'\\''");
		case '\\':
			sb.append("'\\\\'");
		}
		sb.append(c);
	}

	public void visitByteMap(Cset e) {
		boolean b[] = e.byteMap;
		for (int start = 0; start < 256; start++) {
			if (b[start]) {
				int end = searchEndChar(b, start + 1);
				if (start == end) {
					file.write("lpeg.P" + this.stringfyByte(start) + " ");
				} else {
					StringBuilder sb = new StringBuilder();
					getRangeChar(start, sb);
					getRangeChar(end, sb);
					file.write("lpeg.R(\"" + sb.toString() + "\") ");
					start = end;
				}
			}
		}
	}

	public void visitAnyChar(Cany e) {
		file.write("lpeg.P(1)");
	}

	@Override
	public void visitCharMultiByte(Cmulti p) {
		// TODO Auto-generated method stub

	}

	protected void visit(String prefix, Unary e, String suffix) {
		if (prefix != null) {
			file.write(prefix);
		}
		if (e.get(0) instanceof NonTerminal/* || e.get(0) instanceof NewClosure */) {
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

	public void visitOption(Poption e) {
		this.visit(null, e, "^-1");
	}

	public void visitRepetition(Pzero e) {
		this.visit(null, e, "^0");
	}

	public void visitRepetition1(Pone e) {
		this.visit(null, e, "^1");
	}

	public void visitAnd(Pand e) {
		this.visit("#", e, null);
	}

	public void visitNot(Pnot e) {
		this.visit("-", e, null);
	}

	public void visitTagging(Ttag e) {
		file.write("lpeg.P\"\" --[[");
		file.write(e.tag.toString());
		file.write("]]");
	}

	public void visitValue(Treplace e) {
		file.write("lpeg.P\"\"");
	}

	public void visitLink(Tlink e) {
		// String predicate = "@";
		// if(e.index != -1) {
		// predicate += "[" + e.index + "]";
		// }
		// this.visit(predicate, e, null);
		this.visitExpression(e.get(0));
	}

	private int appendAsString(Psequence l, int start) {
		int end = l.size();
		String s = "";
		for (int i = start; i < end; i++) {
			Expression e = l.get(i);
			if (e instanceof Cbyte) {
				char c = (char) (((Cbyte) e).byteChar);
				if (c >= ' ' && c < 127) {
					s += c;
					continue;
				}
			}
			end = i;
			break;
		}
		if (s.length() > 1) {
			file.write("lpeg.P" + StringUtils.quoteString('"', s, '"'));
		}
		return end - 1;
	}

	public void visitSequence(Psequence l) {
		for (int i = 0; i < l.size(); i++) {
			if (i > 0) {
				file.write(" ");
			}
			int n = appendAsString(l, i);
			if (n > i) {
				i = n;
				if (i < l.size() - 1) {
					file.write(" * ");
				}
				continue;
			}
			Expression e = l.get(i);
			if (e instanceof Pchoice || e instanceof Psequence) {
				file.write("( ");
				visitExpression(e);
				file.write(" )");
			} else {
				visitExpression(e);
			}
			if (i < l.size() - 1) {
				file.write(" * ");
			}
		}
	}

	public void visitChoice(Pchoice e) {
		for (int i = 0; i < e.size(); i++) {
			if (i > 0) {
				file.write(" + ");
			}
			file.write(" ( ");
			visitExpression(e.get(i));
			file.write(" ) ");
		}
	}

	// public void visitNewClosure(NewClosure e) {
	// file.write("( ");
	// this.visitSequenceImpl(e);
	// file.write(" )");
	// }
	//
	// public void visitLeftNew(LeftNewClosure e) {
	// file.write("( ");
	// this.visitSequenceImpl(e);
	// file.write(" )");
	// }

	public void visitNew(Tnew e) {

	}

	public void visitCapture(Tcapture e) {

	}

	@Override
	public void visitUndefined(Expression e) {
		file.write("lpeg.P\"\" --[[ LPeg Unsupported <");
		file.write(e.getPredicate());
		for (Expression se : e) {
			file.write(" ");
			visitExpression(se);
		}
		file.write("> ]]");
	}

	@Override
	public void visitExpression(Expression e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitReplace(Treplace p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitBlock(Xblock p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitMatchSymbol(Xmatch p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitDefSymbol(Xdef p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitIsSymbol(Xis p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitDefIndent(Xdefindent p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitIsIndent(Xindent p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitExistsSymbol(Xexists p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitLocalTable(Xlocal p) {
		// TODO Auto-generated method stub

	}
}
