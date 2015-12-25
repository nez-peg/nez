package nez.lang;

import java.util.AbstractList;

import nez.ast.SourceLocation;
import nez.ast.Symbol;
import nez.lang.expr.Expressions;
import nez.util.UList;

public abstract class GrammarHacks extends AbstractList<Production> {
	protected SourceLocation getSourcePosition() {
		return null;
	}

	public abstract void addProduction(Production p);

	public final Production newProduction(SourceLocation s, int flag, String name, Expression e) {
		Production p = new Production(s, flag, (Grammar) this, name, e);
		addProduction(p);
		return p;
	}

	public final Production newProduction(String name, Expression e) {
		return newProduction(getSourcePosition(), 0, name, e);
	}

	public final NonTerminal newNonTerminal(SourceLocation s, String name) {
		return Expressions.newNonTerminal(s, (Grammar) this, name);
	}

	public final Expression newEmpty() {
		return Expressions.newEmpty(getSourcePosition());
	}

	public final Expression newFailure() {
		return Expressions.newFailure(getSourcePosition());
	}

	public final Expression newByteChar(int ch) {
		return Expressions.newCbyte(getSourcePosition(), false, ch);
	}

	public final Expression newAnyChar() {
		return Expressions.newCany(getSourcePosition(), false);
	}

	public final Expression newString(String text) {
		return Expressions.newString(getSourcePosition(), text);
	}

	public final Expression newCharSet(String text) {
		return Expressions.newCharSet(getSourcePosition(), text);
	}

	public final Expression newByteMap(boolean[] byteMap) {
		return Expressions.newCset(getSourcePosition(), false, byteMap);
	}

	public final Expression newSequence(Expression... seq) {
		UList<Expression> l = new UList<Expression>(new Expression[8]);
		for (Expression p : seq) {
			Expressions.addSequence(l, p);
		}
		return Expressions.newPsequence(getSourcePosition(), l);
	}

	public final Expression newChoice(Expression... seq) {
		UList<Expression> l = new UList<Expression>(new Expression[8]);
		for (Expression p : seq) {
			Expressions.addChoice(l, p);
		}
		return Expressions.newPchoice(getSourcePosition(), l);
	}

	public final Expression newOption(Expression... seq) {
		return Expressions.newPoption(getSourcePosition(), newSequence(seq));
	}

	public final Expression newRepetition(Expression... seq) {
		return Expressions.newPzero(getSourcePosition(), newSequence(seq));
	}

	public final Expression newRepetition1(Expression... seq) {
		return Expressions.newPone(getSourcePosition(), newSequence(seq));
	}

	public final Expression newAnd(Expression... seq) {
		return Expressions.newPand(getSourcePosition(), newSequence(seq));
	}

	public final Expression newNot(Expression... seq) {
		return Expressions.newPnot(getSourcePosition(), newSequence(seq));
	}

	// PEG4d
	public final Expression newMatch(Expression... seq) {
		return Expressions.newTdetree(getSourcePosition(), newSequence(seq));
	}

	public final Expression newLink(Expression... seq) {
		return Expressions.newTlink(getSourcePosition(), null, newSequence(seq));
	}

	public final Expression newLink(Symbol label, Expression... seq) {
		return Expressions.newTlink(getSourcePosition(), label, newSequence(seq));
	}

	public final Expression newNew(Expression... seq) {
		return Expressions.newNewCapture(getSourcePosition(), false, null, newSequence(seq));
	}

	// public final Expression newLeftNew(Expression ... seq) {
	// return GrammarFactory.newNew(getSourcePosition(), true,
	// newSequence(seq));
	// }

	public final Expression newTagging(String tag) {
		return Expressions.newTtag(getSourcePosition(), Symbol.tag(tag));
	}

	public final Expression newReplace(String msg) {
		return Expressions.newTreplace(getSourcePosition(), msg);
	}

	// Conditional Parsing
	// <if FLAG>
	// <on FLAG e>
	// <on !FLAG e>

	public final Expression newIfFlag(String flagName) {
		return Expressions.newXif(getSourcePosition(), flagName);
	}

	public final Expression newXon(String flagName, Expression... seq) {
		return Expressions.newXon(getSourcePosition(), true, flagName, newSequence(seq));
	}

	public final Expression newBlock(Expression... seq) {
		return Expressions.newXblock(getSourcePosition(), newSequence(seq));
	}

	public final Expression newDefSymbol(NonTerminal n) {
		return Expressions.newXsymbol(getSourcePosition(), n);
	}

	public final Expression newIsSymbol(NonTerminal n) {
		return Expressions.newXis(getSourcePosition(), n);
	}

	public final Expression newIsaSymbol(NonTerminal n) {
		return Expressions.newXisa(getSourcePosition(), n);
	}

	public final Expression newExists(String table, String symbol) {
		return Expressions.newXexists(getSourcePosition(), Symbol.tag(table), symbol);
	}

	public final Expression newLocal(String table, Expression... seq) {
		return Expressions.newXlocal(getSourcePosition(), Symbol.tag(table), newSequence(seq));
	}

	public final Expression newScan(int number, Expression scan, Expression repeat) {
		return null;
	}

	public final Expression newRepeat(Expression e) {
		return null;

	}

}
