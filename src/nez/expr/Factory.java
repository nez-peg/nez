package nez.expr;

import nez.Grammar;
import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.util.StringUtils;
import nez.util.UList;
import nez.util.UMap;


public class Factory {
	public final static UMap<Expression> uniqueMap = new UMap<Expression>();
	static Expression intern(Expression e) {
		if(e.internId == 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(e.getInterningKey());
			for(int i = 0; i < e.size(); i++) {
				Expression sube = e.get(i);
				if(!sube.isInterned()) {
					sube = sube.intern();
					e.set(i, sube);
				}
				sb.append("#" + sube.internId);
			}
			String key = sb.toString();
			Expression u = uniqueMap.get(key);
			if(u == null) {
				u = e;
				e.s = null;
				e.internId = uniqueMap.size() + 1;
				uniqueMap.put(key, e);
			}
			assert(u.getClass() == e.getClass());
			return u;
		}
		return e;
	}
	
	static Expression internImpl(SourcePosition s, Expression e) {
		return (s == null) ? intern(e) : e;
	}
	
//	private static boolean Conservative = false;
//	private static boolean StringSpecialization = true;
//	private static boolean CharacterChoice      = true;
	
	public final static Expression newNonTerminal(SourcePosition s, Grammar peg, String name) {
		return internImpl(s, new NonTerminal(s, peg, name));
	}
	
	public final static Expression newEmpty(SourcePosition s) {
		return internImpl(s, new Empty(s));
	}

	public final static Expression newFailure(SourcePosition s) {
		return internImpl(s, new Failure(s));
	}

	public final static Expression newByteChar(SourcePosition s, int ch) {
		return internImpl(s, new ByteChar(s, ch & 0xff));
	}
	
	public final static Expression newAnyChar(SourcePosition s) {
		return internImpl(s, new AnyChar(s));
	}
	
	public static Expression newByteMap(SourcePosition s, boolean[] byteMap) {
		return internImpl(s, new ByteMap(s, byteMap));
	}
	
	public final static Expression newOption(SourcePosition s, Expression p) {
		s = p.isInterned() ? null : s;
		return internImpl(s, new Option(s, p));
	}
		
	public final static Expression newRepetition(SourcePosition s, Expression p) {
		s = p.isInterned() ? null : s;
		return internImpl(s, new Repetition(s, p));
	}

	public final static Expression newRepetition1(SourcePosition s, Expression p) {
		s = p.isInterned() ? null : s;
		return internImpl(s, new Repetition1(s, p));
	}

	public final static Expression newAnd(SourcePosition s, Expression p) {
		s = p.isInterned() ? null : s;
		return internImpl(s, new And(s, p));
	}
	
	public final static Expression newNot(SourcePosition s, Expression p) {
		s = p.isInterned() ? null : s;
		return internImpl(s, new Not(s, p));
	}

	final static boolean isInterned(UList<Expression> l) {
		for(int i = 0; i < l.size(); i++) {
			if(!l.ArrayValues[i].isInterned()) {
				return false;
			}
		}
		return true;
	}

	public final static Expression newSequence(SourcePosition s, UList<Expression> l) {
		if(l.size() == 0) {
			return internImpl(s, newEmpty(s));
		}
		if(l.size() == 1) {
			return internImpl(s, l.ArrayValues[0]);
		}
		if(s != null && isInterned(l)) {
			s = null;
		}
		return internImpl(s, new Sequence(s, l));
	}

//	public final static Expression newSequence(SourcePosition s, UList<Expression> l, boolean optimized) {
//		if(l.size() == 0) {
//			return internImpl(s, newEmpty(s));
//		}
//		if(l.size() == 1) {
//			return internImpl(s, l.ArrayValues[0]);
//		}
//		if(optimized) {
//			for(int i = 1; i < l.size(); i++) {
//				Expression p = l.ArrayValues[i-1];
//				Expression e = l.ArrayValues[i];
//				if(Expression.isByteConsumed(e)) {
//					if(Expression.isPositionIndependentOperation(p)) {
//						l.ArrayValues[i-1] = e;
//						l.ArrayValues[i]   = p;
//						continue;
//					}
//					if(p instanceof New) {
//						New n = (New)p;
//						l.ArrayValues[i-1] = e;
//						if(n.isInterned()) {
//							l.ArrayValues[i] =  Factory.newNew(s, n.lefted, n.shift - 1);
//						}
//						else {
//							n.shift -= 1;
//							l.ArrayValues[i]   = n;
//						}
//						continue;
//					}
//					if(p instanceof Capture) {
//						Capture n = (Capture)p;
//						l.ArrayValues[i-1] = e;
//						if(n.isInterned()) {
//							l.ArrayValues[i] =  Factory.newCapture(s, n.shift - 1);
//						}
//						else {
//							n.shift -= 1;
//							l.ArrayValues[i]   = n;
//						}
//						continue;
//					}
//				}
//			}
//		}
//		return internImpl(s, new Sequence(s, l));
//	}

