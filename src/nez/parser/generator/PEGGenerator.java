package nez.parser.generator;

import java.util.List;

import nez.lang.Expression;
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
import nez.lang.expr.Tdetree;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.lang.expr.Unary;
import nez.lang.expr.Xblock;
import nez.lang.expr.Xdef;
import nez.lang.expr.Xdefindent;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xif;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Xon;
import nez.parser.GenerativeGrammar;
import nez.parser.ParserGenerator;
import nez.util.StringUtils;

public class PEGGenerator extends ParserGenerator {

	@Override
	protected String getFileExtension() {
		return "peg";
	}

	@Override
	public void visitProduction(GenerativeGrammar gg, Production p) {
		Expression e = p.getExpression();
		L(name(p.getLocalName()));
		Begin("");
		L("<- ");
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
	public void visitPempty(Expression p) {
		W("''");
	}

	@Override
	public void visitPfail(Expression p) {
		W("!''");
	}

	@Override
	public void visitCany(Cany p) {
		W(".");
	}

	@Override
	public void visitCbyte(Cbyte p) {
		W(StringUtils.stringfyCharacter(p.byteChar));
	}

	@Override
	public void visitCset(Cset p) {
		W(StringUtils.stringfyCharacterClass(p.byteMap));
	}

	@Override
	public void visitCmulti(Cmulti p) {
		// TODO Auto-generated method stub

	}

	protected void visitUnary(String prefix, Unary e, String suffix) {
		if (prefix != null) {
			W(prefix);
		}
		Expression inner = e.get(0);
		if (inner instanceof Pchoice || inner instanceof Psequence) {
			W("(");
			this.visitExpression(e.get(0));
			W(")");
		} else {
			this.visitExpression(e.get(0));
		}
		if (suffix != null) {
			W(suffix);
		}
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
	public void visitPchoice(Pchoice p) {
		for (int i = 0; i < p.size(); i++) {
			if (i > 0) {
				W(" / ");
			}
			visitExpression(p.get(i));
		}
	}

	@Override
	public void visitNonTerminal(NonTerminal p) {
		W(name(p.getProduction()));
	}

	protected void SemanticAction(String code) {
		W("{");
		W(code);
		W("}");
	}

	protected void SemanticPredicate(String code) {
		W("&{");
		W(code);
		W("}");
	}

	@Override
	public void visitTlink(Tlink p) {
		SemanticAction("start()");
		visitExpression(p.get(0));
		SemanticAction("commit()");
	}

	@Override
	public void visitTnew(Tnew p) {
		W(p.leftFold ? "{@" : "{");
	}

	@Override
	public void visitTcapture(Tcapture p) {
		SemanticAction("capture()");
	}

	@Override
	public void visitTtag(Ttag p) {
		SemanticAction("tag(" + p.getTagName() + ")");
	}

	@Override
	public void visitTreplace(Treplace p) {
		SemanticAction("replace(" + StringUtils.quoteString('"', p.value, '"') + ")");
	}

	@Override
	public void visitTdetree(Tdetree p) {
		SemanticAction("start()");
		visitExpression(p.get(0));
		SemanticAction("abort()");
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
		SemanticPredicate("match(" + p.getTableName() + ")");
	}

	@Override
	public void visitXis(Xis p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXif(Xif p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXon(Xon p) {
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
