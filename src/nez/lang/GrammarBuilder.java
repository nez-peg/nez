package nez.lang;

import nez.ast.Symbol;
import nez.lang.expr.Expressions;
import nez.util.UList;

public class GrammarBuilder {
	Grammar g;

	public GrammarBuilder(Grammar g) {
		this.g = g;
	}

	public void sampleProduction() {
		define("A", "AB", //
				OR, S("B", OR, "C"), //
				OR, "ccc");
	}

	public Expression sampleExpression() {
		return S("AB", //
				OR, S("B", OR, "C"), //
				OR, "ccc");
	}

	protected final Expression ANY = Expressions.newCany(null, false);
	protected final Expression OR = null;

	protected void define(String name, Object... args) {
		g.newProduction(name, S(args));
	}

	protected final Expression S(Object... args) {
		if (args.length == 0) {
			return Expressions.newEmpty(null);
		}
		if (args.length == 1) {
			return E(args[0]);
		}
		UList<Expression> l = new UList<>(new Expression[args.length]);
		UList<Expression> choice = null;
		for (int i = 0; i < args.length; i++) {
			if (args[i] == null) {
				if (choice == null) {
					choice = new UList<>(new Expression[args.length]);
				}
				Expressions.addChoice(choice, Expressions.newPair(null, l));
				l.clear(0);
			} else {
				Expressions.addSequence(l, E(args[i]));
			}
		}
		if (choice != null) {
			Expressions.addChoice(choice, Expressions.newPair(null, l));
			return Expressions.newPchoice(null, choice);
		}
		return Expressions.newPair(null, l);
	}

	protected final Expression E(Object value) {
		if (value instanceof Expression) {
			return (Expression) value;
		}
		if (value instanceof Symbol) {
			return Expressions.newTtag(null, (Symbol) value);
		}
		return Expressions.newString(null, value.toString());
	}

	protected final Expression P(String name) {
		return g.newNonTerminal(null, name);
	}

	protected final Expression C(String t) {
		return Expressions.newCharSet(null, t);
	}

	protected final Expression R0(Object... args) {
		return Expressions.newPzero(null, S(args));
	}

	protected final Expression R1(Object... args) {
		return Expressions.newPone(null, S(args));
	}

	protected final Expression Opt(Object... args) {
		return Expressions.newPoption(null, S(args));
	}

	protected final Expression And(Object... args) {
		return Expressions.newPand(null, S(args));
	}

	protected final Expression Not(Object... args) {
		return Expressions.newPnot(null, S(args));
	}

	protected final Expression New(Object... args) {
		return S(Expressions.newTnew(null, 0), S(args), Expressions.newTcapture(null, 0));
	}

	protected final Expression Set(String name, Object... args) {
		return Expressions.newTlink(null, Symbol.tag(name), S(args));
	}

	protected final Expression Add(Object... args) {
		return Expressions.newTlink(null, null, S(args));
	}

	protected final Expression Tag(String t) {
		return Expressions.newTtag(null, Symbol.tag(t));
	}

	protected final Expression Val(String t) {
		return Expressions.newTreplace(null, t);
	}

}
