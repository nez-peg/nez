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

	static Expression internImpl(SourcePosition s, Expression e) {
		return (s == null) ? intern(e) : e;
	}

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
		if (e instanceof Sequence) {
			for (int i = 0; i < e.size(); i++) {
				addSequence(l, e.get(i));
			}
			return;
		}
		if (e instanceof Empty) {
			return;
		}
		if (l.size() > 0) {
			Expression prev = l.ArrayValues[l.size() - 1];
			if (prev instanceof Failure) {
				return;
			}
		}
		l.add(e);
	}

	public final static void addChoice(UList<Expression> l, Expression e) {
		if (e instanceof Choice) {
			for (int i = 0; i < e.size(); i++) {
				addChoice(l, e.get(i));
			}
			return;
		}
		if (e instanceof Failure) {
			return;
		}
		if (l.size() > 0) {
			Expression prev = l.ArrayValues[l.size() - 1];
			if (prev instanceof Empty) {
				return;
			}
		}
		l.add(e);
	}

	// -----------------------------------------------------------------------

	public final static Expression newNonTerminal(SourcePosition s, Grammar g, String name) {
		return internImpl(s, new NonTerminal(s, g, name));
	}

	public final static Expression newEmpty(SourcePosition s) {
		return internImpl(s, new Empty(s));
	}

	public final static Expression newFailure(SourcePosition s) {
		return internImpl(s, new Failure(s));
	}

	/* Terminal */

	public final static Expression newAnyChar(SourcePosition s, boolean binary) {
		return internImpl(s, new Cany(s, binary));
	}

	public final static Expression newByteChar(SourcePosition s, boolean binary, int ch) {
		if (ch == 0) {
			binary = true;
		}
		return internImpl(s, new Cbyte(s, binary, ch & 0xff));
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

	public static Expression newMultiChar(SourcePosition s, boolean binary, byte[] utf8) {
		return internImpl(s, new Cmulti(s, binary, utf8));
	}

	public static Expression newByteMap(SourcePosition s, boolean binary, boolean[] byteMap) {
		int byteChar = uniqueByteChar(byteMap);
		if (byteChar != -1) {
			return internImpl(s, newByteChar(s, binary, byteChar));
		}
		return internImpl(s, new Cset(s, binary, byteMap));
	}

	public static final Expression newString(SourcePosition s, String text) {
		byte[] utf8 = StringUtils.toUtf8(text);
		if (utf8.length == 0) {
			return newEmpty(s);
		}
		if (utf8.length == 1) {
			return newByteChar(s, false, utf8[0]);
		}
		return newByteSequence(s, false, utf8);
	}

	public final static Expression newByteSequence(SourcePosition s, boolean binary, byte[] utf8) {
		UList<Expression> l = new UList<Expression>(new Expression[utf8.length]);
		for (int i = 0; i < utf8.length; i++) {
			l.add(newByteChar(s, binary, utf8[i]));
		}
		return newSequence(s, l);
	}

	public final static Expression newCharSet(SourcePosition s, String text) {
		boolean b[] = StringUtils.parseByteMap(text);
		return internImpl(s, new Cset(s, false, b));
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
			return newByteChar(s, binary, c);
		}
		return internImpl(s, new Cset(s, binary, c, c2));
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

	private final static Expression newUnicodeRange(SourcePosition s, byte[] b, byte[] b2) {
		if (b[b.length - 1] == b2[b.length - 1]) {
			return newByteSequence(s, false, b);
		} else {
			UList<Expression> l = new UList<Expression>(new Expression[b.length]);
			for (int i = 0; i < b.length - 1; i++) {
				l.add(newByteChar(s, false, b[i]));
			}
			l.add(newByteRange(s, false, b[b.length - 1] & 0xff, b2[b2.length - 1] & 0xff));
			return newSequence(s, l);
		}
	}

	/* Unary */

	public final static Expression newOption(SourcePosition s, Expression p) {
		return new Uoption(s, p);
	}

	public final static Expression newRepetition(SourcePosition s, Expression p) {
		return new Uzero(s, p);
	}

	public final static Expression newRepetition1(SourcePosition s, Expression p) {
		return new Uone(s, p);
	}

	public final static Uand newAnd(SourcePosition s, Expression p) {
		return new Uand(s, p);
	}

	public final static Unot newNot(SourcePosition s, Expression p) {
		return new Unot(s, p);
	}

	public final static Expression newSequence(SourcePosition s, UList<Expression> l) {
		if (l.size() == 0) {
			return newEmpty(s);
		}
		return newSequence(s, 0, l);
	}

	private final static Expression newSequence(SourcePosition s, int start, UList<Expression> l) {
		Expression first = internImpl(s, l.ArrayValues[start]);
		if (start + 1 == l.size()) {
			return first;
		}
		Expression seq = new Sequence(s, first, newSequence(s, start + 1, l));
		return internImpl(s, seq);
	}

	public final static Expression newSequence(SourcePosition s, Expression p, Expression p2) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		addSequence(l, p);
		addSequence(l, p2);
		return newSequence(s, l);
	}

	public final static Expression newChoice(SourcePosition s, UList<Expression> l) {
		int size = l.size();
		for (int i = 0; i < size; i++) {
			if (l.ArrayValues[i] instanceof Empty) {
				size = i + 1;
				break;
			}
		}
		if (size == 1) {
			return l.ArrayValues[0];
		}
		return new Choice(s, l, size);
	}

	public final static Expression newChoice(SourcePosition s, Expression p, Expression p2) {
		if (p == null)
			return newEmpty(s);
		if (p2 == null)
			return p;
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		addChoice(l, p);
		addChoice(l, p2);
		return newChoice(s, l);
	}

	// AST Construction

	public final static Expression newMatch(SourcePosition s, Expression p) {
		return new Umatch(s, p);
	}

	public final static Expression newLink(SourcePosition s, Expression p) {
		return newLink(s, null, p);
	}

	public final static Expression newLink(SourcePosition s, SymbolId label, Expression p) {
		return new Tlink(s, label, p);
	}

	public final static Expression newNew(SourcePosition s, boolean lefted, SymbolId label, int shift) {
		return new Tnew(s, lefted, label, shift);
	}

	public final static Expression newCapture(SourcePosition s, int shift) {
		return new Tcapture(s, shift);
	}

	public final static Expression newNew(SourcePosition s, boolean lefted, SymbolId label, Expression e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size() + 3]);
		ExpressionCommons.addSequence(l, internImpl(s, new Tnew(s, lefted, label, 0)));
		ExpressionCommons.addSequence(l, e);
		ExpressionCommons.addSequence(l, ExpressionCommons.newCapture(s, 0));
		return newSequence(s, l);
	}

	public final static Expression newLeftFoldOption(SourcePosition s, SymbolId label, Expression e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size() + 3]);
		ExpressionCommons.addSequence(l, internImpl(s, new Tnew(s, true, label, 0)));
		ExpressionCommons.addSequence(l, e);
		ExpressionCommons.addSequence(l, ExpressionCommons.newCapture(s, 0));
		return newOption(s, ExpressionCommons.newSequence(s, l));
	}

	public final static Expression newLeftFoldRepetition(SourcePosition s, SymbolId label, Expression e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size() + 3]);
		ExpressionCommons.addSequence(l, internImpl(s, new Tnew(s, true, label, 0)));
		ExpressionCommons.addSequence(l, e);
		ExpressionCommons.addSequence(l, ExpressionCommons.newCapture(s, 0));
		return newRepetition(s, ExpressionCommons.newSequence(s, l));
	}

	public final static Expression newLeftFoldRepetition1(SourcePosition s, SymbolId label, Expression e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size() + 3]);
		ExpressionCommons.addSequence(l, internImpl(s, new Tnew(s, true, label, 0)));
		ExpressionCommons.addSequence(l, e);
		ExpressionCommons.addSequence(l, ExpressionCommons.newCapture(s, 0));
		return newRepetition1(s, ExpressionCommons.newSequence(s, l));
	}

	public final static Expression newTagging(SourcePosition s, SymbolId tag) {
		return internImpl(s, new Ttag(s, tag));
	}

	public final static Expression newReplace(SourcePosition s, String msg) {
		return internImpl(s, new Treplace(s, msg));
	}

	// Conditional Parsing
	// <if FLAG>
	// <on FLAG e>
	// <on! FLAG e>

	public final static Expression newIfFlag(SourcePosition s, String flagName) {
		return internImpl(s, new Xif(s, true, flagName));
	}

	public final static Expression newXon(SourcePosition s, boolean predicate, String flagName, Expression e) {
		return internImpl(s, new Xon(s, predicate, flagName, e));
	}

	public final static Expression newBlock(SourcePosition s, Expression e) {
		return internImpl(s, new Xblock(s, e));
	}

	public final static Expression newLocal(SourcePosition s, SymbolId tableName, Expression e) {
		return internImpl(s, new Xlocal(s, tableName, e));
	}

	public final static Expression newDefSymbol(SourcePosition s, Grammar g, SymbolId tableName, Expression e) {
		return internImpl(s, new Xdef(s, g, tableName, e));
	}

	public final static Expression newMatchSymbol(SourcePosition s, SymbolId tableName) {
		return internImpl(s, new Xmatch(s, tableName));
	}

	public final static Expression newIsSymbol(SourcePosition s, Grammar g, SymbolId tableName) {
		return internImpl(s, new Xis(s, g, tableName, /* is */true));
	}

	public final static Expression newIsaSymbol(SourcePosition s, Grammar g, SymbolId tableName) {
		return internImpl(s, new Xis(s, g, tableName, /* is */false));
	}

	public final static Expression newExists(SourcePosition s, SymbolId tableName, String symbol) {
		return internImpl(s, new Xexists(s, tableName, symbol));
	}

	public final static Expression newDefIndent(SourcePosition s) {
		return internImpl(s, new Xdefindent(s));
	}

	public final static Expression newIndent(SourcePosition s) {
		return internImpl(s, new Xindent(s));
	}

	@Deprecated
	public final static Expression newScan(SourcePosition s, int number, Expression scan, Expression repeat) {
		return null;
	}

	@Deprecated
	public final static Expression newRepeat(SourcePosition s, Expression e) {
		return null;
	}

	// ----------------------------------------------------------------------

	// protected GrammarMap getGrammar() {
	// return null;
	// }
	//
	// public final Expression newNonTerminal(String name) {
	// return ExpressionCommons.newNonTerminal(getSourcePosition(),
	// getGrammar(), name);
	// }
	//
	// @Override
	// public final Expression newEmpty() {
	// return ExpressionCommons.newEmpty(getSourcePosition());
	// }
	//
	// @Override
	// public final Expression newFailure() {
	// return ExpressionCommons.newFailure(getSourcePosition());
	// }
	//
	// public final Expression newByteChar(int ch) {
	// return ExpressionCommons.newByteChar(getSourcePosition(), false, ch);
	// }
	//
	// public final Expression newAnyChar() {
	// return ExpressionCommons.newAnyChar(getSourcePosition(), false);
	// }
	//
	// public final Expression newString(String text) {
	// return ExpressionCommons.newString(getSourcePosition(), text);
	// }
	//
	// public final Expression newCharSet(String text) {
	// return ExpressionCommons.newCharSet(getSourcePosition(), text);
	// }
	//
	// public final Expression newByteMap(boolean[] byteMap) {
	// return ExpressionCommons.newByteMap(getSourcePosition(), false, byteMap);
	// }
	//
	// public final Expression newSequence(Expression... seq) {
	// UList<Expression> l = new UList<Expression>(new Expression[8]);
	// for (Expression p : seq) {
	// ExpressionCommons.addSequence(l, p);
	// }
	// return ExpressionCommons.newSequence(getSourcePosition(), l);
	// }
	//
	// public final Expression newChoice(Expression... seq) {
	// UList<Expression> l = new UList<Expression>(new Expression[8]);
	// for (Expression p : seq) {
	// ExpressionCommons.addChoice(l, p);
	// }
	// return ExpressionCommons.newChoice(getSourcePosition(), l);
	// }
	//
	// public final Expression newOption(Expression... seq) {
	// return ExpressionCommons.newOption(getSourcePosition(),
	// newSequence(seq));
	// }
	//
	// public final Expression newRepetition(Expression... seq) {
	// return ExpressionCommons.newRepetition(getSourcePosition(),
	// newSequence(seq));
	// }
	//
	// public final Expression newRepetition1(Expression... seq) {
	// return ExpressionCommons.newRepetition1(getSourcePosition(),
	// newSequence(seq));
	// }
	//
	// public final Expression newAnd(Expression... seq) {
	// return ExpressionCommons.newAnd(getSourcePosition(), newSequence(seq));
	// }
	//
	// public final Expression newNot(Expression... seq) {
	// return ExpressionCommons.newNot(getSourcePosition(), newSequence(seq));
	// }
	//
	// // public final Expression newByteRange(int c, int c2) {
	// // if(c == c2) {
	// // return newByteChar(s, c);
	// // }
	// // return internImpl(s, new ByteMap(s, c, c2));
	// // }
	//
	// // PEG4d
	// public final Expression newMatch(Expression... seq) {
	// return ExpressionCommons.newMatch(getSourcePosition(), newSequence(seq));
	// }
	//
	// public final Expression newLink(Expression... seq) {
	// return ExpressionCommons.newLink(getSourcePosition(), null,
	// newSequence(seq));
	// }
	//
	// public final Expression newLink(Tag label, Expression... seq) {
	// return ExpressionCommons.newLink(getSourcePosition(), label,
	// newSequence(seq));
	// }
	//
	// public final Expression newNew(Expression... seq) {
	// return ExpressionCommons.newNew(getSourcePosition(), false, null,
	// newSequence(seq));
	// }
	//
	// // public final Expression newLeftNew(Expression ... seq) {
	// // return GrammarFactory.newNew(getSourcePosition(), true,
	// // newSequence(seq));
	// // }
	//
	// public final Expression newTagging(String tag) {
	// return ExpressionCommons.newTagging(getSourcePosition(), Tag.tag(tag));
	// }
	//
	// public final Expression newReplace(String msg) {
	// return ExpressionCommons.newReplace(getSourcePosition(), msg);
	// }
	//
	// // Conditional Parsing
	// // <if FLAG>
	// // <on FLAG e>
	// // <on !FLAG e>
	//
	// public final Expression newIfFlag(String flagName) {
	// return ExpressionCommons.newIfFlag(getSourcePosition(), flagName);
	// }
	//
	// public final Expression newXon(String flagName, Expression... seq) {
	// return ExpressionCommons.newXon(getSourcePosition(), true, flagName,
	// newSequence(seq));
	// }
	//
	// public final Expression newScan(int number, Expression scan, Expression
	// repeat) {
	// return null;
	// }
	//
	// public final Expression newRepeat(Expression e) {
	// return null;
	// }
	//
	// public final Expression newBlock(Expression... seq) {
	// return ExpressionCommons.newBlock(getSourcePosition(), newSequence(seq));
	// }
	//
	// public final Expression newDefSymbol(String table, Expression... seq) {
	// return ExpressionCommons.newDefSymbol(getSourcePosition(), getGrammar(),
	// Tag.tag(table), newSequence(seq));
	// }
	//
	// public final Expression newIsSymbol(String table) {
	// return ExpressionCommons.newIsSymbol(getSourcePosition(), getGrammar(),
	// Tag.tag(table));
	// }
	//
	// public final Expression newIsaSymbol(String table) {
	// return ExpressionCommons.newIsaSymbol(getSourcePosition(), getGrammar(),
	// Tag.tag(table));
	// }
	//
	// public final Expression newExists(String table, String symbol) {
	// return ExpressionCommons.newExists(getSourcePosition(), Tag.tag(table),
	// symbol);
	// }
	//
	// public final Expression newLocal(String table, Expression... seq) {
	// return ExpressionCommons.newLocal(getSourcePosition(), Tag.tag(table),
	// newSequence(seq));
	// }
	//
	// public final Expression newDefIndent() {
	// return ExpressionCommons.newDefIndent(getSourcePosition());
	// }
	//
	// public final Expression newIndent() {
	// return ExpressionCommons.newIndent(getSourcePosition());
	// }

}
