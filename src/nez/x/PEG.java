package nez.x;

import nez.Grammar;
import nez.ParserCombinator;
import nez.ast.Tag;
import nez.expr.Expression;
import nez.expr.GrammarChecker;

public class PEG extends ParserCombinator {

	PEG(Grammar grammar) {
		super(grammar);
	}
	
	private static Grammar peg = null;
	public final static Grammar newGrammar() {
		if(peg == null) {
			peg = new PEG(new Grammar("peg")).load(new GrammarChecker());
		}
		return peg;
	}

//	public Expression EOL() {
//		return c("\\r\\n");
//	}
	
	public Expression A() {
		return t("a");
	}

	public Expression B() {
		return t("b");
	}

	public Expression STRAB() {
		return t("ab");
	}
	
	public Expression NOTA() {
		return Not(t("a"));
	}

	public Expression OPTIONA() {
		return Option(t("a"));
	}

	public Expression ZEROMOREA() {
		return ZeroMore(t("a"));
	}

	public Expression AB() {
		return Sequence(t("a"), t("b"));
	}

	public Expression AORB() {
		return Choice(t("a"), t("b"));
	}

	public Expression P_A() {
		return P("A");
	}
	
	public Expression P_NOTA() {
		return Not(P("A"));
	}

	public Expression P_OPTIONA() {
		return Option(P("A"));
	}

	public Expression P_ZEROMOREA() {
		return ZeroMore(P("A"));
	}

	public Expression P_AB() {
		return Sequence(P("A"), P("B"));
	}

	public Expression P_AORB() {
		return Choice(P("A"), P("B"));
	}

	public Expression ANY() {
		return AnyChar();
	}

	public Expression EOF() {
		return Not(AnyChar());
	}

	public Expression HEX() {
		return c("A-Fabcdef0-9");
	}

	public Expression P_HEX() {
		return P("HEX");
	}
	
