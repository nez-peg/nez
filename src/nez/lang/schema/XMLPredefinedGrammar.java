package nez.lang.schema;

import nez.lang.Expression;
import nez.lang.GrammarFile;
import nez.lang.Production;

public class XMLPredefinedGrammar extends AbstractPredefinedGrammar {

	public XMLPredefinedGrammar(GrammarFile grammar) {
		super(grammar);
	}

	@Override
	public void define() {
		defToplevel();
		defDocument();
		defProlog();
		defDoctype();
		defCDATA();
		defString();
		defNameToken();
		defIdToken();
		defText();
		defAttribute();
		defCDATASECT();
		defMISC();
		defCOMMENT();
		defPCDATA();
		defSingle();
		defEMPTY();
		defANY();
		defSpacing();
		defENDTAG();
		defNotAny();
		defPreEntity();
	}

	private final void defToplevel() {
		grammar.addProduction(null, "Toplevel", _NonTerminal("Document"));

	}

	private final void defDocument() {
		Expression[] l = { newOption(_NonTerminal("PROLOG")), _NonTerminal("S"), newOption(_NonTerminal("DOCTYPE")), _NonTerminal("S"), newRepetition1(_NonTerminal("Root")), _NonTerminal("S"), };
		grammar.addProduction(null, "Document", newSequence(l));
	}

	private final void defProlog() {
		Expression[] l = { newString("<?xml"), _NonTerminal("S"), newRepetition(_NonTerminal("ATTRIBUTE")), _NonTerminal("S"), newString("?>"), };
		grammar.addProduction(null, "PROLOG", newSequence(l));
	}

	private final void defDoctype() {
		Expression[] l = { newString("<!DOCTYPE"), _NonTerminal("S"), newRepetition(_NonTerminal("NMTOKEN")), _NonTerminal("S"), newOption(newString("SYSTEM")), _NonTerminal("S"), _NonTerminal("String"), _NonTerminal("S"), newString(">"), };
		grammar.addProduction(null, "DOCTYPE", newSequence(l));
	}

	private final void defNameToken() {
		Expression[] l = { newCharSet("A-Za-z_:"), newRepetition(newCharSet("-A-Za-z0-9:._")) };
		grammar.addProduction(null, "NMTOKEN", newSequence(l));
	}

	private final void defString() {
		Expression notSeq = newSequence(newNot(newByteChar('"')), newAnyChar());
		Expression strValue = newChoice(newString("\\\""), newString("\\\\"), notSeq);
		Expression[] seq = { newByteChar('"'), newRepetition(strValue), newByteChar('"'), _NonTerminal("S") };
		grammar.addProduction(null, "String", newSequence(seq));
	}

	private final void defCDATA() {
		Expression expr = newRepetition(newNot(newByteChar('"')), newAnyChar());
		grammar.addProduction(null, "CDATA", expr);
	}

	private final void defIdToken() {
		Expression[] l = { newCharSet("-A-Za-z0-9:._"), newRepetition(newCharSet("\\-A-Za-z0-9:._")), };
		grammar.addProduction(null, "IDTOKEN", newSequence(l));
	}

	private final void defText() {
		Expression expr = newSequence(newNot(newCharSet("<&")), newAnyChar());
		grammar.addProduction(null, "TEXT", newRepetition1(expr));
	}

	private final void defAttribute() {
		Expression[] l = { _NonTerminal("NMTOKEN"), _NonTerminal("S"), newByteChar('='), _NonTerminal("S"), _NonTerminal("String"), _NonTerminal("S") };
		grammar.addProduction(null, "ATTRIBUTE", newSequence(l));
	}

	private final void defCDATASECT() {
		Expression[] l = { newString("<![CDATA["), newRepetition(newNot(newString("]]>")), newAnyChar()), newString("]]>") };
		grammar.addProduction(null, "CDATASECT", newSequence(l));
	}

	private final void defMISC() {
		grammar.addProduction(null, "S", newRepetition(newChoice(_NonTerminal("SPACING"), _NonTerminal("COMMENT"))));
	}

	private final void defCOMMENT() {
		Expression[] l = { newString("<!--"), newRepetition(newNot(newString("-->")), newAnyChar()), newString("-->"), newRepetition(_NonTerminal("SPACING")) };
		grammar.addProduction(null, "COMMENT", newSequence(l));
	}

	private final void defEMPTY() {
		grammar.addProduction(null, "EMPTY", _NonTerminal("S"));
	}

	private final void defANY() {
		Expression[] l = { _NonTerminal("TEXT"), _NonTerminal("S"), };
		grammar.addProduction(null, "ANY", newSequence(l));
	}

	private final void defSpacing() {
		grammar.addProduction(null, "SPACING", newCharSet(" \t\r\n"));
	}

	private final void defENDTAG() {
		Expression l = newChoice(newAnd(newByteChar('>')), newAnd(newString("/>")));
		grammar.addProduction(null, "ENDTAG", l);
	}

	private final void defNotAny() {
		Expression l = newNot(newAnyChar());
		grammar.addProduction(null, "NotAny", l);
	}

	private final void defPCDATA() {
		Expression[] seq = { _NonTerminal("PreEntity"), _NonTerminal("Entity") };
		Expression entity = newSequence(newByteChar('&'), newChoice(seq), newByteChar(';'));
		Expression text = newRepetition1(newChoice(_NonTerminal("TEXT"), entity));
		grammar.addProduction(null, "PCDATA", text);
	}

	private final void defSingle() {
		Expression[] l = { _NonTerminal("PCDATA"), newEmpty() };
		grammar.addProduction(null, "SINGLE_PCDATA", newChoice(l));
	}

	private final void defPreEntity() {
		Expression[] keywords = { newString("lt"), newString("gt"), newString("amp"), newString("apos"), newString("quot"), newRepetition1(newCharSet("#a-zA-Z0-9")) };
		grammar.addProduction(null, "PreEntity", newChoice(keywords));
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
