package nez.tool.peg;

import java.util.List;

import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Production;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Cset;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pand;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pnot;
import nez.lang.expr.Pone;
import nez.lang.expr.Poption;
import nez.lang.expr.Psequence;
import nez.lang.expr.Pzero;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Tlfold;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.lang.expr.Xblock;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Xsymbol;
import nez.util.StringUtils;

public class NezTranslator extends PEGTranslator {

	@Override
	protected String getFileExtension() {
		return "nez";
	}

	@Override
	public void visitProduction(Grammar gg, Production rule) {
		Expression e = rule.getExpression();
		if (rule.isPublic()) {
			L("public ");
			W(name(rule.getLocalName()));
		} else {
			L(name(rule.getLocalName()));
		}
		Begin("");
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
		End("");
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
		W(name(e.getLocalName()));
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
	public void visitCmulti(Cmulti p) {
		W(p.toString());
	}

	@Override
	public void visitCany(Cany e) {
		W(".");
	}

	@Override
	public void visitPoption(Poption e) {
		visitUnary(null, e, "?");
	}

	@Override
	public void visitPzero(Pzero e) {
		visitUnary(null, e, "*");
	}

	@Override
	public void visitPone(Pone e) {
		visitUnary(null, e, "+");
	}

	@Override
	public void visitPand(Pand e) {
		visitUnary("&", e, null);
	}

	@Override
	public void visitPnot(Pnot e) {
		visitUnary("!", e, null);
	}

	@Override
	public void visitPsequence(Psequence p) {
		int c = 0;
		List<Expression> l = p.toList();
		for (Expression e : l) {
			if (c > 0) {
				W(" ");
			}
			if (e instanceof Pchoice) {
				W("(");
				visitExpression(e);
				W(")");
			} else {
				visitExpression(e);
			}
			c++;
		}
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
		W("{");
	}

	@Override
	public void visitTlfold(Tlfold e) {
		W(e.getLabel() == null ? "{$" : "{$" + e.getLabel());
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
		visitUnary(predicate + "(", e, ")");
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
	public void visitXdef(Xsymbol e) {
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

	@Override
	public void visitXindent(Xindent p) {
		this.visitUndefined(p);
	}

}
