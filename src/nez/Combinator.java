package nez;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import nez.ast.SourcePosition;
import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.expr.Cset;
import nez.lang.expr.ExpressionCommons;
import nez.main.Verbose;
import nez.util.UList;

public class Combinator extends Grammar {

	public Combinator(String ns, String start) {
		super(ns);
		load(start);
	}

	//
	// public final GrammarFile load() {
	// if (this.file == null) {
	// Class<?> c = this.getClass();
	// file = GrammarFile.newGrammarFile(c.getName(),
	// NezOption.newDefaultOption());
	// if (file.isEmpty()) {
	// load(file);
	// }
	// }
	// return file;
	// }

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
			this.newProduction(e.getSourcePosition(), 0, name, e);
		} catch (IllegalAccessException e1) {
			Verbose.traceException(e1);
		} catch (IllegalArgumentException e1) {
			Verbose.traceException(e1);
		} catch (InvocationTargetException e1) {
			Verbose.traceException(e1);
		}
	}

	// public final GrammarFile load(GrammarFile file) {
	// Class<?> c = this.getClass();
	// for (Method m : c.getDeclaredMethods()) {
	// if (m.getReturnType() == Expression.class && m.getParameterTypes().length
	// == 0) {
	// String name = m.getName();
	// if (name.startsWith("p")) {
	// name = name.substring(1);
	// }
	// try {
	// Expression e = (Expression) m.invoke(this);
	// file.addProduction(e.getSourcePosition(), name, e);
	// } catch (IllegalAccessException e1) {
	// Verbose.traceException(e1);
	// } catch (IllegalArgumentException e1) {
	// Verbose.traceException(e1);
	// } catch (InvocationTargetException e1) {
	// Verbose.traceException(e1);
	// }
	// }
	// }
	// return file;
	// }

	private SourcePosition src() {
		Exception e = new Exception();
		StackTraceElement[] stacks = e.getStackTrace();
		// System.out.println("^0 " + stacks[0]);
		// System.out.println("^1 " + stacks[1]);
		// System.out.println("^2 " + stacks[2]);
		class JavaSourcePosition implements SourcePosition {
			StackTraceElement e;

			JavaSourcePosition(StackTraceElement e) {
				this.e = e;
			}

			@Override
			public String formatSourceMessage(String type, String msg) {
				return e + " " + type + " " + msg;
			}

			@Override
			public String formatDebugSourceMessage(String msg) {
				return e + " " + msg;
			}
		}
		return new JavaSourcePosition(stacks[2]);
	}

	protected final Expression P(String name) {
		return ExpressionCommons.newNonTerminal(src(), this, name);
	}

	protected final Expression t(char c) {
		return ExpressionCommons.newString(src(), String.valueOf(c));
	}

	protected final Expression t(String token) {
		return ExpressionCommons.newString(src(), token);
	}

	protected final Expression c(String text) {
		return ExpressionCommons.newCharSet(src(), text);
	}

	protected final Expression c(int... chars) {
		boolean[] b = Cset.newMap(false);
		boolean binary = false;
		for (int c : chars) {
			b[c] = true;
			if (c == 0) {
				binary = true;
			}
		}
		return ExpressionCommons.newCset(src(), binary, b);
	}

	protected final Expression ByteChar(int byteChar) {
		return ExpressionCommons.newCbyte(src(), false, byteChar);
	}

	protected final Expression AnyChar() {
		return ExpressionCommons.newCany(src(), false);
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
			ExpressionCommons.addSequence(l, e);
		}
		return ExpressionCommons.newPsequence(src(), l);
	}

	protected final Expression Choice(Expression... elist) {
		UList<Expression> l = new UList<Expression>(new Expression[8]);
		for (Expression e : elist) {
			ExpressionCommons.addChoice(l, e);
		}
		return ExpressionCommons.newPchoice(src(), l);
	}

	protected final Expression Option(Expression... e) {
		return ExpressionCommons.newPoption(src(), Sequence(e));
	}

	protected final Expression Option(String t) {
		return ExpressionCommons.newPoption(src(), t(t));
	}

	protected final Expression ZeroMore(Expression... e) {
		return ExpressionCommons.newPzero(src(), Sequence(e));
	}

	protected final Expression OneMore(Expression... e) {
		return ExpressionCommons.newPone(src(), Sequence(e));
	}

	protected final Expression Not(String t) {
		return ExpressionCommons.newPnot(src(), ExpressionCommons.newString(src(), t));
	}

	protected final Expression Not(Expression... e) {
		return ExpressionCommons.newPnot(src(), Sequence(e));
	}

	protected final Expression And(Expression... e) {
		return ExpressionCommons.newPand(src(), Sequence(e));
	}

	protected final Expression NCapture(int shift) {
		return ExpressionCommons.newTnew(src(), shift);
	}

	protected final Expression LCapture(int shift, String label) {
		return ExpressionCommons.newTlfold(src(), label == null ? null : Symbol.tag(label), shift);
	}

	protected final Expression Capture(int shift) {
		return ExpressionCommons.newTcapture(src(), shift);
	}

	protected final Expression New(Expression... e) {
		return ExpressionCommons.newNewCapture(src(), false, null, Sequence(e));
	}

	protected final Expression LeftFoldOption(String label, Expression... e) {
		return ExpressionCommons.newLeftFoldOption(src(), label == null ? null : Symbol.tag(label), Sequence(e));
	}

	protected final Expression LeftFoldZeroMore(String label, Expression... e) {
		return ExpressionCommons.newLeftFoldRepetition(src(), label == null ? null : Symbol.tag(label), Sequence(e));
	}

	protected final Expression LeftFoldOneMore(String label, Expression... e) {
		return ExpressionCommons.newLeftFoldRepetition1(src(), label == null ? null : Symbol.tag(label), Sequence(e));
	}

	protected Expression Link(String label, Expression e) {
		return ExpressionCommons.newTlink(src(), label == null ? null : Symbol.tag(label), e);
	}

	protected Expression Link(String label, String nonTerminal) {
		return ExpressionCommons.newTlink(src(), label == null ? null : Symbol.tag(label), P(nonTerminal));
	}

	protected Expression Msg(String label, String msg) {
		return ExpressionCommons.newTlink(src(), Symbol.tag(label), New(Replace(msg)));
	}

	// protected final Expression Tag(Tag t) {
	// return GrammarFactory.newTagging(src(), t);
	// }

	protected final Expression Tag(String tag) {
		return ExpressionCommons.newTtag(src(), Symbol.tag(tag));
	}

	protected final Expression Replace(char c) {
		return ExpressionCommons.newTreplace(src(), String.valueOf(c));
	}

	protected final Expression Replace(String value) {
		return ExpressionCommons.newTreplace(src(), value);
	}

}
