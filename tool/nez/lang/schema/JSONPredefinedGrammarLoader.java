package nez.lang.schema;

import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.Grammar;

public class JSONPredefinedGrammarLoader extends PredefinedGrammarLoader {

	static final Symbol _Key = Symbol.unique("key");
	static final Symbol _Value = Symbol.unique("value");

	public JSONPredefinedGrammarLoader(Grammar grammar) {
		super(grammar, "File");
	}

	public final Expression pFile() {
		Expression[] l = { _NonTerminal("S"), _NonTerminal("Root"), _NonTerminal("S") };
		return newSequence(l);
	}

	public final Expression pAny() {
		return newSequence(newLinkTree(_NonTerminal("Member")), _NonTerminal("S"), newOption(null, _NonTerminal("VALUESEP")), newTag(null, Symbol.unique("Any")));
	}

	public final Expression pMember() {
		Expression[] l = { newBeginTree(null, 0), newLinkTree(null, _Key, _NonTerminal("String")), _NonTerminal("NAMESEP"), newLinkTree(null, _Value, _NonTerminal("Value")), newEndTree(null, 0) };
		return newSequence(l);
	}

	public final Expression pValue() {
		Expression[] l = { _NonTerminal("String"), _NonTerminal("Number"), _NonTerminal("JSONObject"), _NonTerminal("Array"), _NonTerminal("Null"), _NonTerminal("True"), _NonTerminal("False") };
		return newChoice(l);
	}

	public final Expression pJSONObject() {
		Expression[] l = { newByte(null, '{'), _NonTerminal("S"), newBeginTree(null, 0), newLinkTree(_NonTerminal("Member")), _NonTerminal("S"), newZeroMore(null, newSequence(_NonTerminal("VALUESEP"), newLinkTree(_NonTerminal("Member")))),
				newTag(null, Symbol.unique("Object")), newEndTree(null, 0), _NonTerminal("S"), newByte(null, '}'), };
		return newSequence(l);
	}

	public final Expression pArray() {
		Expression[] valueSeq = { _NonTerminal("S"), newLinkTree(_NonTerminal("Value")), _NonTerminal("S"), newZeroMore(null, newSequence(_NonTerminal("VALUESEP"), newLinkTree(_NonTerminal("Value")))) };
		Expression[] l = { newByte(null, '['), newBeginTree(null, 0), newSequence(valueSeq), newTag(null, Symbol.unique("Array")), newEndTree(null, 0), _NonTerminal("S"), newByte(null, ']') };
		return newSequence(l);
	}

	public final Expression pString() {
		Expression notSeq = newSequence(newNot(null, newByte(null, '"')), newAny(null));
		Expression strValue = newChoice(newExpression(null, "\\\""), newExpression(null, "\\\\"), notSeq);
		Expression[] seq = { newByte(null, '"'), newBeginTree(null, 0), newZeroMore(null, strValue), newTag(null, Symbol.unique("String")), newEndTree(null, 0), newByte(null, '"'), _NonTerminal("S") };
		return newSequence(seq);
	}

	public final Expression pNumber() {
		return newChoice(_NonTerminal("Integer"), _NonTerminal("Float"));
	}

	public final Expression pInteger() {
		Expression[] l = { newBeginTree(null, 0), newOption(null, newByte(null, '-')), _NonTerminal("INT"), newTag(null, Symbol.unique("Integer")), newEndTree(null, 0), _NonTerminal("S") };
		return newSequence(l);
	}

	public final Expression pFloat() {
		Expression[] l = { newBeginTree(null, 0), newOption(null, newByte(null, '-')), _NonTerminal("INT"), _NonTerminal("FRAC"), newOption(null, _NonTerminal("EXP")), newTag(null, Symbol.unique("Float")), newEndTree(null, 0), _NonTerminal("S") };
		return newSequence(l);
	}

	public final Expression pTrue() {
		return newSequence(newBeginTree(null, 0), newExpression(null, "true"), newTag(null, Symbol.unique("True")), newEndTree(null, 0));
	}

	public final Expression pFalse() {
		return newSequence(newBeginTree(null, 0), newExpression(null, "false"), newTag(null, Symbol.unique("False")), newEndTree(null, 0));
	}

	public final Expression pNull() {
		return newSequence(newBeginTree(null, 0), newExpression(null, "null"), newTag(null, Symbol.unique("Null")), newEndTree(null, 0));
	}

	public final Expression pNAMESEP() {
		Expression[] l = { newByte(null, ':'), _NonTerminal("S") };
		return newSequence(l);

	}

	public final Expression pVALUESEP() {
		Expression[] l = { newByte(null, ','), _NonTerminal("S") };
		return newSequence(l);
	}

	public final Expression pBOOLEAN() {
		Expression[] l = { newExpression(null, "true"), newExpression(null, "false") };
		return newChoice(l);
	}

	public final Expression pINT() {
		Expression[] l = { newByte(null, '0'), newSequence(newCharSet(null, null, "1-9"), newZeroMore(null, _NonTerminal("DIGIT"))) };
		return newChoice(l);
	}

	public final Expression pDIGIT() {
		return newCharSet(null, "0-9");
	}

	public final Expression pFRAC() {
		Expression[] l = { newByte(null, '.'), newOneMore(null, _NonTerminal("DIGIT")) };
		return newSequence(l);
	}

	public final Expression pEXP() {
		Expression choice = newChoice(newByte(null, '-'), newByte(null, '+'));
		Expression[] l = { newCharSet(null, "Ee"), newOption(null, choice), newOneMore(null, _NonTerminal("DIGIT")) };
		return newSequence(l);
	}

	public final Expression pSTRING() {
		Expression notSeq = newSequence(newNot(null, newByte(null, '"')), newAny(null));
		Expression strValue = newChoice(newExpression(null, "\\\""), newExpression(null, "\\\\"), notSeq);
		Expression[] seq = { newByte(null, '"'), newZeroMore(null, strValue), newByte(null, '"'), _NonTerminal("S") };
		return newSequence(seq);
	}

	public final Expression pS() {
		Expression spacing = newCharSet(null, "\t\n\r ");
		return newZeroMore(null, spacing);
	}
}
