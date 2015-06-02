package nez.lang;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.main.Command;
import nez.main.Verbose;
import nez.util.StringUtils;
import nez.util.UList;
import nez.util.UMap;


public class GrammarFactory {
	public final static UMap<Expression> uniqueMap = new UMap<Expression>();
	
	static Expression intern(Expression e) {
		if(e.internId == 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(e.key());
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
			if(Command.ReleasePreview) {
				if(!u.equalsExpression(e)) {
					Verbose.debug("Mismatched Interning: " + e.getClass() + "\n\te="+e + "\n\tinterned="+u);
				}
				assert(u.equalsExpression(e));
			}
			return u;
		}
		return e;
	}
	
	static Expression internImpl(SourcePosition s, Expression e) {
		return (s == null) ? intern(e) : e;
	}
	
	// -----------------------------------------------------------------------
	// Utils
	
	public final static Expression resolveNonTerminal(Expression e) {
		while(e instanceof NonTerminal) {
			NonTerminal nterm = (NonTerminal) e;
			e = nterm.deReference();
		}
		return e;
	}
	
	public final static UList<Expression> newList(int size) {
		return new UList<Expression>(new Expression[size]);
	}
	
	public final static void addSequence(UList<Expression> l, Expression e) {
		if(e instanceof Sequence) {
			for(int i = 0; i < e.size(); i++) {
				addSequence(l, e.get(i));
			}
			return;
		}
		if(e instanceof Empty) {
			return;
		}
		if(l.size() > 0) {
			Expression prev = l.ArrayValues[l.size()-1];
//			if(e instanceof Not && pe instanceof Not) {
//				((Not) pe).inner = appendAsChoice(((Not) pe).inner, ((Not) e).inner);
//				return;
//			}
			if(prev instanceof Failure) {
				return;
			}
		}
		l.add(e);
	}

	public final static void addChoice(UList<Expression> l, Expression e) {
		if(e instanceof Choice) {
			for(int i = 0; i < e.size(); i++) {
				addChoice(l, e.get(i));
			}
			return;
		}
		if(e instanceof Failure) {
			return ;
		}
		if(l.size() > 0) {
			Expression prev = l.ArrayValues[l.size()-1];
			if(prev instanceof Empty) {
				return ;
			}
		}
		l.add(e);
	}

//	private final static Expression appendAsChoice(Expression e, Expression e2) {
//		if(e == null) return e2;
//		if(e2 == null) return e;
//		UList<Expression> l = new UList<Expression>(new Expression[e.size()+e2.size()]);
//		addChoice(l, e);
//		addChoice(l, e2);
//		return newChoice(null, l);
//	}

	// -----------------------------------------------------------------------
		
	public final static Expression newNonTerminal(SourcePosition s, NameSpace peg, String name) {
		return internImpl(s, new NonTerminal(s, peg, name));
	}
	
	public final static Expression newEmpty(SourcePosition s) {
		return internImpl(s, new Empty(s));
	}

	public final static Expression newFailure(SourcePosition s) {
		return internImpl(s, new Failure(s));
	}

	/* Terminal */
	
	public final static Expression newAnyChar(SourcePosition s, boolean binary) {
		return internImpl(s, new AnyChar(s, binary));
	}

	public final static Expression newByteChar(SourcePosition s, boolean binary, int ch) {
		if(ch == 0) {
			binary = true;
		}
		return internImpl(s, new ByteChar(s, binary, ch & 0xff));
	}
	
	private static int uniqueByteChar(boolean[] byteMap) {
		int byteChar = -1;
		for(int i = 0; i < byteMap.length; i++) {
			if(byteMap[i]) {
				if(byteChar != -1) return -1;
				byteChar = i;
			}
		}
		return byteChar;
	}
	
	public static Expression newByteMap(SourcePosition s, boolean binary, boolean[] byteMap) {
		int byteChar = uniqueByteChar(byteMap);
		if(byteChar != -1) {
			return internImpl(s, newByteChar(s, binary, byteChar));
		}
		return internImpl(s, new ByteMap(s, binary, byteMap));
	}

