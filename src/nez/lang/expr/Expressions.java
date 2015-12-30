package nez.lang.expr;

import java.util.List;

import nez.ast.SourceLocation;
import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Nez;
import nez.lang.NonTerminal;
import nez.util.StringUtils;
import nez.util.UList;

public abstract class Expressions {

	// -----------------------------------------------------------------------
	// Utils

	public final static Expression resolveNonTerminal(Expression e) {
		while (e instanceof NonTerminal) {
			NonTerminal nterm = (NonTerminal) e;
			e = nterm.deReference();
		}
		return e;
	}

	public final static UList<Expression> newList(int size) {
		return new UList<Expression>(new Expression[size]);
	}

	public final static List<Expression> newList2(int size) {
		return new UList<Expression>(new Expression[size]);
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

	public final static Expression newEmpty(SourceLocation s) {
		return new Pempty(s);
	}

	public final static Expression newFailure(SourceLocation s) {
		return new Pfail(s);
	}

	/* Terminal */

	public final static Expression newAny(SourceLocation s) {
		return new Cany(s, false);
	}

	public final static Expression newByte(SourceLocation s, int ch) {
		return new Cbyte(s, false, ch & 0xff);
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

	public static Expression newMultiByte(SourceLocation s, byte[] utf8) {
		return new Cmulti(s, false, utf8);
	}

	public static Expression newByteSet(SourceLocation s, boolean[] byteMap) {
		int byteChar = uniqueByteChar(byteMap);
		if (byteChar != -1) {
			return newByte(s, byteChar);
		}
		return new Cset(s, false, byteMap);
	}

	/* Unary */

	public final static Expression newOption(SourceLocation s, Expression p) {
		return new Poption(s, p);
	}

	public final static Expression newZeroMore(SourceLocation s, Expression p) {
		return new Pzero(s, p);
	}

	public final static Expression newOneMore(SourceLocation s, Expression p) {
		return new Pone(s, p);
	}

	public final static Expression newAnd(SourceLocation s, Expression p) {
		return new Pand(s, p);
	}

	public final static Expression newNot(SourceLocation s, Expression p) {
		return new Pnot(s, p);
	}

	public final static Expression newPair(SourceLocation s, UList<Expression> l) {
		if (l.size() == 0) {
			return newEmpty(s);
		}
		return newPair(s, 0, l);
	}

	public final static Expression newPair(SourceLocation s, Expression p, Expression p2) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		addSequence(l, p);
		addSequence(l, p2);
		return newPair(s, l);
	}

	private final static Expression newPair(SourceLocation s, int start, UList<Expression> l) {
		Expression first = l.ArrayValues[start];
		if (start + 1 == l.size()) {
			return first;
		}
		return new Psequence(s, first, newPair(s, start + 1, l));
	}

	public final static Expression newSequence(SourceLocation s, List<Expression> l) {
		if (l.size() == 0) {
			return newEmpty(s);
		}
		if (l.size() == 1) {
			return l.get(0);
		}
		Expression e = new Nez.Sequence(compact(l));
		e.setSourceLocation(s);
		return e;
	}

	private static Expression[] compact(List<Expression> l) {
		Expression[] a = new Expression[l.size()];
		for (int i = 0; i < l.size(); i++) {
			a[i] = l.get(i);
		}
		return a;
	}

	public final static Expression newChoice(SourceLocation s, UList<Expression> l) {
		int size = l.size();
		for (int i = 0; i < size; i++) {
			if (l.ArrayValues[i] instanceof Pempty) {
				size = i + 1;
				break;
			}
		}
		if (size == 1) {
			return l.ArrayValues[0];
		}
		return new Pchoice(s, l, size);
	}

	public final static Expression newChoice(SourceLocation s, Expression p, Expression p2) {
		if (p == null) {
			return p2 == null ? newEmpty(s) : p2;
		}
		if (p2 == null) {
			return p;
		}
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		addChoice(l, p);
		addChoice(l, p2);
		return newChoice(s, l);
	}

	// AST Construction

	public final static Expression newDetree(SourceLocation s, Expression p) {
		return new Tdetree(s, p);
	}

	public final static Expression newLinkTree(SourceLocation s, Expression p) {
		return newLinkTree(s, null, p);
	}

	public final static Expression newLinkTree(SourceLocation s, Symbol label, Expression p) {
		return new Tlink(s, label, p);
	}

	// public final static Expression newTnew(SourcePosition s, boolean lefted,
	// Symbol label, int shift) {
	// return new Tnew(s, lefted, label, shift);
	// }

	public final static Expression newBeginTree(SourceLocation s, int shift) {
		return new Tnew(s, shift);
	}

	public final static Expression newLeftFold(SourceLocation s, Symbol label, int shift) {
		return new Tlfold(s, label, shift);
	}

	public final static Expression newEndTree(SourceLocation s, int shift) {
		return new Tcapture(s, shift);
	}

	public final static Expression newTag(SourceLocation s, Symbol tag) {
		return new Ttag(s, tag);
	}

	public final static Expression newReplace(SourceLocation s, String msg) {
		return new Treplace(s, msg);
	}

	// Conditional Parsing
	// <if FLAG>
	// <on FLAG e>
	// <on! FLAG e>

	public final static Expression newIf(SourceLocation s, String flagName) {
		return new Xif(s, true, flagName);
	}

	public final static Expression newOn(SourceLocation s, boolean predicate, String flagName, Expression e) {
		return new Xon(s, predicate, flagName, e);
	}

	public final static Expression newBlockScope(SourceLocation s, Expression e) {
		return new Xblock(s, e);
	}

	public final static Expression newLocalScope(SourceLocation s, Symbol tableName, Expression e) {
		return new Xlocal(s, tableName, e);
	}

	@Deprecated
	public final static Expression newXdef(SourceLocation s, Grammar g, String name, Expression e) {
		NonTerminal pat = g.newNonTerminal(s, name);
		g.newProduction(name, e);
		return new Xsymbol(s, pat);
	}

	public final static Expression newSymbolAction(SourceLocation s, NonTerminal pat) {
		return new Xsymbol(s, pat);
	}

	// public final static Expression newXsymbol(SourceLocation s, Symbol table,
	// Expression e) {
	// return new Xsymbol(s, table, e);
	// }

	public final static Expression newSymbolMatch(SourceLocation s, Symbol tableName) {
		return new Xmatch(s, tableName);
	}

	public final static Expression newSymbolPredicate(SourceLocation s, NonTerminal pat, boolean is) {
		return new Xis(s, pat, is);
	}

	public final static Expression newSymbolPredicate(SourceLocation s, Symbol table, Expression e, boolean is) {
		return new Xis(s, table, e, is);
	}

	public final static Expression newSymbolPredicate(SourceLocation s, NonTerminal pat) {
		return new Xis(s, pat, /* is */true);
	}

	public final static Expression newXisa(SourceLocation s, NonTerminal pat) {
		return new Xis(s, pat, /* is */false);
	}

	public final static Expression newSymbolExists(SourceLocation s, Symbol tableName, String symbol) {
		return new Xexists(s, tableName, symbol);
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

	public static final Expression newMultiByte(SourceLocation s, String text) {
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
		UList<Expression> l = new UList<Expression>(new Expression[utf8.length]);
		for (int i = 0; i < utf8.length; i++) {
			l.add(newByte(s, utf8[i]));
		}
		return newPair(s, l);
	}

	public final static Expression newCharSet(SourceLocation s, String text) {
		boolean b[] = StringUtils.parseByteMap(text);
		return new Cset(s, false, b);
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
		return new Cset(s, binary, c, c2);
	}

	private final static Expression newUnicodeRange(SourceLocation s, int c, int c2) {
		byte[] b = StringUtils.toUtf8(String.valueOf((char) c));
		byte[] b2 = StringUtils.toUtf8(String.valueOf((char) c2));
		if (equalsBase(b, b2)) {
			return newUnicodeRange(s, b, b2);
		}
		UList<Expression> l = new UList<Expression>(new Expression[b.length]);
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
		return newChoice(s, l);
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
			UList<Expression> l = new UList<Expression>(new Expression[b.length]);
			for (int i = 0; i < b.length - 1; i++) {
				l.add(newByte(s, b[i]));
			}
			l.add(newByteRange(s, false, b[b.length - 1] & 0xff, b2[b2.length - 1] & 0xff));
			return newPair(s, l);
		}
	}

	public final static Expression newTree(SourceLocation s, Expression e) {
		return newTree(s, false, null, e);
	}

	public final static Expression newTree(SourceLocation s, boolean lefted, Symbol label, Expression e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size() + 3]);
		Expressions.addSequence(l, lefted ? new Tlfold(s, label, 0) : new Tnew(s, 0));
		Expressions.addSequence(l, e);
		Expressions.addSequence(l, Expressions.newEndTree(s, 0));
		return newPair(s, l);
	}

	public final static Expression newLeftFoldOption(SourceLocation s, Symbol label, Expression e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size() + 3]);
		Expressions.addSequence(l, new Tlfold(s, label, 0));
		Expressions.addSequence(l, e);
		Expressions.addSequence(l, Expressions.newEndTree(s, 0));
		return newOption(s, Expressions.newPair(s, l));
	}

	public final static Expression newLeftFoldRepetition(SourceLocation s, Symbol label, Expression e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size() + 3]);
		Expressions.addSequence(l, new Tlfold(s, label, 0));
		Expressions.addSequence(l, e);
		Expressions.addSequence(l, Expressions.newEndTree(s, 0));
		return newZeroMore(s, Expressions.newPair(s, l));
	}

	public final static Expression newLeftFoldRepetition1(SourceLocation s, Symbol label, Expression e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size() + 3]);
		Expressions.addSequence(l, new Tlfold(s, label, 0));
		Expressions.addSequence(l, e);
		Expressions.addSequence(l, Expressions.newEndTree(s, 0));
		return newOneMore(s, Expressions.newPair(s, l));
	}

}
