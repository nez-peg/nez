package nez;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.lang.ByteMap;
import nez.lang.Expression;
import nez.lang.Factory;
import nez.lang.Grammar;
import nez.lang.GrammarChecker;
import nez.lang.NameSpace;
import nez.main.Verbose;
import nez.util.UList;

public class ParserCombinator {

	protected NameSpace ns = null;
		
	public final NameSpace load(GrammarChecker checker) {
		if(this.ns == null) {
			Class<?> c = this.getClass();
			this.ns = NameSpace.newNameSpace(c.getName());
			for(Method m : c.getDeclaredMethods()) {
				if(m.getReturnType() == Expression.class && m.getParameterTypes().length == 0) {
					String name = m.getName();
					if(name.startsWith("p")) {
						name = name.substring(1);
					}
//					if(name.equals("SPACING")) {
//						name = "_";
//					}
					try {
						Expression e = (Expression)m.invoke(this);
						ns.defineProduction(e.getSourcePosition(), name, e);
					} catch (IllegalAccessException e1) {
						Verbose.traceException(e1);
					} catch (IllegalArgumentException e1) {
						Verbose.traceException(e1);
					} catch (InvocationTargetException e1) {
						Verbose.traceException(e1);
					}
				}
			}
			if(checker != null) {
				checker.verify(ns);
			}
		}
		return ns;
	}

	public final NameSpace load() {
		return this.load(new GrammarChecker());
	}

	public final Grammar newGrammar(String name) {
		return this.load(new GrammarChecker()).newGrammar(name);
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
		}
		return new JavaSourcePosition(stacks[2]);
	}
	
	protected final Expression P(String name) {
		return Factory.newNonTerminal(src(), this.ns, name);
	}

	protected final Expression t(char c) {
		return Factory.newString(src(), String.valueOf(c));
	}

	protected final Expression t(String token) {
		return Factory.newString(src(), token);
	}

	protected final Expression c(String text) {
		return Factory.newCharSet(src(), text);
	}

	protected final Expression c(int ... chars) {
		boolean[] b = ByteMap.newMap(false);
		for(int c : chars) {
			b[c] = true;
		}
		return Factory.newByteMap(src(), b);
	}

	protected final Expression ByteChar(int byteChar) {
		return Factory.newByteChar(src(), byteChar);
	}

	protected final Expression AnyChar() {
		return Factory.newAnyChar(src());
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
			Factory.addSequence(l, e);
		}
		return Factory.newSequence(src(), l);
	}

	protected final Expression Choice(Expression ... elist) {
		UList<Expression> l = new UList<Expression>(new Expression[8]);
		for(Expression e : elist) {
			Factory.addChoice(l, e);
		}
		return Factory.newChoice(src(), l);
	}
	
	protected final Expression Option(Expression ... e) {
		return Factory.newOption(src(), Sequence(e));
	}

	protected final Expression Option(String t) {
		return Factory.newOption(src(), t(t));
	}

	protected final Expression ZeroMore(Expression ... e) {
		return Factory.newRepetition(src(), Sequence(e));
	}
	
	protected final Expression OneMore(Expression ... e) {
		return Factory.newRepetition1(src(), Sequence(e));
	}

	protected final Expression Not(String t) {
		return Factory.newNot(src(), Factory.newString(src(), t));
	}

	protected final Expression Not(Expression ... e) {
		return Factory.newNot(src(), Sequence(e));
	}

	protected final Expression And(Expression ... e) {
		return Factory.newAnd(src(), Sequence(e));
	}

	protected final Expression NCapture(int shift) {
		return Factory.newNew(src(), false, shift);
	}

	protected final Expression LCapture(int shift) {
		return Factory.newNew(src(), true, shift);
	}

	protected final Expression Capture(int shift) {
		return Factory.newCapture(src(), shift);
	}

	protected final Expression New(Expression ... e) {
		return Factory.newNew(src(), false, Sequence(e));
	}

	protected final Expression LeftNewOption(Expression ... e) {
		return Factory.newLeftNewOption(src(), Sequence(e));
	}

	protected final Expression LeftNewZeroMore(Expression ... e) {
		return Factory.newLeftNewRepetition(src(), Sequence(e));
	}

	protected final Expression LeftNewOneMore(Expression ... e) {
		return Factory.newLeftNewRepetition1(src(), Sequence(e));
	}
		
	protected Expression Link(String nonterminal) {
		return Factory.newLink(src(), P(nonterminal), -1);
	}

	protected Expression Link(Expression ... e) {
		return Factory.newLink(src(), Sequence(e), -1);
	}
	
	protected Expression Link(int index, Expression ... e) {
		return Factory.newLink(src(), Sequence(e), index);
	}

	protected Expression Link(int index, String nonterminal) {
		return Factory.newLink(src(), P(nonterminal), index);
	}

	protected final Expression Tag(Tag t) {
		return Factory.newTagging(src(), t);
	}

	protected final Expression Tagging(String tag) {
		return Factory.newTagging(src(), Tag.tag(tag));
	}

	protected final Expression Replace(char c) {
		return Factory.newReplace(src(), String.valueOf(c));
	}

	protected final Expression Replace(String value) {
		return Factory.newReplace(src(), value);
	}

	
	
}
