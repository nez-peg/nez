package nez.lang;

import java.util.Arrays;
import java.util.List;

import nez.ast.SourceLocation;
import nez.ast.Symbol;
import nez.util.StringUtils;
import nez.util.UList;

/**
 * This class consists of static utility methods operating an expression.
 * 
 * @author kiki
 *
 */

public abstract class Expressions {

	// ---------------------------------------------------------------------

	public final static UList<Expression> newUList(int size) {
		return new UList<>(new Expression[size]);
	}

	public final static List<Expression> newList(int size) {
		return new UList<>(new Expression[size]);
	}

	public final static void addSequence(List<Expression> l, Expression e) {
		if (e instanceof Nez.Empty) {
			return;
		}
		if (e instanceof Nez.Sequence) {
			for (int i = 0; i < e.size(); i++) {
				addSequence(l, e.get(i));
			}
			return;
		}
		if (e instanceof Nez.Pair) {
			addSequence(l, e.get(0));
			addSequence(l, e.get(1));
			return;
		}
		if (l.size() > 0) {
			Expression prev = l.get(l.size() - 1);
			if (prev instanceof Nez.Fail) {
				return;
			}
		}
		l.add(e);
	}

	public final static void addChoice(List<Expression> l, Expression e) {
		if (e instanceof Nez.Choice) {
			for (int i = 0; i < e.size(); i++) {
				addChoice(l, e.get(i));
			}
			return;
		}
		if (e instanceof Nez.Fail) {
			return;
		}
		if (l.size() > 0) {
			Expression prev = l.get(l.size() - 1);
			if (prev instanceof Nez.Empty) {
				return;
			}
		}
		l.add(e);
	}

	public static void swap(List<Expression> l, int i, int j) {
		Expression e = l.get(i);
		l.set(i, l.get(j));
		l.set(j, e);
	}

	// -----------------------------------------------------------------------

	public final static NonTerminal newNonTerminal(SourceLocation s, Grammar g, String name) {
		return new NonTerminal(s, g, name);
	}

	// Immutable Expressions

	private static Expression emptyExpression = new Nez.Empty();
	private static Expression failExpression = new Nez.Fail();
	private static Expression anyExpression = new Nez.Any();

	/**
	 * Creates an new empty expression, equals to '' in Nez.
	 * 
	 * @return
	 */

	public final static Expression newEmpty() {
		return emptyExpression;
	}

	/**
	 * Creates an new empty expression, equals to '' in Nez.
	 * 
	 * @param s
	 * @return
	 */

	public final static Expression newEmpty(SourceLocation s) {
		Expression e = new Nez.Empty();
		e.setSourceLocation(s);
		return e;
	}

	/**
	 * Creates an always-fail expression, equals to !'' in Nez.
	 * 
	 * @return
	 */

	public final static Expression newFail() {
		return failExpression;
	}

	/**
	 * Creates an always-fail expression, equals to !'' in Nez.
	 * 
	 * @param s
	 * @return
	 */

	public final static Expression newFail(SourceLocation s) {
		Expression e = new Nez.Fail();
		e.setSourceLocation(s);
		return e;
	}

	/* Terminal */

	/**
	 * Creates an any character expression, equals to . in Nez.
	 */

	public final static Expression newAny() {
		return anyExpression;
	}

	/**
	 * Creates an any character expression, equals to . in Nez.
	 * 
	 * @param s
	 * @return
	 */

	public final static Expression newAny(SourceLocation s) {
		Expression e = new Nez.Any();
		e.setSourceLocation(s);
		return e;
	}

	/**
	 * Creates an byte-matching expression, equals to c in Nez
	 * 
	 * @param c
	 * @return
	 */

	public final static Expression newByte(int c) {
		return new Nez.Byte(c & 0xff);
	}

	/**
	 * Creates an byte-matching expression, equals to c in Nez
	 * 
	 * @param s
	 * @param c
	 * @return
	 */

	public final static Expression newByte(SourceLocation s, int c) {
		Expression e = new Nez.Byte(c & 0xff);
		e.setSourceLocation(s);
		return e;
	}

