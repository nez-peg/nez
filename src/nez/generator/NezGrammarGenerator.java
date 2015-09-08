package nez.generator;

import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Production;
import nez.lang.expr.Uand;
import nez.lang.expr.Cany;
import nez.lang.expr.Xblock;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cset;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Choice;
import nez.lang.expr.Xdef;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xis;
import nez.lang.expr.Tlink;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Tnew;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Unot;
import nez.lang.expr.Uoption;
import nez.lang.expr.Uzero;
import nez.lang.expr.Uone;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.util.StringUtils;

public class NezGrammarGenerator extends GrammarGenerator {
	@Override
	public String getDesc() {
		return "a Nez grammar";
	}

	public NezGrammarGenerator() {
	}

	@Override
	public void makeHeader(Grammar g) {
		L("// Parsing Expression Grammars for Nez");
		L("// ");
	}

	String stringfyName(String s) {
		return s;
	}

	@Override
	public void visitProduction(Production rule) {
		Expression e = rule.getExpression();
		if (rule.isPublic()) {
			L("public ");
			W(stringfyName(rule.getLocalName()));
		} else {
			L(stringfyName(rule.getLocalName()));
		}
		inc();
		L("= ");
		if (e instanceof Choice) {
			for (int i = 0; i < e.size(); i++) {
				if (i > 0) {
					L("/ ");
				}
				visitExpression(e.get(i));
			}
		} else {
			visitExpression(e);
		}
		dec();
	}

	@Override
	public void visitEmpty(Expression e) {
		W("''");
	}

	@Override
	public void visitFailure(Expression e) {
		W("!''");
	}

	@Override
	public void visitNonTerminal(NonTerminal e) {
		W(stringfyName(e.getLocalName()));
	}

	@Override
	public void visitByteChar(Cbyte e) {
		W(StringUtils.stringfyCharacter(e.byteChar));
	}

	@Override
	public void visitByteMap(Cset e) {
		W(StringUtils.stringfyCharacterClass(e.byteMap));
	}

	@Override
	public void visitAnyChar(Cany e) {
		W(".");
	}

	@Override
	public void visitOption(Uoption e) {
		Unary(null, e, "?");
	}

	@Override
	public void visitRepetition(Uzero e) {
		Unary(null, e, "*");
	}

	@Override
	public void visitRepetition1(Uone e) {
		Unary(null, e, "+");
	}

	@Override
	public void visitAnd(Uand e) {
		Unary("&", e, null);
	}

	@Override
	public void visitNot(Unot e) {
		Unary("!", e, null);
	}

	@Override
	public void visitChoice(Choice e) {
		for (int i = 0; i < e.size(); i++) {
			if (i > 0) {
				W(" / ");
			}
			visitExpression(e.get(i));
		}
	}

	@Override
	public void visitNew(Tnew e) {
		W(e.leftFold ? "{@" : "{");
	}

	@Override
	public void visitCapture(Tcapture e) {
		W("}");
	}

	@Override
	public void visitTagging(Ttag e) {
		W("#");
		W(e.tag.getName());
	}

	@Override
	public void visitReplace(Treplace e) {
		W(StringUtils.quoteString('`', e.value, '`'));
	}

	@Override
	public void visitLink(Tlink e) {
		String predicate = "$";
		if (e.getLabel() != null) {
			predicate += e.getLabel().toString();
		}
		Unary(predicate + "(", e, ")");
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
	public void visitBlock(Xblock e) {
		W("<block ");
		visitExpression(e.get(0));
		W(">");
	}

	@Override
	public void visitDefSymbol(Xdef e) {
		W("<def ");
		W(e.getTableName());
		W(" ");
		visitExpression(e.get(0));
		W(">");
	}

	@Override
	public void visitMatchSymbol(Xmatch e) {
		W("<match ");
		W(e.getTableName());
		W(">");
	};

	@Override
	public void visitIsSymbol(Xis e) {
		W("<is ");
		W(e.getTableName());
		W(">");
	}

	@Override
	public void visitExistsSymbol(Xexists e) {
		String symbol = e.getSymbol();
		W("<exists ");
		W(e.getTableName());
		if (symbol != null) {
			W(" ");
			W("'" + symbol + "'");
		}
		W(">");
	}

	@Override
	public void visitLocalTable(Xlocal e) {
		W("<local ");
		W(e.getTableName());
		W(" ");
		visitExpression(e.get(0));
		W(">");
	}

}
