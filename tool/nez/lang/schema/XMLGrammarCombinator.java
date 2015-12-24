package nez.lang.schema;

import nez.lang.Expression;
import nez.lang.Grammar;

public class XMLGrammarCombinator extends PredefinedGrammarCombinator {

	public XMLGrammarCombinator(Grammar grammar) {
		super(grammar, "Toplevel");
	}

	public final Expression pToplevel() {
		return _NonTerminal("Document");
	}

	public final Expression pDocument() {
		Expression[] l = { newOption(_NonTerminal("PROLOG")), _NonTerminal("S"), newOption(_NonTerminal("DOCTYPE")), _NonTerminal("S"), newRepetition1(_NonTerminal("Root")), _NonTerminal("S"), };
		return newSequence(l);
	}

	public final Expression pPROLOG() {
		Expression[] l = { newString("<?xml"), _NonTerminal("S"), newRepetition(_NonTerminal("ATTRIBUTE")), _NonTerminal("S"), newString("?>"), };
		return newSequence(l);
	}

	public final Expression pDOCTYPE() {
		Expression[] l = { newString("<!DOCTYPE"), _NonTerminal("S"), newRepetition(_NonTerminal("NMTOKEN")), _NonTerminal("S"), newOption(newString("SYSTEM")), _NonTerminal("S"), _NonTerminal("String"), _NonTerminal("S"), newString(">"), };
		return newSequence(l);
	}

	public final Expression pNMTOKEN() {
		Expression[] l = { newCharSet("A-Za-z_:"), newRepetition(newCharSet("-A-Za-z0-9:._")) };
		return newSequence(l);
	}

	public final Expression pString() {
		Expression notSeq = newSequence(newNot(newByteChar('"')), newAnyChar());
		Expression strValue = newChoice(newString("\\\""), newString("\\\\"), notSeq);
		Expression[] seq = { newByteChar('"'), newRepetition(strValue), newByteChar('"'), _NonTerminal("S") };
		return newSequence(seq);
	}

	public final Expression pCDATA() {
		return newRepetition(newNot(newByteChar('"')), newAnyChar());
	}

	public final Expression pIDTOKEN() {
		Expression[] l = { newCharSet("-A-Za-z0-9:._"), newRepetition(newCharSet("\\-A-Za-z0-9:._")), };
		return newSequence(l);
	}

	public final Expression pTEXT() {
		Expression expr = newSequence(newNot(newCharSet("<&")), newAnyChar());
		return newRepetition1(expr);
	}

	public final Expression pATTRIBUTE() {
		Expression[] l = { _NonTerminal("NMTOKEN"), _NonTerminal("S"), newByteChar('='), _NonTerminal("S"), _NonTerminal("String"), _NonTerminal("S") };
		return newSequence(l);
	}

	public final Expression pCDATASECT() {
		Expression[] l = { newString("<![CDATA["), newRepetition(newNot(newString("]]>")), newAnyChar()), newString("]]>") };
		return newSequence(l);
	}

	public final Expression pS() {
		return newRepetition(newChoice(_NonTerminal("SPACING"), _NonTerminal("COMMENT")));
	}

	public final Expression pCOMMENT() {
		Expression[] l = { newString("<!--"), newRepetition(newNot(newString("-->")), newAnyChar()), newString("-->"), newRepetition(_NonTerminal("SPACING")) };
		return newSequence(l);
	}

	public final Expression pEMPTY() {
		return _NonTerminal("S");
	}

	public final Expression pANY() {
		Expression[] l = { _NonTerminal("TEXT"), _NonTerminal("S"), };
		return newSequence(l);
	}

	public final Expression pSPACING() {
		return newCharSet(" \t\r\n");
	}

	public final Expression pENDTAG() {
		return newChoice(newAnd(newByteChar('>')), newAnd(newString("/>")));
	}

	public final Expression pNotAny() {
		return newNot(newAnyChar());
	}

	public final Expression pPCDATA() {
		Expression[] seq = { _NonTerminal("PreEntity"), _NonTerminal("Entity") };
		Expression entity = newSequence(newByteChar('&'), newChoice(seq), newByteChar(';'));
		return newRepetition1(newChoice(_NonTerminal("TEXT"), entity));
	}

	public final Expression pSINGLE_PCDATA() {
		Expression[] l = { _NonTerminal("PCDATA"), newEmpty() };
		return newChoice(l);
	}

	public final Expression pPreEntity() {
		Expression[] keywords = { newString("lt"), newString("gt"), newString("amp"), newString("apos"), newString("quot"), newRepetition1(newCharSet("#a-zA-Z0-9")) };
		return newChoice(keywords);
	}
}
