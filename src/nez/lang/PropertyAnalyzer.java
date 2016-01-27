package nez.lang;

public interface PropertyAnalyzer<T> {
	/**
	 * Analyzes the property of the production
	 * 
	 * @param p
	 *            a production
	 * @return an object representing the property
	 */
	T analyze(Production p);

	/**
	 * Analyzes the property of the expression
	 * 
	 * @param e
	 *            a parsing expression
	 * @return an object representing the property
	 */

	T analyze(Expression e);
}