	/**
	 * Creates an byte-set expression, equals to [c] in Nez
	 * 
	 * @param byteMap
	 * @return
	 */

	public static Expression newByteSet(boolean[] byteMap) {
		int byteChar = uniqueByteChar(byteMap);
		if (byteChar != -1) {
			return newByte(byteChar);
		}
		return new Nez.ByteSet(byteMap);
	}

	/**
	 * Creates an byte-set expression, equals to [c] in Nez
	 * 
	 * @param s
	 * @param byteMap
	 * @return
	 */

	public static Expression newByteSet(SourceLocation s, boolean[] byteMap) {
		int byteChar = uniqueByteChar(byteMap);
		if (byteChar != -1) {
			return newByte(s, byteChar);
		}
		Expression e = new Nez.ByteSet(byteMap);
		e.setSourceLocation(s);
		return e;
	}

	private static int uniqueByteChar(boolean[] byteMap) {
		int byteChar = -1;
		for (int i = 0; i < byteMap.length; i++) {
			if (byteMap[i]) {
				if (byteChar != -1)
					return -1;
				byteChar = i;
			}
		}
		return byteChar;
	}

	/**
	 * Creates a multibyte matching, equals to 'abc' in Nez.
	 * 
	 * @param utf8
	 * @return
	 */

	public static Expression newMultiByte(byte[] utf8) {
		if (utf8.length == 0) {
			return Expressions.newEmpty();
		}
		if (utf8.length == 1) {
			return Expressions.newByte(utf8[0]);
		}
		return new Nez.MultiByte(utf8);
	}

	/**
	 * Creates a multibyte matching, equals to 'abc' in Nez.
	 * 
	 * @param s
	 * @param utf8
	 * @return
	 */

	public static Expression newMultiByte(SourceLocation s, byte[] utf8) {
		if (utf8.length == 0) {
			return Expressions.newEmpty(s);
		}
		if (utf8.length == 1) {
			return Expressions.newByte(s, utf8[0]);
		}
		Expression e = new Nez.MultiByte(utf8);
		e.setSourceLocation(s);
		return e;
	}

	/* Unary */

	/**
	 * Creates an option expression, equals to e?.
	 * 
	 * @param p
	 * @return
	 */

	public final static Expression newOption(Expression p) {
		return new Nez.Option(p);
	}

	/**
	 * Creates an option expression, equals to e?.
	 * 
	 * @param s
	 * @param p
	 * @return
	 */

	public final static Expression newOption(SourceLocation s, Expression p) {
		Expression e = new Nez.Option(p);
		e.setSourceLocation(s);
		return e;
	}

	/**
	 * Creates a zero-and-more repetition, equals to e*.
	 * 
	 * @param p
	 * @return
	 */

	public final static Expression newZeroMore(Expression p) {
		return new Nez.ZeroMore(p);
	}

	/**
	 * Creates a zero-and-more repetition, equals to e*.
	 * 
	 * @param s
	 * @param p
	 * @return
	 */
	public final static Expression newZeroMore(SourceLocation s, Expression p) {
		Expression e = new Nez.ZeroMore(p);
		e.setSourceLocation(s);
		return e;
	}

	/**
	 * Creates a one-and-more repetition, equals to e+.
	 * 
	 * @param p
	 * @return
	 */

	public final static Expression newOneMore(Expression p) {
		return new Nez.OneMore(p);
	}

	/**
	 * Creates a one-and-more repetition, equals to e+.
	 * 
	 * @param p
	 * @return
	 */

	public final static Expression newOneMore(SourceLocation s, Expression p) {
		Expression e = new Nez.OneMore(p);
		e.setSourceLocation(s);
		return e;
	}

	/**
	 * Creates an and-predicate, equals to &e.
	 * 
	 * @param s
	 * @param p
	 * @return
	 */

	public final static Expression newAnd(SourceLocation s, Expression p) {
		Expression e = new Nez.And(p);
		e.setSourceLocation(s);
		return e;
	}