	private New shift(New n, int shift) {
		n.shift += shift;
		return n;
	}

	private Capture shift(Capture n, int shift) {
		n.shift += shift;
		return n;
	}

	
	public final static Expression newChoice(SourcePosition s, UList<Expression> l) {
		int size = l.size();
		for(int i = 0; i < size; i++) {
			if(l.ArrayValues[i] instanceof Empty) {
				size = i + 1;
				break;
			}
		}
		if(size == 1) {
			return l.ArrayValues[0];
		}
		if(s != null && isInterned(l)) {
			s = null;
		}
		if(l.ArrayValues[size - 1] instanceof Empty) {
			return newOption(s, new Choice(s, l, size-1));  //     e / '' => e?
		}
		return internImpl(s, new Choice(s, l, size));
	}
	
	// FIXME
	public final static Expression newDirectChoice(SourcePosition s, UList<Expression> l) {
		int size = l.size();
		for(int i = 0; i < size; i++) {
			if(l.ArrayValues[i] instanceof Empty) {
				size = i + 1;
				break;
			}
		}
		if(size == 1) {
			return l.ArrayValues[0];
		}
		if(s != null && isInterned(l)) {
			s = null;
		}
		return internImpl(s, new Choice(s, l, size));
	}

	public final static Expression newChoice(SourcePosition s, Expression p, Expression p2) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		addChoice(l, p);
		addChoice(l, p2);
		return newChoice(s, l);
	}
	
	public final static void addChoice(UList<Expression> l, Expression e) {
		if(e instanceof Choice) {
			for(int i = 0; i < e.size(); i++) {
				addChoice(l, e.get(i));
			}
		}
		else if (!(e instanceof Failure)) {
			l.add(e);
		}
	}

	public final static Expression newCharSet(SourcePosition s, String text) {
		boolean b[] = StringUtils.parseByteMap(text);
		return internImpl(s, new ByteMap(s, b));
	}

	public final static Expression appendAsChoice(Expression e, Expression e2) {
		if(e == null) return e2;
		if(e2 == null) return e;
		UList<Expression> l = new UList<Expression>(new Expression[e.size()+e2.size()]);
		addChoice(l, e);
		addChoice(l, e2);
		return newChoice(null, l);
	}

	public final static void addSequence(UList<Expression> l, Expression e) {
		if(e instanceof Empty) {
			return;
		}
		if(e instanceof Sequence) {
			for(int i = 0; i < e.size(); i++) {
				addSequence(l, e.get(i));
			}
			return;
		}
		if(l.size() > 0) {
			Expression pe = l.ArrayValues[l.size()-1];
			if(e instanceof Not && pe instanceof Not) {
				((Not) pe).inner = appendAsChoice(((Not) pe).inner, ((Not) e).inner);
				return;
			}
			if(pe instanceof Failure) {
				return;
			}
		}
		l.add(e);
	}

	public static final Expression newString(SourcePosition s, String text) {
		byte[] utf8 = StringUtils.toUtf8(text);
		if(utf8.length == 0) {
			return newEmpty(s);
		}
		if(utf8.length == 1) {
			return newByteChar(s, utf8[0]);
		}
		UList<Expression> l = new UList<Expression>(new Expression[utf8.length]);
		for(int i = 0; i < utf8.length; i++) {
			l.add(newByteChar(s, utf8[i]));
		}
		return newSequence(s, l);
	}
	
	public final static Expression newByteSequence(SourcePosition s, byte[] utf8) {
		UList<Expression> l = new UList<Expression>(new Expression[utf8.length]);
		for(int i = 0; i < utf8.length; i++) {
			l.add(newByteChar(s, utf8[i]));
		}
		return newSequence(s, l);
	}

	public final static Expression newByteRange(SourcePosition s, int c, int c2) {
		if(c == c2) {
			return newByteChar(s, c);
		}
		return internImpl(s, new ByteMap(s, c, c2));
	}

	public final static Expression newCharSet(SourcePosition s, String t, String t2) {
		int c = StringUtils.parseAscii(t);
		int c2 = StringUtils.parseAscii(t2);
		if(c != -1 && c2 != -1) {
			return newByteRange(s, c, c2);
		}
		c = StringUtils.parseUnicode(t);
		c2 = StringUtils.parseUnicode(t2);
		if(c < 128 && c2 < 128) {
			return newByteRange(s, c, c2);
		}
		else {
			return newUnicodeRange(s, c, c2);
		}
	}
	
	private final static Expression newUnicodeRange(SourcePosition s, int c, int c2) {
		byte[] b = StringUtils.toUtf8(String.valueOf((char)c));
		byte[] b2 = StringUtils.toUtf8(String.valueOf((char)c2));
		if(equalsBase(b, b2)) {
			return newUnicodeRange(s, b, b2);
		}
		UList<Expression> l = new UList<Expression>(new Expression[b.length]);
		b2 = b;
		for(int pc = c + 1; pc <= c2; pc++) {
			byte[] b3 = StringUtils.toUtf8(String.valueOf((char)pc));
			if(equalsBase(b, b3)) {
				b2 = b3;
				continue;
			}
			l.add(newUnicodeRange(s, b, b2));
			b = b3;
			b2 = b3;
		}
		b2 = StringUtils.toUtf8(String.valueOf((char)c2));
		l.add(newUnicodeRange(s, b, b2));
		return newChoice(s, l);
	}

	private final static boolean equalsBase(byte[] b, byte[] b2) {
		if(b.length == b2.length) {
			switch(b.length) {
			case 3: return b[0] == b2[0] && b[1] == b2[1];
			case 4: return b[0] == b2[0] && b[1] == b2[1] && b[2] == b2[2];
			}
			return b[0] == b2[0];
		}
		return false;
	}

	private final static Expression newUnicodeRange(SourcePosition s, byte[] b, byte[] b2) {
		if(b[b.length-1] == b2[b.length-1]) {
			return newByteSequence(s, b);
		}
		else {
			UList<Expression> l = new UList<Expression>(new Expression[b.length]);
			for(int i = 0; i < b.length-1; i++) {
				l.add(newByteChar(s, b[i]));
			}
			l.add(newByteRange(s, b[b.length-1] & 0xff, b2[b2.length-1] & 0xff));
			return newSequence(s, l);
		}
	}

	
	// PEG4d

	public final static Expression newMatch(SourcePosition s, Expression p) {
		if(p.isInterned()) {
			s = null;
		}
		return internImpl(s, new Match(s, p));
	}
	
	public final static Expression newLink(SourcePosition s, Expression p, int index) {
		if(p.isInterned()) {
			s = null;
		}
		return internImpl(s, new Link(s, p, index));
	}

