package nez.lang;

import nez.ast.SourceLocation;
import nez.lang.expr.Expressions;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class Production /* extends Expression */{

	Grammar grammar;
	String name;
	String uname;
	Expression body;

	Production(SourceLocation s, int flag, Grammar g, String name, Expression body) {
		// super(s);
		this.grammar = g;
		this.name = name;
		this.uname = g.uniqueName(name);
		this.body = (body == null) ? Expressions.newEmpty(s) : body;
	}

	public final Grammar getGrammar() {
		return this.grammar;
	}

	public final String getUniqueName() {
		return this.uname;
	}

	public final String getLocalName() {
		return this.name;
	}

	public final Expression getExpression() {
		return this.body;
	}

	public final void setExpression(Expression e) {
		this.body = e;
	}

	public final boolean isPublic() {
		return true;
	}

	public final boolean isTerminal() {
		return this.name.startsWith("\"");
	}

	public final void dump() {
		UList<String> l = new UList<String>(new String[4]);
		if (this.isPublic()) {
			l.add("public");
		}
		ConsoleUtils.println(l + "\n" + this.getLocalName() + " = " + this.getExpression());
	}

}
