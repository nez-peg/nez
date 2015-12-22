package nez.lang.schema;

import nez.ast.Symbol;
import nez.junks.GrammarFile;
import nez.lang.Expression;

public class JSONGrammarCombinator extends PredefinedGrammarCombinator {

	static final Symbol _Key = Symbol.tag("key");
	static final Symbol _Value = Symbol.tag("value");

	public JSONGrammarCombinator(GrammarFile grammar) {
		super(grammar, "File");
	}

	public final Expression pFile() {
		Expression[] l = { _NonTerminal("S"), _NonTerminal("Root"), _NonTerminal("S") };
		return newSequence(l);
	}

	public final Expression pAny() {
		return newSequence(newNew(newLink(_NonTerminal("Member")), _NonTerminal("S"), newOption(_NonTerminal("VALUESEP")), newTagging("Any")));
	}

	public final Expression pMember() {
		Expression[] l = { newNew(newLink(_Key, _NonTerminal("String")), _NonTerminal("NAMESEP"), newLink(_Value, _NonTerminal("Value"))) };
		return newSequence(l);
	}

	public final Expression pValue() {
		Expression[] l = { _NonTerminal("String"), _NonTerminal("Number"), _NonTerminal("JSONObject"), _NonTerminal("Array"), _NonTerminal("Null"), _NonTerminal("True"), _NonTerminal("False") };
		return newChoice(l);
	}

	public final Expression pJSONObject() {
		Expression[] l = { newByteChar('{'), _NonTerminal("S"), newNew(newLink(_NonTerminal("Member")), _NonTerminal("S"), newRepetition(_NonTerminal("VALUESEP"), newLink(_NonTerminal("Member"))), newTagging("Object")), _NonTerminal("S"),
				newByteChar('}'), };
		return newSequence(l);
	}

	public final Expression pArray() {
		Expression[] valueSeq = { _NonTerminal("S"), newLink(_NonTerminal("Value")), _NonTerminal("S"), newRepetition(_NonTerminal("VALUESEP"), newLink(_NonTerminal("Value"))) };
		Expression[] l = { newByteChar('['), newNew(newSequence(valueSeq), newTagging("Array")), _NonTerminal("S"), newByteChar(']') };
		return newSequence(l);
	}

	public final Expression pString() {
		Expression notSeq = newSequence(newNot(newByteChar('"')), newAnyChar());
		Expression strValue = newChoice(newString("\\\""), newString("\\\\"), notSeq);
		Expression[] seq = { newByteChar('"'), newNew(newRepetition(strValue), newTagging("String")), newByteChar('"'), _NonTerminal("S") };
		return newSequence(seq);
	}

	public final Expression pNumber() {
		return newChoice(_NonTerminal("Integer"), _NonTerminal("Float"));
	}

	public final Expression pInteger() {
		Expression[] l = { newNew(newOption(newByteChar('-')), _NonTerminal("INT"), newTagging("Integer")), _NonTerminal("S") };
		return newSequence(l);
	}

	public final Expression pFloat() {
		Expression[] l = { newNew(newOption(newByteChar('-')), _NonTerminal("INT"), _NonTerminal("FRAC"), newOption(_NonTerminal("EXP")), newTagging("Float")), _NonTerminal("S") };
		return newSequence(l);
	}

	public final Expression pTrue() {
		return newNew(newString("true"), newTagging("True"));
	}

	public final Expression pFalse() {
		return newNew(newString("false"), newTagging("False"));
	}

	public final Expression pNull() {
		return newNew(newString("null"), newTagging("Null"));
	}

	public final Expression pNAMESEP() {
		Expression[] l = { newByteChar(':'), _NonTerminal("S") };
		return newSequence(l);

	}

	public final Expression pVALUESEP() {
		Expression[] l = { newByteChar(','), _NonTerminal("S") };
		return newSequence(l);
	}

	public final Expression pBOOLEAN() {
		Expression[] l = { newString("true"), newString("false") };
		return newChoice(l);
	}

	public final Expression pINT() {
		Expression[] l = { newByteChar('0'), newSequence(newCharSet("1-9"), newRepetition(_NonTerminal("DIGIT"))) };
		return newChoice(l);
	}

	public final Expression pDIGIT() {
		return newCharSet("0-9");
	}

	public final Expression pFRAC() {
		Expression[] l = { newByteChar('.'), newRepetition1(_NonTerminal("DIGIT")) };
		return newSequence(l);
	}

	public final Expression pEXP() {
		Expression choice = newChoice(newByteChar('-'), newByteChar('+'));
		Expression[] l = { newCharSet("Ee"), newOption(choice), newRepetition1(_NonTerminal("DIGIT")) };
		return newSequence(l);
	}

	public final Expression pSTRING() {
		Expression notSeq = newSequence(newNot(newByteChar('"')), newAnyChar());
		Expression strValue = newChoice(newString("\\\""), newString("\\\\"), notSeq);
		Expression[] seq = { newByteChar('"'), newRepetition(strValue), newByteChar('"'), _NonTerminal("S") };
		return newSequence(seq);
	}

	public final Expression pS() {
		Expression spacing = newCharSet("\t\n\r ");
		return newRepetition(spacing);
	}
}
