package nez;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.expr.Expression;
import nez.expr.Factory;
import nez.expr.GrammarChecker;
import nez.util.UList;

public class ParserCombinator {
	NameSpace grammar;
	protected ParserCombinator(NameSpace grammar) {
		this.grammar = grammar;
	}
	
	public final NameSpace load(GrammarChecker checker) {
		Class<?> c = this.getClass();
		for(Method m : c.getDeclaredMethods()) {
			if(m.getReturnType() == Expression.class && m.getParameterTypes().length == 0) {
				String name = m.getName();
				//System.out.println("rule name: " + name);
				if(name.equals("SPACING")) {
					name = "_";
				}
				try {
					Expression e = (Expression)m.invoke(this);
					grammar.defineProduction(e.getSourcePosition(), name, e);
				} catch (IllegalAccessException e1) {
					e1.printStackTrace();
				} catch (IllegalArgumentException e1) {
					e1.printStackTrace();
				} catch (InvocationTargetException e1) {
					e1.printStackTrace();
				}
			}
		}
		if(checker != null) {
			checker.verify(grammar);
		}
		return grammar;
	}

	public final NameSpace load() {
		return this.load(new GrammarChecker());
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
	
	protected final Expression P(String ruleName) {
		return Factory.newNonTerminal(src(), this.grammar, ruleName);
	}

	protected final Expression t(String token) {
		return Factory.newString(src(), token);
	}

	protected final Expression c(String text) {
		return Factory.newCharSet(src(), text);
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

	protected final Expression Not(Expression ... e) {
		return Factory.newNot(src(), Sequence(e));
	}

	protected final Expression And(Expression ... e) {
		return Factory.newAnd(src(), Sequence(e));
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
	
//	protected final Expression NewClosure(Expression ... elist) {
//		UList<Expression> l = new UList<Expression>(new Expression[8]);
//		for(Expression e : elist) {
//			Factory.addSequence(l, e);
//		}
//		return Factory.newNew(src(), l);
//	}
//	
//	protected Expression LeftNewClosure(Expression ... elist) {
//		UList<Expression> l = new UList<Expression>(new Expression[8]);
//		for(Expression e : elist) {
//			Factory.addSequence(l, e);
//		}
//		return Factory.newLeftNew(src(), l);
//	}
	
	protected Expression Link(Expression ... e) {
		return Factory.newLink(src(), Sequence(e), -1);
	}
	
	protected Expression Link(int index, Expression ... e) {
		return Factory.newLink(src(), Sequence(e), index);
	}

	protected final Expression Tag(Tag t) {
		return Factory.newTagging(src(), t);
	}

	protected final Expression Tag(String tag) {
		return Factory.newTagging(src(), Tag.tag(tag));
	}

}
