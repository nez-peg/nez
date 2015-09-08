package nez;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.GrammarFile;
import nez.lang.expr.ByteMap;
import nez.lang.expr.ExpressionCommons;
import nez.main.Verbose;
import nez.util.UList;

public class ParserCombinator {

	protected GrammarFile file = null;

	public final GrammarFile load() {
		if (this.file == null) {
			Class<?> c = this.getClass();
			file = GrammarFile.newGrammarFile(c.getName(), NezOption.newDefaultOption());
			if (file.isEmpty()) {
				load(file);
			}
		}
		return file;
	}

	public final GrammarFile load(GrammarFile file) {
		Class<?> c = this.getClass();
		for (Method m : c.getDeclaredMethods()) {
			if (m.getReturnType() == Expression.class && m.getParameterTypes().length == 0) {
				String name = m.getName();
				if (name.startsWith("p")) {
					name = name.substring(1);
				}
				try {
					Expression e = (Expression) m.invoke(this);
					file.defineProduction(e.getSourcePosition(), name, e);
				} catch (IllegalAccessException e1) {
					Verbose.traceException(e1);
				} catch (IllegalArgumentException e1) {
					Verbose.traceException(e1);
				} catch (InvocationTargetException e1) {
					Verbose.traceException(e1);
				}
			}
		}
		file.verify();
		return file;
	}

	public final Grammar newGrammar(String name) {
		return this.load().newGrammar(name);
	}

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
		return ExpressionCommons.newNonTerminal(src(), this.file, name);
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
		boolean[] b = ByteMap.newMap(false);
		boolean binary = false;
		for (int c : chars) {
			b[c] = true;
			if (c == 0) {
				binary = true;
			}
		}
		return ExpressionCommons.newByteMap(src(), binary, b);
	}

	protected final Expression ByteChar(int byteChar) {
		return ExpressionCommons.newByteChar(src(), false, byteChar);
	}

	protected final Expression AnyChar() {
		return ExpressionCommons.newAnyChar(src(), false);
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
		return ExpressionCommons.newSequence(src(), l);
	}

	protected final Expression Choice(Expression... elist) {
		UList<Expression> l = new UList<Expression>(new Expression[8]);
		for (Expression e : elist) {
			ExpressionCommons.addChoice(l, e);
		}
		return ExpressionCommons.newChoice(src(), l);
	}

	protected final Expression Option(Expression... e) {
		return ExpressionCommons.newOption(src(), Sequence(e));
	}

	protected final Expression Option(String t) {
		return ExpressionCommons.newOption(src(), t(t));
	}

	protected final Expression ZeroMore(Expression... e) {
		return ExpressionCommons.newRepetition(src(), Sequence(e));
	}

	protected final Expression OneMore(Expression... e) {
		return ExpressionCommons.newRepetition1(src(), Sequence(e));
	}

	protected final Expression Not(String t) {
		return ExpressionCommons.newNot(src(), ExpressionCommons.newString(src(), t));
	}

	protected final Expression Not(Expression... e) {
		return ExpressionCommons.newNot(src(), Sequence(e));
	}

	protected final Expression And(Expression... e) {
		return ExpressionCommons.newAnd(src(), Sequence(e));
	}

	protected final Expression NCapture(int shift) {
		return ExpressionCommons.newNew(src(), false, null, shift);
	}

	protected final Expression LCapture(int shift, String label) {
		return ExpressionCommons.newNew(src(), true, label == null ? null : Tag.tag(label), shift);
	}

	protected final Expression Capture(int shift) {
		return ExpressionCommons.newCapture(src(), shift);
	}

	protected final Expression New(Expression... e) {
		return ExpressionCommons.newNew(src(), false, null, Sequence(e));
	}

	protected final Expression LeftFoldOption(String label, Expression... e) {
		return ExpressionCommons.newLeftFoldOption(src(), label == null ? null : Tag.tag(label), Sequence(e));
	}

	protected final Expression LeftFoldZeroMore(String label, Expression... e) {
		return ExpressionCommons.newLeftFoldRepetition(src(), label == null ? null : Tag.tag(label), Sequence(e));
	}

	protected final Expression LeftFoldOneMore(String label, Expression... e) {
		return ExpressionCommons.newLeftFoldRepetition1(src(), label == null ? null : Tag.tag(label), Sequence(e));
	}

	protected Expression Link(String label, Expression e) {
		return ExpressionCommons.newLink(src(), label == null ? null : Tag.tag(label), e);
	}

	protected Expression Link(String label, String nonTerminal) {
		return ExpressionCommons.newLink(src(), label == null ? null : Tag.tag(label), P(nonTerminal));
	}

	protected Expression OptionalLink(String label, String nonTerminal) {
		return ExpressionCommons.newOption(src(), ExpressionCommons.newLink(src(), label == null ? null : Tag.tag(label), P(nonTerminal)));
	}

	protected Expression Msg(String label, String msg) {
		return ExpressionCommons.newLink(src(), Tag.tag(label), New(Replace(msg)));
	}

	// protected final Expression Tag(Tag t) {
	// return GrammarFactory.newTagging(src(), t);
	// }

	protected final Expression Tag(String tag) {
		return ExpressionCommons.newTagging(src(), Tag.tag(tag));
	}

	protected final Expression Replace(char c) {
		return ExpressionCommons.newReplace(src(), String.valueOf(c));
	}

	protected final Expression Replace(String value) {
		return ExpressionCommons.newReplace(src(), value);
	}

}
