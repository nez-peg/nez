package nez.lang;

public enum Predicate {
	_if, on, //
	block, local, //
	symbol, //
	match, is, isa, exists, //
	setcount, count;

	@Override
	public String toString() {
		return name();
	}

}