//	public final static Expression newNew(SourcePosition s, UList<Expression> l) {
//		if(s != null && isInterned(l)) {
//			s = null;
//		}
//		return internImpl(s, new NewClosure(s, l));
//	}
//
//	public final static Expression newLeftNew(SourcePosition s, UList<Expression> l) {
//		if(s != null && isInterned(l)) {
//			s = null;
//		}
//		return internImpl(s, new LeftNewClosure(s, l));
//	}

	public final static Expression newNew(SourcePosition s, boolean lefted, int shift) {
		return internImpl(s, new New(s, lefted, shift));
	}

	public final static Expression newCapture(SourcePosition s, int shift) {
		return internImpl(s, new Capture(s, shift));
	}

	public final static Expression newNew(SourcePosition s, boolean lefted, Expression e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size() + 3]);
		Factory.addSequence(l, internImpl(s, new New(s, lefted, 0)));
		Factory.addSequence(l, e);
		Factory.addSequence(l, Factory.newCapture(s, 0));
		return newSequence(s, l);
	}

	public final static Expression newLeftNewOption(SourcePosition s, Expression e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size() + 3]);
		Factory.addSequence(l, internImpl(s, new New(s, true, 0)));
		Factory.addSequence(l, e);
		Factory.addSequence(l, Factory.newCapture(s, 0));
		return newOption(s, Factory.newSequence(s, l));
	}

	public final static Expression newLeftNewRepetition(SourcePosition s, Expression e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size() + 3]);
		Factory.addSequence(l, internImpl(s, new New(s, true, 0)));
		Factory.addSequence(l, e);
		Factory.addSequence(l, Factory.newCapture(s, 0));
		return newRepetition(s, Factory.newSequence(s, l));
	}

	public final static Expression newLeftNewRepetition1(SourcePosition s, Expression e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size() + 3]);
		Factory.addSequence(l, internImpl(s, new New(s, true, 0)));
		Factory.addSequence(l, e);
		Factory.addSequence(l, Factory.newCapture(s, 0));
		return newRepetition1(s, Factory.newSequence(s, l));
	}

	
	public final static UList<Expression> toSequenceList(Expression e) {
		UList<Expression> l = null;
		if(e instanceof Sequence) {
			l = ((Sequence) e).newList();
			for(Expression se : e) {
				l.add(se);
			}
			return l;
		}
		else {
			l = new UList<Expression>(new Expression[1]);
			l.add(e);
		}
		return l;
	}
		
	public final static Expression newTagging(SourcePosition s, Tag tag) {
		return internImpl(s, new Tagging(s, tag));
	}

	public final static Expression newReplace(SourcePosition s, String msg) {
		return internImpl(s, new Replace(s, msg));
	}
	
