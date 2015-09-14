package nez.generator;

import nez.Parser;
import nez.lang.Expression;
import nez.lang.Production;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Cset;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pand;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pempty;
import nez.lang.expr.Pfail;
import nez.lang.expr.Pnot;
import nez.lang.expr.Pone;
import nez.lang.expr.Poption;
import nez.lang.expr.Psequence;
import nez.lang.expr.Pzero;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.lang.expr.Xblock;
import nez.lang.expr.Xdef;
import nez.lang.expr.Xdefindent;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.parser.GenerativeGrammar;
import nez.parser.ParserGenerator;
import nez.util.StringUtils;

public class CombinatorGenerator extends ParserGenerator {

	@Override
	protected String getFileExtension() {
		return "java";
	}

	protected String _Delim() {
		return ", ";
	}

	@Override
	public void makeHeader(GenerativeGrammar gg) {
		L("/* Parsing Expression Grammars for Nez */");
		L("import nez.ParserCombinator;");
		L("import nez.lang.Expression;");
		L("");
		L("class G extends ParserCombinator");
		Begin();
	}

	public void makeFooter(Parser g) {
		End();
	}

	@Override
	public void visitProduction(GenerativeGrammar gg, Production p) {
		Expression e = p.getExpression();
		L("public Expression p");
		W(name(p));
		W("() ");
		Begin();
		L("return ");
		visitExpression(e);
		W(";");
		End();
	}

	public void visitEmpty(Pempty e) {
		C("Empty");
	}

	public void visitFailure(Pfail e) {
		C("Failure");
	}

	@Override
	public void visitNonTerminal(NonTerminal e) {
		C("P", name(e.getProduction()));
	}

	@Override
	public void visitCbyte(Cbyte e) {
		C("t", StringUtils.stringfyByte('"', e.byteChar, '"'));
	}

	@Override
	public void visitCset(Cset e) {
		C("c", e.byteMap);
	}

	public void visitString(String s) {
		C("t", s);
	}

	@Override
	public void visitCany(Cany e) {
		C("AnyChar");
	}

	@Override
	public void visitPoption(Poption e) {
		C("Option", e);
	}

	@Override
	public void visitPzero(Pzero e) {
		C("ZeroMore", e);
	}

	@Override
	public void visitPone(Pone e) {
		C("OneMore", e);
	}

	@Override
	public void visitPand(Pand e) {
		C("And", e);
	}

	@Override
	public void visitPnot(Pnot e) {
		C("Not", e);
	}

	@Override
	public void visitPchoice(Pchoice e) {
		C("Choice", e);
	}

	@Override
	public void visitPsequence(Psequence e) {
		W("Sequence(");
		// super.visitPsequence(e);
		W(")");
	}

	@Override
	public void visitTnew(Tnew e) {
		if (e.leftFold) {
			C("LCapture", e.shift);
		} else {
			C("NCapture", e.shift);
		}
	}

	@Override
	public void visitTcapture(Tcapture e) {
		C("Capture", e.shift);
	}

	@Override
	public void visitTtag(Ttag e) {
		C("Tagging", e.getTagName());
	}

	@Override
	public void visitTreplace(Treplace e) {
		C("Replace", StringUtils.quoteString('"', e.value, '"'));
	}

	@Override
	public void visitTlink(Tlink e) {
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

	@Override
	public void visitPempty(Expression p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitPfail(Expression p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitCmulti(Cmulti p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXblock(Xblock p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXlocal(Xlocal p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXdef(Xdef p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXexists(Xexists p) {
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

}
