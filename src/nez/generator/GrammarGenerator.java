package nez.generator;

import nez.lang.Expression;
import nez.lang.Production;
import nez.lang.expr.And;
import nez.lang.expr.AnyChar;
import nez.lang.expr.Block;
import nez.lang.expr.ByteChar;
import nez.lang.expr.ByteMap;
import nez.lang.expr.Capture;
import nez.lang.expr.Choice;
import nez.lang.expr.DefIndent;
import nez.lang.expr.DefSymbol;
import nez.lang.expr.ExistsSymbol;
import nez.lang.expr.IsIndent;
import nez.lang.expr.IsSymbol;
import nez.lang.expr.Link;
import nez.lang.expr.LocalTable;
import nez.lang.expr.MatchSymbol;
import nez.lang.expr.MultiChar;
import nez.lang.expr.New;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Not;
import nez.lang.expr.Option;
import nez.lang.expr.Repetition;
import nez.lang.expr.Repetition1;
import nez.lang.expr.Replace;
import nez.lang.expr.Sequence;
import nez.lang.expr.Tagging;
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
		if (e instanceof Choice) {
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

	public void visitEmpty(Expression e) {
		W("" + Quoatation() + Quoatation());
	}

	public void visitFailure(Expression e) {
		W(_Not() + Quoatation() + Quoatation());
	}

	public void visitNonTerminal(NonTerminal e) {
		W(_NonTerminal(e.getProduction()));
	}

	public void visitByteChar(ByteChar e) {
		W(StringUtils.stringfyByte(Quoatation(), e.byteChar, Quoatation()));
	}

	public void visitByteMap(ByteMap e) {
		W(StringUtils.stringfyCharacterClass(e.byteMap));
	}

	public void visitAnyChar(AnyChar e) {
		W(_Any());
	}

	@Override
	public void visitCharMultiByte(MultiChar p) {
		W(p.toString());
	}

	public void visitOption(Option e) {
		Unary(null, e, _Option());
	}

	public void visitRepetition(Repetition e) {
		Unary(null, e, _ZeroAndMore());
	}

	public void visitRepetition1(Repetition1 e) {
		Unary(null, e, _OneAndMore());
	}

	public void visitAnd(And e) {
		Unary(_And(), e, null);
	}

	public void visitNot(Not e) {
		Unary(_Not(), e, null);
	}

	public void visitChoice(Choice e) {
		for (int i = 0; i < e.size(); i++) {
			if (i > 0) {
				W(" " + _Choice() + " ");
			}
			visitExpression(e.get(i));
		}
	}

	public void visitNew(New e) {
		// W(e.lefted ? "{@" : "{");
	}

	public void visitCapture(Capture e) {
		// W("}");
	}

	public void visitTagging(Tagging e) {
		// W("#");
		// W(e.tag.getName());
	}

	public void visitValue(Replace e) {
		// W(StringUtils.quoteString('`', e.value, '`'));
	}

	public void visitLink(Link e) {
		// String predicate = "@";
		// if(e.index != -1) {
		// predicate += "[" + e.index + "]";
		// }
		// Unary(predicate, e, null);
		visitExpression(e.get(0));
	}

	public void visitSequence(Sequence e) {
		int c = 0;
		for (int i = 0; i < e.size(); i++) {
			if (c > 0) {
				W(_Delim());
			}
			Expression s = e.get(i);
			if (s instanceof ByteChar && i + 1 < e.size() && e.get(i + 1) instanceof ByteChar) {
				i = checkString(e, i);
				c++;
				continue;
			}
			if (s instanceof Choice || s instanceof Sequence) {
				visitGrouping(s);
				c++;
			} else {
				visitExpression(s);
				c++;
			}
		}
	}

	private int checkString(Sequence l, int start) {
		int n = 0;
		for (int i = start; i < l.size(); i++) {
			Expression e = l.get(i);
			if (e instanceof ByteChar) {
				n++;
				continue;
			}
			break;
		}
		byte[] utf8 = new byte[n];
		for (int i = 0; i < n; i++) {
			utf8[i] = (byte) (((ByteChar) l.get(start + i)).byteChar);
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
	public void visitReplace(Replace p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitBlock(Block p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitDefSymbol(DefSymbol p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitMatchSymbol(MatchSymbol p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitIsSymbol(IsSymbol p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitDefIndent(DefIndent p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitIsIndent(IsIndent p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitExistsSymbol(ExistsSymbol p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitLocalTable(LocalTable p) {
		// TODO Auto-generated method stub

	}

}
