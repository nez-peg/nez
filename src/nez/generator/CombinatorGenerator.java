package nez.generator;

import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Production;
import nez.lang.expr.Uand;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cset;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Choice;
import nez.lang.expr.Empty;
import nez.lang.expr.Failure;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Unot;
import nez.lang.expr.Uoption;
import nez.lang.expr.Uzero;
import nez.lang.expr.Uone;
import nez.lang.expr.Treplace;
import nez.lang.expr.Sequence;
import nez.lang.expr.Ttag;
import nez.util.StringUtils;

public class CombinatorGenerator extends GrammarGenerator {
	@Override
	public String getDesc() {
		return "a Nez combinator for Java";
	}

	protected String _Delim() {
		return ", ";
	}

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

	public void visitByteChar(Cbyte e) {
		C("t", StringUtils.stringfyByte('"', e.byteChar, '"'));
	}

	public void visitByteMap(Cset e) {
		C("c", e.byteMap);
	}

	public void visitString(String s) {
		C("t", s);
	}

	public void visitAnyChar(Cany e) {
		C("AnyChar");
	}

	public void visitOption(Uoption e) {
		C("Option", e);
	}

	public void visitRepetition(Uzero e) {
		C("ZeroMore", e);
	}

	public void visitRepetition1(Uone e) {
		C("OneMore", e);
	}

	public void visitAnd(Uand e) {
		C("And", e);
	}

	public void visitNot(Unot e) {
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

	public void visitNew(Tnew e) {
		if (e.leftFold) {
			C("LCapture", e.shift);
		} else {
			C("NCapture", e.shift);
		}
	}

	public void visitCapture(Tcapture e) {
		C("Capture", e.shift);
	}

	public void visitTagging(Ttag e) {
		C("Tagging", e.getTagName());
	}

	public void visitReplace(Treplace e) {
		C("Replace", StringUtils.quoteString('"', e.value, '"'));
	}

	public void visitLink(Tlink e) {
		if (e.index != -1) {
			C("Link", String.valueOf(e.index), e);
		} else {
			C("Link", e);
		}
	}

	@Override
	public void visitUndefined(Expression e) {
		W("<");
		W(e.getPredicate());
		for (Expression se : e) {
			W(" ");
			visitExpression(se);
		}
		W(">");
	}

}