//	public final static Expression newDebug(Expression e) {
//		return checkUnique(new ParsingAssert(e), e.isInterned());
//	}
//
//	public final static Expression newFail(String message) {
//		return new ParsingFailure(message);
//	}
//
//	public final static Expression newCatch() {
//		return new ParsingCatch();
//	}
	
	// Conditional Parsing
	// <if FLAG>
	// <with FLAG e>
	// <without FLAG e>
	
	public final static Expression newIfFlag(SourcePosition s, String flagName) {
		return internImpl(s, new IfFlag(s, flagName));
	}
	
	public final static Expression newWithFlag(SourcePosition s, String flagName, Expression e) {
		return internImpl(s, new WithFlag(s, flagName, e));
	}

	public final static Expression newWithoutFlag(SourcePosition s, String flagName, Expression e) {
		return internImpl(s, new WithoutFlag(s, flagName, e));
	}

	public final static Expression newScan(SourcePosition s, int number, Expression scan, Expression repeat) {
		return null;
	}
	
	public final static Expression newRepeat(SourcePosition s, Expression e) {
		return null;
	}

//	public final static AbstractExpression newPermutation(UList<AbstractExpression> l) {
//		if(l.size() == 0) {
//			return newEmpty();
//		}
//		if(l.size() == 1) {
//			return l.ArrayValues[0];
//		}
//		return new ParsingPermutation(l);
//	}
	
	public final static Expression newBlock(SourcePosition s, Expression e) {
		return internImpl(s, new Block(s, e));
	}

	public static Expression newDefSymbol(SourcePosition s, Tag table, Expression e) {
		return internImpl(s, new DefSymbol(s, table, e));
	}

	public static Expression newIsSymbol(SourcePosition s, Tag table) {
		return internImpl(s, new IsSymbol(s, /*checkLastSymbolOnly*/true, table));
	}
	
	public static Expression newIsaSymbol(SourcePosition s, Tag table) {
		return internImpl(s, new IsSymbol(s, /*checkLastSymbolOnly*/false, table));
	}

	public static Expression newDefIndent(SourcePosition s) {
		return internImpl(s, new DefIndent(s));
	}

	public final static Expression newIndent(SourcePosition s) {
		return internImpl(s, new IsIndent(s));
	}

	// -----------------------------------------------------------------------
	
	public final static Expression resolveNonTerminal(Expression e) {
		while(e instanceof NonTerminal) {
			NonTerminal nterm = (NonTerminal) e;
			e = nterm.deReference();
		}
		return e;
	}

	
}
