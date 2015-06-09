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
import nez.lang.Grammar;
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
import nez.lang.Tagging;
import nez.util.StringUtils;

public class CombinatorGenerator extends GrammarGenerator {
	@Override
	public String getDesc() {
		return "a Nez combinator for Java" ;
	}
	
	protected String _Delim() { return ", "; }
	
	@Override
	public void makeHeader(Grammar g) {
		L("/* Parsing Expression Grammars for Nez */");
		L("import nez.ParserCombinator;");
		L("import nez.lang.Expression;");
		L("");
		L("class G extends ParserCombinator").Begin();
	}
	
	public void makeFooter(Grammar g) {
		End();
	}

	@Override
	public void visitProduction(Production rule) {
		Expression e = rule.getExpression();
		L("public Expression p").W(_NonTerminal(rule)).W("() ").Begin();
		L("return ");
		visitExpression(e);
		W(";");
		End();
	}	
	
	public void visitEmpty(Empty e) {
		C("Empty");
	}

	public void visitFailure(Failure e) {
		C("Failure");
	}

	public void visitNonTerminal(NonTerminal e) {
		C("P", _NonTerminal(e.getProduction()));
	}
	
	public void visitByteChar(ByteChar e) {
		C("t", StringUtils.stringfyByte('"', e.byteChar, '"'));
	}

	public void visitByteMap(ByteMap e) {
		C("c", e.byteMap);
	}

	public void visitString(String s) {
		C("t", s);
	}

	public void visitAnyChar(AnyChar e) {
		C("AnyChar");
	}

	public void visitOption(Option e) {
		C("Option", e);
	}
	
	public void visitRepetition(Repetition e) {
		C("ZeroMore", e);
	}
	
	public void visitRepetition1(Repetition1 e) {
		C("OneMore", e);
	}

	public void visitAnd(And e) {
		C("And", e);
	}
	
	public void visitNot(Not e) {
		C("Not", e);
	}
	
	public void visitChoice(Choice e) {
		C("Choice", e);
	}
	
	public void visitSequence(Sequence e) {
		W("Sequence(");
		super.visitSequence(e);
		W(")");
	}
	
	public void visitNew(New e) {
		if(e.lefted) {
			C("LCapture", e.shift);
		}
		else {
			C("NCapture", e.shift);
		}
	}

	public void visitCapture(Capture e) {
		C("Capture", e.shift);
	}

	public void visitTagging(Tagging e) {
		C("Tagging", e.getTagName());
	}
	
	public void visitReplace(Replace e) {
		C("Replace", StringUtils.quoteString('"', e.value, '"'));
	}
	
	public void visitLink(Link e) {
		if(e.index != -1) {
			C("Link", String.valueOf(e.index), e);
		}
		else {
			C("Link", e);
		}
	}

	@Override
	public void visitUndefined(Expression e) {
		W("<");
		W(e.getPredicate());
		for(Expression se : e) {
			W(" ");
			visitExpression(se);
		}
		W(">");
	}

}
