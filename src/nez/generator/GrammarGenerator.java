package nez.generator;

import nez.lang.And;
import nez.lang.AnyChar;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.Capture;
import nez.lang.Choice;
import nez.lang.Empty;
import nez.lang.Expression;
import nez.lang.Failure;
import nez.lang.Link;
import nez.lang.New;
import nez.lang.NonTerminal;
import nez.lang.Not;
import nez.lang.Option;
import nez.lang.Production;
import nez.lang.Repetition;
import nez.lang.Repetition1;
import nez.lang.Replace;
import nez.lang.Sequence;
import nez.lang.Multinary;
import nez.lang.Tagging;
import nez.lang.Unary;
import nez.util.StringUtils;

public abstract class GrammarGenerator extends NezGenerator {

	protected GrammarGenerator(String fileName) {
		super(fileName);
	}

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

	protected GrammarGenerator L(String line) {
		file.writeIndent(line);
		return this;
	}
	
	protected char _LiteralQuote() {
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

	protected String _NonTerminal(Production p) {
		return p.getLocalName().replace("~", "_").replace("!", "_Off");
	}
	
	protected GrammarGenerator Unary(String prefix, Unary e, String suffix) {
		if(prefix != null) {
			W(prefix);
		}
		if(e.get(0) instanceof NonTerminal) {
			this.visit(e.get(0));
		}
		else {
			W("(");
			this.visit(e.get(0));
			W(")");
		}
		if(suffix != null) {
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
		if(e instanceof Choice) {
			for(int i = 0; i < e.size(); i++) {
				if(i > 0) {
					L(_Choice() + " ");
				}
				visit(e.get(i));
			}
		}
		else {
			visit(e);
		}
		dec();
	}	

	
	public void visitEmpty(Empty e) {
		W(""+ _LiteralQuote()+_LiteralQuote());
	}

	public void visitFailure(Failure e) {
		W(_Not()+ _LiteralQuote()+_LiteralQuote());
	}

	public void visitNonTerminal(NonTerminal e) {
		W(_NonTerminal(e.getProduction()));
	}
	
	public void visitByteChar(ByteChar e) {
		W(StringUtils.stringfyByte(_LiteralQuote(), e.byteChar, _LiteralQuote()));
	}

	public void visitByteMap(ByteMap e) {
		W(StringUtils.stringfyCharClass(e.byteMap));
	}
	
	public void visitAnyChar(AnyChar e) {
		W(_Any());
	}

	public void visitOption(Option e) {
		Unary( null, e, _Option());
	}
	
	public void visitRepetition(Repetition e) {
		Unary( null, e, _ZeroAndMore());
	}
	
	public void visitRepetition1(Repetition1 e) {
		Unary( null, e, _OneAndMore());
	}

	public void visitAnd(And e) {
		Unary( _And(), e, null);
	}
	
	public void visitNot(Not e) {
		Unary( _Not(), e, null);
	}
	
	public void visitChoice(Choice e) {
		for(int i = 0; i < e.size(); i++) {
			if(i > 0) {
				W(" " + _Choice() + " ");
			}
			visit(e.get(i));
		}
	}
	
	public void visitNew(New e) {
//		W(e.lefted ? "{@" : "{");
	}

	public void visitCapture(Capture e) {
//		W("}");
	}

	public void visitTagging(Tagging e) {
//		W("#");
//		W(e.tag.getName());
	}
	
	public void visitValue(Replace e) {
//		W(StringUtils.quoteString('`', e.value, '`'));
	}
	
	public void visitLink(Link e) {
//		String predicate = "@";
//		if(e.index != -1) {
//			predicate += "[" + e.index + "]";
//		}
//		Unary(predicate, e, null);
		visit(e.get(0));
	}

	public void visitSequence(Sequence e) {
		for(int i = 0; i < e.size(); i++) {
			if(i > 0) {
				W(" ");
			}
			int n = appendAsString(e, i);
			if(n > i) {
				i = n;
				continue;
			}
			Expression s = e.get(i);
			if(s instanceof Choice || s instanceof Sequence) {
				W("( ");
				visit(s);
				W(" )");
				continue;
			}
			visit(s);
		}
	}

	private int appendAsString(Multinary l, int start) {
		int end = l.size();
		String s = "";
		for(int i = start; i < end; i++) {
			Expression e = l.get(i);
			if(e instanceof ByteChar) {
				char c = (char)(((ByteChar) e).byteChar);
				if(c >= ' ' && c < 127) {
					s += c;
					continue;
				}
			}
			end = i;
			break;
		}
		if(s.length() > 1) {
			W(StringUtils.quoteString(_LiteralQuote(), s, _LiteralQuote()));
		}
		return end - 1;
	}

	@Override
	public void visitUndefined(Expression e) {
		if(e.size() > 0) {
			visit(e.get(0));
		}
	}


}