	/**
	 * Creates an and-predicate, equals to &e.
	 * 
	 * @param p
	 * @return
	 */

	public final static Expression newAnd(Expression p) {
		return new Nez.And(p);
	}

	/**
	 * Creates an negation-predicate, equals to !e.
	 * 
	 * @param s
	 * @param p
	 * @return
	 */

	public final static Expression newNot(SourceLocation s, Expression p) {
		Expression e = new Nez.Not(p);
		e.setSourceLocation(s);
		return e;
	}

	/**
	 * Creates an negation-predicate, equals to !e.
	 * 
	 * @param p
	 * @return
	 */

	public final static Expression newNot(Expression p) {
		return new Nez.Not(p);
	}

	/* Pair */

	/**
	 * Creates a pair of two expressions, equals to e1 e2 in Nez.
	 * 
	 * @param s
	 * @param p
	 * @param p2
	 * @return
	 */

	public final static Expression newPair(SourceLocation s, Expression p, Expression p2) {
		UList<Expression> l = new UList<>(new Expression[2]);
		addSequence(l, p);
		addSequence(l, p2);
		return newPair(l);
	}

	/**
	 * Creates a pair expression from a list of expressions
	 * 
	 * @param l
	 *            a list of expressions
	 * @return
	 */

	public final static Expression newPair(List<Expression> l) {
		if (l.size() == 0) {
			return newEmpty();
		}
		if (l.size() == 1) {
			return l.get(0);
		}
		return newPair(0, l);
	}

	private final static Expression newPair(int start, List<Expression> l) {
		Expression first = l.get(start);
		if (start + 1 == l.size()) {
			return first;
		}
		return new Nez.Pair(first, newPair(start + 1, l));
	}

	/**
	 * Creates a pair expression from a list of expressions
	 * 
	 * @param expressions
	 * @return
	 */

	public final static Expression newPair(Expression... expressions) {
		UList<Expression> l = new UList<>(new Expression[expressions.length]);
		for (Expression e : expressions) {
			addSequence(l, e);
		}
		return newPair(l);
	}

	/* Pair */

	public final static Expression first(Expression e) {
		if (e instanceof Nez.Pair || e instanceof Nez.Sequence) {
			return e.get(0);
		}
		return e;
	}

	public final static Expression next(Expression e) {
		if (e instanceof Nez.Pair) {
			return e.get(1);
		}
		if (e instanceof Nez.Sequence) {
			Nez.Sequence seq = (Nez.Sequence) e;
			if (seq.size() == 1) {
				return null;
			}
			Expression[] inners = new Expression[seq.size() - 1];
			for (int i = 0; i < inners.length; i++) {
				inners[i] = seq.get(i + 1);
			}
			return new Nez.Sequence(inners);
		}
		return null;
	}

	public final static List<Expression> flatten(Expression e) {
		UList<Expression> l = Expressions.newUList(4);
		flatten(e, l);
		return l;
	}

	private static void flatten(Expression e, List<Expression> l) {
		if (e instanceof Nez.Pair) {
			flatten(e.get(0), l);
			flatten(e.get(1), l);
			return;
		}
		if (e instanceof Nez.Sequence) {
			for (Expression sub : e) {
				flatten(sub, e);
			}
			return;
		}
		if (e instanceof Nez.Empty) {
			return;
		}
		l.add(e);
	}

	/* Sequence */

	/**
	 * Creates a sequence expression, equals to e1 e2 ... in Nez.
	 * 
	 * @param l
	 *            a list of expressions
	 * @return
	 */

	public final static Expression newSequence(List<Expression> l) {
		if (l.size() == 0) {
			return newEmpty();
		}
		if (l.size() == 1) {
			return l.get(0);
		}
		return new Nez.Sequence(compact(l));
	}

	private static Expression[] compact(List<Expression> l) {
		Expression[] a = new Expression[l.size()];
		for (int i = 0; i < l.size(); i++) {
			a[i] = l.get(i);
		}
		return a;
	}

	/**
	 * Creates a sequence expression, equals to e1 e2 ... in Nez.
	 * 
	 * @param expressions
	 * @return
	 */

