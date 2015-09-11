package nez.peg.dtd;

import nez.lang.Expression;
import nez.lang.GrammarFile;
import nez.lang.expr.ExpressionCommons;

public class PredefinedRules {
	GrammarFile grammar;
	String rootElement;

	public PredefinedRules(GrammarFile grammar, String rootElement) {
		this.grammar = grammar;
		this.rootElement = rootElement;
	}

	public void defineRule() {
		defToplevel();
		defChunk();
		defFile();
		defDocument();
		defProlog();
		defDoctype();
		defContent();
		defName();
		defString();
		defNameToken();
		defIdToken();
		defText();
		defAttribute();
		defCDATASECT();
		defMISC();
		defCOMMENT();
		defPCDATA();
		defEMPTY();
		defANY();
		defSpacing();
		defENDTAG();
		defNotAny();
		defRootElement();
		defPreEntity();
	}

	final void defToplevel() {
		grammar.defineProduction(null, "Toplevel", ExpressionCommons.newNonTerminal(null, grammar, "Document"));

	}

	final void defChunk() {
		grammar.defineProduction(null, "Chunk", ExpressionCommons.newNonTerminal(null, grammar, "Document"));
	}

	final void defFile() {
		grammar.defineProduction(null, "File", ExpressionCommons.newNonTerminal(null, grammar, "Document"));
	}

