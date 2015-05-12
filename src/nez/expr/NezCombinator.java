package nez.expr;

import nez.NameSpace;
import nez.ParserCombinator;

public class NezCombinator extends ParserCombinator {

	NezCombinator(NameSpace grammar) {
		super(grammar);
	}
	
	private static NameSpace peg = null;
	public final static NameSpace newGrammar() {
		if(peg == null) {
			peg = new NezCombinator(NameSpace.newNameSpace("nez")).load();
		}
		return peg;
	}
	
	public Expression EOT() {
		return Not(AnyChar());
	}

	public Expression EOL() {
		return Choice(t("\n"), Sequence(t("\r"), Option("\n")), P("EOT"));
	}

//	public Expression SEMI() {
//		return Option(";");
//	}

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
				t("import"), t("syntaxtree"), t("grammar"), 
				t("example"), t("rebut")),
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

	public Expression SPACING() {
		return ZeroMore(Choice(P("S"), P("COMMENT")));
	}
	
	public Expression Integer() {
		return New(P("INT"), Tag(NezTag.Integer));
	}

	public Expression Name() {
		return New(P("NAME"), Tag(NezTag.Name));
	}

	public Expression DotName() {
		return New(P("LETTER"), ZeroMore(c("A-Za-z0-9_.")), Tag(NezTag.Name));
	}

//	public Expression HyphenName() {
//		return New(P("LETTER"), ZeroMore(Choice(P("W"), t("-"))), Tag(NezTag.Name));
//	}

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
		return New(
			P("NAME"), 
			//ZeroMore(c("A-Za-z0-9_:")), 
			Tag(NezTag.NonTerminal)
		);
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
			New (P("CHAR"), Tag(NezTag.Class)), 
			LeftNewOption(t("-"), Link(New(P("CHAR"), Tag(NezTag.Class))), Tag(NezTag.List))
		);
		return Sequence(t("["), New(ZeroMore(Link(_CharChunk)), Tag(NezTag.Class)), t("]"));
	}

	public Expression Constructor() {
		return New(
			t("{"), 
			Choice(
				Sequence(t("@"), P("S"), Tag(NezTag.LeftNew)), 
				Tag(NezTag.New)
			), 
			P("_"), 
			Option(Link(P("Expr")), P("_")),
			t("}")
		);
	}

	public Expression Func() {
		return Sequence(t("<"), 
			New(Choice(
			Sequence(t("match"),   P("S"), Link(P("Expr")), P("_"), t(">"), Tag(NezTag.Match)),
			Sequence(t("if"), P("S"), Option(t("!")), Link(P("Name")), Tag(NezTag.If)),
			Sequence(t("with"),  P("S"), Link(P("Name")), P("S"), Link(P("Expr")), Tag(NezTag.With)),
			Sequence(t("without"), P("S"), Link(P("Name")), P("S"), Link(P("Expr")), Tag(NezTag.Without)),
			Sequence(t("block"), Option(Sequence(P("S"), Link(P("Expr")))), Tag(NezTag.Block)),
			Sequence(t("indent"), Tag(NezTag.Indent)),
			Sequence(t("is"), P("S"), Link(P("Name")), Tag(NezTag.Is)),
			Sequence(t("isa"), P("S"), Link(P("Name")), Tag(NezTag.Isa)),
			Sequence(t("def"),  P("S"), Link(P("Name")), P("S"), Link(P("Expr")), Tag(NezTag.Def)),
			Sequence(t("scan"), P("S"), Link(New(DIGIT(), ZeroMore(DIGIT()))), t(","), P("S"), Link(P("Expr")), t(","), P("S"), Link(P("Expr")), Tag(NezTag.Scan)),
			Sequence(t("repeat"), P("S"), Link(P("Expr")), Tag(NezTag.Repeat))
			)), P("_"), t(">")
		);
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
			LeftNewOption(
				Choice(
					Sequence(t("*"), Option(Link(1, P("Integer"))), Tag(NezTag.Repetition)), 
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
					Sequence(t("@["), P("_"), Link(1, P("Integer")), P("_"), t("]"), Tag(NezTag.Link)),							
					Sequence(t("@"), Tag(NezTag.Link))
				), 
				Link(0, P("SuffixTerm"))
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
			LeftNewOption(
				OneMore(
				P("_"), 
				P("NOTRULE"),
				Link(P("Predicate"))
				),
				Tag(NezTag.Sequence) 
			)
		);
	}

	public Expression Expr() {
		return Sequence(
			P("Sequence"), 
			LeftNewOption(
				OneMore(
				P("_"), t("/"), P("_"), 
				Link(P("Sequence"))
				),
				Tag(NezTag.Choice) 
			)
		);
	}
		