	public final static Expression newSequence(Expression... expressions) {
		UList<Expression> l = new UList<>(new Expression[expressions.length]);
		for (Expression e : expressions) {
			addSequence(l, e);
		}
		return newSequence(l);
	}

	/* Choice */

	/**
	 * Creates a new choice from a list of expressions
	 * 
	 * @param l
	 * @return
	 */

	public final static Expression newChoice(List<Expression> l) {
		int size = l.size();
		for (int i = 0; i < size; i++) {
			if (l.get(i) instanceof Nez.Empty) {
				size = i + 1;
				break;
			}
		}
		if (size == 1) {
			return l.get(0);
		}
		Expression[] inners = new Expression[size];
		for (int i = 0; i < size; i++) {
			inners[i] = l.get(i);
		}
		return new Nez.Choice(inners);
	}

	/**
	 * Creates a choice from two expressions
	 * 
	 * @param p
	 * @param p2
	 * @return
	 */

	public final static Expression newChoice(Expression p, Expression p2) {
		if (p == null) {
			return p2 == null ? null : p2;
		}
		if (p2 == null) {
			return p;
		}
		UList<Expression> l = new UList<>(new Expression[2]);
		addChoice(l, p);
		addChoice(l, p2);
		return newChoice(l);
	}

	public final static Expression tryCommonFactoring(Nez.Choice choice) {
		List<Expression> l = Expressions.newList(choice.size());
		int[] indexes = new int[256];
		Arrays.fill(indexes, -1);
		tryCommonFactoring(choice, l, indexes);
		for (int i = 0; i < l.size(); i++) {
			l.set(i, tryChoiceCommonFactring(l.get(i)));
		}
		return Expressions.newChoice(l);
	}

	private static void tryCommonFactoring(Nez.Choice choice, List<Expression> l, int[] indexes) {
		for (Expression inner : choice) {
			if (inner instanceof Nez.Choice) {
				tryCommonFactoring((Nez.Choice) inner, l, indexes);
				continue;
			}
			Expression first = Expressions.first(inner);
			if (first instanceof Nez.Byte) {
				int ch = ((Nez.Byte) first).byteChar;
				if (indexes[ch] == -1) {
					indexes[ch] = l.size();
					l.add(inner);
				} else {
					Expression prev = l.get(indexes[ch]);
					Expression second = Expressions.newChoice(Expressions.next(prev), Expressions.next(inner));
					prev = Expressions.newPair(prev.getSourceLocation(), first, second);
					l.set(indexes[ch], prev);
				}
			} else {
				Expressions.addChoice(l, inner);
			}
		}
	}

	public static Expression tryChoiceCommonFactring(Expression e) {
		if (e instanceof Nez.Choice) {
			return tryCommonFactoring((Nez.Choice) e);
		}
		for (int i = 0; i < e.size(); i++) {
			Expression sub = e.get(i);
			if (sub instanceof Nez.Choice) {
				e.set(i, tryCommonFactoring((Nez.Choice) sub));
			}
		}
		return e;
	}

	// AST Construction

	public final static Expression newDetree(SourceLocation s, Expression p) {
		Expression e = new Nez.Detree(p);
		e.setSourceLocation(s);
		return e;
	}

	public final static Expression newLinkTree(SourceLocation s, Expression p) {
		return newLinkTree(s, null, p);
	}

	public final static Expression newLinkTree(SourceLocation s, Symbol label, Expression p) {
		Expression e = new Nez.LinkTree(label, p);
		e.setSourceLocation(s);
		return e;
	}

	// public final static Expression newTnew(SourcePosition s, boolean lefted,
	// Symbol label, int shift) {
	// return new Tnew(s, lefted, label, shift);
	// }

	/**
	 * Creates a { expression
	 * 
	 * @return
	 */

	public final static Expression newBeginTree() {
		return new Nez.BeginTree(0);
	}

	/**
	 * Creates a { expression.
	 * 
	 * @param s
	 * @param shift
	 * @return
	 */

