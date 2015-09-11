package nez.lang;

import nez.NezOption;
import nez.Parser;
import nez.ParserCombinator;

public class NezGrammar2 extends ParserCombinator {

	private static GrammarFile file = null;

	public final static GrammarFile newGrammarFile() {
		if (file == null) {
			file = new NezGrammar2().load();
		}
		return file;
	}

	public final static Parser newParser(String name, NezOption option) {
		return newGrammarFile().newParser(name, option);
	}

	public Expression pEOT() {
		return Not(AnyChar());
	}

	public Expression pEOL() {
		return Choice(t("\n"), Sequence(t("\r"), Option("\n")), P("EOT"));
	}

	public Expression pS() {
		return Choice(c(" \\t\\r\\n"), t("\u3000"));
	}

	public Expression pDIGIT() {
		return c("0-9");
	}

	public Expression pLETTER() {
		return c("A-Za-z_");
	}

	public Expression pHEX() {
		return c("0-9A-Fa-f");
	}

	public Expression pW() {
		return c("A-Za-z0-9_");
	}

	public Expression pINT() {
		return Sequence(P("DIGIT"), ZeroMore(P("DIGIT")));
	}

	public Expression pKEYWORD() {
		return Sequence(Choice(t("public"), t("inline"), t("import"), t("type"), t("grammar"), t("example"), t("format"), t("define")), Not(P("W")));
	}

	public Expression pNAME() {
		return Sequence(Not(P("KEYWORD")), P("LETTER"), ZeroMore(P("W")));
	}

	public Expression pCOMMENT() {
		return Choice(Sequence(t("/*"), ZeroMore(Not(t("*/")), AnyChar()), t("*/")), Sequence(t("//"), ZeroMore(Not(P("EOL")), AnyChar()), P("EOL")));
	}

	public Expression p_() {
		return ZeroMore(Choice(P("S"), P("COMMENT")));
	}

	public Expression pInteger() {
		return New(P("INT"), Tag("Integer"));
	}

	public Expression pName() {
		return New(P("NAME"), Tag("Name"));
	}

	public Expression pFile() {
		return New(P("_"), ZeroMore(Link(null, "Chunk")), Tag("Source"));
	}

	public Expression pChunk() {
		return Sequence(P("_"), Choice(P("Import"), P("Example"), P("Format"), P("Production")), P("_"), Option(t(";"), P("_")));
	}

	// import
	// import x.Xml from "hoge"

	public Expression pImport() {
		return New(t("import"), P("S"), Link("name", "ImportName"), P("S"), t("from"), P("S"), Link("from", Choice(P("Character"), P("String"))), Tag("Import"));
	}

	public Expression pImportName() {
		return New(Choice(t("*"), Sequence(P("NAME"), Option(t("."), Choice(t("*"), P("NAME"))))), Tag("Name"));
	}

	public Expression pExample() {
		return New(
				t("example"),
				P("S"),
				Choice(Sequence(t("!"), Tag("Rebuttal")), Tag("Example")),
				Link("name", "NonTerminal"),
				Option(t("&"), Link("name2", "NonTerminal")),
				ZeroMore(c(" \t")),
				Choice(Sequence(t("'''"), P("EOL"), Link("text", New(ZeroMore(NotAny("\n'''")))), P("EOL"), t("'''")), Sequence(t("```"), P("EOL"), Link("text", New(ZeroMore(NotAny("\n```")))), P("EOL"), t("```")),
						Sequence(t("\"\"\""), P("EOL"), Link("text", New(ZeroMore(NotAny("\n\"\"\"")))), P("EOL"), t("\"\"\"")), Sequence(Link("text", New(ZeroMore(NotAny(P("EOL"))))), P("EOL"))));
	}

	public Expression pIndex() {
		return New(Option("-"), P("INT"), Tag("Integer"));
	}

	public Expression pFormat() {
		return New(t("format"), Tag("Format"), P("_"), t("#"), Link("name", "Name"), t("["), P("_"), Link("size", "FormatSize"), P("_"), t("]"), P("_"), t("`"), Link("format", "Formatter"), t("`"));
	}

