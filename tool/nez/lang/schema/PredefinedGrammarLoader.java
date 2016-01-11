package nez.lang.schema;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import nez.lang.Expression;
import nez.lang.Expressions;
import nez.lang.Grammar;
import nez.lang.NonTerminal;
import nez.util.UList;
import nez.util.Verbose;

public abstract class PredefinedGrammarLoader extends Expressions {
	Grammar grammar;

	public PredefinedGrammarLoader(Grammar grammar, String start) {
		this.grammar = grammar;
		load(start);
	}

	public final void load(String start) {
		Class<?> c = this.getClass();
		Method startMethod = null;
		try {
			startMethod = c.getMethod("p" + start);
			addProduction(start, startMethod);
		} catch (NoSuchMethodException e2) {
			Verbose.println(e2.toString());
		} catch (SecurityException e2) {
			Verbose.traceException(e2);
		}
		for (Method m : c.getDeclaredMethods()) {
			if (m.getReturnType() == Expression.class && m.getParameterTypes().length == 0) {
				String name = m.getName();
				if (name.startsWith("p")) {
					name = name.substring(1);
				}
				addProduction(name, m);
			}
		}
	}

	private void addProduction(String name, Method m) {
		try {
			Expression e = (Expression) m.invoke(this);
			grammar.addProduction(e.getSourceLocation(), name, e);
		} catch (IllegalAccessException e1) {
			Verbose.traceException(e1);
		} catch (IllegalArgumentException e1) {
			Verbose.traceException(e1);
		} catch (InvocationTargetException e1) {
			Verbose.traceException(e1);
		}
	}

	protected final NonTerminal _NonTerminal(String nonterm) {
		return Expressions.newNonTerminal(null, grammar, nonterm);
	}

	protected final Expression newChoice(Expression... seq) {
		UList<Expression> l = new UList<>(new Expression[8]);
		for (Expression p : seq) {
			Expressions.addChoice(l, p);
		}
		return newChoice(l);
	}

}