	public final static Expression newBeginTree(SourceLocation s, int shift) {
		Expression e = new Nez.BeginTree(shift);
		e.setSourceLocation(s);
		return e;
	}

	/**
	 * Creates a } expression.
	 * 
	 * @return
	 */
	public final static Expression newEndTree() {
		return new Nez.EndTree(0);
	}

	/**
	 * Creates a } expression.
	 * 
	 * @param s
	 * @param shift
	 * @return
	 */

	public final static Expression newEndTree(SourceLocation s, int shift) {
		Expression e = new Nez.EndTree(shift);
		e.setSourceLocation(s);
		return e;
	}

	public final static Expression newFoldTree(SourceLocation s, Symbol label, int shift) {
		Expression e = new Nez.FoldTree(shift, label);
		e.setSourceLocation(s);
		return e;
	}

	/**
	 * Creates a #tag expression.
	 * 
	 * @param tag
	 * @return
	 */

	public final static Expression newTag(Symbol tag) {
		return new Nez.Tag(tag);
	}

	/**
	 * Creates a #tag expression.
	 * 
	 * @param s
	 * @param tag
	 * @return
	 */

	public final static Expression newTag(SourceLocation s, Symbol tag) {
		Expression e = new Nez.Tag(tag);
		e.setSourceLocation(s);
		return e;
	}

	/**
	 * Creates a `v` expression.
	 * 
	 * @param v
	 * @return
	 */

	public final static Expression newReplace(String v) {
		return new Nez.Replace(v);
	}

	/**
	 * Creates a `v` expression.
	 * 
	 * @param s
	 * @param v
	 * @return
	 */

	public final static Expression newReplace(SourceLocation s, String v) {
		Expression e = new Nez.Replace(v);
		e.setSourceLocation(s);
		return e;
	}

	/* Conditional Parsing */

	/**
	 * Creates <if C>
	 * 
	 * @param c
	 * @return
	 */
	public final static Expression newIfCondition(String c) {
		return Expressions.newIfCondition(null, c);
	}

	/**
	 * Creates <if C>
	 * 
	 * @param s
	 * @param c
	 * @return
	 */

	public final static Expression newIfCondition(SourceLocation s, String c) {
		boolean predicate = true;
		if (c.startsWith("!")) {
			predicate = false;
			c = c.substring(1);
		}
		Expression e = new Nez.IfCondition(predicate, c);
		e.setSourceLocation(s);
		return e;
	}

	/**
	 * Creates <on C e>
	 * 
	 * @param c
	 * @param e
	 * @return
	 */

	public final static Expression newOnCondition(String c, Expression e) {
		return Expressions.newOnCondition(null, c, e);
	}

	/**
	 * Creates <on C e>
	 * 
	 * @param s
	 * @param c
	 * @param e
	 * @return
	 */

	public final static Expression newOnCondition(SourceLocation s, String c, Expression e) {
		boolean predicate = true;
		if (c.startsWith("!")) {
			predicate = false;
			c = c.substring(1);
		}
		Expression p = new Nez.OnCondition(predicate, c, e);
		p.setSourceLocation(s);
		return p;
	}

	/* Symbol Table */

	/**
	 * Creates a <block e> expression
	 * 
	 * @param e
	 * @return
	 */
	public final static Expression newBlockScope(Expression e) {
		return new Nez.BlockScope(e);
	}

	/**
	 * Creates a <block e> expression
	 * 
	 * @param s
	 * @param e
	 * @return
	 */

	public final static Expression newBlockScope(SourceLocation s, Expression e) {
		Expression p = new Nez.BlockScope(e);
		p.setSourceLocation(s);
		return p;
	}

	/**
	 * Creates a <local Name e> expression
	 * 
	 * @param tableName
	 * @param e
	 * @return
	 */
	public final static Expression newLocalScope(Symbol tableName, Expression e) {
		return new Nez.LocalScope(tableName, e);
	}

	/**
	 * Creates a <local A e> expression
	 * 
	 * @param s
	 * @param tableName
	 * @param e
	 * @return
	 */