	public Expression EOL() {
		return Choice(t("\r"), t("\n"));
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

	public Expression W() {
		return c("A-Za-z0-9_");
	}

	public Expression INT() {
		return Sequence(P("DIGIT"), ZeroMore(P("DIGIT")));
	}
	
	public Expression NAME() {
		return Sequence(P("LETTER"), ZeroMore(P("W")));
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

	// PEG4d
	private final static Tag TagA = Tag.tag("A");
	private final static Tag TagB = Tag.tag("B");
	
	public Expression Oa() {
		return New(P("A"), Tag(TagA));
	}

	public Expression Ob() {
		return New(P("B"), Tag(TagB));
	}

	public Expression Oab() {
		return New(Link(0, P("Oa")), Link(1, P("Ob")));
	}

	/**
	public Expression Integer() {
		return New(P("INT"), Tag(NezTag.Integer));
	}

	public Expression Name() {
		return New(P("LETTER"), ZeroMore(P("W")), Tag(NezTag.Name));
	}

	public Expression DotName() {
		return New(P("LETTER"), ZeroMore(c("A-Za-z0-9_.")), Tag(NezTag.Name));
	}

	public Expression HyphenName() {
		return New(P("LETTER"), ZeroMore(Choice(P("W"), t("-"))), Tag(NezTag.Name));
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
		return Sequence(t("'"),  New(StringContent, Tag(NezTag.CharacterSequence)), t("'"));
	}

	public Expression ValueReplacement() {
		Expression ValueContent = ZeroMore(Choice(
			t("\\`"), t("\\\\"), Sequence(Not(t("`")), AnyChar())
		));
		return Sequence(t("`"), New(ValueContent, Tag(NezTag.Value)), t("`"));
	}

	public Expression NonTerminal() {
		return New(
				P("LETTER"), 
				ZeroMore(c("A-Za-z0-9_:")), 
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
			New (P("CHAR"), Tag(NezTag.Character)), 
			Option(
				NewLeftLink(t("-"), Link(New(P("CHAR"), Tag(NezTag.Character))), Tag(NezTag.List))
			)
		);
		return Sequence(t("["), New(ZeroMore(Link(_CharChunk)), Tag(NezTag.Character)), t("]"));
	}

	public Expression Constructor() {
		Expression ConstructorBegin = Choice(t("{"), t("<{"), t("<<"), t("8<"));
		Expression Connector  = Choice(t("@"), t("^"));
		Expression ConstructorEnd   = Choice(t("}>"), t("}"), t(">>"), t(">8"));
		return New(
			ConstructorBegin, 
			Choice(
				Sequence(Connector, P("S"), Tag(NezTag.LeftJoin)), 
				Tag(NezTag.Constructor)
			), 
			P("_"), 
			Option(Sequence(Link(P("Expr")), P("_"))),
			ConstructorEnd
		);
	}
	
	public Expression Func() {
		return Sequence(t("<"), New(
		Choice(
//			Sequence(t("debug"),   P("S"), Link(P("Expr")), Tag(NezTag.Debug)),
//			Sequence(t("memo"),   P("S"), Link(P("Expr")), P("_"), t(">"), Tag(NezTag.Memo)),
			Sequence(t("match"),   P("S"), Link(P("Expr")), P("_"), t(">"), Tag(NezTag.Match)),
//			Sequence(t("fail"),   P("S"), Link(P("SingleQuotedString")), P("_"), t(">"), Tag(NezTag.Fail)),
//			Sequence(t("catch"), Tag(NezTag.Catch)),
			Sequence(t("if"), P("S"), Option(t("!")), Link(P("Name")), Tag(NezTag.If)),
			Sequence(t("with"),  P("S"), Link(P("Name")), P("S"), Link(P("Expr")), Tag(NezTag.With)),
			Sequence(t("without"), P("S"), Link(P("Name")), P("S"), Link(P("Expr")), Tag(NezTag.Without)),
			Sequence(t("block"), Option(Sequence(P("S"), Link(P("Expr")))), Tag(NezTag.Block)),
			Sequence(t("indent"), Tag(NezTag.Indent)),
				//						Sequence(t("choice"), Tag(NezTag.Choice)),
//			Sequence(t("powerset"), P("S"), Link(P("Expr")), Tag(NezTag.PowerSet)),
//			Sequence(t("permutation"), P("S"), Link(P("Expr")), Tag(NezTag.Permutation)),
//			Sequence(t("perm"), P("S"), Link(P("Expr")), Tag(NezTag.PermutationExpr)),
			Sequence(t("scan"), P("S"), Link(New(DIGIT(), ZeroMore(DIGIT()))), t(","), P("S"), Link(P("Expr")), t(","), P("S"), Link(P("Expr")), Tag(NezTag.Scan)),
			Sequence(t("repeat"), P("S"), Link(P("Expr")), Tag(NezTag.Repeat)),
			Sequence(t("is"), P("S"), Link(P("Name")), Tag(NezTag.Is)),
			Sequence(t("isa"), P("S"), Link(P("Name")), Tag(NezTag.Isa)),
			Sequence(t("def"),  P("S"), Link(P("Name")), P("S"), Link(P("Expr")), Tag(NezTag.Def)),
			Sequence(t("name"),  P("S"), Link(P("Name")), P("S"), Link(P("Expr")), Tag(NezTag.Def)),
			Sequence(Option(t("|")), t("append-choice"), Tag(NezTag.Choice))
//			Sequence(Optional(t("|")), t("stringfy"), Tag(NezTag.Stringfy)),
//			Sequence(Optional(t("|")), t("apply"), P("S"), Link(P("Expr")), Tag(NezTag.Apply))
		)), P("_"), t(">")
		);
	}

	public Expression Term() {
		Expression _Any = New(t("."), Tag(NezTag.Any));
		Expression _Tagging = Sequence(t("#"), New(c("A-Za-z0-9"), ZeroMore(c("A-Za-z0-9_.")), Tag(NezTag.Tagging)));
		Expression _Byte = New(t("0x"), P("HEX"), P("HEX"), Tag(NezTag.Byte));
		Expression _Unicode = New(t("U+"), P("HEX"), P("HEX"), P("HEX"), P("HEX"), Tag(NezTag.Byte));
		return Choice(
			P("SingleQuotedString"), P("Charset"), P("Func"),  
			_Any, P("ValueReplacement"), _Tagging, _Byte, _Unicode,
			Sequence(t("("), P("_"), P("Expr"), P("_"), t(")")),
			P("Constructor"), P("String"), P("NonTerminal") 
		);
	}
	
	public Expression SuffixTerm() {
		Expression Connector  = Choice(t("@"), t("^"));
		return Sequence(
			P("Term"), 
			Option(
				NewLeftLink(
					Choice(
						Sequence(t("*"), Option(Link(1, P("Integer"))), Tag(NezTag.Repetition)), 
						Sequence(t("+"), Tag(NezTag.OneMoreRepetition)), 
						Sequence(t("?"), Tag(NezTag.Option)),
						Sequence(Connector, Option(Link(1, P("Integer"))), Tag(NezTag.Connector))
					)
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
					Sequence(t("@["), P("_"), Link(1, P("Integer")), P("_"), t("]"), Tag(NezTag.Connector)),							
					Sequence(t("@"), Tag(NezTag.Connector))
				), 
				Link(0, P("SuffixTerm"))
			), 
			P("SuffixTerm")
		);
	}

	public Expression NOTRULE() {
		return Not(Choice(P("Rule"), P("Import")));
	}

	public Expression Sequence() {
		return Sequence(
			P("Predicate"), 
			Option(
				NewLeftLink(
					P("_"), 
					P("NOTRULE"),
					Link(P("Predicate")),
					ZeroMore(
						P("_"), 
						P("NOTRULE"),
						Link(P("Predicate"))
					),
					Tag(NezTag.Sequence) 
				)
			)
		);
	}

	public Expression Expr() {
		return Sequence(
			P("Sequence"), 
			Option(
				NewLeftLink(
					P("_"), t("/"), P("_"), 
					Link(P("Sequence")), 
					ZeroMore(
						P("_"), t("/"), P("_"), 
						Link(P("Sequence"))
					),
					Tag(NezTag.Choice) 
				)
			)
		);
	}
		
	public Expression DOC() {
		return Sequence(
			ZeroMore(Not(t("]")), Not(t("[")), AnyChar()),
			Option(Sequence(t("["), P("DOC"), t("]"), P("DOC") ))
		);
	}

	public Expression Annotation() {
		return Sequence(
			t("["),
			New(
				Link(P("HyphenName")),
				Option(
					t(":"),  P("_"), 
					Link(New(P("DOC"), Tag(NezTag.Text))),
					Tag(NezTag.Annotation)
				)
			),
			t("]"),
			P("_")
		);
	}

	public Expression Annotations() {
		return New(
			Link(P("Annotation")),
			ZeroMore(Link(P("Annotation"))),
			Tag(NezTag.List) 
		);	
	}
	
	public Expression Rule() {
		return New(
			Link(0, Choice(P("Name"), P("String"))), P("_"), 
//			Optional(Sequence(Link(3, P("Param_")), P("_"))),
			Option(Sequence(Link(2, P("Annotations")), P("_"))),
			t("="), P("_"), 
			Link(1, P("Expr")),
			Tag(NezTag.Rule) 
		);
	}
	
	public Expression Import() {
//		return Constructor(
//			t("import"), 
//			P("S"), 
//			Link(Choice(P("SingleQuotedString"), P("String"), P("DotName"))), 
//			Optional(Sequence(P("S"), t("as"), P("S"), Link(P("Name")))),
//			Tag(NezTag.Import)
//		);
		return New(
			t("import"), P("S"), 
			Link(P("NonTerminal")),
			ZeroMore(P("_"), t(","), P("_"),  Link(P("NonTerminal"))), P("_"), 
			t("from"), P("S"), 
			Link(Choice(P("SingleQuotedString"), P("String"), P("DotName"))), 
		Tag(NezTag.Import)
	);
	}
	
	public Expression Chunk() {
		return Sequence(
			P("_"), 
			Choice(
				P("Rule"), 
				P("Import")
			), 
			P("_"), 
			Option(Sequence(t(";"), P("_")))
		);
	}

	public Expression File() {
		return New(
			P("_"), 
			ZeroMore(Link(P("Chunk"))),
			Tag(NezTag.List)
		);
	}
	**/
}
