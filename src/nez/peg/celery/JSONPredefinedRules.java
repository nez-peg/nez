package nez.peg.celery;

import nez.lang.Expression;
import nez.lang.GrammarFactory;
import nez.lang.NameSpace;

public class JSONPredefinedRules {
	NameSpace grammar;
	String rootClassName;

	public JSONPredefinedRules(NameSpace grammar, String rootClassName) {
		this.grammar = grammar;
		this.rootClassName = rootClassName;
	}

	public void defineRule() {
		defFile();
		defRoot();
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
		// grammar.dump();
	}

	final void defFile() {
		Expression[] l = {
				grammar.newNonTerminal("SPACING"),
				grammar.newNonTerminal("Root"),
				grammar.newNonTerminal("SPACING")
		};
		grammar.defineProduction(null, "File", grammar.newSequence(l));
	}

	final void defRoot() {
		grammar.defineProduction(null, "Root", grammar.newNonTerminal(rootClassName));
	}

	final void defAny() {
		Expression l = grammar.newRepetition(grammar.newNonTerminal("Value"),
				grammar.newNonTerminal("SPACING"));
		grammar.defineProduction(null, "Any", l);
	}

	final void defMember() {
		Expression[] l = {
				grammar.newNonTerminal("STRING"),
				grammar.newNonTerminal("NAMESEP"),
				grammar.newNonTerminal("Value"),
		};
		grammar.defineProduction(null, "Member", grammar.newSequence(l));
	}

	final void defValue() {
		Expression[] l = {
				grammar.newNonTerminal("String"),
				grammar.newNonTerminal("Number"),
				grammar.newNonTerminal("JSONObject"),
				grammar.newNonTerminal("Array"),
				grammar.newNonTerminal("Null"),
				grammar.newNonTerminal("True"),
				grammar.newNonTerminal("False")
		};
		grammar.defineProduction(null, "Value", grammar.newChoice(l));
	}

	final void defJSONObject() {
		Expression[] l = {
				grammar.newByteChar('{'),
				grammar.newNonTerminal("SPACING"),
				grammar.newNonTerminal("Member"),
				grammar.newRepetition(grammar.newNonTerminal("VALUESEP"),
						grammar.newNonTerminal("Member")),
				grammar.newNonTerminal("SPACING"),
				grammar.newByteChar('}'),
		};
		grammar.defineProduction(null, "JSONObject", grammar.newSequence(l));
	}

	final void defArray() {
		Expression[] valueSeq = {
				grammar.newNonTerminal("SPACING"),
				grammar.newNonTerminal("Value"),
				grammar.newRepetition(grammar.newNonTerminal("VALUESEP"),
						grammar.newNonTerminal("Value"))
		};
		Expression[] l = {
				grammar.newByteChar('['),
				grammar.newOption(valueSeq),
				grammar.newNonTerminal("SPACING"),
				grammar.newByteChar(']')
		};
		grammar.defineProduction(null, "Array", grammar.newSequence(l));
	}

	final void defString() {
		Expression notSeq = grammar.newSequence(grammar.newNot(grammar.newByteChar('"')),
				grammar.newAnyChar());
		Expression strValue = grammar.newChoice(grammar.newString("\""), grammar.newString("\\\\"),
				notSeq);
		Expression[] seq = {
				grammar.newByteChar('"'), grammar.newRepetition(strValue), grammar.newByteChar('"')
		};
		grammar.defineProduction(null, "String", grammar.newSequence(seq));
	}

	final void defNumber() {
		Expression choice = grammar.newChoice(grammar.newSequence(grammar.newNonTerminal("FRAC"),
				grammar.newOption(grammar.newNonTerminal("EXP"))));
		Expression[] l = {
				grammar.newOption(grammar.newByteChar('-')), grammar.newNonTerminal("INT"), choice
		};
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
		Expression[] l = {
				grammar.newNonTerminal("SPACING"),
				grammar.newByteChar(':'),
				grammar.newNonTerminal("SPACING")
		};
		grammar.defineProduction(null, "NAMESEP", grammar.newSequence(l));
	}

	final void defVALUESEP() {
		Expression[] l = {
				grammar.newNonTerminal("SPACING"),
				grammar.newByteChar(','),
				grammar.newNonTerminal("SPACING")
		};
		grammar.defineProduction(null, "VALUESEP", grammar.newSequence(l));
	}

	final void defBOOLEAN() {
		Expression[] l = {
				grammar.newString("true"), grammar.newString("false")
		};
		grammar.defineProduction(null, "BOOLEAN", grammar.newChoice(l));
	}

	final void defINT() {
		Expression[] l = {
				grammar.newByteChar('0'),
				grammar.newSequence(GrammarFactory.newCharSet(null, "1-9"),
						grammar.newRepetition(grammar.newNonTerminal("DIGIT")))
		};
		grammar.defineProduction(null, "INT", grammar.newChoice(l));
	}

	final void defDIGIT() {
		grammar.defineProduction(null, "DIGIT", GrammarFactory.newCharSet(null, "0-9"));
	}

	final void defFRAC() {
		Expression[] l = {
				grammar.newByteChar('.'), grammar.newRepetition1(grammar.newNonTerminal("DIGIT"))
		};
		grammar.defineProduction(null, "FRAC", grammar.newSequence(l));
	}

	final void defEXP() {
		Expression choice = grammar.newChoice(grammar.newByteChar('-'), grammar.newByteChar('+'));
		Expression[] l = {
				GrammarFactory.newCharSet(null, "Ee"),
				grammar.newOption(choice),
				grammar.newRepetition1(grammar.newNonTerminal("DIGIT"))
		};
		grammar.defineProduction(null, "EXP", grammar.newSequence(l));
	}

	final void defSTRING() {
		Expression[] l = {
				grammar.newByteChar('"'),
				grammar.newRepetition(grammar.newAnyChar()),
				grammar.newByteChar('"')
		};
		grammar.defineProduction(null, "STRING", grammar.newSequence(l));
	}

	final void defSPACING() {
		Expression spacing = GrammarFactory.newCharSet(null, "\t\n\r ");
		grammar.defineProduction(null, "SPACING", grammar.newRepetition(spacing));
	}
}
