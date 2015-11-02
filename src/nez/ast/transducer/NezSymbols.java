package nez.ast.transducer;

import nez.ast.Symbol;

public interface NezSymbols {
	public final static Symbol _name = Symbol.tag("name");
	public final static Symbol _expr = Symbol.tag("expr");
	public final static Symbol _symbol = Symbol.tag("symbol");
	public final static Symbol _hash = Symbol.tag("hash"); // example
	public final static Symbol _name2 = Symbol.tag("name2"); // example
	public final static Symbol _text = Symbol.tag("text"); // example

	public final static Symbol _String = Symbol.tag("String");
	public final static Symbol _Integer = Symbol.tag("Integer");
	public final static Symbol _List = Symbol.tag("List");
	public final static Symbol _Name = Symbol.tag("Name");
	public final static Symbol _Format = Symbol.tag("Format");
	public final static Symbol _Class = Symbol.tag("Class");

	public final static Symbol _anno = Symbol.tag("anno");

}
