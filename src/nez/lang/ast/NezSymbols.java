package nez.lang.ast;

import nez.ast.Symbol;

public interface NezSymbols {
	Symbol _name = Symbol.tag("name");
	Symbol _expr = Symbol.tag("expr");
	Symbol _symbol = Symbol.tag("symbol");
	Symbol _hash = Symbol.tag("hash"); // example
	Symbol _name2 = Symbol.tag("name2"); // example
	Symbol _text = Symbol.tag("text"); // example

	Symbol _String = Symbol.tag("String");
	Symbol _Integer = Symbol.tag("Integer");
	Symbol _List = Symbol.tag("List");
	Symbol _Name = Symbol.tag("Name");
	Symbol _Format = Symbol.tag("Format");
	Symbol _Class = Symbol.tag("Class");

	Symbol _anno = Symbol.tag("anno");

}