//
//	public Expression Annotation() {
//		return Sequence(
//			t("["),
//			New(
//				Link(P("HyphenName")),
//				Option(
//					t(":"),  P("_"), 
//					Link(New(P("DOC"), Tag(NezTag.Text))),
//					Tag(NezTag.Annotation)
//				)
//			),
//			t("]"),
//			P("_")
//		);
//	}
//
//	public Expression Annotations() {
//		return New(
//			Link(P("Annotation")),
//			ZeroMore(Link(P("Annotation"))),
//			Tag(NezTag.List) 
//		);	
//	}
//	
//	public Expression Rule0() {
//		return New(
//			Link(0, Choice(P("Name"), P("String"))), P("_"), 
////			Optional(Sequence(Link(3, P("Param_")), P("_"))),
//			Option(Sequence(Link(2, P("Annotations")), P("_"))),
//			t("="), P("_"), 
//			Link(1, P("Expr")),
//			Tag(NezTag.Rule) 
//		);
//	}
	
	public Expression RuleHead() {
		return New(
			P("addQualifers"), 
			Link(0, Choice(P("Name"), P("String"))), P("_"), 
			P("SKIP"), P("_"), t("=")  
		);
	}

	public Expression Rule() {
		return New(
			P("addQualifers"), 
			Link(0, Choice(P("Name"), P("String"))), P("_"), 
			P("SKIP"),
			t("="), P("_"), 
			Link(1, P("Expr")),
			Tag(NezTag.Rule) 
		);
	}
	
	public Expression QUALIFERS() {
		return Sequence(Choice(t("public"), t("inline")), Not(P("W")));
	}

	public Expression Qualifers() {
		return New(ZeroMore(Link(New(P("QUALIFERS"))), P("S")));
	}

	public Expression addQualifers() {
		return Option(And(P("QUALIFERS")), Link(2, P("Qualifers")));
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

	public Expression Import() {
		return New(
			t("import"), P("S"), Link(P("NonTerminal")),
			ZeroMore(P("_"), t(","), P("_"),  Link(P("NonTerminal"))), P("_"), 
			t("from"), P("S"), 
			Link(Choice(P("SingleQuotedString"), P("String"), P("DotName"))), 
			Tag(NezTag.Import)
		);
	}

	
	public Expression Example() {
		return New(
			Choice(Sequence(t("example"), Tag(NezTag.Example)), 
					Sequence(t("rebut"), Tag(NezTag.Rebut))),
			P("S"), Link(P("NonTerminal")), ZeroMore(c(" \t")), 
			Choice(
				Sequence(t("'''"), P("EOL"), Link(New(ZeroMore(NotAny("\n'''")))), t("\n'''")),
				Sequence(t("```"), P("EOL"), Link(New(ZeroMore(NotAny("\n```")))), t("\n```")),
				Sequence(t("\"\"\""), P("EOL"), Link(New(ZeroMore(NotAny("\n\"\"\"")))), t("\n\"\"\"")),
				Sequence(Link(New(ZeroMore(NotAny(P("EOL"))))), P("EOL"))
			)
		);
	}
	
	public Expression Chunk() {
		return Sequence(
			P("_"), 
			Choice(
				P("Example"),
				P("Import"),
				P("Rule")
			), 
			P("_"), 
			Option(t(";"), P("_"))
		);
	}

	public Expression File() {
		return New(
			P("_"), 
			ZeroMore(Link(P("Chunk"))),
			Tag(NezTag.List)
		);
	}

}
