package nez.generator;

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

public abstract class GrammarGenerator extends NezGenerator {

	protected GrammarGenerator W(String word) {
		file.write(word);
		return this;
	}

	protected GrammarGenerator L() {
		file.writeIndent();
		return this;
	}

	protected GrammarGenerator inc() {
		file.incIndent();
		return this;
	}

	protected GrammarGenerator dec() {
		file.decIndent();
		return this;
	}

	protected GrammarGenerator Begin() {
		W("{").inc();
		return this;
	}

	protected GrammarGenerator End() {
		dec().L("}");
		return this;
	}

	protected GrammarGenerator L(String line) {
		file.writeIndent(line);
		return this;
	}

	protected char Quoatation() {
		return '\'';
	}

	protected String _RuleDef() {
		return "=";
	}

	protected String _Choice() {
		return "/";
	}

	protected String _Option() {
		return "?";
	}

	protected String _ZeroAndMore() {
		return "*";
	}

	protected String _OneAndMore() {
		return "+";
	}

	protected String _And() {
		return "&";
	}

	protected String _Not() {
		return "!";
	}

	protected String _Any() {
		return ".";
	}

	protected String _OpenGrouping() {
		return "(";
	}

	protected String _CloseGrouping() {
		return ")";
	};

	public void visitGrouping(Expression e) {
		W(_OpenGrouping());
		visitExpression(e);
		W(_CloseGrouping());
	}

	protected String _Open() {
		return "(";
	}

	protected String _Delim() {
		return " ";
	}

	protected String _Close() {
		return ")";
	};

	protected String _Name(Production p) {
		return p.getLocalName().replace("~", "_").replace("!", "_W");
	}

	protected GrammarGenerator C(String name, Expression e) {
		int c = 0;
		W(name).W(_Open());
		for (Expression sub : e) {
			if (c > 0) {
				W(_Delim());
			}
			visitExpression(sub);
			c++;
		}
		W(_Close());
		return this;
	}

	protected GrammarGenerator C(String name, String first, Expression e) {
		W(name).W(_Open());
		W(first).W(_Delim());
		for (Expression sub : e) {
			visitExpression(sub);
		}
		W(_Close());
		return this;
	}

	protected GrammarGenerator C(String name) {
		W(name).W(_Open()).W(_Close());
		return this;
	}

	protected GrammarGenerator C(String name, String arg) {
		if (arg.length() > 1 && arg.startsWith("\"") && arg.endsWith("\"")) {
		} else {
			arg = StringUtils.quoteString('"', arg, '"');
		}
		W(name).W(_Open()).W(arg).W(_Close());
		return this;
	}

	protected GrammarGenerator C(String name, int arg) {
		W(name).W(_Open()).W(String.valueOf(arg)).W(_Close());
		return this;
	}

	protected GrammarGenerator C(String name, boolean[] arg) {
		int cnt = 0;
		W(name).W(_Open());
		for (int c = 0; c < arg.length; c++) {
			if (arg[c]) {
				if (cnt > 0) {
					W(_Delim());
				}
				W(String.valueOf(c));
				cnt++;
			}
		}
		W(_Close());
		return this;
	}

	protected String _NonTerminal(Production p) {
		return p.getLocalName().replace("~", "_").replace("!", "NOT").replace(".", "DOT");
	}

	protected GrammarGenerator Unary(String prefix, Unary e, String suffix) {
		if (prefix != null) {
			W(prefix);
		}
		if (e.get(0) instanceof NonTerminal) {
			this.visitExpression(e.get(0));
		} else {
			visitGrouping(e.get(0));
		}
		if (suffix != null) {
			W(suffix);
		}
		return this;
	}

	@Override
	public void visitProduction(Production rule) {
		Expression e = rule.getExpression();
		L(_NonTerminal(rule));
		inc();
		L(_RuleDef() + " ");
		if (e instanceof Pchoice) {
			for (int i = 0; i < e.size(); i++) {
				if (i > 0) {
					L(_Choice() + " ");
				}
				visitExpression(e.get(i));
			}
		} else {
			visitExpression(e);
		}
		dec();
	}