	final void defDocument() {
		Expression[] l = { ExpressionCommons.newPoption(null, ExpressionCommons.newNonTerminal(null, grammar, "PROLOG")), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "MISC")),
				ExpressionCommons.newPoption(null, ExpressionCommons.newNonTerminal(null, grammar, "DOCTYPE")), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "MISC")), ExpressionCommons.newNonTerminal(null, grammar, "Content"),
				ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "MISC")), };
		grammar.defineProduction(null, "Document", grammar.newSequence(l));
	}

	final void defProlog() {
		Expression[] l = { ExpressionCommons.newString(null, "<?xml"), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "ATTRIBUTE")),
				ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newString(null, "?>"), };
		grammar.defineProduction(null, "PROLOG", grammar.newSequence(l));
	}

	final void defDoctype() {
		Expression[] l = { ExpressionCommons.newString(null, "<!DOCTYPE"), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "NAME")),
				ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newPoption(null, ExpressionCommons.newString(null, "SYSTEM")),
				ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newNonTerminal(null, grammar, "STRING"), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "S")),
				ExpressionCommons.newString(null, ">"), };
		grammar.defineProduction(null, "DOCTYPE", grammar.newSequence(l));
	}

	final void defContent() {
		Expression choice = grammar.newChoice(ExpressionCommons.newNonTerminal(null, grammar, "RootElement"), ExpressionCommons.newNonTerminal(null, grammar, "COMMENT"));
		Expression[] l = { ExpressionCommons.newPone(null, choice) };
		grammar.defineProduction(null, "Content", grammar.newSequence(l));
	}

	final void defName() {
		Expression[] l = { ExpressionCommons.newCharSet(null, "A-Za-z_:"), ExpressionCommons.newPzero(null, ExpressionCommons.newCharSet(null, "-A-Za-z0-9:._")) };
		grammar.defineProduction(null, "NAME", grammar.newSequence(l));
	}

	final void defString() {
		Expression[] l = { ExpressionCommons.newCbyte(null, false, '"'), grammar.newRepetition(ExpressionCommons.newPnot(null, ExpressionCommons.newCbyte(null, false, '"')), ExpressionCommons.newCany(null, false)), ExpressionCommons.newCbyte(null, false, '"') };
		grammar.defineProduction(null, "STRING", grammar.newSequence(l));
	}

	final void defNameToken() {
		Expression[] l = { ExpressionCommons.newCbyte(null, false, '"'), ExpressionCommons.newNonTerminal(null, grammar, "NAME"), ExpressionCommons.newCbyte(null, false, '"') };
		grammar.defineProduction(null, "NMTOKEN", grammar.newSequence(l));
	}

	final void defIdToken() {
		Expression[] l = { ExpressionCommons.newCharSet(null, "-A-Za-z0-9:._"), grammar.newRepetition(ExpressionCommons.newCharSet(null, "\\-A-Za-z0-9:._")), };
		grammar.defineProduction(null, "IDTOKEN", grammar.newSequence(l));
	}

	final void defText() {
		Expression onemoreExpr = grammar.newSequence(ExpressionCommons.newPnot(null, ExpressionCommons.newCharSet(null, "<&")), ExpressionCommons.newCany(null, false));
		grammar.defineProduction(null, "TEXT", ExpressionCommons.newPone(null, onemoreExpr));
	}

	final void defAttribute() {
		Expression[] l = { ExpressionCommons.newNonTerminal(null, grammar, "NAME"), grammar.newRepetition(ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newCbyte(null, false, '='),
				grammar.newRepetition(ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newNonTerminal(null, grammar, "STRING"), grammar.newRepetition(ExpressionCommons.newNonTerminal(null, grammar, "S")) };
		grammar.defineProduction(null, "ATTRIBUTE", grammar.newSequence(l));
	}

	final void defCDATASECT() {
		Expression[] l = { ExpressionCommons.newString(null, "<![CDATA["), grammar.newRepetition(ExpressionCommons.newPnot(null, ExpressionCommons.newString(null, "]]>")), ExpressionCommons.newCany(null, false)), ExpressionCommons.newString(null, "]]>") };
		grammar.defineProduction(null, "CDATASECT", grammar.newSequence(l));
	}

	final void defMISC() {
		grammar.defineProduction(null, "MISC", grammar.newChoice(ExpressionCommons.newNonTerminal(null, grammar, "S"), ExpressionCommons.newNonTerminal(null, grammar, "COMMENT")));
	}

	final void defCOMMENT() {
		Expression[] l = { ExpressionCommons.newString(null, "<!--"), grammar.newRepetition(ExpressionCommons.newPnot(null, ExpressionCommons.newString(null, "-->")), ExpressionCommons.newCany(null, false)), ExpressionCommons.newString(null, "-->"),
				grammar.newRepetition(ExpressionCommons.newNonTerminal(null, grammar, "S")) };
		grammar.defineProduction(null, "COMMENT", grammar.newSequence(l));
	}

	final void defEMPTY() {
		grammar.defineProduction(null, "EMPTY", grammar.newRepetition(ExpressionCommons.newNonTerminal(null, grammar, "S")));
	}

	final void defANY() {
		Expression[] l = { grammar.newRepetition(ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newNonTerminal(null, grammar, "TEXT"), grammar.newRepetition(ExpressionCommons.newNonTerminal(null, grammar, "S")), };
		grammar.defineProduction(null, "ANY", grammar.newSequence(l));
	}

	final void defSpacing() {
		grammar.defineProduction(null, "S", ExpressionCommons.newCharSet(null, " \t\r\n"));
	}

	final void defENDTAG() {
		Expression l = grammar.newChoice(ExpressionCommons.newPand(null, ExpressionCommons.newCbyte(null, false, '>')), ExpressionCommons.newPand(null, ExpressionCommons.newString(null, "/>")));
		grammar.defineProduction(null, "ENDTAG", l);
	}

	final void defNotAny() {
		Expression l = ExpressionCommons.newPnot(null, ExpressionCommons.newCany(null, false));
		grammar.defineProduction(null, "NotAny", l);
	}

	final void defPCDATA() {
		Expression[] seq = { ExpressionCommons.newNonTerminal(null, grammar, "PreEntity"), ExpressionCommons.newNonTerminal(null, grammar, "Entity") };
		Expression[] l = { ExpressionCommons.newNonTerminal(null, grammar, "TEXT"), grammar.newSequence(ExpressionCommons.newCbyte(null, false, '&'), grammar.newChoice(seq), ExpressionCommons.newCbyte(null, false, ';')) };
		grammar.defineProduction(null, "PCDATA", grammar.newChoice(l));
	}

	final void defRootElement() {
		String rootElementName = "Element_" + this.rootElement;
		grammar.defineProduction(null, "RootElement", ExpressionCommons.newNonTerminal(null, grammar, rootElementName));
	}

	final void defPreEntity() {
		Expression[] keywords = { ExpressionCommons.newString(null, "lt"), ExpressionCommons.newString(null, "gt"), ExpressionCommons.newString(null, "amp"), ExpressionCommons.newString(null, "apos"), ExpressionCommons.newString(null, "quot"),
				grammar.newRepetition1(ExpressionCommons.newCharSet(null, "#a-zA-Z0-9")) };
		grammar.defineProduction(null, "PreEntity", grammar.newChoice(keywords));
	}
}
