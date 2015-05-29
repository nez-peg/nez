package nez.peg.dtd;

import nez.lang.Expression;
import nez.lang.GrammarFactory;
import nez.lang.NameSpace;

public class PredefinedRules {
	NameSpace grammar;
	String rootElement;
	public PredefinedRules(NameSpace grammar, String rootElement) {
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
		grammar.defineProduction(null, "Toplevel", grammar.newNonTerminal("Document"));
	}

	final void defChunk() {
		grammar.defineProduction(null, "Chunk", grammar.newNonTerminal("Document"));
	}

	final void defFile() {
		grammar.defineProduction(null, "File", grammar.newNonTerminal("Document"));
	}

	final void defDocument() {
		Expression[] l = {
				grammar.newOption(grammar.newNonTerminal("PROLOG")),
				grammar.newRepetition(grammar.newNonTerminal("MISC")),
				grammar.newOption(grammar.newNonTerminal("DOCTYPE")),
				grammar.newRepetition(grammar.newNonTerminal("MISC")),
				grammar.newNonTerminal("Content"),
				grammar.newRepetition(grammar.newNonTerminal("MISC")),
		};
		grammar.defineProduction(null, "Document", grammar.newSequence(l));
	}

	final void defProlog() {
		Expression[] l = {
				grammar.newString("<?xml"),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newRepetition(grammar.newNonTerminal("ATTRIBUTE")),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newString("?>"),
		};
		grammar.defineProduction(null, "PROLOG", grammar.newSequence(l));
	}

	final void defDoctype() {
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
		grammar.defineProduction(null, "DOCTYPE", grammar.newSequence(l));
	}

	final void defContent() {
		Expression choice = grammar.newChoice(grammar.newNonTerminal("RootElement"),
				grammar.newNonTerminal("COMMENT"));
		Expression[] l = {
			grammar.newRepetition1(choice)
		};
		grammar.defineProduction(null, "Content", grammar.newSequence(l));
	}

	final void defName() {
		Expression[] l = {
				GrammarFactory.newCharSet(null, "A-Za-z_:"),
				grammar.newRepetition(GrammarFactory.newCharSet(null, "-A-Za-z0-9:._"))
		};
		grammar.defineProduction(null, "NAME", grammar.newSequence(l));
	}

	final void defString() {
		Expression[] l = {
				grammar.newByteChar('"'),
				grammar.newRepetition(grammar.newNot(grammar.newByteChar('"')),
						grammar.newAnyChar()),
				grammar.newByteChar('"')
		};
		grammar.defineProduction(null, "STRING", grammar.newSequence(l));
	}

	final void defNameToken() {
		Expression[] l = {
				grammar.newByteChar('"'),
				grammar.newNonTerminal("NAME"),
				grammar.newByteChar('"')
		};
		grammar.defineProduction(null, "NMTOKEN", grammar.newSequence(l));
	}

	final void defIdToken() {
		Expression[] l = {
				GrammarFactory.newCharSet(null, "-A-Za-z0-9:._"),
				grammar.newRepetition(GrammarFactory.newCharSet(null, "\\-A-Za-z0-9:._")),
		};
		grammar.defineProduction(null, "IDTOKEN", grammar.newSequence(l));
	}

	final void defText() {
		Expression onemoreExpr = grammar.newSequence(
				grammar.newNot(GrammarFactory.newCharSet(null, "<&")), grammar.newAnyChar());
		grammar.defineProduction(null, "TEXT", GrammarFactory.newRepetition1(null, onemoreExpr));
	}

	final void defAttribute() {
		Expression[] l = {
				grammar.newNonTerminal("NAME"),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newByteChar('='),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newNonTerminal("STRING"),
				grammar.newRepetition(grammar.newNonTerminal("S"))
		};
		grammar.defineProduction(null, "ATTRIBUTE", grammar.newSequence(l));
	}

	final void defCDATASECT() {
		Expression[] l = {
				grammar.newString("<![CDATA["),
				grammar.newRepetition(grammar.newNot(grammar.newString("]]>")),
						grammar.newAnyChar()),
				grammar.newString("]]>")
		};
		grammar.defineProduction(null, "CDATASECT", grammar.newSequence(l));
	}

	final void defMISC() {
		grammar.defineProduction(null, "MISC",
				grammar.newChoice(grammar.newNonTerminal("S"), grammar.newNonTerminal("COMMENT")));
	}

	final void defCOMMENT() {
		Expression[] l = {
				grammar.newString("<!--"),
				grammar.newRepetition(grammar.newNot(grammar.newString("-->")),
						grammar.newAnyChar()),
				grammar.newString("-->"),
				GrammarFactory.newRepetition(null, grammar.newNonTerminal("S"))
		};
		grammar.defineProduction(null, "COMMENT", grammar.newSequence(l));
	}

	final void defEMPTY() {
		grammar.defineProduction(null, "EMPTY", grammar.newRepetition(grammar.newNonTerminal("S")));
	}

	final void defANY() {
		Expression[] l = {
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newNonTerminal("TEXT"),
				grammar.newRepetition(grammar.newNonTerminal("S")),
		};
		grammar.defineProduction(null, "ANY", grammar.newSequence(l));
	}

	final void defSpacing() {
		grammar.defineProduction(null, "S", GrammarFactory.newCharSet(null, " \t\r\n"));
	}

	final void defENDTAG() {
		Expression l = grammar.newChoice(grammar.newAnd(grammar.newByteChar('>')),
				grammar.newAnd(grammar.newString("/>")));
		grammar.defineProduction(null, "ENDTAG", l);
	}

	final void defNotAny() {
		Expression l = grammar.newNot(grammar.newAnyChar());
		grammar.defineProduction(null, "NotAny", l);
	}

	final void defPCDATA() {
		Expression[] seq = {
				grammar.newNonTerminal("PreEntity"), grammar.newNonTerminal("Entity")
		};
		Expression[] l = {
				grammar.newNonTerminal("TEXT"),
				grammar.newSequence(grammar.newByteChar('&'), grammar.newChoice(seq),
						grammar.newByteChar(';'))
		};
		grammar.defineProduction(null, "PCDATA", grammar.newChoice(l));
	}

	final void defRootElement() {
		String rootElementName = "Element_" + this.rootElement;
		grammar.defineProduction(null, "RootElement", grammar.newNonTerminal(rootElementName));
	}

	final void defPreEntity() {
		Expression[] keywords = {
				grammar.newString("lt"),
				grammar.newString("gt"),
				grammar.newString("amp"),
				grammar.newString("apos"),
				grammar.newString("quot"),
				grammar.newRepetition1(grammar.newCharSet("#a-zA-Z0-9"))
		};
		grammar.defineProduction(null, "PreEntity", grammar.newChoice(keywords));
	}
}
