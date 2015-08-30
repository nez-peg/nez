package nez.lang;

import nez.NezOption;
import nez.ParserCombinator;

public class NezCombinator extends ParserCombinator {

	private static GrammarFile ns = null;
	
	public final static GrammarFile newGrammarFile() {
		if(ns == null) {
			ns = new NezCombinator().load();
		}
		return ns;
	}
	
	public final static Grammar newGrammar(String name, NezOption option) {
		return newGrammarFile().newGrammar(name, option);
	}
	
	public Expression EOT() {
		return Not(AnyChar());
	}

	public Expression EOL() {
		return Choice(t("\n"), Sequence(t("\r"), Option("\n")), P("EOT"));
	}

	public Expression S() {
		return Choice(c(" \\t\\r\\n"), t("\u3000"));
	}

	public Expression DIGIT() {
		return c("0-9");
	}

	public Expression LETTER() {
		return c("A-Za-z_");
	}

	public Expression HEX() {
		return c("0-9A-Fa-f");
	}

	public Expression W() {
		return c("A-Za-z0-9_");
	}

	public Expression INT() {
		return Sequence(P("DIGIT"), ZeroMore(P("DIGIT")));
	}
	
	public Expression KEYWORD() {
		return Sequence(
			Choice(t("public"), t("inline"), 
				t("import"), t("type"), t("grammar"), 
				t("example"), t("format"), t("define")),
			Not(P("W"))
		);
	}

	public Expression NAME() {
		return Sequence(Not(P("KEYWORD")), P("LETTER"), ZeroMore(P("W")));
	}

	public Expression COMMENT() {
		return Choice(
			Sequence(t("/*"), ZeroMore(Not(t("*/")), AnyChar()), t("*/")),
			Sequence(t("//"), ZeroMore(Not(P("EOL")), AnyChar()), P("EOL"))
		);
	}

	public Expression p_() {
		return ZeroMore(Choice(P("S"), P("COMMENT")));
	}
	
	public Expression Integer() {
		return New(P("INT"), Tag(NezTag.Integer));
	}

	public Expression Name() {
		return New(P("NAME"), Tag(NezTag.Name));
	}

	public Expression String() {
		Expression StringContent  = ZeroMore(Choice(
			t("\\\""), t("\\\\"), Sequence(Not(t("\"")), AnyChar())
		));
		return Sequence(t("\""), New(StringContent, Tag(NezTag.String)), t("\""));
	}

	public Expression SingleQuotedString() {
		Expression StringContent  = ZeroMore(Choice(
			t("\\'"), t("\\\\"), Sequence(Not(t("'")), AnyChar())
		));
		return Sequence(t("'"),  New(StringContent, Tag(NezTag.Character)), t("'"));
	}

	public Expression ValueReplacement() {
		Expression ValueContent = ZeroMore(Choice(
			t("\\`"), t("\\\\"), Sequence(Not(t("`")), AnyChar())
		));
		return Sequence(t("`"), New(ValueContent, Tag(NezTag.Replace)), t("`"));
	}

	public Expression NonTerminal() {
		return New(P("NAME"), Option(t('.'), P("NAME")), Tag(NezTag.NonTerminal));
	}

	public Expression CHAR() {
		return Choice( 
			Sequence(t("\\u"), P("HEX"), P("HEX"), P("HEX"), P("HEX")),
			Sequence(t("\\x"), P("HEX"), P("HEX")),
			t("\\n"), t("\\t"), t("\\\\"), t("\\r"), t("\\v"), t("\\f"), t("\\-"), t("\\]"), 
			Sequence(Not(t("]")), AnyChar())
		);
	}

	public Expression Charset() {
		Expression _CharChunk = Sequence(
			New(P("CHAR"), Tag(NezTag.Class)), 
			LeftFoldOption("right", t("-"), Link("left", New(P("CHAR"), Tag(NezTag.Class))), Tag(NezTag.List))
		);
		return Sequence(t("["), New(ZeroMore(Link(null, _CharChunk)), Tag(NezTag.Class)), t("]"));
	}

	public Expression Constructor() {
		return New(
			t("{"), 
			Choice(
				Sequence(t("$"), Option(Link("name", "Name")), P("S"), Tag(NezTag.LeftNew)), 
				Sequence(t("@"), P("S"), Tag(NezTag.LeftNew)), 
				Tag(NezTag.New)
			), 
			P("_"), 
			Option(Link("expr", "Expr"), P("_")),
			t("}")
		);
	}
	
	public Expression FlagName() {
		return New(Option("!"), P("LETTER"), ZeroMore(P("W")), Tag(NezTag.Name));
	}

	public Expression TableName() {
		return New(P("LETTER"), ZeroMore(P("W")), Tag(NezTag.Name));
	}

