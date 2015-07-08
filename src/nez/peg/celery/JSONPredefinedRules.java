package nez.peg.celery;

import nez.lang.Expression;
import nez.lang.GrammarFactory;
import nez.lang.GrammarFile;

public class JSONPredefinedRules {
	GrammarFile grammar;
	String rootClassName;

	public JSONPredefinedRules(GrammarFile grammar) {
		this.grammar = grammar;
	}

	public void defineRule() {
		defFile();
		defAny();
		defMember();
		defValue();
		defArray();
		defJSONObject();
		defString();
		defNumber();
		defTrue();
		defFalse();
		defNull();
		defNAMESEP();
		defVALUESEP();
		defBOOLEAN();
		defINT();
		defDIGIT();
		defFRAC();
		defEXP();
		defSTRING();
		defSPACING();
	}

	final void defFile() {
		Expression[] l = { GrammarFactory.newNonTerminal(null, grammar, "SPACING"), GrammarFactory.newNonTerminal(null, grammar, "Root"), GrammarFactory.newNonTerminal(null, grammar, "SPACING") };
		grammar.defineProduction(null, "File", grammar.newSequence(l));
	}

	final void defAny() {
		Expression l = grammar.newSequence(GrammarFactory.newNonTerminal(null, grammar, "Member"), GrammarFactory.newNonTerminal(null, grammar, "SPACING"), GrammarFactory.newOption(null, GrammarFactory.newNonTerminal(null, grammar, "VALUESEP")));
		grammar.defineProduction(null, "Any", l);
	}

	final void defMember() {
		Expression[] l = { GrammarFactory.newNonTerminal(null, grammar, "String"), GrammarFactory.newNonTerminal(null, grammar, "NAMESEP"), GrammarFactory.newNonTerminal(null, grammar, "Value"), };
		grammar.defineProduction(null, "Member", grammar.newSequence(l));
	}

	final void defValue() {
		Expression[] l = { GrammarFactory.newNonTerminal(null, grammar, "String"), GrammarFactory.newNonTerminal(null, grammar, "Number"), GrammarFactory.newNonTerminal(null, grammar, "JSONObject"), GrammarFactory.newNonTerminal(null, grammar, "Array"),
				GrammarFactory.newNonTerminal(null, grammar, "Null"), GrammarFactory.newNonTerminal(null, grammar, "True"), GrammarFactory.newNonTerminal(null, grammar, "False") };
		grammar.defineProduction(null, "Value", grammar.newChoice(l));
	}

	final void defJSONObject() {
		Expression[] l = { grammar.newByteChar('{'), GrammarFactory.newNonTerminal(null, grammar, "SPACING"), GrammarFactory.newNonTerminal(null, grammar, "Member"), GrammarFactory.newNonTerminal(null, grammar, "SPACING"),
				grammar.newRepetition(GrammarFactory.newNonTerminal(null, grammar, "VALUESEP"), GrammarFactory.newNonTerminal(null, grammar, "Member"), GrammarFactory.newNonTerminal(null, grammar, "SPACING")), grammar.newByteChar('}'), };
		grammar.defineProduction(null, "JSONObject", grammar.newSequence(l));
	}

	final void defArray() {
		Expression[] valueSeq = { GrammarFactory.newNonTerminal(null, grammar, "SPACING"), GrammarFactory.newNonTerminal(null, grammar, "Value"), GrammarFactory.newNonTerminal(null, grammar, "SPACING"),
				grammar.newRepetition(GrammarFactory.newNonTerminal(null, grammar, "VALUESEP"), GrammarFactory.newNonTerminal(null, grammar, "Value")) };
		Expression[] l = { grammar.newByteChar('['), grammar.newSequence(valueSeq), GrammarFactory.newNonTerminal(null, grammar, "SPACING"), grammar.newByteChar(']') };
		grammar.defineProduction(null, "Array", grammar.newSequence(l));
	}