	public Expression pFormatter() {
		return New(
				Tag("List"),
				ZeroMore(
						Not("`"),
						Link(null,
								Choice(Sequence(t("${"), P("Name"), t("}")), Sequence(t("$["), P("_"), P("Index"), P("_"), LeftFoldOption("left", t('`'), Link("format", "Formatter"), t('`'), P("_"), Link("right", "Index"), P("_"), Tag("Format")), t("]")),
										New(Choice(Sequence(t("$$"), Replace('$')), Sequence(t("\\`"), Replace('`')), OneMore(Not("$$"), Not("${"), Not("$["), Not("\\`"), Not("`"), AnyChar())))))));
	}

	public Expression pFormatSize() {
		return New(Choice(t('*'), P("INT")), Tag("Integer"));
	}

	/* Production */

	public Expression pProduction() {
		Expression Anno = Option(Link("anno", P("Qualifers")));
		Expression Name = Link("name", Choice(P("NonTerminal"), P("String")));
		Expression Type = Option(Link("type", P("Tagging")), P("_"));
		Expression Expr = Link("expr", "Expression");
		return New(Anno, Name, P("_"), t("="), P("_"), Type, Expr, Tag("Production"));
	}

	public Expression pQualifers() {
		return New(ZeroMore(Link(null, New(P("QUALIFERS"))), P("S")));
	}

	public Expression pQUALIFERS() {
		Expression At = Sequence(t('@'), P("NAME"));
		return Choice(/* At, */Sequence(t("public"), Not(P("W"))), Sequence(t("inline"), Not(P("W"))));
	}

	public Expression pNOTRULE() {
		return Not(Choice(t(";"), P("RuleHead"), P("Import")));
	}

	public Expression pRuleHead() {
		Expression Anno = Option(Link("anno", P("Qualifers")));
		Expression Name = Link("name", Choice(P("NonTerminal"), P("String")));
		Expression Type = Option(Link("type", P("Tagging")), P("_"));
		return New(Anno, Name, P("_"), Type, t("="));
	}

	public Expression pExpression() {
		return Sequence(P("Sequence"), LeftFoldOption(null, OneMore(P("_"), t("/"), P("_"), Link(null, "Sequence")), Tag("Choice")));
	}

	public Expression pSequence() {
		return Sequence(P("Predicate"), LeftFoldOption(null, OneMore(P("_"), P("NOTRULE"), Link(null, "Predicate")), Tag("Sequence")));
	}

	public Expression pPredicate() {
		Expression And = Sequence(t("&"), Tag("And"));
		Expression Not = Sequence(t("!"), Tag("Not"));
		Expression Match = Sequence(t("~"), Tag("Match"));
		return Choice(New(Choice(And, Not, Match), Link("expr", P("Suffix"))), P("Suffix"));
	}

	public Expression pSuffix() {
		Expression _Zero = Sequence(t("*"), Option(Link("times", P("Integer"))), Tag("Repetition"));
		Expression _One = Sequence(t("+"), Tag("Repetition1"));
		Expression _Option = Sequence(t("?"), Tag("Option"));
		return Sequence(P("Term"), LeftFoldOption("expr", Choice(_Zero, _One, _Option)));
	}

	public Expression pTerm() {
		Expression _Any = New(t("."), Tag("AnyChar"));
		Expression _Byte = New(t("0x"), P("HEX"), P("HEX"), Tag("ByteChar"));
		Expression _Unicode = New(t("U+"), P("HEX"), P("HEX"), P("HEX"), P("HEX"), Tag("ByteChar"));
		Expression _Inner = Sequence(t("("), P("_"), P("Expression"), P("_"), t(")"));
		return Choice(P("Character"), P("Charset"), P("String"), _Any, _Byte, _Unicode, P("Constructor"), P("LabelLink"), P("Replace"), P("Tagging"), _Inner, P("Func"), P("NonTerminal"));
	}

	public Expression pCharacter() {
		Expression StringContent = ZeroMore(Choice(t("\\'"), t("\\\\"), Sequence(Not(t("'")), AnyChar())));
		return Sequence(t("'"), New(StringContent, Tag("Character")), t("'"));
	}

