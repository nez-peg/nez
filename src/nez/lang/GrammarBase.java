package nez.lang;

import java.util.AbstractList;

import nez.Grammar;
import nez.ast.SourcePosition;
import nez.ast.SymbolId;
import nez.lang.expr.ExpressionCommons;
import nez.util.UList;

public abstract class GrammarBase extends AbstractList<Production> {
	protected SourcePosition getSourcePosition() {
		return null;
	}

	public final Production newProduction(int flag, String name, Expression e) {
		return new Production(getSourcePosition(), flag, (Grammar) this, name, e);
	}

	public final Production newProduction(String name, Expression e) {
		return new Production(getSourcePosition(), 0, (Grammar) this, name, e);
	}

	public final Expression newNonTerminal(String name) {
		return ExpressionCommons.newNonTerminal(getSourcePosition(), (Grammar) this, name);
	}

	public final Expression newEmpty() {
		return ExpressionCommons.newEmpty(getSourcePosition());
	}

	public final Expression newFailure() {
		return ExpressionCommons.newFailure(getSourcePosition());
	}

	public final Expression newByteChar(int ch) {
		return ExpressionCommons.newCbyte(getSourcePosition(), false, ch);
	}

	public final Expression newAnyChar() {
		return ExpressionCommons.newCany(getSourcePosition(), false);
	}

	public final Expression newString(String text) {
		return ExpressionCommons.newString(getSourcePosition(), text);
	}

	public final Expression newCharSet(String text) {
		return ExpressionCommons.newCharSet(getSourcePosition(), text);
	}

	public final Expression newByteMap(boolean[] byteMap) {
		return ExpressionCommons.newCset(getSourcePosition(), false, byteMap);
	}

	public final Expression newSequence(Expression... seq) {
		UList<Expression> l = new UList<Expression>(new Expression[8]);
		for (Expression p : seq) {
			ExpressionCommons.addSequence(l, p);
		}
		return ExpressionCommons.newPsequence(getSourcePosition(), l);
	}

	public final Expression newChoice(Expression... seq) {
		UList<Expression> l = new UList<Expression>(new Expression[8]);
		for (Expression p : seq) {
			ExpressionCommons.addChoice(l, p);
		}
		return ExpressionCommons.newPchoice(getSourcePosition(), l);
	}

	public final Expression newOption(Expression... seq) {
		return ExpressionCommons.newUoption(getSourcePosition(), newSequence(seq));
	}

	public final Expression newRepetition(Expression... seq) {
		return ExpressionCommons.newUzero(getSourcePosition(), newSequence(seq));
	}

	public final Expression newRepetition1(Expression... seq) {
		return ExpressionCommons.newUone(getSourcePosition(), newSequence(seq));
	}

	public final Expression newAnd(Expression... seq) {
		return ExpressionCommons.newUand(getSourcePosition(), newSequence(seq));
	}

	public final Expression newNot(Expression... seq) {
		return ExpressionCommons.newUnot(getSourcePosition(), newSequence(seq));
	}

	// public final Expression newByteRange(int c, int c2) {
	// if(c == c2) {
	// return newByteChar(s, c);
	// }
	// return internImpl(s, new ByteMap(s, c, c2));
	// }

	// PEG4d
	public final Expression newMatch(Expression... seq) {
		return ExpressionCommons.newUmatch(getSourcePosition(), newSequence(seq));
	}

	public final Expression newLink(Expression... seq) {
		return ExpressionCommons.newTlink(getSourcePosition(), null, newSequence(seq));
	}

	public final Expression newLink(SymbolId label, Expression... seq) {
		return ExpressionCommons.newTlink(getSourcePosition(), label, newSequence(seq));
	}

	public final Expression newNew(Expression... seq) {
		return ExpressionCommons.newNewCapture(getSourcePosition(), false, null, newSequence(seq));
	}

	// public final Expression newLeftNew(Expression ... seq) {
	// return GrammarFactory.newNew(getSourcePosition(), true,
	// newSequence(seq));
	// }

	public final Expression newTagging(String tag) {
		return ExpressionCommons.newTtag(getSourcePosition(), SymbolId.tag(tag));
	}

	public final Expression newReplace(String msg) {
		return ExpressionCommons.newTreplace(getSourcePosition(), msg);
	}

	// Conditional Parsing
	// <if FLAG>
	// <on FLAG e>
	// <on !FLAG e>

	public final Expression newIfFlag(String flagName) {
		return ExpressionCommons.newXif(getSourcePosition(), flagName);
	}

	public final Expression newXon(String flagName, Expression... seq) {
		return ExpressionCommons.newXon(getSourcePosition(), true, flagName, newSequence(seq));
	}

	public final Expression newBlock(Expression... seq) {
		return ExpressionCommons.newXblock(getSourcePosition(), newSequence(seq));
	}

	public final Expression newDefSymbol(String table, Expression... seq) {
		return ExpressionCommons.newXdef(getSourcePosition(), (Grammar) this, SymbolId.tag(table), newSequence(seq));
	}

	public final Expression newIsSymbol(String table) {
		return ExpressionCommons.newXis(getSourcePosition(), (Grammar) this, SymbolId.tag(table));
	}

	public final Expression newIsaSymbol(String table) {
		return ExpressionCommons.newXisa(getSourcePosition(), (Grammar) this, SymbolId.tag(table));
	}

	public final Expression newExists(String table, String symbol) {
		return ExpressionCommons.newXexists(getSourcePosition(), SymbolId.tag(table), symbol);
	}

	public final Expression newLocal(String table, Expression... seq) {
		return ExpressionCommons.newXlocal(getSourcePosition(), SymbolId.tag(table), newSequence(seq));
	}

	public final Expression newScan(int number, Expression scan, Expression repeat) {
		return null;
	}

	public final Expression newRepeat(Expression e) {
		return null;

	}

}
