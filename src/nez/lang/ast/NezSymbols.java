package nez.lang.ast;

import nez.ast.Symbol;

public interface NezSymbols {
	public final static Symbol _name = Symbol.unique("name");
	public final static Symbol _expr = Symbol.unique("expr");
	public final static Symbol _symbol = Symbol.unique("symbol");
	public final static Symbol _mask = Symbol.unique("mask"); // <scanf >
	public final static Symbol _hash = Symbol.unique("hash"); // example
	public final static Symbol _name2 = Symbol.unique("name2"); // example
	public final static Symbol _text = Symbol.unique("text"); // example
	public final static Symbol _case = Symbol.unique("case");

	public final static Symbol _String = Symbol.unique("String");
	public final static Symbol _Integer = Symbol.unique("Integer");
	public final static Symbol _List = Symbol.unique("List");
	public final static Symbol _Name = Symbol.unique("Name");
	public final static Symbol _Format = Symbol.unique("Format");
	public final static Symbol _Class = Symbol.unique("Class");

	public final static Symbol _anno = Symbol.unique("anno");

}
