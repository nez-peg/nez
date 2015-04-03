package nez.x;

import nez.Grammar;
import nez.expr.Expression;

public class PredefinedRules {
	Grammar grammar;
	String rootElement;
	public PredefinedRules(Grammar grammar, String rootElement) {
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
	}


	void defToplevel() {
		grammar.defineRule(null, "Toplevel", grammar.newNonTerminal("Document"));
	}
	void defChunk() {
		grammar.defineRule(null, "Chunk", grammar.newNonTerminal("Document"));
	}
	void defFile() {
		grammar.defineRule(null, "File", grammar.newNonTerminal("Document"));
	}
	void defDocument() {
		Expression[] l = {
				grammar.newOption(grammar.newNonTerminal("PROLOG")),
				grammar.newRepetition(grammar.newNonTerminal("MISC")),
				grammar.newOption(grammar.newNonTerminal("DOCTYPE")),
				grammar.newRepetition(grammar.newNonTerminal("MISC")),
				grammar.newNonTerminal("Content"),
				grammar.newRepetition(grammar.newNonTerminal("MISC")),
		};
		grammar.defineRule(null, "Document", grammar.newSequence(l));
	}
	void defProlog() {
		Expression[] l = {
				grammar.newString("<?xml"),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newRepetition(grammar.newNonTerminal("ATTRIBUTE")),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newString("?>"),
		};
		grammar.defineRule(null, "PROLOG", grammar.newSequence(l));
	}
	void defDoctype() {
		Expression[] l = {
				grammar.newString("<!DOCTYPE"),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newRepetition(grammar.newNonTerminal("NAME")),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newOption(grammar.newString("SYSTEM")),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newNonTerminal("STRING"),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newString(">"),
		};
		grammar.defineRule(null, "DOCTYPE", grammar.newSequence(l));
	}
	void defContent() {
		Expression choice = grammar.newChoice(grammar.newNonTerminal("RootElement"),
				grammar.newNonTerminal("COMMENT"));
		Expression[] l = {
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newRepetition(choice, grammar.newNonTerminal("S"))
		};
		grammar.defineRule(null, "Content", grammar.newSequence(l));
	}
	void defName() {
		Expression[] l = {
				grammar.newCharSet(null, "A-Za-z_:"),
				grammar.newRepetition(grammar.newCharSet(null, "-A-Za-z0-9:._"))
		};
		grammar.defineRule(null, "NAME", grammar.newSequence(l));
	}
	void defString() {
		Expression[] l = {
				grammar.newByteChar('"'),
				grammar.newRepetition(grammar.newNot(grammar.newByteChar('"')),
						grammar.newAnyChar()),
				grammar.newByteChar('"')
		};
		grammar.defineRule(null, "STRING", grammar.newSequence(l));
	}
	void defNameToken() {
		Expression[] l = {
				grammar.newByteChar('"'),
				grammar.newNonTerminal("NAME"),
				grammar.newByteChar('"')
		};
		grammar.defineRule(null, "NMTOKEN", grammar.newSequence(l));
	}
	void defIdToken() {
		Expression[] l = {
				grammar.newRepetition(grammar.newCharSet(null, "-A-Za-z0-9:._")),
				grammar.newCharSet(null, "-A-Za-z0-9:._"),
		};
		grammar.defineRule(null, "IDTOKEN", grammar.newSequence(l));
	}
	void defText() {
		Expression onemoreExpr = grammar
				.newSequence(grammar.newNot(grammar.newCharSet(null, "<&")), grammar.newAnyChar());
		Expression[] l = {
				onemoreExpr,
				grammar.newRepetition(onemoreExpr)
		};
		grammar.defineRule(null, "TEXT", grammar.newSequence(l));
	}
	void defAttribute() {
		Expression[] l = {
				grammar.newNonTerminal("NAME"),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newByteChar('='),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newNonTerminal("STRING"),
		};
		grammar.defineRule(null, "ATTRIBUTE", grammar.newSequence(l));
	}
	void defCDATASECT() {
		Expression[] l = {
				grammar.newString("<![CDATA["),
				grammar.newRepetition(grammar.newNot(grammar.newString("]]>")),
						grammar.newAnyChar()),
				grammar.newString("]]>")
		};
		grammar.defineRule(null, "CDATASECT", grammar.newSequence(l));
	}
	void defMISC() {
		grammar.defineRule(null, "MISC",
				grammar.newChoice(grammar.newNonTerminal("S"), grammar.newNonTerminal("COMMENT")));
	}
	void defCOMMENT() {
		Expression[] l = {
				grammar.newString("<!--"),
				grammar.newRepetition(grammar.newNot(grammar.newString("-->")),
						grammar.newAnyChar()),
				grammar.newString("-->")
		};
		grammar.defineRule(null, "COMMENT", grammar.newSequence(l));
	}
	void defEMPTY() {
		grammar.defineRule(null, "EMPTY", grammar.newRepetition(grammar.newNonTerminal("S")));
	}
	void defANY() {
		Expression[] l = {
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newNonTerminal("TEXT"),
				grammar.newRepetition(grammar.newNonTerminal("S")),
		};
		grammar.defineRule(null, "ANY", grammar.newSequence(l));
	}
	void defSpacing(){
		grammar.defineRule(null, "S", grammar.newCharSet(null, " \t\r\n"));
	}
	void defENDTAG() {
		Expression l = grammar.newChoice(grammar.newAnd(grammar.newByteChar('>')),
				grammar.newAnd(grammar.newString("/>")));
		grammar.defineRule(null, "ENDTAG", l);
	}
	void defNotAny() {
		Expression l = grammar.newNot(grammar.newAnyChar());
		grammar.defineRule(null, "NotAny", l);
	}
	void defPCDATA() {
		Expression[] l = {
				grammar.newNonTerminal("TEXT"),
				grammar.newSequence(grammar.newByteChar('&'), grammar.newNonTerminal("entity"),
						grammar.newByteChar(';'))
		};
		grammar.defineRule(null, "PCDATA", grammar.newChoice(l));
	}
	void defRootElement(){
		String rootElementName = "Element_" + this.rootElement;
		grammar.defineRule(null, "RootElement", grammar.newNonTerminal(rootElementName));
	}
}
