package nez.x.generator;

import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Nez;
import nez.lang.Production;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pempty;
import nez.lang.expr.Pfail;
import nez.parser.Parser;
import nez.tool.peg.GrammarTranslator;
import nez.util.StringUtils;

public class CombinatorGenerator extends GrammarTranslator {

	@Override
	protected String getFileExtension() {
		return "java";
	}

	protected String _Delim() {
		return ", ";
	}

	@Override
	public void makeHeader(Grammar gg) {
		L("/* Parsing Expression Grammars for Nez */");
		L("import nez.ParserCombinator;");
		L("import nez.lang.Expression;");
		L("");
		L("class G extends ParserCombinator");
		Begin("{");
	}

	public void makeFooter(Parser g) {
		End("}");
	}

	@Override
	public void visitProduction(Grammar gg, Production p) {
		Expression e = p.getExpression();
		L("public Expression p");
		W(name(p));
		W("() ");
		Begin("{");
		L("return ");
		visitExpression(e);
		W(";");
		End("}");
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
	public void visitByte(Nez.Byte e) {
		C("t", StringUtils.stringfyByte('"', e.byteChar, '"'));
	}

	@Override
	public void visitByteSet(Nez.ByteSet e) {
		C("c", e.byteMap);
	}

	public void visitString(String s) {
		C("t", s);
	}

	@Override
	public void visitAny(Nez.Any e) {
		C("AnyChar");
	}

	@Override
	public void visitOption(Nez.Option e) {
		C("Option", e);
	}

	@Override
	public void visitZeroMore(Nez.ZeroMore e) {
		C("ZeroMore", e);
	}

	@Override
	public void visitOneMore(Nez.OneMore e) {
		C("OneMore", e);
	}

	@Override
	public void visitAnd(Nez.And e) {
		C("And", e);
	}

	@Override
	public void visitNot(Nez.Not e) {
		C("Not", e);
	}

	@Override
	public void visitChoice(Nez.Choice e) {
		C("Choice", e);
	}

	@Override
	public void visitPair(Nez.Pair e) {
		W("Sequence(");
		// super.visitPair(e);
		W(")");
	}

	@Override
	public void visitPreNew(Nez.PreNew e) {
		C("NCapture", e.shift);
	}

	@Override
	public void visitLeftFold(Nez.LeftFold e) {
		C("LCapture", e.shift);
	}

	@Override
	public void visitNew(Nez.New e) {
		C("Capture", e.shift);
	}

	@Override
	public void visitTag(Nez.Tag e) {
		C("Tagging", e.getTagName());
	}

	@Override
	public void visitReplace(Nez.Replace e) {
		C("Replace", StringUtils.quoteString('"', e.value, '"'));
	}

	@Override
	public void visitLink(Nez.Link e) {
		if (e.getLabel() != null) {
			C("Link", e.getLabel().toString(), e);
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
	public void visitEmpty(Expression p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitFail(Expression p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitString(Nez.String p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitBlockScope(Nez.BlockScope p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitLocalScope(Nez.LocalScope p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitSymbolAction(Nez.SymbolAction p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitSymbolExists(Nez.SymbolExists p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitSymbolMatch(Nez.SymbolMatch p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitSymbolPredicate(Nez.SymbolPredicate p) {
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

}