	public final static Expression newLocalScope(SourceLocation s, Symbol tableName, Expression e) {
		Expression p = new Nez.LocalScope(tableName, e);
		p.setSourceLocation(s);
		return p;
	}

	/**
	 * Creates a <symbol A> expression
	 * 
	 * @param pat
	 * @return
	 */

	public final static Expression newSymbol(NonTerminal pat) {
		return new Nez.SymbolAction(FunctionName.symbol, pat);
	}

	/**
	 * Crates a <symbol A> expression
	 * 
	 * @param s
	 * @param pat
	 * @return
	 */

	public final static Expression newSymbol(SourceLocation s, NonTerminal pat) {
		Expression p = new Nez.SymbolAction(FunctionName.symbol, pat);
		p.setSourceLocation(s);
		return p;

	}

	/**
	 * Creates a <exists A> expression.
	 * 
	 * @param tableName
	 * @return
	 */
	public final static Expression newSymbolExists(Symbol tableName) {
		return new Nez.SymbolExists(tableName, null);
	}

	/**
	 * Creates a <exists A> expression.
	 * 
	 * @param s
	 * @param tableName
	 * @return
	 */

	public final static Expression newSymbolExists(SourceLocation s, Symbol tableName) {
		Expression p = new Nez.SymbolExists(tableName, null);
		p.setSourceLocation(s);
		return p;

	}

	/**
	 * Creates a <exists A x> expression.
	 * 
	 * @param tableName
	 * @param symbol
	 * @return
	 */

	public final static Expression newSymbolExists(Symbol tableName, String symbol) {
		return new Nez.SymbolExists(tableName, symbol);
	}

	/**
	 * Creates a <exists A x> expression.
	 * 
	 * @param s
	 * @param tableName
	 * @param symbol
	 * @return
	 */

	public final static Expression newSymbolExists(SourceLocation s, Symbol tableName, String symbol) {
		Expression p = new Nez.SymbolExists(tableName, symbol);
		p.setSourceLocation(s);
		return p;
	}

	/**
	 * Creates a <match A> expression
	 * 
	 * @param tableName
	 * @return
	 */

	public final static Expression newSymbolMatch(NonTerminal pat) {
		return new Nez.SymbolMatch(FunctionName.match, pat, null);
	}

	/**
	 * Creates a <match A> expression
	 * 
	 * @param s
	 * @param tableName
	 * @return
	 */
	public final static Expression newSymbolMatch(SourceLocation s, NonTerminal pat) {
		Expression p = new Nez.SymbolMatch(FunctionName.match, pat, null);
		p.setSourceLocation(s);
		return p;
	}

	/**
	 * Creates a <is A> expression.
	 * 
	 * @param pat
	 * @return
	 */

	public final static Expression newIsSymbol(NonTerminal pat) {
		return new Nez.SymbolPredicate(FunctionName.is, pat, null);
	}

	/**
	 * Creates a <is A> expression.
	 * 
	 * @param s
	 * @param pat
	 * @return
	 */

	public final static Expression newIsSymbol(SourceLocation s, NonTerminal pat) {
		Expression p = new Nez.SymbolPredicate(FunctionName.is, pat, null);
		p.setSourceLocation(s);
		return p;
	}

	/**
	 * Creates a <isa A> expression.
	 * 
	 * @param pat
	 * @return
	 */

	public final static Expression newIsaSymbol(NonTerminal pat) {
		return new Nez.SymbolPredicate(FunctionName.isa, pat, null);
	}

	/**
	 * Creates a <isa A> expression.
	 * 
	 * @param s
	 * @param pat
	 * @return
	 */

	public final static Expression newIsaSymbol(SourceLocation s, NonTerminal pat) {
		Expression p = new Nez.SymbolPredicate(FunctionName.isa, pat, null);
		p.setSourceLocation(s);
		return p;
	}

	@Deprecated
	public final static Expression newScan(SourceLocation s, int number, Expression scan, Expression repeat) {
		return null;
	}

	@Deprecated
	public final static Expression newRepeat(SourceLocation s, Expression e) {
		return null;
	}