	public void visitPempty(Expression e) {
		W("" + Quoatation() + Quoatation());
	}

	public void visitPfail(Expression e) {
		W(_Not() + Quoatation() + Quoatation());
	}

	public void visitNonTerminal(NonTerminal e) {
		W(_NonTerminal(e.getProduction()));
	}

	public void visitCbyte(Cbyte e) {
		W(StringUtils.stringfyByte(Quoatation(), e.byteChar, Quoatation()));
	}

	public void visitCset(Cset e) {
		W(StringUtils.stringfyCharacterClass(e.byteMap));
	}

	public void visitCany(Cany e) {
		W(_Any());
	}

	@Override
	public void visitCmulti(Cmulti p) {
		W(p.toString());
	}

	public void visitPoption(Poption e) {
		Unary(null, e, _Option());
	}

	public void visitPzero(Pzero e) {
		Unary(null, e, _ZeroAndMore());
	}

	public void visitPone(Pone e) {
		Unary(null, e, _OneAndMore());
	}

	public void visitPand(Pand e) {
		Unary(_And(), e, null);
	}

	public void visitPnot(Pnot e) {
		Unary(_Not(), e, null);
	}

	public void visitPchoice(Pchoice e) {
		for (int i = 0; i < e.size(); i++) {
			if (i > 0) {
				W(" " + _Choice() + " ");
			}
			visitExpression(e.get(i));
		}
	}

	public void visitTnew(Tnew e) {
		// W(e.lefted ? "{@" : "{");
	}

	public void visitTcapture(Tcapture e) {
		// W("}");
	}

	public void visitTtag(Ttag e) {
		// W("#");
		// W(e.tag.getName());
	}

	public void visitValue(Treplace e) {
		// W(StringUtils.quoteString('`', e.value, '`'));
	}

	public void visitTlink(Tlink e) {
		// String predicate = "@";
		// if(e.index != -1) {
		// predicate += "[" + e.index + "]";
		// }
		// Unary(predicate, e, null);
		visitExpression(e.get(0));
	}

	public void visitPsequence(Psequence e) {
		int c = 0;
		for (int i = 0; i < e.size(); i++) {
			if (c > 0) {
				W(_Delim());
			}
			Expression s = e.get(i);
			if (s instanceof Cbyte && i + 1 < e.size() && e.get(i + 1) instanceof Cbyte) {
				i = checkString(e, i);
				c++;
				continue;
			}
			if (s instanceof Pchoice || s instanceof Psequence) {
				visitGrouping(s);
				c++;
			} else {
				visitExpression(s);
				c++;
			}
		}
	}

	private int checkString(Psequence l, int start) {
		int n = 0;
		for (int i = start; i < l.size(); i++) {
			Expression e = l.get(i);
			if (e instanceof Cbyte) {
				n++;
				continue;
			}
			break;
		}
		byte[] utf8 = new byte[n];
		for (int i = 0; i < n; i++) {
			utf8[i] = (byte) (((Cbyte) l.get(start + i)).byteChar);
		}
		visitString(StringUtils.newString(utf8));
		return start + n - 1;
	}

	public void visitString(String text) {
		W(StringUtils.quoteString('\'', text, '\''));
	}

	@Override
	public void visitUndefined(Expression e) {
		if (e.size() > 0) {
			visitExpression(e.get(0));
		}
	}

	@Override
	public String getDesc() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void visitTreplace(Treplace p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXblock(Xblock p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXdef(Xdef p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXmatch(Xmatch p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXis(Xis p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXdefindent(Xdefindent p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXindent(Xindent p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXexists(Xexists p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXlocal(Xlocal p) {
		// TODO Auto-generated method stub

	}

}
