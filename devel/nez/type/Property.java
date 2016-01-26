package nez.type;

import java.lang.reflect.Type;

import nez.ast.Symbol;

public class Property {
	Symbol label;
	Type type;

	public Property(Symbol label, Type type) {
		this.label = label;
		this.type = type;
	}
}
