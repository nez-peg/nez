package nez.generator;

import nez.Parser;
import nez.lang.Expression;
import nez.lang.Production;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cset;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pand;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pnot;
import nez.lang.expr.Pone;
import nez.lang.expr.Poption;
import nez.lang.expr.Pzero;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.lang.expr.Xblock;
import nez.lang.expr.Xdef;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.util.StringUtils;

public class NezGrammarGenerator extends GrammarGenerator {
	@Override
	public String getDesc() {
		return "a Nez grammar";
	}

	public NezGrammarGenerator() {
	}

	@Override
	public void makeHeader(Parser g) {
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
		if (e instanceof Pchoice) {
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
	public void visitPempty(Expression e) {
		W("''");
	}

	@Override
	public void visitPfail(Expression e) {
		W("!''");
	}

	@Override
	public void visitNonTerminal(NonTerminal e) {
		W(stringfyName(e.getLocalName()));
	}

	@Override
	public void visitCbyte(Cbyte e) {
		W(StringUtils.stringfyCharacter(e.byteChar));
	}

	@Override
	public void visitCset(Cset e) {
		W(StringUtils.stringfyCharacterClass(e.byteMap));
	}

	@Override
	public void visitCany(Cany e) {
		W(".");
	}

	@Override
	public void visitPoption(Poption e) {
		Unary(null, e, "?");
	}

	@Override
	public void visitPzero(Pzero e) {
		Unary(null, e, "*");
	}

	@Override
	public void visitPone(Pone e) {
		Unary(null, e, "+");
	}

	@Override
	public void visitPand(Pand e) {
		Unary("&", e, null);
	}

	@Override
	public void visitPnot(Pnot e) {
		Unary("!", e, null);
	}

	@Override
	public void visitPchoice(Pchoice e) {
		for (int i = 0; i < e.size(); i++) {
			if (i > 0) {
				W(" / ");
			}
			visitExpression(e.get(i));
		}
	}

	@Override
	public void visitTnew(Tnew e) {
		W(e.leftFold ? "{@" : "{");
	}

	@Override
	public void visitTcapture(Tcapture e) {
		W("}");
	}

	@Override
	public void visitTtag(Ttag e) {
		W("#");
		W(e.tag.getSymbol());
	}

	@Override
	public void visitTreplace(Treplace e) {
		W(StringUtils.quoteString('`', e.value, '`'));
	}

	@Override
	public void visitTlink(Tlink e) {
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
	public void visitXblock(Xblock e) {
		W("<block ");
		visitExpression(e.get(0));
		W(">");
	}

	@Override
	public void visitXdef(Xdef e) {
		W("<def ");
		W(e.getTableName());
		W(" ");
		visitExpression(e.get(0));
		W(">");
	}

	@Override
	public void visitXmatch(Xmatch e) {
		W("<match ");
		W(e.getTableName());
		W(">");
	};

	@Override
	public void visitXis(Xis e) {
		W("<is ");
		W(e.getTableName());
		W(">");
	}

	@Override
	public void visitXexists(Xexists e) {
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
	public void visitXlocal(Xlocal e) {
		W("<local ");
		W(e.getTableName());
		W(" ");
		visitExpression(e.get(0));
		W(">");
	}

}
