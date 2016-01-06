package nez.tool.peg;

import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Nez;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.util.StringUtils;

public class LPegTranslator extends GrammarTranslator {

	@Override
	protected String getFileExtension() {
		return "lua";
	}

	@Override
	public void makeBody(Grammar gg) {
		file.writeIndent("local lpeg = require \"lpeg\"");
		for (Production r : gg) {
			if (!r.getLocalName().startsWith("\"")) {
				String localName = r.getLocalName();
				file.writeIndent("local " + localName + " = lpeg.V\"" + localName + "\"");
			}
		}
		file.writeIndent("G = lpeg.P{ File,");
		file.incIndent();
		for (Production r : gg) {
			if (!r.getLocalName().startsWith("\"")) {
				visitProduction(gg, r);
			}
		}
		file.decIndent();
		file.writeIndent("}");
		file.writeIndent();
	}

	@Override
	public void makeHeader(Grammar gg) {
	}

	@Override
	public void visitProduction(Grammar gg, Production rule) {
		Expression e = rule.getExpression();
		file.incIndent();
		file.writeIndent(rule.getLocalName() + " = ");
		if (e instanceof Nez.Choice) {
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
	public void makeFooter(Grammar gg) {
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

	@Override
	public void visitEmpty(Expression e) {
		file.write("lpeg.P\"\"");
	}

	@Override
	public void visitFail(Expression e) {
		file.write("- lpeg.P(1) ");
	}

	@Override
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

	@Override
	public void visitByte(Nez.Byte e) {
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

	@Override
	public void visitByteSet(Nez.ByteSet e) {
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

	@Override
	public void visitAny(Nez.Any e) {
		file.write("lpeg.P(1)");
	}

	@Override
	public void visitString(Nez.MultiByte p) {
		// TODO Auto-generated method stub

	}

	protected void visit(String prefix, Nez.Unary e, String suffix) {
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

	@Override
	public void visitOption(Nez.Option e) {
		this.visit(null, e, "^-1");
	}

	@Override
	public void visitZeroMore(Nez.ZeroMore e) {
		this.visit(null, e, "^0");
	}

	@Override
	public void visitOneMore(Nez.OneMore e) {
		this.visit(null, e, "^1");
	}

	@Override
	public void visitAnd(Nez.And e) {
		this.visit("#", e, null);
	}

	@Override
	public void visitNot(Nez.Not e) {
		this.visit("-", e, null);
	}

	@Override
	public void visitTag(Nez.Tag e) {
		file.write("lpeg.P\"\" --[[");
		file.write(e.tag.toString());
		file.write("]]");
	}

	public void visitValue(Nez.Replace e) {
		file.write("lpeg.P\"\"");
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

	private int appendAsString(Nez.Pair l, int start) {
		int end = l.size();
		String s = "";
		for (int i = start; i < end; i++) {
			Expression e = l.get(i);
			if (e instanceof Nez.Byte) {
				char c = (char) (((Nez.Byte) e).byteChar);
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

	@Override
	public void visitPair(Nez.Pair l) {
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
			if (e instanceof Nez.Choice || e instanceof Nez.Sequence) {
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

	@Override
	public void visitChoice(Nez.Choice e) {
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

	@Override
	public void visitPreNew(Nez.BeginTree e) {

	}

	@Override
	public void visitNew(Nez.EndTree e) {

	}

	@Override
	public void visitUndefined(Expression e) {
		file.write("lpeg.P\"\" --[[ LPeg Unsupported <");
		// file.write(e.getPredicate());
		for (Expression se : e) {
			file.write(" ");
			visitExpression(se);
		}
		file.write("> ]]");
	}

	@Override
	public void visitReplace(Nez.Replace p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitBlockScope(Nez.BlockScope p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitSymbolMatch(Nez.SymbolMatch p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitSymbolAction(Nez.SymbolAction p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitSymbolPredicate(Nez.SymbolPredicate p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitSymbolExists(Nez.SymbolExists p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitLocalScope(Nez.LocalScope p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitDetree(Nez.Detree p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitIf(Nez.If p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitOn(Nez.On p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitLeftFold(Nez.LeftFold p) {
		// TODO Auto-generated method stub

	}

}