	public static final Expression newString(SourcePosition s, String text) {
		byte[] utf8 = StringUtils.toUtf8(text);
		if(utf8.length == 0) {
			return newEmpty(s);
		}
		if(utf8.length == 1) {
			return newByteChar(s, false, utf8[0]);
		}
		return newByteSequence(s, false, utf8);
	}

	public final static Expression newByteSequence(SourcePosition s, boolean binary, byte[] utf8) {
		UList<Expression> l = new UList<Expression>(new Expression[utf8.length]);
		for(int i = 0; i < utf8.length; i++) {
			l.add(newByteChar(s, binary, utf8[i]));
		}
		return newSequence(s, l);
	}

	public final static Expression newCharSet(SourcePosition s, String text) {
		boolean b[] = StringUtils.parseByteMap(text);
		return internImpl(s, new ByteMap(s, false, b));
	}

	public final static Expression newCharSet(SourcePosition s, String t, String t2) {
		int c = StringUtils.parseAscii(t);
		int c2 = StringUtils.parseAscii(t2);
		if(c != -1 && c2 != -1) {
			return newByteRange(s, false, c, c2);
		}
		c = StringUtils.parseUnicode(t);
		c2 = StringUtils.parseUnicode(t2);
		if(c < 128 && c2 < 128) {
			return newByteRange(s, false, c, c2);
		}
		else {
			return newUnicodeRange(s, c, c2);
		}
	}

