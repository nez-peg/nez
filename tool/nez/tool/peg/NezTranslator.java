package nez.tool.peg;

import java.util.List;

import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Nez;
import nez.lang.Production;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pchoice;
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
	public void visitEmpty(Expression e) {
		W("''");
	}

	@Override
	public void visitFail(Expression e) {
		W("!''");
	}

	@Override
	public void visitNonTerminal(NonTerminal e) {
		W(name(e.getLocalName()));
	}

	@Override
	public void visitByte(Nez.Byte e) {
		W(StringUtils.stringfyCharacter(e.byteChar));
	}

	@Override
	public void visitByteSet(Nez.ByteSet e) {
		W(StringUtils.stringfyCharacterClass(e.byteMap));
	}

	@Override
	public void visitString(Nez.String p) {
		W(p.toString());
	}

	@Override
	public void visitAny(Nez.Any e) {
		W(".");
	}

	@Override
	public void visitOption(Nez.Option e) {
		visitUnary(null, e, "?");
	}

	@Override
	public void visitZeroMore(Nez.ZeroMore e) {
		visitUnary(null, e, "*");
	}

	@Override
	public void visitOneMore(Nez.OneMore e) {
		visitUnary(null, e, "+");
	}

	@Override
	public void visitAnd(Nez.And e) {
		visitUnary("&", e, null);
	}

	@Override
	public void visitNot(Nez.Not e) {
		visitUnary("!", e, null);
	}

	@Override
	public void visitPair(Nez.Pair p) {
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
	public void visitChoice(Nez.Choice e) {
		for (int i = 0; i < e.size(); i++) {
			if (i > 0) {
				W(" / ");
			}
			visitExpression(e.get(i));
		}
	}

	@Override
	public void visitPreNew(Nez.PreNew e) {
		W("{");
	}

	@Override
	public void visitLeftFold(Nez.LeftFold e) {
		W(e.getLabel() == null ? "{$" : "{$" + e.getLabel());
	}

	@Override
	public void visitNew(Nez.New e) {
		W("}");
	}

	@Override
	public void visitTag(Nez.Tag e) {
		W("#");
		W(e.tag.getSymbol());
	}

	@Override
	public void visitReplace(Nez.Replace e) {
		W(StringUtils.quoteString('`', e.value, '`'));
	}

	@Override
	public void visitLink(Nez.Link e) {
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
	public void visitBlockScope(Nez.BlockScope e) {
		W("<block ");
		visitExpression(e.get(0));
		W(">");
	}

	@Override
	public void visitSymbolAction(Nez.SymbolAction e) {
		W("<symbol ");
		W(e.tableName);
		W(" ");
		visitExpression(e.get(0));
		W(">");
	}

	@Override
	public void visitSymbolMatch(Nez.SymbolMatch e) {
		W("<match ");
		W(e.tableName);
		W(">");
	};

	@Override
	public void visitSymbolPredicate(Nez.SymbolPredicate e) {
		W("<is ");
		W(e.tableName);
		W(">");
	}

	@Override
	public void visitSymbolExists(Nez.SymbolExists e) {
		String symbol = e.symbol;
		W("<exists ");
		W(e.tableName);
		if (symbol != null) {
			W(" ");
			W("'" + symbol + "'");
		}
		W(">");
	}

	@Override
	public void visitLocalScope(Nez.LocalScope e) {
		W("<local ");
		W(e.tableName);
		W(" ");
		visitExpression(e.get(0));
		W(">");
	}

}
