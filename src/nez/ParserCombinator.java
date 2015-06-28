package nez;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.lang.ByteMap;
import nez.lang.Expression;
import nez.lang.GrammarFactory;
import nez.lang.Grammar;
import nez.lang.GrammarChecker;
import nez.lang.GrammarFile;
import nez.main.Verbose;
import nez.util.UList;

public class ParserCombinator {

	protected GrammarFile gfile = null;
		
	public final GrammarFile load() {
		if(this.gfile == null) {
			Class<?> c = this.getClass();
			gfile = GrammarFile.newGrammarFile(c.getName(), NezOption.newDefaultOption());
			if(gfile.isEmpty()) {
				for(Method m : c.getDeclaredMethods()) {
					if(m.getReturnType() == Expression.class && m.getParameterTypes().length == 0) {
						String name = m.getName();
						if(name.startsWith("p")) {
							name = name.substring(1);
						}
						try {
							Expression e = (Expression)m.invoke(this);
							gfile.defineProduction(e.getSourcePosition(), name, e);
						} catch (IllegalAccessException e1) {
							Verbose.traceException(e1);
						} catch (IllegalArgumentException e1) {
							Verbose.traceException(e1);
						} catch (InvocationTargetException e1) {
							Verbose.traceException(e1);
						}
					}
				}
				gfile.verify();
			}
		}
		return gfile;
	}

	public final Grammar newGrammar(String name) {
		return this.load().newGrammar(name);
	}

	private SourcePosition src() {
		Exception e =  new Exception();
		StackTraceElement[] stacks = e.getStackTrace();
//		System.out.println("^0 " + stacks[0]);
//		System.out.println("^1 " + stacks[1]);
//		System.out.println("^2 " + stacks[2]);
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
		return GrammarFactory.newNonTerminal(src(), this.gfile, name);
	}

	protected final Expression t(char c) {
		return GrammarFactory.newString(src(), String.valueOf(c));
	}

	protected final Expression t(String token) {
		return GrammarFactory.newString(src(), token);
	}

	protected final Expression c(String text) {
		return GrammarFactory.newCharSet(src(), text);
	}

	protected final Expression c(int ... chars) {
		boolean[] b = ByteMap.newMap(false);
		boolean binary = false;
		for(int c : chars) {
			b[c] = true;
			if(c == 0) {
				binary = true;
			}
		}
		return GrammarFactory.newByteMap(src(), binary, b);
	}

	protected final Expression ByteChar(int byteChar) {
		return GrammarFactory.newByteChar(src(), false, byteChar);
	}

	protected final Expression AnyChar() {
		return GrammarFactory.newAnyChar(src(), false);
	}
	
	protected final Expression NotAny(String token) {
		return Sequence(Not(t(token)), AnyChar());
	}

	protected final Expression NotAny(Expression ... e) {
		return Sequence(Not(Sequence(e)), AnyChar());
	}
	
	protected final Expression Sequence(Expression ... elist) {
		UList<Expression> l = new UList<Expression>(new Expression[8]);
		for(Expression e : elist) {
			GrammarFactory.addSequence(l, e);
		}
		return GrammarFactory.newSequence(src(), l);
	}

	protected final Expression Choice(Expression ... elist) {
		UList<Expression> l = new UList<Expression>(new Expression[8]);
		for(Expression e : elist) {
			GrammarFactory.addChoice(l, e);
		}
		return GrammarFactory.newChoice(src(), l);
	}
	
	protected final Expression Option(Expression ... e) {
		return GrammarFactory.newOption(src(), Sequence(e));
	}

	protected final Expression Option(String t) {
		return GrammarFactory.newOption(src(), t(t));
	}

	protected final Expression ZeroMore(Expression ... e) {
		return GrammarFactory.newRepetition(src(), Sequence(e));
	}
	
	protected final Expression OneMore(Expression ... e) {
		return GrammarFactory.newRepetition1(src(), Sequence(e));
	}

	protected final Expression Not(String t) {
		return GrammarFactory.newNot(src(), GrammarFactory.newString(src(), t));
	}

	protected final Expression Not(Expression ... e) {
		return GrammarFactory.newNot(src(), Sequence(e));
	}

	protected final Expression And(Expression ... e) {
		return GrammarFactory.newAnd(src(), Sequence(e));
	}

	protected final Expression NCapture(int shift) {
		return GrammarFactory.newNew(src(), false, shift);
	}

	protected final Expression LCapture(int shift) {
		return GrammarFactory.newNew(src(), true, shift);
	}

	protected final Expression Capture(int shift) {
		return GrammarFactory.newCapture(src(), shift);
	}

	protected final Expression New(Expression ... e) {
		return GrammarFactory.newNew(src(), false, Sequence(e));
	}

	protected final Expression LeftNewOption(Expression ... e) {
		return GrammarFactory.newLeftNewOption(src(), Sequence(e));
	}

	protected final Expression LeftNewZeroMore(Expression ... e) {
		return GrammarFactory.newLeftNewRepetition(src(), Sequence(e));
	}

	protected final Expression LeftNewOneMore(Expression ... e) {
		return GrammarFactory.newLeftNewRepetition1(src(), Sequence(e));
	}
		
	protected Expression Link(String nonterminal) {
		return GrammarFactory.newLink(src(), P(nonterminal), -1);
	}

	protected Expression Link(Expression ... e) {
		return GrammarFactory.newLink(src(), Sequence(e), -1);
	}
	
	protected Expression Link(int index, Expression ... e) {
		return GrammarFactory.newLink(src(), Sequence(e), index);
	}

	protected Expression Link(int index, String nonterminal) {
		return GrammarFactory.newLink(src(), P(nonterminal), index);
	}

	protected final Expression Tag(Tag t) {
		return GrammarFactory.newTagging(src(), t);
	}

	protected final Expression Tagging(String tag) {
		return GrammarFactory.newTagging(src(), Tag.tag(tag));
	}

	protected final Expression Replace(char c) {
		return GrammarFactory.newReplace(src(), String.valueOf(c));
	}

	protected final Expression Replace(String value) {
		return GrammarFactory.newReplace(src(), value);
	}
	
}