	// -----------------------------------------------------------------------

	public static final Expression newExpression(SourceLocation s, String text) {
		byte[] utf8 = StringUtils.toUtf8(text);
		if (utf8.length == 0) {
			return newEmpty(s);
		}
		if (utf8.length == 1) {
			return newByte(s, utf8[0]);
		}
		return newByteSequence(s, false, utf8);
	}

	public final static Expression newByteSequence(SourceLocation s, boolean binary, byte[] utf8) {
		UList<Expression> l = new UList<>(new Expression[utf8.length]);
		for (int i = 0; i < utf8.length; i++) {
			l.add(newByte(s, utf8[i]));
		}
		return newPair(l);
	}

	public final static Expression newCharSet(SourceLocation s, String text) {
		boolean b[] = StringUtils.parseByteMap(text);
		return newByteSet(s, b);
	}

	public final static Expression newCharSet(SourceLocation s, String t, String t2) {
		int c = StringUtils.parseAscii(t);
		int c2 = StringUtils.parseAscii(t2);
		if (c != -1 && c2 != -1) {
			return newByteRange(s, false, c, c2);
		}
		c = StringUtils.parseUnicode(t);
		c2 = StringUtils.parseUnicode(t2);
		if (c < 128 && c2 < 128) {
			return newByteRange(s, false, c, c2);
		} else {
			return newUnicodeRange(s, c, c2);
		}
	}

	public final static Expression newByteRange(SourceLocation s, boolean binary, int c, int c2) {
		if (c == c2) {
			return newByte(s, c);
		}
		boolean[] byteMap = Bytes.newMap(false);
		Bytes.appendRange(byteMap, c, c2);
		return newByteSet(s, byteMap);
	}

	private final static Expression newUnicodeRange(SourceLocation s, int c, int c2) {
		byte[] b = StringUtils.toUtf8(String.valueOf((char) c));
		byte[] b2 = StringUtils.toUtf8(String.valueOf((char) c2));
		if (equalsBase(b, b2)) {
			return newUnicodeRange(s, b, b2);
		}
		UList<Expression> l = new UList<>(new Expression[b.length]);
		b2 = b;
		for (int pc = c + 1; pc <= c2; pc++) {
			byte[] b3 = StringUtils.toUtf8(String.valueOf((char) pc));
			if (equalsBase(b, b3)) {
				b2 = b3;
				continue;
			}
			l.add(newUnicodeRange(s, b, b2));
			b = b3;
			b2 = b3;
		}
		b2 = StringUtils.toUtf8(String.valueOf((char) c2));
		l.add(newUnicodeRange(s, b, b2));
		return newChoice(l);
	}

	private final static boolean equalsBase(byte[] b, byte[] b2) {
		if (b.length == b2.length) {
			switch (b.length) {
			case 3:
				return b[0] == b2[0] && b[1] == b2[1];
			case 4:
				return b[0] == b2[0] && b[1] == b2[1] && b[2] == b2[2];
			}
			return b[0] == b2[0];
		}
		return false;
	}

	private final static Expression newUnicodeRange(SourceLocation s, byte[] b, byte[] b2) {
		if (b[b.length - 1] == b2[b.length - 1]) {
			return newByteSequence(s, false, b);
		} else {
			UList<Expression> l = new UList<>(new Expression[b.length]);
			for (int i = 0; i < b.length - 1; i++) {
				l.add(newByte(s, b[i]));
			}
			l.add(newByteRange(s, false, b[b.length - 1] & 0xff, b2[b2.length - 1] & 0xff));
			return newPair(l);
		}
	}

	public final static Expression newTree(SourceLocation s, Expression e) {
		return newTree(s, false, null, e);
	}

	public final static Expression newTree(SourceLocation s, boolean lefted, Symbol label, Expression e) {
		UList<Expression> l = new UList<>(new Expression[e.size() + 3]);
		Expressions.addSequence(l, lefted ? newFoldTree(s, label, 0) : newBeginTree(s, 0));
		Expressions.addSequence(l, e);
		Expressions.addSequence(l, Expressions.newEndTree(s, 0));
		return newPair(l);
	}

