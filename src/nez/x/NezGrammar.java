package nez.x;

/* Parsing Expression Grammars for Nez */
import nez.ParserCombinator;
import nez.lang.Expression;

public class NezGrammar extends ParserCombinator {
//	public NezGrammar() {
//		super();
//	}
   public Expression pFile() {
      return Sequence(NCapture(0), ZeroMore(P("S")), ZeroMore(Link(P("Statement"))), Tagging("List"), Capture(0));
   }
   public Expression pS() {
      return Choice(c(9, 10), t("'\\r'"), t(" "), Sequence(t("　")));
   }
   public Expression pStatement() {
      return Sequence((Choice(P("Document"), P("Example"), P("Import"), P("Format"), P("Production"))), P("_"), Option(Sequence(t(";"), P("_"))));
   }
   public Expression pDocument() {
      return Sequence(NCapture(0), P("COMMENT"), ZeroMore(Sequence(ZeroMore(P("S")), P("COMMENT"))), Tagging("Comment"), Capture(0));
   }
   public Expression pCOMMENT() {
      return Choice(Sequence(t("/*"), ZeroMore(Sequence(Not(Sequence(t("*/"))), AnyChar())), t("*/")), Sequence(t("//"), ZeroMore(Sequence(Not(P("EOL")), AnyChar())), P("EOL")));
   }
   public Expression pEOL() {
      return Choice(t("'\\n'"), Sequence(t("'\\r'"), Option(t("'\\n'"))), P("EOT"));
   }
   public Expression pEOT() {
      return Not(AnyChar());
   }
   public Expression pExample() {
      return Sequence(NCapture(0), t("example"), OneMore(P("S")), (Choice(Sequence(t("!"), Tagging("Rebut")), Tagging("Example"))), Link(P("NonTerminal")), OneMore(P("S")), P("addInputText"), Capture(0));
   }
   public Expression pNonTerminal() {
      return Sequence(NCapture(0), P("NAME"), Option(Sequence(t("."), P("NAME"))), Tagging("NonTerminal"), Capture(0));
   }
   public Expression pNAME() {
      return Sequence(Not(P("KEYWORD")), P("LETTER"), ZeroMore(P("W")));
   }
   public Expression pKEYWORD() {
      return Sequence((Choice(Sequence(t("public")), Sequence(t("inline")), Sequence(t("import")), Sequence(t("type")), Sequence(t("grammar")), Sequence(t("example")), Sequence(t("format")), Sequence(t("define")))), Not(P("W")));
   }
   public Expression pW() {
      return Choice(c(48, 49, 50, 51, 52, 53, 54, 55, 56, 57), c(65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90), t("_"), c(97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122));
   }
   public Expression pLETTER() {
      return Choice(c(65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90), t("_"), c(97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122));
   }
   public Expression paddInputText() {
      return Choice(Sequence(t("'''"), P("EOL"), Link(Sequence(NCapture(0), ZeroMore(Sequence(Not(Sequence(t("\n'''"))), AnyChar())), Capture(0))), t("\n'''")), Sequence(t("```"), P("EOL"), Link(Sequence(NCapture(0), ZeroMore(Sequence(Not(Sequence(t("\n```"))), AnyChar())), Capture(0))), t("\n```")), Sequence(t("\""), P("EOL"), Link(Sequence(NCapture(0), ZeroMore(Sequence(Not(Sequence(t("\n\"\"\""))), AnyChar())), Capture(0))), t("\n\"\"\"")), Sequence(Link(Sequence(NCapture(0), ZeroMore(Sequence(Not(P("EOL")), AnyChar())), Capture(0))), P("EOL")));
   }
   public Expression pImport() {
      return Sequence(NCapture(0), t("import"), OneMore(P("S")), Link(P("ImportName")), OneMore(P("S")), t("from"), OneMore(P("S")), Link(Choice(P("Character"), P("String"))), Tagging("Import"), Capture(0));
   }
   public Expression pImportName() {
      return Sequence(NCapture(0), (Choice(t("*"), Sequence(P("NAME"), Option(Sequence(t("."), (Choice(t("*"), P("NAME")))))))), Tagging("Name"), Capture(0));
   }
   public Expression pCharacter() {
      return Sequence(t("'\\''"), NCapture(0), ZeroMore(Choice(Sequence(t("\\'")), Sequence(t("\\\\")), Sequence(Not(t("'\\''")), AnyChar()))), Tagging("Character"), Capture(0), t("'\\''"));
   }
   public Expression pString() {
      return Sequence(t("\34"), NCapture(0), ZeroMore(Choice(Sequence(t("\\\"")), Sequence(t("\\\\")), Sequence(Not(t("\34")), AnyChar()))), Tagging("String"), Capture(0), t("\34"));
   }
   public Expression pFormat() {
      return Sequence(NCapture(0), t("format"), OneMore(P("S")), t("#"), Link(P("Name")), t("["), P("_"), Link(P("FormatSize")), P("_"), t("]"), P("_"), t("`"), Link(P("Formatter")), t("`"), Tagging("Format"), Capture(0));
   }
   public Expression pName() {
      return Sequence(NCapture(0), P("NAME"), Tagging("Name"), Capture(0));
   }
   public Expression p_() {
      return ZeroMore(Choice(P("S"), P("COMMENT")));
   }
   public Expression pFormatSize() {
      return Sequence(NCapture(0), (Choice(t("*"), P("INT"))), Tagging("Integer"), Capture(0));
   }
   public Expression pINT() {
      return Sequence(P("DIGIT"), ZeroMore(P("DIGIT")));
   }
   public Expression pDIGIT() {
      return c(48, 49, 50, 51, 52, 53, 54, 55, 56, 57);
   }
   public Expression pFormatter() {
      return Sequence(NCapture(0), Tagging("List"), ZeroMore(Sequence(Not(t("`")), Link(Choice(Sequence(t("${"), P("Name"), t("}")), Sequence(t("$["), P("_"), P("Index"), P("_"), Option(Sequence(LCapture(0), t("`"), Link(P("Formatter")), t("`"), P("_"), Link(P("Index")), P("_"), Tagging("Format"), Capture(0))), t("]")), Sequence(NCapture(0), (Choice(Sequence(t("$$"), Replace("$")), Sequence(t("\\`"), Replace("\\`")), OneMore(Sequence(Not(Choice(Sequence(t("$$")), Sequence(t("${")), Sequence(t("$[")), Sequence(t("\\`")), t("`"))), AnyChar())))), Capture(0)))))), Capture(0));
   }
   public Expression pIndex() {
      return Sequence(NCapture(0), Option(t("-")), P("INT"), Tagging("Integer"), Capture(0));
   }
   public Expression pProduction() {
      return Sequence(NCapture(0), P("addQualifers"), Link(0, Choice(P("Name"), P("String"))), P("_"), P("SKIP"), t("="), P("_"), Link(1, P("Expression")), Tagging("Production"), Capture(0));
   }
   public Expression paddQualifers() {
      return Option(Sequence(And(P("QUALIFERS")), Link(2, P("Qualifers"))));
   }
   public Expression pQUALIFERS() {
      return Sequence((Choice(Sequence(t("public")), Sequence(t("inline")))), Not(P("W")));
   }
   public Expression pQualifers() {
      return Sequence(NCapture(0), ZeroMore(Sequence(Link(Sequence(NCapture(0), P("QUALIFERS"), Capture(0))), P("S"))), Capture(0));
   }
   public Expression pSKIP() {
      return ZeroMore(P("ANNOTATION"));
   }
   public Expression pANNOTATION() {
      return Sequence(t("["), P("DOC"), t("]"), P("_"));
   }
   public Expression pDOC() {
      return Sequence(ZeroMore(Sequence(Not(Choice(t("]"), t("["))), AnyChar())), Option(Sequence(t("["), P("DOC"), t("]"), P("DOC"))));
   }
   public Expression pExpression() {
      return Sequence(P("Sequence"), Option(Sequence(LCapture(0), OneMore(Sequence(P("_"), t("/"), P("_"), Link(P("Sequence")))), Tagging("Choice"), Capture(0))));
   }
   public Expression pSequence() {
      return Sequence(P("Prefix"), Option(Sequence(LCapture(0), OneMore(Sequence(P("_"), P("NOTRULE"), Link(P("Prefix")))), Tagging("Sequence"), Capture(0))));
   }
   public Expression pPrefix() {
      return Choice(Sequence(NCapture(0), (Choice(Sequence(t("&"), Tagging("And")), Sequence(t("!"), Tagging("Not")), Sequence(t("@["), P("_"), Link(1, P("Integer")), P("_"), t("]"), Tagging("Link")), Sequence(t("@"), Tagging("Link")), Sequence(t("~"), Tagging("Match")))), Link(0, P("Suffix")), Capture(0)), P("Suffix"));
   }
   public Expression pInteger() {
      return Sequence(NCapture(0), P("INT"), Tagging("Integer"), Capture(0));
   }
   public Expression pSuffix() {
      return Sequence(P("Primary"), Option(Sequence(LCapture(0), (Choice(Sequence(t("*"), Option(Link(1, P("Integer"))), Tagging("Repetition")), Sequence(t("+"), Tagging("Repetition1")), Sequence(t("?"), Tagging("Option")))), Capture(0))));
   }
   public Expression pPrimary() {
      return Choice(P("Character"), P("Charset"), Sequence(NCapture(0), t("."), Tagging("Any"), Capture(0)), Sequence(NCapture(0), t("0x"), P("HEX"), P("HEX"), Tagging("Byte"), Capture(0)), Sequence(NCapture(0), t("U+"), P("HEX"), P("HEX"), P("HEX"), P("HEX"), Tagging("Byte"), Capture(0)), Sequence(t("("), P("_"), P("Expression"), P("_"), t(")")), P("Constructor"), P("Replace"), P("Tagging"), P("String"), P("Extension"), P("NonTerminal"));
   }
   public Expression pCharset() {
      return Sequence(t("["), NCapture(0), ZeroMore(Link(Sequence(NCapture(0), P("CHAR"), Tagging("Class"), Capture(0), Option(Sequence(LCapture(0), t("-"), Link(Sequence(NCapture(0), P("CHAR"), Tagging("Class"), Capture(0))), Tagging("List"), Capture(0)))))), Tagging("Class"), Capture(0), t("]"));
   }
   public Expression pCHAR() {
      return Choice(Sequence(t("\\u"), P("HEX"), P("HEX"), P("HEX"), P("HEX")), Sequence(t("\\x"), P("HEX"), P("HEX")), Sequence(t("\\n")), Sequence(t("\\t")), Sequence(t("\\\\")), Sequence(t("\\r")), Sequence(t("\\v")), Sequence(t("\\f")), Sequence(t("\\-")), Sequence(t("\\]")), Sequence(Not(t("]")), AnyChar()));
   }
   public Expression pHEX() {
      return Choice(c(48, 49, 50, 51, 52, 53, 54, 55, 56, 57), c(65, 66, 67, 68, 69, 70), c(97, 98, 99, 100, 101, 102));
   }
   public Expression pConstructor() {
      return Sequence(NCapture(0), t("{"), (Choice(Sequence(t("@"), P("S"), Tagging("LeftNew")), Tagging("New"))), P("_"), Option(Sequence(Link(P("Expression")), P("_"))), t("}"), Capture(0));
   }
   public Expression pReplace() {
      return Sequence(t("`"), NCapture(0), ZeroMore(Choice(Sequence(t("\\`")), Sequence(t("\\\\")), Sequence(Not(t("`")), AnyChar()))), Tagging("Replace"), Capture(0), t("`"));
   }
   public Expression pTagging() {
      return Sequence(t("#"), NCapture(0), (Choice(c(48, 49, 50, 51, 52, 53, 54, 55, 56, 57), c(65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90), c(97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122))), ZeroMore(Choice(t("."), c(48, 49, 50, 51, 52, 53, 54, 55, 56, 57), c(65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90), t("_"), c(97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122))), Tagging("Tagging"), Capture(0));
   }
   public Expression pExtension() {
      return Sequence(t("<"), NCapture(0), P("addExtension"), Capture(0), t(">"));
   }
   public Expression paddExtension() {
      return Choice(Sequence(t("if"), P("S"), Link(P("FlagName")), Tagging("If")), Sequence(t("on"), P("S"), Link(P("FlagName")), P("S"), Link(P("Expression")), Tagging("On")), Sequence(t("block"), P("S"), Link(P("Expression")), Tagging("Block")), Sequence(t("def"), P("S"), Link(P("TableName")), P("S"), Link(P("Expression")), Tagging("Def")), Sequence(t("is"), P("S"), Link(P("TableName")), Tagging("Is")), Sequence(t("isa"), P("S"), Link(P("TableName")), Tagging("Isa")), Sequence(t("exists"), P("S"), Link(P("TableName")), Tagging("Exists")), Sequence(t("local"), P("S"), Link(P("TableName")), P("S"), Link(P("Expression")), Tagging("Local")), Sequence(t("x"), P("S"), Link(P("NonTerminal")), P("S"), Link(P("NonTerminal")), P("S"), Link(P("Expression")), P("S"), ZeroMore(Sequence(t(","), P("S"), Link(P("NonTerminal")), P("S"), Link(P("Expression")), P("S"))), Tagging("Expand")), Sequence(OneMore(Sequence(Not(t(">")), AnyChar())), Tagging("Undefined")));
   }
   public Expression pFlagName() {
      return Sequence(NCapture(0), Option(t("!")), P("LETTER"), ZeroMore(P("W")), Tagging("Name"), Capture(0));
   }
   public Expression pTableName() {
      return Sequence(NCapture(0), P("LETTER"), ZeroMore(P("W")), Tagging("Name"), Capture(0));
   }
   public Expression pNOTRULE() {
      return Not(Choice(t(";"), P("RuleHead"), P("Import")));
   }
   public Expression pRuleHead() {
      return Sequence(NCapture(0), P("addQualifers"), Link(0, Choice(P("Name"), P("String"))), P("_"), P("SKIP"), t("="), Capture(0));
   }
}