	public final static Expression newByteRange(SourcePosition s, boolean binary, int c, int c2) {
		if(c == c2) {
			return newByteChar(s, binary, c);
		}
		return internImpl(s, new ByteMap(s, binary, c, c2));
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
			return newByteSequence(s, false, b);
		}
		else {
			UList<Expression> l = new UList<Expression>(new Expression[b.length]);
			for(int i = 0; i < b.length-1; i++) {
				l.add(newByteChar(s, false, b[i]));
			}
			l.add(newByteRange(s, false, b[b.length-1] & 0xff, b2[b2.length-1] & 0xff));
			return newSequence(s, l);
		}
	}
	
	/* Unary */
	
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

	public final static And newAnd(SourcePosition s, Expression p) {
		s = p.isInterned() ? null : s;
		return (And)internImpl(s, new And(s, p));
	}
	
	public final static Not newNot(SourcePosition s, Expression p) {
		s = p.isInterned() ? null : s;
		return (Not)internImpl(s, new Not(s, p));
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
		if(s != null && isInterned(l)) {
			s = null;
		}
		return internImpl(s, newSequence(s, 0, l));
	}

	private final static Expression newSequence(SourcePosition s, int start, UList<Expression> l) {
		Expression first = internImpl(s, l.ArrayValues[start]);
		if(start + 1 ==  l.size()) {
			return first;
		}
		Expression seq = new Sequence(s, first, newSequence(s, start+1, l));
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
//		if(l.ArrayValues[size - 1] instanceof Empty) {
//			return newOption(s, new Choice(s, l, size-1));  //     e / '' => e?
//		}
		return internImpl(s, new Choice(s, l, size));
	}

	public final static Expression newChoice(SourcePosition s, Expression p, Expression p2) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		addChoice(l, p);
		addChoice(l, p2);
		return newChoice(s, l);
	}

	@Deprecated //FIXME
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


	// AST Construction

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

	public final static Expression newNew(SourcePosition s, boolean lefted, int shift) {
		return internImpl(s, new New(s, lefted, shift));
	}

	public final static Expression newCapture(SourcePosition s, int shift) {
		return internImpl(s, new Capture(s, shift));
	}

	public final static Expression newNew(SourcePosition s, boolean lefted, Expression e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size() + 3]);
		GrammarFactory.addSequence(l, internImpl(s, new New(s, lefted, 0)));
		GrammarFactory.addSequence(l, e);
		GrammarFactory.addSequence(l, GrammarFactory.newCapture(s, 0));
		return newSequence(s, l);
	}

	public final static Expression newLeftNewOption(SourcePosition s, Expression e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size() + 3]);
		GrammarFactory.addSequence(l, internImpl(s, new New(s, true, 0)));
		GrammarFactory.addSequence(l, e);
		GrammarFactory.addSequence(l, GrammarFactory.newCapture(s, 0));
		return newOption(s, GrammarFactory.newSequence(s, l));
	}

	public final static Expression newLeftNewRepetition(SourcePosition s, Expression e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size() + 3]);
		GrammarFactory.addSequence(l, internImpl(s, new New(s, true, 0)));
		GrammarFactory.addSequence(l, e);
		GrammarFactory.addSequence(l, GrammarFactory.newCapture(s, 0));
		return newRepetition(s, GrammarFactory.newSequence(s, l));
	}

	public final static Expression newLeftNewRepetition1(SourcePosition s, Expression e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size() + 3]);
		GrammarFactory.addSequence(l, internImpl(s, new New(s, true, 0)));
		GrammarFactory.addSequence(l, e);
		GrammarFactory.addSequence(l, GrammarFactory.newCapture(s, 0));
		return newRepetition1(s, GrammarFactory.newSequence(s, l));
	}
		
	public final static Expression newTagging(SourcePosition s, Tag tag) {
		return internImpl(s, new Tagging(s, tag));
	}

	public final static Expression newReplace(SourcePosition s, String msg) {
		return internImpl(s, new Replace(s, msg));
	}
	
	// Conditional Parsing
	// <if FLAG>
	// <on FLAG e>
	// <on! FLAG e>
	
	public final static Expression newIfFlag(SourcePosition s, String flagName) {
		return internImpl(s, new IfFlag(s, true, flagName));
	}

	public final static Expression newOnFlag(SourcePosition s, boolean predicate, String flagName, Expression e) {
		return internImpl(s, new OnFlag(s, predicate, flagName, e));
	}
	
	public final static Expression newBlock(SourcePosition s, Expression e) {
		return internImpl(s, new Block(s, e));
	}

	public final static Expression newLocal(SourcePosition s, NameSpace ns, Tag tableName, Expression e) {
		return internImpl(s, new LocalTable(s, ns, tableName, e));
	}

	public final static Expression newDefSymbol(SourcePosition s, NameSpace ns, Tag tableName, Expression e) {
		return internImpl(s, new DefSymbol(s, ns, tableName, e));
	}

	public final static Expression newIsSymbol(SourcePosition s, NameSpace ns,  Tag tableName) {
		return internImpl(s, new IsSymbol(s, ns, tableName, /*checkLastSymbolOnly*/true));
	}
	
	public final static Expression newIsaSymbol(SourcePosition s, NameSpace ns, Tag tableName) {
		return internImpl(s, new IsSymbol(s, ns, tableName, /*checkLastSymbolOnly*/false));
	}

	public final static Expression newExists(SourcePosition s, NameSpace ns, Tag tableName) {
		return internImpl(s, new ExistsSymbol(s, ns, tableName));
	}

	public final static Expression newDefIndent(SourcePosition s) {
		return internImpl(s, new DefIndent(s));
	}

	public final static Expression newIndent(SourcePosition s) {
		return internImpl(s, new IsIndent(s));
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

	protected NameSpace getNameSpace() {
		return null;
	}

	protected SourcePosition getSourcePosition() {
		return null;
	}

	public final Expression newNonTerminal(String name) {
		return GrammarFactory.newNonTerminal(getSourcePosition(), getNameSpace(), name);
	}
	
	public final Expression newEmpty() {
		return GrammarFactory.newEmpty(getSourcePosition());
	}

	public final Expression newFailure() {
		return GrammarFactory.newFailure(getSourcePosition());
	}

	public final Expression newByteChar(int ch) {
		return GrammarFactory.newByteChar(getSourcePosition(), false, ch);
	}
	
	public final Expression newAnyChar() {
		return GrammarFactory.newAnyChar(getSourcePosition(), false);
	}
	
	public final Expression newString(String text) {
		return GrammarFactory.newString(getSourcePosition(), text);
	}
	
	public final Expression newCharSet(String text) {
		return GrammarFactory.newCharSet(getSourcePosition(), text);
	}

	public final Expression newByteMap(boolean[] byteMap) {
		return GrammarFactory.newByteMap(getSourcePosition(), false, byteMap);
	}
	
	public final Expression newSequence(Expression ... seq) {
		UList<Expression> l = new UList<Expression>(new Expression[8]);
		for(Expression p: seq) {
			GrammarFactory.addSequence(l, p);
		}
		return GrammarFactory.newSequence(getSourcePosition(), l);
	}

	public final Expression newChoice(Expression ... seq) {
		UList<Expression> l = new UList<Expression>(new Expression[8]);
		for(Expression p: seq) {
			GrammarFactory.addChoice(l, p);
		}
		return GrammarFactory.newChoice(getSourcePosition(), l);
	}

	public final Expression newOption(Expression ... seq) {
		return GrammarFactory.newOption(getSourcePosition(), newSequence(seq));
	}
		
	public final Expression newRepetition(Expression ... seq) {
		return GrammarFactory.newRepetition(getSourcePosition(), newSequence(seq));
	}

	public final Expression newRepetition1(Expression ... seq) {
		return GrammarFactory.newRepetition1(getSourcePosition(), newSequence(seq));
	}

	public final Expression newAnd(Expression ... seq) {
		return GrammarFactory.newAnd(getSourcePosition(), newSequence(seq));
	}

	public final Expression newNot(Expression ... seq) {
		return GrammarFactory.newNot(getSourcePosition(), newSequence(seq));
	}
	
//	public final Expression newByteRange(int c, int c2) {
//		if(c == c2) {
//			return newByteChar(s, c);
//		}
//		return internImpl(s, new ByteMap(s, c, c2));
//	}
	
	// PEG4d
	public final Expression newMatch(Expression ... seq) {
		return GrammarFactory.newMatch(getSourcePosition(), newSequence(seq));
	}
	
	public final Expression newLink(Expression ... seq) {
		return GrammarFactory.newLink(getSourcePosition(), newSequence(seq), -1);
	}

	public final Expression newLink(int index, Expression ... seq) {
		return GrammarFactory.newLink(getSourcePosition(), newSequence(seq), index);
	}

	public final Expression newNew(Expression ... seq) {
		return GrammarFactory.newNew(getSourcePosition(), false, newSequence(seq));
	}

	public final Expression newLeftNew(Expression ... seq) {
		return GrammarFactory.newNew(getSourcePosition(), true, newSequence(seq));
	}

	public final Expression newTagging(String tag) {
		return GrammarFactory.newTagging(getSourcePosition(), Tag.tag(tag));
	}

	public final Expression newReplace(String msg) {
		return GrammarFactory.newReplace(getSourcePosition(), msg);
	}
	
	// Conditional Parsing
	// <if FLAG>
	// <on FLAG e>
	// <on !FLAG e>
	
	public final Expression newIfFlag(String flagName) {
		return GrammarFactory.newIfFlag(getSourcePosition(), flagName);
	}

	public final Expression newOnFlag(String flagName, Expression ... seq) {
		return GrammarFactory.newOnFlag(getSourcePosition(), true, flagName, newSequence(seq));
	}

	public final Expression newScan(int number, Expression scan, Expression repeat) {
		return null;
	}
	
	public final Expression newRepeat(Expression e) {
		return null;
	}
	
	public final Expression newBlock(Expression ... seq) {
		return GrammarFactory.newBlock(getSourcePosition(), newSequence(seq));
	}

	public final Expression newDefSymbol(String table, Expression ... seq) {
		return GrammarFactory.newDefSymbol(getSourcePosition(), getNameSpace(), Tag.tag(table), newSequence(seq));
	}

	public final Expression newIsSymbol(String table) {
		return GrammarFactory.newIsSymbol(getSourcePosition(), getNameSpace(), Tag.tag(table));
	}
	
	public final Expression newIsaSymbol(String table) {
		return GrammarFactory.newIsaSymbol(getSourcePosition(), getNameSpace(), Tag.tag(table));
	}

	public final Expression newExists(String table) {
		return GrammarFactory.newExists(getSourcePosition(), getNameSpace(), Tag.tag(table));
	}

	public final Expression newLocal(String table, Expression ... seq) {
		return GrammarFactory.newLocal(getSourcePosition(), getNameSpace(), Tag.tag(table), newSequence(seq));
	}

	public final Expression newDefIndent() {
		return GrammarFactory.newDefIndent(getSourcePosition());
	}

	public final Expression newIndent() {
		return GrammarFactory.newIndent(getSourcePosition());
	}
	
}
