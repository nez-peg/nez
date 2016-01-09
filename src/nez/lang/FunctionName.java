package nez.lang;

/**
 * FunctionName provides a set of Nez function names. FunctionName is used to
 * Identify the type of function in Nez.Function.
 * 
 * @author kiki
 *
 */

public enum FunctionName {
	_if, on, //
	block, local, //
	symbol, //
	match, is, isa, exists, //
	setcount, count;

	@Override
	public String toString() {
		String s = name();
		if (s.startsWith("_")) {
			s = s.substring(1);
		}
		return s;
	}

}
