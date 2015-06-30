package nez.peg.dtd;

import nez.lang.Expression;
import nez.lang.GrammarFactory;
import nez.lang.GrammarFile;

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
		grammar.defineProduction(null, "Toplevel",
				GrammarFactory.newNonTerminal(null, grammar, "Document"));

	}

	final void defChunk() {
		grammar.defineProduction(null, "Chunk",
				GrammarFactory.newNonTerminal(null, grammar, "Document"));
	}

	final void defFile() {
		grammar.defineProduction(null, "File",
				GrammarFactory.newNonTerminal(null, grammar, "Document"));
	}

	final void defDocument() {
		Expression[] l = {
				GrammarFactory.newOption(null,
						GrammarFactory.newNonTerminal(null, grammar, "PROLOG")),
				GrammarFactory.newRepetition(null,
						GrammarFactory.newNonTerminal(null, grammar, "MISC")),
				GrammarFactory.newOption(null,
						GrammarFactory.newNonTerminal(null, grammar, "DOCTYPE")),
				GrammarFactory.newRepetition(null,
						GrammarFactory.newNonTerminal(null, grammar, "MISC")),
				GrammarFactory.newNonTerminal(null, grammar, "Content"),
				GrammarFactory.newRepetition(null,
						GrammarFactory.newNonTerminal(null, grammar, "MISC")),
		};
		grammar.defineProduction(null, "Document", grammar.newSequence(l));
	}

	final void defProlog() {
		Expression[] l = {
				GrammarFactory.newString(null, "<?xml"),
				GrammarFactory.newRepetition(null,
						GrammarFactory.newNonTerminal(null, grammar, "S")),
				GrammarFactory.newRepetition(null,
						GrammarFactory.newNonTerminal(null, grammar, "ATTRIBUTE")),
				GrammarFactory.newRepetition(null,
						GrammarFactory.newNonTerminal(null, grammar, "S")),
				GrammarFactory.newString(null, "?>"),
		};
		grammar.defineProduction(null, "PROLOG", grammar.newSequence(l));
	}

	final void defDoctype() {
		Expression[] l = {
				GrammarFactory.newString(null, "<!DOCTYPE"),
				GrammarFactory.newRepetition(null,
						GrammarFactory.newNonTerminal(null, grammar, "S")),
				GrammarFactory.newRepetition(null,
						GrammarFactory.newNonTerminal(null, grammar, "NAME")),
				GrammarFactory.newRepetition(null,
						GrammarFactory.newNonTerminal(null, grammar, "S")),
				GrammarFactory.newOption(null, GrammarFactory.newString(null, "SYSTEM")),
				GrammarFactory.newRepetition(null,
						GrammarFactory.newNonTerminal(null, grammar, "S")),
				GrammarFactory.newNonTerminal(null, grammar, "STRING"),
				GrammarFactory.newRepetition(null,
						GrammarFactory.newNonTerminal(null, grammar, "S")),
				GrammarFactory.newString(null, ">"),
		};
		grammar.defineProduction(null, "DOCTYPE", grammar.newSequence(l));
	}

	final void defContent() {
		Expression choice = grammar.newChoice(
				GrammarFactory.newNonTerminal(null, grammar, "RootElement"),
				GrammarFactory.newNonTerminal(null, grammar, "COMMENT"));
		Expression[] l = {
			GrammarFactory.newRepetition1(null, choice)
		};
		grammar.defineProduction(null, "Content", grammar.newSequence(l));
	}

	final void defName() {
		Expression[] l = {
				GrammarFactory.newCharSet(null, "A-Za-z_:"),
				GrammarFactory
						.newRepetition(null, GrammarFactory.newCharSet(null, "-A-Za-z0-9:._"))
		};
		grammar.defineProduction(null, "NAME", grammar.newSequence(l));
	}

	final void defString() {
		Expression[] l = {
				GrammarFactory.newByteChar(null, false, '"'),
				grammar.newRepetition(
						GrammarFactory.newNot(null, GrammarFactory.newByteChar(null, false, '"')),
						GrammarFactory.newAnyChar(null, false)),
				GrammarFactory.newByteChar(null, false, '"')
		};
		grammar.defineProduction(null, "STRING", grammar.newSequence(l));
	}

	final void defNameToken() {
		Expression[] l = {
				GrammarFactory.newByteChar(null, false, '"'),
				GrammarFactory.newNonTerminal(null, grammar, "NAME"),
				GrammarFactory.newByteChar(null, false, '"')
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
				GrammarFactory.newNot(null, GrammarFactory.newCharSet(null, "<&")),
				GrammarFactory.newAnyChar(null, false));
		grammar.defineProduction(null, "TEXT", GrammarFactory.newRepetition1(null, onemoreExpr));
	}

	final void defAttribute() {
		Expression[] l = {
				GrammarFactory.newNonTerminal(null, grammar, "NAME"),
				grammar.newRepetition(GrammarFactory.newNonTerminal(null, grammar, "S")),
				GrammarFactory.newByteChar(null, false, '='),
				grammar.newRepetition(GrammarFactory.newNonTerminal(null, grammar, "S")),
				GrammarFactory.newNonTerminal(null, grammar,"STRING"),
				grammar.newRepetition(GrammarFactory.newNonTerminal(null, grammar, "S"))
		};
		grammar.defineProduction(null, "ATTRIBUTE", grammar.newSequence(l));
	}

	final void defCDATASECT() {
		Expression[] l = {
				GrammarFactory.newString(null, "<![CDATA["),
				grammar.newRepetition(
						GrammarFactory.newNot(null, GrammarFactory.newString(null, "]]>")),
						GrammarFactory.newAnyChar(null, false)),
				GrammarFactory.newString(null, "]]>")
		};
		grammar.defineProduction(null, "CDATASECT", grammar.newSequence(l));
	}

	final void defMISC() {
		grammar.defineProduction(null, "MISC",
				grammar.newChoice(GrammarFactory.newNonTerminal(null, grammar, "S"),
						GrammarFactory.newNonTerminal(null, grammar, "COMMENT")));
	}

	final void defCOMMENT() {
		Expression[] l = {
				GrammarFactory.newString(null, "<!--"),
				grammar.newRepetition(
						GrammarFactory.newNot(null, GrammarFactory.newString(null, "-->")),
						GrammarFactory.newAnyChar(null, false)),
				GrammarFactory.newString(null, "-->"),
				grammar.newRepetition(GrammarFactory.newNonTerminal(null, grammar, "S"))
		};
		grammar.defineProduction(null, "COMMENT", grammar.newSequence(l));
	}

	final void defEMPTY() {
		grammar.defineProduction(null, "EMPTY",
				grammar.newRepetition(GrammarFactory.newNonTerminal(null, grammar, "S")));
	}

	final void defANY() {
		Expression[] l = {
				grammar.newRepetition(GrammarFactory.newNonTerminal(null, grammar, "S")),
				GrammarFactory.newNonTerminal(null, grammar, "TEXT"),
				grammar.newRepetition(GrammarFactory.newNonTerminal(null, grammar, "S")),
		};
		grammar.defineProduction(null, "ANY", grammar.newSequence(l));
	}

	final void defSpacing() {
		grammar.defineProduction(null, "S", GrammarFactory.newCharSet(null, " \t\r\n"));
	}

	final void defENDTAG() {
		Expression l = grammar.newChoice(
				GrammarFactory.newAnd(null, GrammarFactory.newByteChar(null, false, '>')),
				GrammarFactory.newAnd(null, GrammarFactory.newString(null, "/>")));
		grammar.defineProduction(null, "ENDTAG", l);
	}

	final void defNotAny() {
		Expression l = GrammarFactory.newNot(null, GrammarFactory.newAnyChar(null, false));
		grammar.defineProduction(null, "NotAny", l);
	}

	final void defPCDATA() {
		Expression[] seq = {
				GrammarFactory.newNonTerminal(null, grammar, "PreEntity"),
				GrammarFactory.newNonTerminal(null, grammar, "Entity")
		};
		Expression[] l = {
				GrammarFactory.newNonTerminal(null, grammar,"TEXT"),
				grammar.newSequence(GrammarFactory.newByteChar(null, false, '&'),
						grammar.newChoice(seq), GrammarFactory.newByteChar(null, false, ';'))
		};
		grammar.defineProduction(null, "PCDATA", grammar.newChoice(l));
	}

	final void defRootElement() {
		String rootElementName = "Element_" + this.rootElement;
		grammar.defineProduction(null, "RootElement",
				GrammarFactory.newNonTerminal(null, grammar, rootElementName));
	}

	final void defPreEntity() {
		Expression[] keywords = {
				GrammarFactory.newString(null, "lt"),
				GrammarFactory.newString(null, "gt"),
				GrammarFactory.newString(null, "amp"),
				GrammarFactory.newString(null, "apos"),
				GrammarFactory.newString(null, "quot"),
				grammar.newRepetition1(GrammarFactory.newCharSet(null, "#a-zA-Z0-9"))
		};
		grammar.defineProduction(null, "PreEntity", grammar.newChoice(keywords));
	}
}