	public final static Expression newLeftFoldOption(SourceLocation s, Symbol label, Expression e) {
		UList<Expression> l = new UList<>(new Expression[e.size() + 3]);
		Expressions.addSequence(l, newFoldTree(s, label, 0));
		Expressions.addSequence(l, e);
		Expressions.addSequence(l, Expressions.newEndTree(s, 0));
		return newOption(s, Expressions.newPair(l));
	}

	public final static Expression newLeftFoldRepetition(SourceLocation s, Symbol label, Expression e) {
		UList<Expression> l = new UList<>(new Expression[e.size() + 3]);
		Expressions.addSequence(l, newFoldTree(s, label, 0));
		Expressions.addSequence(l, e);
		Expressions.addSequence(l, Expressions.newEndTree(s, 0));
		return newZeroMore(s, Expressions.newPair(l));
	}

	public final static Expression newLeftFoldRepetition1(SourceLocation s, Symbol label, Expression e) {
		UList<Expression> l = new UList<>(new Expression[e.size() + 3]);
		Expressions.addSequence(l, newFoldTree(s, label, 0));
		Expressions.addSequence(l, e);
		Expressions.addSequence(l, Expressions.newEndTree(s, 0));
		return newOneMore(s, Expressions.newPair(l));
	}

	/* Optimization */

	public final static Expression resolveNonTerminal(Expression e) {
		while (e instanceof NonTerminal) {
			NonTerminal nterm = (NonTerminal) e;
			e = nterm.deReference();
		}
		return e;
	}

	public final static Expression tryConvertingByteSet(Nez.Choice choice) {
		boolean byteMap[] = Bytes.newMap(false);
		if (tryConvertingByteSet(choice, byteMap)) {
			return Expressions.newByteSet(choice.getSourceLocation(), byteMap);
		}
		return choice;
	}

	private static boolean tryConvertingByteSet(Nez.Choice choice, boolean[] byteMap) {
		for (Expression e : choice) {
			e = Expressions.resolveNonTerminal(e);
			if (e instanceof Nez.Choice) {
				if (!tryConvertingByteSet((Nez.Choice) e, byteMap)) {
					return false;
				}
				continue; // OK
			}
			if (e instanceof Nez.Byte) {
				byteMap[((Nez.Byte) e).byteChar] = true;
				continue; // OK
			}
			if (e instanceof Nez.ByteSet) {
				Bytes.appendBitMap(byteMap, ((Nez.ByteSet) e).byteMap);
				continue; // OK
			}
			return false;
		}
		return true;
	}

	public final static Expression tryMultiCharSequence(Expression e) {
		if (e instanceof Nez.Sequence || e instanceof Nez.Pair) {
			List<Expression> el = flatten(e);
			List<Expression> el2 = new UList<>(new Expression[el.size()]);
			UList<Byte> bytes = new UList<>(new Byte[el.size()]);
			int next = 0;
			while (next < el.size()) {
				next = appendExpressionOrMultiChar(el, next, el2, bytes);
			}
			e = Expressions.newSequence(el2);
		}
		return e;
	}

	private final static int appendExpressionOrMultiChar(List<Expression> el, int start, List<Expression> el2, UList<Byte> bytes) {
		int next = start + 1;
		Expression e = el.get(start);
		if (e instanceof Nez.Byte) {
			bytes.clear();
			for (int i = start; i < el.size(); i++) {
				Expression sub = el.get(i);
				if (sub instanceof Nez.Byte) {
					bytes.add(((byte) ((Nez.Byte) sub).byteChar));
					continue;
				}
				next = i;
				break;
			}
			if (bytes.size() > 1) {
				byte[] byteSeq = new byte[bytes.size()];
				for (int i = 0; i < bytes.size(); i++) {
					byteSeq[i] = bytes.get(i);
				}
				e = Expressions.newMultiByte(e.getSourceLocation(), byteSeq);
			}
		}
		el2.add(e);
		return next;
	}

}
