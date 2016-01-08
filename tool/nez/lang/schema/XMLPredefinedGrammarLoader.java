package nez.lang.schema;

import nez.lang.Expression;
import nez.lang.Grammar;

public class XMLPredefinedGrammarLoader extends PredefinedGrammarLoader {

	public XMLPredefinedGrammarLoader(Grammar grammar) {
		super(grammar, "Toplevel");
	}

	public final Expression pToplevel() {
		return _NonTerminal("Document");
	}

	public final Expression pDocument() {
		Expression[] l = { newOption(null, _NonTerminal("PROLOG")), _NonTerminal("S"), newOption(null, _NonTerminal("DOCTYPE")), _NonTerminal("S"), newOneMore(null, _NonTerminal("Root")), _NonTerminal("S"), };
		return newSequence(l);
	}

	public final Expression pPROLOG() {
		Expression[] l = { newExpression(null, "<?xml"), _NonTerminal("S"), newZeroMore(null, _NonTerminal("ATTRIBUTE")), _NonTerminal("S"), newExpression(null, "?>"), };
		return newSequence(l);
	}

	public final Expression pDOCTYPE() {
		Expression[] l = { newExpression(null, "<!DOCTYPE"), _NonTerminal("S"), newZeroMore(null, _NonTerminal("NMTOKEN")), _NonTerminal("S"), newOption(null, newExpression(null, "SYSTEM")), _NonTerminal("S"), _NonTerminal("String"), _NonTerminal("S"),
				newExpression(null, ">"), };
		return newSequence(l);
	}

	public final Expression pNMTOKEN() {
		Expression[] l = { newCharSet(null, "A-Za-z_:"), newZeroMore(null, newCharSet(null, "-A-Za-z0-9:._")) };
		return newSequence(l);
	}

	public final Expression pString() {
		Expression notSeq = newSequence(newNot(null, newByte(null, '"')), newAny(null));
		Expression strValue = newChoice(newExpression(null, "\\\""), newExpression(null, "\\\\"), notSeq);
		Expression[] seq = { newByte(null, '"'), newZeroMore(null, strValue), newByte(null, '"'), _NonTerminal("S") };
		return newSequence(seq);
	}

	public final Expression pCDATA() {
		return newZeroMore(null, newSequence(newNot(null, newByte(null, '"')), newAny(null)));
	}

	public final Expression pIDTOKEN() {
		Expression[] l = { newCharSet(null, "-A-Za-z0-9:._"), newZeroMore(null, newCharSet(null, "\\-A-Za-z0-9:._")), };
		return newSequence(l);
	}

	public final Expression pTEXT() {
		Expression expr = newSequence(newNot(null, newCharSet(null, "<&")), newAny(null));
		return newOneMore(null, expr);
	}

	public final Expression pATTRIBUTE() {
		Expression[] l = { _NonTerminal("NMTOKEN"), _NonTerminal("S"), newByte(null, '='), _NonTerminal("S"), _NonTerminal("String"), _NonTerminal("S") };
		return newSequence(l);
	}

	public final Expression pCDATASECT() {
		Expression[] l = { newExpression(null, "<![CDATA["), newZeroMore(null, newSequence(newNot(null, newExpression(null, "]]>")), newAny(null))), newExpression(null, "]]>") };
		return newSequence(l);
	}

	public final Expression pS() {
		return newZeroMore(null, newChoice(_NonTerminal("SPACING"), _NonTerminal("COMMENT")));
	}

	public final Expression pCOMMENT() {
		Expression[] l = { newExpression(null, "<!--"), newZeroMore(null, newSequence(newNot(null, newExpression(null, "-->")), newAny(null))), newExpression(null, "-->"), newZeroMore(null, _NonTerminal("SPACING")) };
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
		return newCharSet(null, " \t\r\n");
	}

	public final Expression pENDTAG() {
		return newChoice(newAnd(null, newByte(null, '>')), newAnd(null, newExpression(null, "/>")));
	}

	public final Expression pNotAny() {
		return newNot(null, newAny(null));
	}

	public final Expression pPCDATA() {
		Expression[] seq = { _NonTerminal("PreEntity"), _NonTerminal("Entity") };
		Expression entity = newSequence(newByte(null, '&'), newChoice(seq), newByte(null, ';'));
		return newOneMore(null, newChoice(_NonTerminal("TEXT"), entity));
	}

	public final Expression pSINGLE_PCDATA() {
		Expression[] l = { _NonTerminal("PCDATA"), newEmpty(null) };
		return newChoice(l);
	}

	public final Expression pPreEntity() {
		Expression[] keywords = { newExpression(null, "lt"), newExpression(null, "gt"), newExpression(null, "amp"), newExpression(null, "apos"), newExpression(null, "quot"), newOneMore(null, newCharSet(null, "#a-zA-Z0-9")) };
		return newChoice(keywords);
	}
}
