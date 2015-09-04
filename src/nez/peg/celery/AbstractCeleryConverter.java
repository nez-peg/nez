package nez.peg.celery;

import nez.ast.AbstractTree;
import nez.ast.AbstractTreeVisitor;
import nez.lang.Expression;
import nez.lang.GrammarFactory;
import nez.lang.GrammarFile;

public abstract class AbstractCeleryConverter extends AbstractTreeVisitor {

	protected GrammarFile grammar;
	protected String rootClassName;

	public final void convert(AbstractTree<?> node, GrammarFile grammar) {
		this.grammar = grammar;
		loadPredefinedRules(node);
		this.visit("visit", node);
	}

	abstract protected void loadPredefinedRules(AbstractTree<?> node);

	public final void setRootClassName(String filePath) {
		int offset = filePath.lastIndexOf('/');
		int end = filePath.indexOf('.');
		this.rootClassName = filePath.substring(offset + 1, end);
	}

	abstract protected void visitRoot(AbstractTree<?> node);

	abstract protected void visitStruct(AbstractTree<?> node);

	abstract protected void visitRequired(AbstractTree<?> node);

	abstract protected void visitOption(AbstractTree<?> node);

	abstract protected void visitUntypedRequired(AbstractTree<?> node);

	abstract protected void visitUntypedOption(AbstractTree<?> node);

	protected final Expression toExpression(AbstractTree<?> node) {
		return (Expression) this.visit("to", node);
	}

	public final Expression toTBoolean(AbstractTree<?> node) {
		return GrammarFactory.newNonTerminal(null, grammar, "BOOLEAN");
	}

	public final Expression toTInteger(AbstractTree<?> node) {
		return GrammarFactory.newNonTerminal(null, grammar, "INT");
	}

	public final Expression toTFloat(AbstractTree<?> node) {
		return GrammarFactory.newNonTerminal(null, grammar, "Number");
	}

	public final Expression toTString(AbstractTree<?> node) {
		return GrammarFactory.newNonTerminal(null, grammar, "String");
	}

	public final Expression toTAny(AbstractTree<?> node) {
		return GrammarFactory.newNonTerminal(null, grammar, "Any");
	}

	abstract public Expression toTObject(AbstractTree<?> node);

	abstract public Expression toTClass(AbstractTree<?> node);

	abstract public Expression toTArray(AbstractTree<?> node);

	abstract public Expression toTEnum(AbstractTree<?> node);

}