	public Expression Func() {
		return Sequence(t("<"), 
			New(Choice(
			Sequence(t("if"), P("S"), Link("name", "FlagName"), Tag(NezTag.If)),
			Sequence(t("on"), P("S"), Link("name", "FlagName"), P("S"), Link("expr", "Expr"), Tag(NezTag.On)),
			
			Sequence(t("block"), P("S"), Link("expr", "Expr"), Tag(NezTag.Block)),
			Sequence(t("def"),   P("S"), Link("name", "TableName"), P("S"), Link("expr", "Expr"), Tag(NezTag.Def)),
			Sequence(t("match"), P("S"), Link("name", "TableName"), Tag(NezTag.Match)),
			Sequence(t("is"),    P("S"), Link("name", "TableName"), Tag(NezTag.Is)),
			Sequence(t("isa"),   P("S"), Link("name", "TableName"), Tag(NezTag.Isa)),
			Sequence(t("exists"), P("S"), Link("name", "TableName"), Option(P("S"), Link("symbol", "SingleQuotedString")), Tag(NezTag.Exists)),
			Sequence(t("local"), P("S"), Link("name", "TableName"), P("S"), Link("expr", "Expr"), Tag(NezTag.Local)),

//			Sequence(t("x"),  P("S"), Link("NonTerminal"), P("S"), Link("NonTerminal"), P("S"), Link("Expr"), P("S"), 
//					ZeroMore(t(","), P("S"), Link("NonTerminal"), P("S"), Link("Expr"), P("S")),
//					t(">"), Tag(NezTag.Match)),

			Sequence(t("scan"), P("S"), Link("name", "TableName"), P("S"), Option(Link("mask", "Integer"), P("S")), Link("expr", "Expr"), Tag(NezTag.Scan)),
			Sequence(t("repeat"), P("S"), Link("name", "TableName"), P("S"), Link("expr", "Expr"), Tag(NezTag.Repeat)),
			/* Deprecated */
			Sequence(t("with"),  P("S"), Link("name", P("Name")), P("S"), Link("expr", "Expr"), Tag(NezTag.With)),
			Sequence(t("without"), P("S"), Link("name", P("Name")), P("S"), Link("expr", "Expr"), Tag(NezTag.Without)),
			Sequence(t("indent"), Tag(NezTag.Indent)),
			Sequence(OneMore(Not(">"), AnyChar()), Tag(NezTag.Undefined))
			)), P("_"), t(">")
		);
	}
	
	public Expression LabelLink() {
		return Sequence(t('$'), New(
			Option(Link("name", "Name")), t("("), P("_"), Link("expr", "Expr"), P("_"), t(")"), Tag(NezTag.Link)
		));
	}

	public Expression Term() {
		Expression _Any = New(t("."), Tag(NezTag.AnyChar));
		Expression _Tagging = Sequence(t("#"), New(c("A-Za-z0-9"), ZeroMore(c("A-Za-z0-9_.")), Tag(NezTag.Tagging)));
		Expression _Byte = New(t("0x"), P("HEX"), P("HEX"), Tag(NezTag.ByteChar));
		Expression _Unicode = New(t("U+"), P("HEX"), P("HEX"), P("HEX"), P("HEX"), Tag(NezTag.ByteChar));
		return Choice(
			P("SingleQuotedString"), 
			P("Charset"), 
			P("Func"),  
			P("LabelLink"),
			_Any, 
			P("ValueReplacement"), 
			_Tagging, 
			_Byte, 
			_Unicode,
			Sequence(t("("), P("_"), P("Expr"), P("_"), t(")")),
			P("Constructor"), 
			P("String"), 
			P("NonTerminal") 
		);
	}
	
	public Expression SuffixTerm() {
		return Sequence(
			P("Term"), 
			LeftFoldOption("expr", 
				Choice(
					Sequence(t("*"), Option(Link("times", P("Integer"))), Tag(NezTag.Repetition)), 
					Sequence(t("+"), Tag(NezTag.Repetition1)), 
					Sequence(t("?"), Tag(NezTag.Option))
					)
				)
			);
	}
	
	public Expression Predicate() {
		return Choice(
			New(
				Choice(
					Sequence(t("&"), Tag(NezTag.And)),
					Sequence(t("!"), Tag(NezTag.Not)),
					Sequence(t("@["), P("_"), Link("index", P("Integer")), P("_"), t("]"), Tag(NezTag.Link)),							
					Sequence(t("@"), Tag(NezTag.Link)),
					Sequence(t("~"), Tag(NezTag.Match))
				), 
				Link("expr", P("SuffixTerm"))
			), 
			P("SuffixTerm")
		);
	}
	
	public Expression NOTRULE() {
		return Not(Choice(t(";"), P("RuleHead"), P("Import")));
	}

	public Expression Sequence() {
		return Sequence(
			P("Predicate"), 
			LeftFoldOption(null, 
				OneMore(
				P("_"), 
				P("NOTRULE"),
				Link(null, "Predicate")
				),
				Tag(NezTag.Sequence) 
			)
		);
	}

	public Expression Expr() {
		return Sequence(
			P("Sequence"), 
			LeftFoldOption(null, 
				OneMore(
				P("_"), t("/"), P("_"), 
				Link(null, "Sequence")
				),
				Tag(NezTag.Choice) 
			)
		);
	}
		
