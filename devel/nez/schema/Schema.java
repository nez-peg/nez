package nez.schema;

import java.util.HashMap;

import nez.lang.Expression;
import nez.lang.Grammar;

public class Schema {
	HashMap<String, Class<?>> typeMap;
}

class Schemas {
	Member[] members(Class<?> c) {
		return null;
	}
}

class Member {
	String name;
	Class<?> type;
}

class SchemaWriter {

	Grammar grammar;

	Expression StringData() {
		return null;
	}
}