	public Expression pString() {
		Expression StringContent = ZeroMore(Choice(t("\\\""), t("\\\\"), Sequence(Not(t("\"")), AnyChar())));
		return Sequence(t("\""), New(StringContent, Tag("String")), t("\""));
	}

	public Expression pCharset() {
		Expression _CharChunk = Sequence(New(P("CHAR"), Tag("Class")), LeftFoldOption("right", t("-"), Link("left", New(P("CHAR"), Tag("Class"))), Tag("List")));
		return Sequence(t("["), New(ZeroMore(Link(null, _CharChunk)), Tag("Class")), t("]"));
	}

	public Expression pCHAR() {
		return Choice(Sequence(t("\\u"), P("HEX"), P("HEX"), P("HEX"), P("HEX")), Sequence(t("\\x"), P("HEX"), P("HEX")), t("\\n"), t("\\t"), t("\\\\"), t("\\r"), t("\\v"), t("\\f"), t("\\-"), t("\\]"), Sequence(Not(t("]")), AnyChar()));
	}

	public Expression pConstructor() {
		return New(t("{"), Choice(Sequence(t("$"), Option(Link("name", "Name")), P("S"), Tag("LeftFold")), Tag("New")), P("_"), Option(Link("expr", "Expression"), P("_")), t("}"));
	}

	public Expression pTagging() {
		Expression Tag = New(ZeroMore(c("A-Za-z0-9_.")), Tag("Tagging"));
		return Sequence(Choice(t('#'), t(':')), Tag);
	}

	public Expression pReplace() {
		Expression ValueContent = ZeroMore(Choice(t("\\`"), t("\\\\"), Sequence(Not(t("`")), AnyChar())));
		return Sequence(t("`"), New(ValueContent, Tag("Replace")), t("`"));
	}

	public Expression pLabelLink() {
		return Sequence(t('$'), New(Option(Link("name", "Name")), t("("), P("_"), Link("expr", "Expression"), P("_"), t(")"), Tag("Link")));
	}

	public Expression pFunc() {
		Expression _If = Sequence(t("if"), P("S"), Link("name", "FlagName"), Tag("If"));
		Expression _On = Sequence(t("on"), P("S"), Link("name", "FlagName"), P("S"), Link("expr", "Expression"), Tag("On"));
		Expression _Def = Sequence(t("def"), P("S"), Link("name", "TableName"), P("S"), Link("expr", "Expression"), Tag("Def"));
		Expression _Exists = Sequence(t("exists"), P("S"), Link("name", "TableName"), Option(P("S"), Link("symbol", "Character")), Tag("Exists"));
		Expression _Match = Sequence(t("match"), P("S"), Link("name", "TableName"), Tag("Match"));
		Expression _Is = Sequence(t("is"), P("S"), Link("name", "TableName"), Tag("Is"));
		Expression _Isa = Sequence(t("isa"), P("S"), Link("name", "TableName"), Tag("Isa"));
		Expression _Block = Sequence(t("block"), P("S"), Link("expr", "Expression"), Tag("Block"));
		Expression _Local = Sequence(t("local"), P("S"), Link("name", "TableName"), P("S"), Link("expr", "Expression"), Tag("Local"));
		// Expression _Uniq = ;
		// Expression _Set = ;
		Expression _Undefined = Sequence(OneMore(Not(">"), AnyChar()), Tag("Undefined"));
		return Sequence(t("<"), New(Choice(_If, _On, _Def, _Exists, _Match, _Is, _Isa, _Block, _Local, _Undefined)), P("_"), t(">"));
	}

	public Expression pFlagName() {
		return New(Option("!"), P("LETTER"), ZeroMore(P("W")), Tag("Name"));
	}

	public Expression pTableName() {
		return New(P("LETTER"), ZeroMore(P("W")), Tag("Name"));
	}

	public Expression pNonTerminal() {
		return New(P("NAME"), Option(t('.'), P("NAME")), Tag("NonTerminal"));
	}

}