	public Expression RuleHead() {
		return New(
			P("addQualifers"), 
			Link(null, Choice(P("NonTerminal"), P("String"))), 
			P("_"), 
			P("SKIP"), t("=")  
		);
	}

	public Expression Rule() {
		return New(
			P("addQualifers"), 
			Link("name", Choice(P("NonTerminal"), P("String"))), P("_"), 
			P("SKIP"),
			t("="), P("_"), 
			Link("expr", "Expr"),
			Tag(NezTag.Rule) 
		);
	}

	public Expression addQualifers() {
		return Option(And(P("QUALIFERS")), Link("anno", P("Qualifers")));
	}

	public Expression QUALIFERS() {
		return Sequence(Choice(t("public"), t("inline")), Not(P("W")));
	}

	public Expression Qualifers() {
		return New(ZeroMore(Link(null, New(P("QUALIFERS"))), P("S")));
	}


	public Expression DOC() {
		return Sequence(
			ZeroMore(Not(t("]")), Not(t("[")), AnyChar()),
			Option(Sequence(t("["), P("DOC"), t("]"), P("DOC") ))
		);
	}
	
	public Expression ANNOTATION() {
		return Sequence(t("["), P("DOC"), t("]"), P("_"));
	}

	public Expression SKIP() {
		return ZeroMore(P("ANNOTATION"));
	}

	public Expression ImportName() {
		return New(Choice(
			t("*"), 
			Sequence(P("NAME"), Option(t("."), Choice(t("*"), P("NAME"))))
		), Tag(NezTag.NonTerminal));
	}

	// import 
	// import x.Xml
	public Expression Import() {
		return New(
			t("import"), P("S"), Link("name", "ImportName"),
			P("S"), t("from"), P("S"), 
			Link("from", Choice(P("SingleQuotedString"), P("String"))), 
			Tag(NezTag.Import)
		);
	}
	
	public Expression Example() {
		return New(
			t("example"), 
			P("S"), 
			Choice(Sequence(t("!"), Tag(NezTag.Rebuttal)), Tag(NezTag.Example)),
			Tag(NezTag.Example), Link("name", "NonTerminal"), 
			Option(t("&"), Link("name2", "NonTerminal")),
			ZeroMore(c(" \t")), 
			Choice(
				Sequence(t("'''"), P("EOL"), Link("text", New(ZeroMore(NotAny("\n'''")))), P("EOL"), t("'''")),
				Sequence(t("```"), P("EOL"), Link("text", New(ZeroMore(NotAny("\n```")))), P("EOL"), t("```")),
				Sequence(t("\"\"\""), P("EOL"), Link("text", New(ZeroMore(NotAny("\n\"\"\"")))), P("EOL"), t("\"\"\"")),
				Sequence(Link("text", New(ZeroMore(NotAny(P("EOL"))))), P("EOL"))
			)
		);
	}
	
	public Expression Index() {
		return New(Option("-"), P("INT"), Tag(NezTag.Integer));
	}

	public Expression Formatter() {
		return New(
				Tag(NezTag.List), 
				ZeroMore( Not("`"), Link(null, Choice(
					Sequence(t("${"), P("Name"), t("}")),
					Sequence(t("$["), P("_"), P("Index"), P("_"), 
						LeftFoldOption("left", t('`'), Link("format", "Formatter"), t('`'), P("_"), Link("right", "Index"), P("_"), Tag(NezTag.Format)), t("]")),
					New(Choice(
						Sequence(t("$$"), Replace('$')), 
						Sequence(t("\\`"), Replace('`')),
						OneMore(Not("$$"), Not("${"), Not("$["), Not("\\`"), Not("`"), AnyChar())
					))
				)))
		);
	}

	public Expression FormatSize() {
		return New(Choice(t('*'), P("INT")), Tag(NezTag.Integer));
	}

	public Expression Format() {
		return New(
				t("format"), Tag(NezTag.Format), 
				P("_"), t("#"), Link("name", "Name"), 
				t("["), P("_"), Link("size", "FormatSize"), P("_"), t("]"), P("_"),
				t("`"), Link("format", "Formatter"), t("`")
			);
	}
	
	public Expression TemplateStatement() {
		return New(
			t("template"), 
			OneMore(P("S")), 
			Link("name", "Name"), 
			P("_"),
			t("<"), P("_"),
			Link("param", "TemplateParameter"),
			t(">"), P("_"),
			t("="), P("_"),
			Link("expr", "Expr"),
			Tag(NezTag.Template)
		);
	}

	public Expression TemplateParameter() {
		return New(
				Link(null, "Name"), 
				P("_"),
				ZeroMore(t(","), P("_"), Link(null, "Name"), P("_")),
				Tag(NezTag.List)
		);
	}
	
	public Expression Chunk() {
		return Sequence(
			P("_"), 
			Choice(
				P("Example"),
				P("Import"),
				P("Format"),
				P("Rule")
			), 
			P("_"), 
			Option(t(";"), P("_"))
		);
	}

	public Expression File() {
		return New(
			P("_"), 
			ZeroMore(Link(null, "Chunk")),
			Tag(NezTag.List)
		);
	}

}
