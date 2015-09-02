package nez.generator;

import nez.lang.And;
import nez.lang.AnyChar;
import nez.lang.Block;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.Capture;
import nez.lang.Choice;
import nez.lang.DefSymbol;
import nez.lang.ExistsSymbol;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.IsSymbol;
import nez.lang.Link;
import nez.lang.New;
import nez.lang.NonTerminal;
import nez.lang.Not;
import nez.lang.Option;
import nez.lang.Production;
import nez.lang.Repetition;
import nez.lang.Repetition1;
import nez.lang.Replace;
import nez.lang.Tagging;
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
	public void visitByteChar(ByteChar e) {
		W(StringUtils.stringfyCharacter(e.byteChar));
	}

	@Override
	public void visitByteMap(ByteMap e) {
		W(StringUtils.stringfyCharacterClass(e.byteMap));
	}

	@Override
	public void visitAnyChar(AnyChar e) {
		W(".");
	}

	@Override
	public void visitOption(Option e) {
		Unary(null, e, "?");
	}

	@Override
	public void visitRepetition(Repetition e) {
		Unary(null, e, "*");
	}

	@Override
	public void visitRepetition1(Repetition1 e) {
		Unary(null, e, "+");
	}

	@Override
	public void visitAnd(And e) {
		Unary("&", e, null);
	}

	@Override
	public void visitNot(Not e) {
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
	public void visitNew(New e) {
		W(e.leftFold ? "{@" : "{");
	}

	@Override
	public void visitCapture(Capture e) {
		W("}");
	}

	@Override
	public void visitTagging(Tagging e) {
		W("#");
		W(e.tag.getName());
	}

	@Override
	public void visitReplace(Replace e) {
		W(StringUtils.quoteString('`', e.value, '`'));
	}

	@Override
	public void visitLink(Link e) {
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
	public void visitBlock(Block e) {
		W("<block ");
		visitExpression(e.get(0));
		W(">");
	}

	@Override
	public void visitDefSymbol(DefSymbol e) {
		W("<def ");
		W(e.getTableName());
		W(" ");
		visitExpression(e.get(0));
		W(">");
	}

	@Override
	public void visitIsSymbol(IsSymbol e) {
		W("<is ");
		W(e.getTableName());
		W(">");
	}

	@Override
	public void visitExistsSymbol(ExistsSymbol e) {
		String symbol = e.getSymbol();
		W("<exists ");
		W(e.getTableName());
		if (symbol != null) {
			W(" ");
			W("'" + symbol + "'");
		}
		W(">");
	}

}
