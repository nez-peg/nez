package nez.lang.expr;

import nez.Grammar;
import nez.ast.SourcePosition;
import nez.ast.SymbolId;
import nez.lang.Expression;
import nez.util.StringUtils;
import nez.util.UList;
import nez.util.UMap;

public abstract class ExpressionCommons extends Expression {

	protected ExpressionCommons(SourcePosition s) {
		super(s);
	}

	@Override
	public final boolean equals(Object o) {
		if (o instanceof Expression) {
			return this.equalsExpression((Expression) o);
		}
		return false;
	}

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		format(sb);
		return sb.toString();
	}

	@Override
	public void format(StringBuilder sb) {
		sb.append("<");
		sb.append(this.getClass().getSimpleName());
		for (Expression se : this) {
			sb.append(" ");
			se.format(sb);
		}
		sb.append(">");
	}

	// ---------------------

	public final static UMap<Expression> uniqueMap = new UMap<Expression>();

	public static Expression intern(Expression e) {
		// if (e.internId == 0) {
		// StringBuilder sb = new StringBuilder();
		// sb.append(e.key());
		// for (int i = 0; i < e.size(); i++) {
		// Expression sube = e.get(i);
		// if (!sube.isInterned()) {
		// sube = sube.intern();
		// e.set(i, sube);
		// }
		// sb.append("#" + sube.internId);
		// }
		// String key = sb.toString();
		// Expression u = uniqueMap.get(key);
		// if (u == null) {
		// u = e;
		// e.s = null;
		// e.internId = uniqueMap.size() + 1;
		// uniqueMap.put(key, e);
		// }
		// if (Command.ReleasePreview) {
		// if (!u.equalsExpression(e)) {
		// Verbose.debug("Mismatched Interning: " + e.getClass() + "\n\te=" + e
		// + "\n\tinterned=" + u);
		// }
		// assert (u.equalsExpression(e));
		// }
		// return u;
		// }
		return e;
	}

	private static int id = 1;

	static void setId(Expression e) {
		// if (e.internId == 0) {
		// e.internId = id++;
		// for (int i = 0; i < e.size(); i++) {
		// Expression sube = e.get(i);
		// setId(sube);
		// }
		// }
	}

	// static Expression internImpl(SourcePosition s, Expression e) {
	// return (s == null) ? intern(e) : e;
	// }

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

	public final static void addSequence(UList<Expression> l, Expression e) {
		if (e instanceof Psequence) {
			for (int i = 0; i < e.size(); i++) {
				addSequence(l, e.get(i));
			}
			return;
		}
		if (e instanceof Pempty) {
			return;
		}
		if (l.size() > 0) {
			Expression prev = l.ArrayValues[l.size() - 1];
			if (prev instanceof Pfail) {
				return;
			}
		}
		l.add(e);
	}

	public final static void addChoice(UList<Expression> l, Expression e) {
		if (e instanceof Pchoice) {
			for (int i = 0; i < e.size(); i++) {
				addChoice(l, e.get(i));
			}
			return;
		}
		if (e instanceof Pfail) {
			return;
		}
		if (l.size() > 0) {
			Expression prev = l.ArrayValues[l.size() - 1];
			if (prev instanceof Pempty) {
				return;
			}
		}
		l.add(e);
	}

	// -----------------------------------------------------------------------

	public final static NonTerminal newNonTerminal(SourcePosition s, Grammar g, String name) {
		return new NonTerminal(s, g, name);
	}

	public final static Expression newEmpty(SourcePosition s) {
		return new Pempty(s);
	}

	public final static Expression newFailure(SourcePosition s) {
		return new Pfail(s);
	}

	/* Terminal */

	public final static Expression newCany(SourcePosition s, boolean binary) {
		return new Cany(s, binary);
	}

	public final static Expression newCbyte(SourcePosition s, boolean binary, int ch) {
		if (ch == 0) {
			binary = true;
		}
		return new Cbyte(s, binary, ch & 0xff);
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

	public static Expression newCmulti(SourcePosition s, boolean binary, byte[] utf8) {
		return new Cmulti(s, binary, utf8);
	}

	public static Expression newCset(SourcePosition s, boolean binary, boolean[] byteMap) {
		int byteChar = uniqueByteChar(byteMap);
		if (byteChar != -1) {
			return newCbyte(s, binary, byteChar);
		}
		return new Cset(s, binary, byteMap);
	}

	/* Unary */

	public final static Expression newUoption(SourcePosition s, Expression p) {
		return new Uoption(s, p);
	}

	public final static Expression newUzero(SourcePosition s, Expression p) {
		return new Uzero(s, p);
	}

	public final static Expression newUone(SourcePosition s, Expression p) {
		return new Uone(s, p);
	}

	public final static Uand newUand(SourcePosition s, Expression p) {
		return new Uand(s, p);
	}

	public final static Unot newUnot(SourcePosition s, Expression p) {
		return new Unot(s, p);
	}

	public final static Expression newPsequence(SourcePosition s, UList<Expression> l) {
		if (l.size() == 0) {
			return newEmpty(s);
		}
		return newPsequence(s, 0, l);
	}

	private final static Expression newPsequence(SourcePosition s, int start, UList<Expression> l) {
		Expression first = l.ArrayValues[start];
		if (start + 1 == l.size()) {
			return first;
		}
		return new Psequence(s, first, newPsequence(s, start + 1, l));
	}

	public final static Expression newPsequence(SourcePosition s, Expression p, Expression p2) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		addSequence(l, p);
		addSequence(l, p2);
		return newPsequence(s, l);
	}

	public final static Expression newPchoice(SourcePosition s, UList<Expression> l) {
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

	public final static Expression newPchoice(SourcePosition s, Expression p, Expression p2) {
		if (p == null) {
			return p2 == null ? newEmpty(s) : p2;
		}
		if (p2 == null) {
			return p;
		}
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		addChoice(l, p);
		addChoice(l, p2);
		return newPchoice(s, l);
	}

	// AST Construction

	public final static Expression newUmatch(SourcePosition s, Expression p) {
		return new Umatch(s, p);
	}

	public final static Expression newTlink(SourcePosition s, Expression p) {
		return newTlink(s, null, p);
	}

	public final static Expression newTlink(SourcePosition s, SymbolId label, Expression p) {
		return new Tlink(s, label, p);
	}

	public final static Expression newTnew(SourcePosition s, boolean lefted, SymbolId label, int shift) {
		return new Tnew(s, lefted, label, shift);
	}

	public final static Expression newTcapture(SourcePosition s, int shift) {
		return new Tcapture(s, shift);
	}

	public final static Expression newTtag(SourcePosition s, SymbolId tag) {
		return new Ttag(s, tag);
	}

	public final static Expression newTreplace(SourcePosition s, String msg) {
		return new Treplace(s, msg);
	}

	// Conditional Parsing
	// <if FLAG>
	// <on FLAG e>
	// <on! FLAG e>

	public final static Expression newXif(SourcePosition s, String flagName) {
		return new Xif(s, true, flagName);
	}

	public final static Expression newXon(SourcePosition s, boolean predicate, String flagName, Expression e) {
		return new Xon(s, predicate, flagName, e);
	}

	public final static Expression newXblock(SourcePosition s, Expression e) {
		return new Xblock(s, e);
	}

	public final static Expression newXlocal(SourcePosition s, SymbolId tableName, Expression e) {
		return new Xlocal(s, tableName, e);
	}

	public final static Expression newXdef(SourcePosition s, Grammar g, SymbolId tableName, Expression e) {
		return new Xdef(s, g, tableName, e);
	}

	public final static Expression newXmatch(SourcePosition s, SymbolId tableName) {
		return new Xmatch(s, tableName);
	}

	public final static Expression newXis(SourcePosition s, Grammar g, SymbolId tableName, boolean is) {
		return new Xis(s, g, tableName, is);
	}

	public final static Expression newXis(SourcePosition s, Grammar g, SymbolId tableName) {
		return new Xis(s, g, tableName, /* is */true);
	}

	public final static Expression newXisa(SourcePosition s, Grammar g, SymbolId tableName) {
		return new Xis(s, g, tableName, /* is */false);
	}

	public final static Expression newXexists(SourcePosition s, SymbolId tableName, String symbol) {
		return new Xexists(s, tableName, symbol);
	}

	public final static Expression newDefIndent(SourcePosition s) {
		return new Xdefindent(s);
	}

	public final static Expression newIndent(SourcePosition s) {
		return new Xindent(s);
	}

	@Deprecated
	public final static Expression newScan(SourcePosition s, int number, Expression scan, Expression repeat) {
		return null;
	}

	@Deprecated
	public final static Expression newRepeat(SourcePosition s, Expression e) {
		return null;
	}

	// -----------------------------------------------------------------------

	public static final Expression newString(SourcePosition s, String text) {
		byte[] utf8 = StringUtils.toUtf8(text);
		if (utf8.length == 0) {
			return newEmpty(s);
		}
		if (utf8.length == 1) {
			return newCbyte(s, false, utf8[0]);
		}
		return newByteSequence(s, false, utf8);
	}

	public final static Expression newByteSequence(SourcePosition s, boolean binary, byte[] utf8) {
		UList<Expression> l = new UList<Expression>(new Expression[utf8.length]);
		for (int i = 0; i < utf8.length; i++) {
			l.add(newCbyte(s, binary, utf8[i]));
		}
		return newPsequence(s, l);
	}

	public final static Expression newCharSet(SourcePosition s, String text) {
		boolean b[] = StringUtils.parseByteMap(text);
		return new Cset(s, false, b);
	}

	public final static Expression newCharSet(SourcePosition s, String t, String t2) {
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

	public final static Expression newByteRange(SourcePosition s, boolean binary, int c, int c2) {
		if (c == c2) {
			return newCbyte(s, binary, c);
		}
		return new Cset(s, binary, c, c2);
	}

	private final static Expression newUnicodeRange(SourcePosition s, int c, int c2) {
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
		return newPchoice(s, l);
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

	private final static Expression newUnicodeRange(SourcePosition s, byte[] b, byte[] b2) {
		if (b[b.length - 1] == b2[b.length - 1]) {
			return newByteSequence(s, false, b);
		} else {
			UList<Expression> l = new UList<Expression>(new Expression[b.length]);
			for (int i = 0; i < b.length - 1; i++) {
				l.add(newCbyte(s, false, b[i]));
			}
			l.add(newByteRange(s, false, b[b.length - 1] & 0xff, b2[b2.length - 1] & 0xff));
			return newPsequence(s, l);
		}
	}

	public final static Expression newNewCapture(SourcePosition s, Expression e) {
		return newNewCapture(s, false, null, e);
	}

	public final static Expression newNewCapture(SourcePosition s, boolean lefted, SymbolId label, Expression e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size() + 3]);
		ExpressionCommons.addSequence(l, new Tnew(s, lefted, label, 0));
		ExpressionCommons.addSequence(l, e);
		ExpressionCommons.addSequence(l, ExpressionCommons.newTcapture(s, 0));
		return newPsequence(s, l);
	}

	public final static Expression newLeftFoldOption(SourcePosition s, SymbolId label, Expression e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size() + 3]);
		ExpressionCommons.addSequence(l, new Tnew(s, true, label, 0));
		ExpressionCommons.addSequence(l, e);
		ExpressionCommons.addSequence(l, ExpressionCommons.newTcapture(s, 0));
		return newUoption(s, ExpressionCommons.newPsequence(s, l));
	}

	public final static Expression newLeftFoldRepetition(SourcePosition s, SymbolId label, Expression e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size() + 3]);
		ExpressionCommons.addSequence(l, new Tnew(s, true, label, 0));
		ExpressionCommons.addSequence(l, e);
		ExpressionCommons.addSequence(l, ExpressionCommons.newTcapture(s, 0));
		return newUzero(s, ExpressionCommons.newPsequence(s, l));
	}

	public final static Expression newLeftFoldRepetition1(SourcePosition s, SymbolId label, Expression e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size() + 3]);
		ExpressionCommons.addSequence(l, new Tnew(s, true, label, 0));
		ExpressionCommons.addSequence(l, e);
		ExpressionCommons.addSequence(l, ExpressionCommons.newTcapture(s, 0));
		return newUone(s, ExpressionCommons.newPsequence(s, l));
	}

}
