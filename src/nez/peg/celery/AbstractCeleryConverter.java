package nez.peg.celery;

import nez.ast.CommonTree;
import nez.ast.CommonTreeVisitor;
import nez.lang.Expression;
import nez.lang.GrammarFactory;
import nez.lang.GrammarFile;


public abstract class AbstractCeleryConverter extends CommonTreeVisitor {

	protected GrammarFile grammar;
	protected String rootClassName;

	public final void convert(CommonTree node, GrammarFile grammar) {
		this.grammar = grammar;
		loadPredefinedRules(node);
		this.visit("visit", node);
	}

	abstract protected void loadPredefinedRules(CommonTree node);

	public final void setRootClassName(String filePath) {
		int offset = filePath.lastIndexOf('/');
		int end = filePath.indexOf('.');
		this.rootClassName = filePath.substring(offset + 1, end);
	}

	abstract protected void visitRoot(CommonTree node);

	abstract protected void visitStruct(CommonTree node);

	abstract protected void visitRequired(CommonTree node);

	abstract protected void visitOption(CommonTree node);

	abstract protected void visitUntypedRequired(CommonTree node);

	abstract protected void visitUntypedOption(CommonTree node);


	protected final Expression toExpression(CommonTree node) {
		return (Expression) this.visit("to", node);
	}

	public final Expression toTBoolean(CommonTree node) {
		return GrammarFactory.newNonTerminal(null, grammar, "BOOLEAN");
	}

	public final Expression toTInteger(CommonTree node) {
		return GrammarFactory.newNonTerminal(null, grammar, "INT");
	}

	public final Expression toTFloat(CommonTree node) {
		return GrammarFactory.newNonTerminal(null, grammar, "Number");
	}

	public final Expression toTString(CommonTree node) {
		return GrammarFactory.newNonTerminal(null, grammar, "String");
	}

	public final Expression toTAny(CommonTree node) {
		return GrammarFactory.newNonTerminal(null, grammar, "Any");
	}

	abstract public Expression toTObject(CommonTree node);

	abstract public Expression toTClass(CommonTree node);

	abstract public Expression toTArray(CommonTree node);

	abstract public Expression toTEnum(CommonTree node);

}
