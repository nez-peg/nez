package nez.lang;

/**
 * NezFunction provides a set of Nez function names. NezFunction is used to
 * Identify the type of function in Nez.Function.
 * 
 * @author kiki
 *
 */

public enum NezFunction {
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
