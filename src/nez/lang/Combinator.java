package nez.lang;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import nez.ast.Source;
import nez.ast.SourceLocation;
import nez.ast.Symbol;
import nez.util.UList;
import nez.util.Verbose;

public class Combinator {

	public Combinator() {
	}

	protected Grammar g;

	public final Grammar load(Grammar g, String start) {
		this.g = g;
		Class<?> c = this.getClass();
		Method startMethod = null;
		if (start != null) {
			try {
				startMethod = c.getMethod("p" + start);
				addProduction(start, startMethod);
			} catch (NoSuchMethodException | SecurityException e2) {
				Verbose.println(e2.toString());
			}
		}
		for (Method m : c.getDeclaredMethods()) {
			if (m == startMethod) {
				continue;
			}
			if (m.getReturnType() == Expression.class && m.getParameterTypes().length == 0) {
				String name = m.getName();
				if (name.startsWith("p")) {
					name = name.substring(1);
				}
				addProduction(name, m);
			}
		}
		return g;
	}

	private void addProduction(String name, Method m) {
		try {
			Expression e = (Expression) m.invoke(this);
			g.newProduction(e.getSourceLocation(), 0, name, e);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
			Verbose.traceException(e1);
		}
	}

	private SourceLocation src() {
		Exception e = new Exception();
		StackTraceElement[] stacks = e.getStackTrace();
		// System.out.println("^0 " + stacks[0]);
		// System.out.println("^1 " + stacks[1]);
		// System.out.println("^2 " + stacks[2]);
		class JavaSourcePosition implements SourceLocation {
			StackTraceElement e;

			JavaSourcePosition(StackTraceElement e) {
				this.e = e;
			}

			@Override
			public String formatSourceMessage(String type, String msg) {
				return e + " " + type + " " + msg;
			}

			@Override
			public Source getSource() {
				return null;
			}

			@Override
			public long getSourcePosition() {
				return 0;
			}

			@Override
			public int getLineNum() {
				return 0;
			}

			@Override
			public int getColumn() {
				return 0;
			}
		}
		return new JavaSourcePosition(stacks[2]);
	}

	protected final Expression P(String name) {
		return Expressions.newNonTerminal(src(), g, name);
	}

	protected final Expression t(char c) {
		return Expressions.newExpression(src(), String.valueOf(c));
	}

	protected final Expression t(String token) {
		return Expressions.newExpression(src(), token);
	}

	protected final Expression c(String text) {
		return Expressions.newCharSet(src(), text);
	}

	protected final Expression c(int... chars) {
		boolean[] b = Bytes.newMap(false);
		boolean binary = false;
		for (int c : chars) {
			b[c] = true;
			if (c == 0) {
				binary = true;
			}
		}
		return Expressions.newByteSet(src(), b);
	}

	protected final Expression ByteChar(int byteChar) {
		return Expressions.newByte(src(), byteChar);
	}

	protected final Expression AnyChar() {
		return Expressions.newAny(src());
	}

	protected final Expression NotAny(String token) {
		return Sequence(Not(t(token)), AnyChar());
	}

	protected final Expression NotAny(Expression... e) {
		return Sequence(Not(Sequence(e)), AnyChar());
	}

	protected final Expression Sequence(Expression... elist) {
		UList<Expression> l = new UList<Expression>(new Expression[8]);
		for (Expression e : elist) {
			Expressions.addSequence(l, e);
		}
		return Expressions.newPair(l);
	}

	protected final Expression Choice(Expression... elist) {
		UList<Expression> l = new UList<Expression>(new Expression[8]);
		for (Expression e : elist) {
			Expressions.addChoice(l, e);
		}
		return Expressions.newChoice(l);
	}

	protected final Expression Option(Expression... e) {
		return Expressions.newOption(src(), Sequence(e));
	}

	protected final Expression Option(String t) {
		return Expressions.newOption(src(), t(t));
	}

	protected final Expression ZeroMore(Expression... e) {
		return Expressions.newZeroMore(src(), Sequence(e));
	}

	protected final Expression OneMore(Expression... e) {
		return Expressions.newOneMore(src(), Sequence(e));
	}

	protected final Expression Not(String t) {
		return Expressions.newNot(src(), Expressions.newExpression(src(), t));
	}

	protected final Expression Not(Expression... e) {
		return Expressions.newNot(src(), Sequence(e));
	}

	protected final Expression And(Expression... e) {
		return Expressions.newAnd(src(), Sequence(e));
	}

	protected final Expression NCapture(int shift) {
		return Expressions.newBeginTree(src(), shift);
	}

	protected final Expression LCapture(int shift, String label) {
		return Expressions.newFoldTree(src(), toSymbol(label), shift);
	}

	protected final Expression Capture(int shift) {
		return Expressions.newEndTree(src(), shift);
	}

	protected final Expression New(Expression... e) {
		return Expressions.newTree(src(), false, null, Sequence(e));
	}

	protected final Expression LeftFoldOption(String label, Expression... e) {
		return Expressions.newLeftFoldOption(src(), toSymbol(label), Sequence(e));
	}

	protected final Expression LeftFoldZeroMore(String label, Expression... e) {
		return Expressions.newLeftFoldRepetition(src(), toSymbol(label), Sequence(e));
	}

	protected final Expression LeftFoldOneMore(String label, Expression... e) {
		return Expressions.newLeftFoldRepetition1(src(), toSymbol(label), Sequence(e));
	}

	protected Expression Link(String label, Expression e) {
		return Expressions.newLinkTree(src(), toSymbol(label), e);
	}

	protected Expression Link(String label, String nonTerminal) {
		return Expressions.newLinkTree(src(), toSymbol(label), P(nonTerminal));
	}

	private Symbol toSymbol(String label) {
		if (label == null) {
			return null;
		}
		if (label.startsWith("$")) {
			return Symbol.tag(label.substring(1));
		}
		return Symbol.tag(label);
	}

	protected Expression Msg(String label, String msg) {
		return Expressions.newLinkTree(src(), Symbol.tag(label), New(Replace(msg)));
	}

	// protected final Expression Tag(Tag t) {
	// return GrammarFactory.newTagging(src(), t);
	// }

	protected final Expression Tag(String tag) {
		return Expressions.newTag(src(), Symbol.tag(tag));
	}

	protected final Expression Replace(char c) {
		return Expressions.newReplace(src(), String.valueOf(c));
	}

	protected final Expression Replace(String value) {
		return Expressions.newReplace(src(), value);
	}

}