	final void defString() {
		Expression notSeq = grammar.newSequence(grammar.newNot(grammar.newByteChar('"')), grammar.newAnyChar());
		Expression strValue = grammar.newChoice(grammar.newString("\\\""), grammar.newString("\\\\"), notSeq);
		Expression[] seq = { grammar.newByteChar('"'), grammar.newRepetition(strValue), grammar.newByteChar('"'), GrammarFactory.newNonTerminal(null, grammar, "SPACING") };
		grammar.defineProduction(null, "String", grammar.newSequence(seq));
	}

	final void defNumber() {
		Expression choice = grammar.newChoice(grammar.newSequence(GrammarFactory.newNonTerminal(null, grammar, "FRAC"), grammar.newOption(GrammarFactory.newNonTerminal(null, grammar, "EXP"))), grammar.newEmpty());
		Expression[] l = { grammar.newOption(grammar.newByteChar('-')), GrammarFactory.newNonTerminal(null, grammar, "INT"), choice, GrammarFactory.newNonTerminal(null, grammar, "SPACING") };
		grammar.defineProduction(null, "Number", grammar.newSequence(l));
	}

	final void defTrue() {
		grammar.defineProduction(null, "True", grammar.newString("true"));
	}

	final void defFalse() {
		grammar.defineProduction(null, "False", grammar.newString("false"));
	}

	final void defNull() {
		grammar.defineProduction(null, "Null", grammar.newString("null"));
	}

	final void defNAMESEP() {
		Expression[] l = { grammar.newByteChar(':'), GrammarFactory.newNonTerminal(null, grammar, "SPACING") };
		grammar.defineProduction(null, "NAMESEP", grammar.newSequence(l));
	}

	final void defVALUESEP() {
		Expression[] l = { grammar.newByteChar(','), GrammarFactory.newNonTerminal(null, grammar, "SPACING") };
		grammar.defineProduction(null, "VALUESEP", grammar.newSequence(l));
	}

	final void defBOOLEAN() {
		Expression[] l = { grammar.newString("true"), grammar.newString("false") };
		grammar.defineProduction(null, "BOOLEAN", grammar.newChoice(l));
	}

	final void defINT() {
		Expression[] l = { grammar.newByteChar('0'), grammar.newSequence(GrammarFactory.newCharSet(null, "1-9"), grammar.newRepetition(GrammarFactory.newNonTerminal(null, grammar, "DIGIT"))) };
		grammar.defineProduction(null, "INT", grammar.newChoice(l));
	}

	final void defDIGIT() {
		grammar.defineProduction(null, "DIGIT", GrammarFactory.newCharSet(null, "0-9"));
	}

	final void defFRAC() {
		Expression[] l = { grammar.newByteChar('.'), grammar.newRepetition1(GrammarFactory.newNonTerminal(null, grammar, "DIGIT")) };
		grammar.defineProduction(null, "FRAC", grammar.newSequence(l));
	}

	final void defEXP() {
		Expression choice = grammar.newChoice(grammar.newByteChar('-'), grammar.newByteChar('+'));
		Expression[] l = { GrammarFactory.newCharSet(null, "Ee"), grammar.newOption(choice), grammar.newRepetition1(GrammarFactory.newNonTerminal(null, grammar, "DIGIT")) };
		grammar.defineProduction(null, "EXP", grammar.newSequence(l));
	}

	final void defSTRING() {
		Expression notSeq = grammar.newSequence(grammar.newNot(grammar.newByteChar('"')), grammar.newAnyChar());
		Expression strValue = grammar.newChoice(grammar.newString("\\\""), grammar.newString("\\\\"), notSeq);
		Expression[] seq = { grammar.newByteChar('"'), grammar.newRepetition(strValue), grammar.newByteChar('"'), GrammarFactory.newNonTerminal(null, grammar, "SPACING") };
		grammar.defineProduction(null, "STRING", grammar.newSequence(seq));
	}

	final void defSPACING() {
		Expression spacing = GrammarFactory.newCharSet(null, "\t\n\r ");
		grammar.defineProduction(null, "SPACING", grammar.newRepetition(spacing));
	}
}
