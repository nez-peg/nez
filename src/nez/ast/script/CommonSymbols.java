package nez.ast.script;

import nez.ast.Symbol;

public interface CommonSymbols {

	public final static Symbol _anno = Symbol.tag("anno");
	public final static Symbol _name = Symbol.tag("name");
	public final static Symbol _super = Symbol.tag("super");
	public final static Symbol _impl = Symbol.tag("impl");
	public final static Symbol _body = Symbol.tag("body");
	public final static Symbol _type = Symbol.tag("type");
	public final static Symbol _expr = Symbol.tag("expr");
	public final static Symbol _list = Symbol.tag("list");
	public final static Symbol _param = Symbol.tag("param");
	public final static Symbol _throws = Symbol.tag("throws");
	public final static Symbol _base = Symbol.tag("base");
	public final static Symbol _extends = Symbol.tag("extends");
	public final static Symbol _cond = Symbol.tag("cond");
	public final static Symbol _msg = Symbol.tag("msg");
	public final static Symbol _then = Symbol.tag("then");
	public final static Symbol _else = Symbol.tag("else");
	public final static Symbol _init = Symbol.tag("init");
	public final static Symbol _iter = Symbol.tag("iter");
	public final static Symbol _label = Symbol.tag("label");
	public final static Symbol _try = Symbol.tag("try");
	public final static Symbol _catch = Symbol.tag("catch");
	public final static Symbol _finally = Symbol.tag("finally");
	public final static Symbol _left = Symbol.tag("left");
	public final static Symbol _right = Symbol.tag("right");
	public final static Symbol _recv = Symbol.tag("recv");
	public final static Symbol _size = Symbol.tag("size");
	public final static Symbol _prefix = Symbol.tag("prefix");

	public final static Symbol _Name = Symbol.tag("Name");
	public final static Symbol _ArrayType = Symbol.tag("ArrayType");
	public final static Symbol _GenericType = Symbol.tag("GenericType");
}
