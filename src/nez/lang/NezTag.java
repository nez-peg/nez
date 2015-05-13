package nez.lang;

import nez.ast.Tag;

public class NezTag {

	static final Tag Text         = Tag.tag("Text");
	static final Tag Integer      = Tag.tag("Integer");
	static final Tag Name         = Tag.tag("Name");

	static final Tag AnyChar      = Tag.tag("Any");
	static final Tag ByteChar     = Tag.tag("Byte");
	static final Tag Class        = Tag.tag("Class");
	static final Tag Character    = Tag.tag("Character");
	static final Tag String       = Tag.tag("String");
	static final Tag List         = Tag.tag("List");
	
	static final Tag NonTerminal  = Tag.tag("NonTerminal");
	static final Tag Choice       = Tag.tag("Choice");
	static final Tag Sequence     = Tag.tag("Sequence");
	static final Tag Repetition   = Tag.tag("Repetition");
	static final Tag Repetition1  = Tag.tag("Repetition1");
	static final Tag Option       = Tag.tag("Option");
	static final Tag Not          = Tag.tag("Not");
	static final Tag And          = Tag.tag("And");

	static final Tag Match       = Tag.tag("Match");
	static final Tag New         = Tag.tag("New");
	static final Tag LeftNew     = Tag.tag("LeftNew");
	static final Tag Link        = Tag.tag("Link");
	static final Tag Tagging     = Tag.tag("Tagging");
	static final Tag Replace     = Tag.tag("Replace");
	

//	static final Tag Debug       = Tag.tag("Debug");
//	static final Tag Memo        = Tag.tag("Memo");
	static final Tag If          = Tag.tag("If");
	static final Tag On          = Tag.tag("On");
	static final Tag With        = Tag.tag("With");
	static final Tag Without     = Tag.tag("Without");
	
	static final Tag Block       = Tag.tag("Block");
	static final Tag Def         = Tag.tag("Def");
	static final Tag Is          = Tag.tag("Is");
	static final Tag Isa         = Tag.tag("Isa");
	static final Tag Exists      = Tag.tag("Exists");
	static final Tag Local       = Tag.tag("Local");
	
	static final Tag DefIndent      = Tag.tag("DefIndent");
	public static final Tag Indent      = Tag.tag("Indent");
	
	static final Tag Scan        = Tag.tag("Scan");
	static final Tag Repeat      = Tag.tag("Repeat");
	static final Tag Undefined   = Tag.tag("Undefined");
	
	
	static final Tag Rule         = Tag.tag("Rule");
	static final Tag Import       = Tag.tag("Import");
	static final Tag Example      = Tag.tag("Example");
	static final Tag Rebut        = Tag.tag("Rebut");
	static final Tag Format       = Tag.tag("Format");

}
