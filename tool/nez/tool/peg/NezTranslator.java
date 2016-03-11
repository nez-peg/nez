//package nez.tool.peg;
//
//import java.util.List;
//
//import nez.lang.Expression;
//import nez.lang.Expressions;
//import nez.lang.Grammar;
//import nez.lang.Nez;
//import nez.lang.NonTerminal;
//import nez.lang.Production;
//import nez.util.StringUtils;
//
//public class NezTranslator extends PEGTranslator {
//
//	@Override
//	protected String getFileExtension() {
//		return "nez";
//	}
//
//	@Override
//	public void visitProduction(Grammar gg, Production rule) {
//		Expression e = rule.getExpression();
//		if (rule.isPublic()) {
//			L("public ");
//			W(name(rule.getLocalName()));
//		} else {
//			L(name(rule.getLocalName()));
//		}
//		Begin("");
//		L("= ");
//		if (e instanceof Nez.Choice) {
//			for (int i = 0; i < e.size(); i++) {
//				if (i > 0) {
//					L("/ ");
//				}
//				visitExpression(e.get(i));
//			}
//		} else {
//			visitExpression(e);
//		}
//		End("");
//	}
//
//	@Override
//	public void visitEmpty(Expression e) {
//		W("''");
//	}
//
//	@Override
//	public void visitFail(Expression e) {
//		W("!''");
//	}
//
//	@Override
//	public void visitNonTerminal(NonTerminal e) {
//		W(name(e.getLocalName()));
//	}
//
//	@Override
//	public void visitByte(Nez.Byte e) {
//		W(StringUtils.stringfyByte(e.byteChar));
//	}
//
//	@Override
//	public void visitByteSet(Nez.ByteSet e) {
//		W(StringUtils.stringfyByteSet(e.byteset));
//	}
//
//	@Override
//	public void visitString(Nez.MultiByte p) {
//		W(p.toString());
//	}
//
//	@Override
//	public void visitAny(Nez.Any e) {
//		W(".");
//	}
//
//	@Override
//	public void visitOption(Nez.Option e) {
//		visitUnary(null, e, "?");
//	}
//
//	@Override
//	public void visitZeroMore(Nez.ZeroMore e) {
//		visitUnary(null, e, "*");
//	}
//
//	@Override
//	public void visitOneMore(Nez.OneMore e) {
//		visitUnary(null, e, "+");
//	}
//
//	@Override
//	public void visitAnd(Nez.And e) {
//		visitUnary("&", e, null);
//	}
//
//	@Override
//	public void visitNot(Nez.Not e) {
//		visitUnary("!", e, null);
//	}
//
//	@Override
//	public void visitPair(Nez.Pair p) {
//		int c = 0;
//		List<Expression> l = Expressions.flatten(p);
//		for (Expression e : l) {
//			if (c > 0) {
//				W(" ");
//			}
//			if (e instanceof Nez.Choice) {
//				W("(");
//				visitExpression(e);
//				W(")");
//			} else {
//				visitExpression(e);
//			}
//			c++;
//		}
//	}
//
//	@Override
//	public void visitChoice(Nez.Choice e) {
//		for (int i = 0; i < e.size(); i++) {
//			if (i > 0) {
//				W(" / ");
//			}
//			visitExpression(e.get(i));
//		}
//	}
//
//	@Override
//	public void visitPreNew(Nez.BeginTree e) {
//		W("{");
//	}
//
//	@Override
//	public void visitLeftFold(Nez.FoldTree e) {
//		W(e.label == null ? "{$" : "{$" + e.label);
//	}
//
//	@Override
//	public void visitNew(Nez.EndTree e) {
//		W("}");
//	}
//
//	@Override
//	public void visitTag(Nez.Tag e) {
//		W("#");
//		W(e.tag.getSymbol());
//	}
//
//	@Override
//	public void visitReplace(Nez.Replace e) {
//		W(StringUtils.quoteString('`', e.value, '`'));
//	}
//
//	@Override
//	public void visitLink(Nez.LinkTree e) {
//		String predicate = "$";
//		if (e.label != null) {
//			predicate += e.label.toString();
//		}
//		visitUnary(predicate + "(", e, ")");
//	}
//
//	@Override
//	public void visitUndefined(Expression e) {
//		W("<");
//		// W(e.getPredicate());
//		for (Expression se : e) {
//			W(" ");
//			visitExpression(se);
//		}
//		W(">");
//	}
//
//	@Override
//	public void visitBlockScope(Nez.BlockScope e) {
//		W("<block ");
//		visitExpression(e.get(0));
//		W(">");
//	}
//
//	@Override
//	public void visitSymbolAction(Nez.SymbolAction e) {
//		W("<symbol ");
//		W(e.tableName);
//		W(" ");
//		visitExpression(e.get(0));
//		W(">");
//	}
//
//	@Override
//	public void visitSymbolMatch(Nez.SymbolMatch e) {
//		W("<match ");
//		W(e.tableName);
//		W(">");
//	};
//
//	@Override
//	public void visitSymbolPredicate(Nez.SymbolPredicate e) {
//		W("<is ");
//		W(e.tableName);
//		W(">");
//	}
//
//	@Override
//	public void visitSymbolExists(Nez.SymbolExists e) {
//		String symbol = e.symbol;
//		W("<exists ");
//		W(e.tableName);
//		if (symbol != null) {
//			W(" ");
//			W("'" + symbol + "'");
//		}
//		W(">");
//	}
//
//	@Override
//	public void visitLocalScope(Nez.LocalScope e) {
//		W("<local ");
//		W(e.tableName);
//		W(" ");
//		visitExpression(e.get(0));
//		W(">");
//	}
//
// }
