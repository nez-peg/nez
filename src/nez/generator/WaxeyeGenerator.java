package nez.generator;

import nez.lang.And;
import nez.lang.AnyChar;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.Choice;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Link;
import nez.lang.NonTerminal;
import nez.lang.Not;
import nez.lang.Option;
import nez.lang.Production;
import nez.lang.Repetition;
import nez.lang.Repetition1;
import nez.lang.Replace;
import nez.util.StringUtils;

public class WaxeyeGenerator extends GrammarGenerator {
	@Override
	public String getDesc() {
		return "a Waxeye Grammar";
	}

	public WaxeyeGenerator() {
	}

	// public WaxeyeGenerator(String fileName) {
	// super();
	// }

	@Override
	public void makeHeader(Grammar g) {
		L("# Parsing Expression Grammars for Waxeye");
		L("# Translated from Nez");
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
		L("<- ");
		if (e instanceof Choice) {
			for (int i = 0; i < e.size(); i++) {
				if (i > 0) {
					L("| ");
				}
				visitExpression(e.get(i));
			}
		} else {
			visitExpression(e);
		}
		dec();
	}

	public void visitEmpty(Expression e) {
		W("\' \'");
	}

	public void visitFailure(Expression e) {
		W("!''");
	}

	public void visitNonTerminal(NonTerminal e) {
		W(stringfyName(e.getLocalName()));
	}

	public void visitByteChar(ByteChar e) {
		W(StringUtils.stringfyCharacter(e.byteChar));
	}

	public void visitByteMap(ByteMap e) {
		W(StringUtils.stringfyCharacterClass(e.byteMap));
	}

	public void visitAnyChar(AnyChar e) {
		W(".");
	}

	public void visitOption(Option e) {
		Unary("?", e, null);
	}

	public void visitRepetition(Repetition e) {
		Unary("*", e, null);
	}

	public void visitRepetition1(Repetition1 e) {
		Unary("+", e, null);
	}

	public void visitAnd(And e) {
		Unary("&", e, null);
	}

	public void visitNot(Not e) {
		Unary("!", e, " ");
	}

	protected String _OpenGrouping() {
		return "(";
	}

	protected String _CloseGrouping() {
		return ")";
	};

	public void visitGrouping(Expression e) {
		W(_OpenGrouping());
		visitExpression(e);
		W(_CloseGrouping());
	}

	public void visitChoice(Choice e) {
		for (int i = 0; i < e.size(); i++) {
			if (i > 0) {
				W(" | ");
			}
			visitExpression(e.get(i));
		}
	}

	// public void visitNew(New e) {
	// W(e.lefted ? "{@" : "{");
	// }

	// public void visitCapture(Capture e) {
	// W("}");
	// }

	// public void visitTagging(Tagging e) {
	// W("#");
	// W(e.tag.getName());
	// }

	public void visitReplace(Replace e) {
		W(StringUtils.quoteString('`', e.value, '`'));
	}

	public void visitLink(Link e) {
		// String predicate = "@";
		// if (e.index != -1) {
		// predicate += "[" + e.index + "]";
		// }
		// Unary(predicate, e, null);
		this.visitExpression(e.get(0));
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
