package nez.lang.schema;

import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Production;

public class JSONPredefinedGrammar extends AbstractPredefinedGrammar {
	Grammar grammar;
	String rootClassName;

	public JSONPredefinedGrammar(Grammar grammar) {
		super(grammar);
	}

	@Override
	public void define() {
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
		defSPACING();
	}

	final void defFile() {
		Expression[] l = { _NonTerminal("S"), _NonTerminal("Root"), _NonTerminal("S") };
		grammar.addProduction(null, "File", newSequence(l));
	}

	final void defAny() {
		Expression l = newSequence(_NonTerminal("Member"), _NonTerminal("S"), newOption(_NonTerminal("VALUESEP")));
		grammar.addProduction(null, "Any", l);
	}

	final void defMember() {
		Expression[] l = { _NonTerminal("String"), _NonTerminal("NAMESEP"), _NonTerminal("Value"), };
		grammar.addProduction(null, "Member", newSequence(l));
	}

	final void defValue() {
		Expression[] l = { _NonTerminal("String"), _NonTerminal("Number"), _NonTerminal("JSONObject"), _NonTerminal("Array"), _NonTerminal("Null"), _NonTerminal("True"), _NonTerminal("False") };
		grammar.addProduction(null, "Value", newChoice(l));
	}

	final void defJSONObject() {
		Expression[] l = { newByteChar('{'), _NonTerminal("S"), _NonTerminal("Member"), _NonTerminal("S"), newRepetition(_NonTerminal("VALUESEP"), _NonTerminal("Member"), _NonTerminal("S")), newByteChar('}'), };
		grammar.addProduction(null, "JSONObject", newSequence(l));
	}

	final void defArray() {
		Expression[] valueSeq = { _NonTerminal("S"), _NonTerminal("Value"), _NonTerminal("S"), newRepetition(_NonTerminal("VALUESEP"), _NonTerminal("Value")) };
		Expression[] l = { newByteChar('['), newSequence(valueSeq), _NonTerminal("S"), newByteChar(']') };
		grammar.addProduction(null, "Array", newSequence(l));
	}

	final void defString() {
		Expression notSeq = newSequence(newNot(newByteChar('"')), newAnyChar());
		Expression strValue = newChoice(newString("\\\""), newString("\\\\"), notSeq);
		Expression[] seq = { newByteChar('"'), newRepetition(strValue), newByteChar('"'), _NonTerminal("S") };
		grammar.addProduction(null, "String", newSequence(seq));
	}

	final void defNumber() {
		Expression choice = newChoice(newSequence(_NonTerminal("FRAC"), newOption(_NonTerminal("EXP"))), newEmpty());
		Expression[] l = { newOption(newByteChar('-')), _NonTerminal("INT"), choice, _NonTerminal("S") };
		grammar.addProduction(null, "Number", newSequence(l));
	}

	final void defTrue() {
		grammar.addProduction(null, "True", newString("true"));
	}

	final void defFalse() {
		grammar.addProduction(null, "False", newString("false"));
	}

	final void defNull() {
		grammar.addProduction(null, "Null", newString("null"));
	}

	final void defNAMESEP() {
		Expression[] l = { newByteChar(':'), _NonTerminal("S") };
		grammar.addProduction(null, "NAMESEP", newSequence(l));
	}

	final void defVALUESEP() {
		Expression[] l = { newByteChar(','), _NonTerminal("S") };
		grammar.addProduction(null, "VALUESEP", newSequence(l));
	}

	final void defBOOLEAN() {
		Expression[] l = { newString("true"), newString("false") };
		grammar.addProduction(null, "BOOLEAN", newChoice(l));
	}

	final void defINT() {
		Expression[] l = { newByteChar('0'), newSequence(newCharSet("1-9"), newRepetition(_NonTerminal("DIGIT"))) };
		grammar.addProduction(null, "INT", newChoice(l));
	}

	final void defDIGIT() {
		grammar.addProduction(null, "DIGIT", newCharSet("0-9"));
	}

	final void defFRAC() {
		Expression[] l = { newByteChar('.'), newRepetition1(_NonTerminal("DIGIT")) };
		grammar.addProduction(null, "FRAC", newSequence(l));
	}

	final void defEXP() {
		Expression choice = newChoice(newByteChar('-'), newByteChar('+'));
		Expression[] l = { newCharSet("Ee"), newOption(choice), newRepetition1(_NonTerminal("DIGIT")) };
		grammar.addProduction(null, "EXP", newSequence(l));
	}

	final void defSTRING() {
		Expression notSeq = newSequence(newNot(newByteChar('"')), newAnyChar());
		Expression strValue = newChoice(newString("\\\""), newString("\\\\"), notSeq);
		Expression[] seq = { newByteChar('"'), newRepetition(strValue), newByteChar('"'), _NonTerminal("S") };
		grammar.addProduction(null, "STRING", newSequence(seq));
	}

	final void defSPACING() {
		Expression spacing = newCharSet("\t\n\r ");
		grammar.addProduction(null, "S", newRepetition(spacing));
	}

	@Override
	public void addProduction(Production p) {
		// TODO Auto-generated method stub

	}

	@Override
	public Production get(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}
}
