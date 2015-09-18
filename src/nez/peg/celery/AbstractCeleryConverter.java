package nez.peg.celery;

import nez.ast.Tree;
import nez.ast.TreeVisitor;
import nez.lang.Expression;
import nez.lang.GrammarFile;
import nez.lang.expr.ExpressionCommons;

public abstract class AbstractCeleryConverter extends TreeVisitor {

	protected GrammarFile grammar;
	protected String rootClassName;

	public final void convert(Tree<?> node, GrammarFile grammar) {
		this.grammar = grammar;
		loadPredefinedRules(node);
		this.visit("visit", node);
	}

	abstract protected void loadPredefinedRules(Tree<?> node);

	public final void setRootClassName(String filePath) {
		int offset = filePath.lastIndexOf('/');
		int end = filePath.indexOf('.');
		this.rootClassName = filePath.substring(offset + 1, end);
	}

	abstract protected void visitRoot(Tree<?> node);

	abstract protected void visitStruct(Tree<?> node);

	abstract protected void visitRequired(Tree<?> node);

	abstract protected void visitOption(Tree<?> node);

	abstract protected void visitUntypedRequired(Tree<?> node);

	abstract protected void visitUntypedOption(Tree<?> node);

	protected final Expression toExpression(Tree<?> node) {
		return (Expression) this.visit("to", node);
	}

	public final Expression toTBoolean(Tree<?> node) {
		return ExpressionCommons.newNonTerminal(null, grammar, "BOOLEAN");
	}

	public final Expression toTInteger(Tree<?> node) {
		return ExpressionCommons.newNonTerminal(null, grammar, "INT");
	}

	public final Expression toTFloat(Tree<?> node) {
		return ExpressionCommons.newNonTerminal(null, grammar, "Number");
	}

	public final Expression toTString(Tree<?> node) {
		return ExpressionCommons.newNonTerminal(null, grammar, "String");
	}

	public final Expression toTAny(Tree<?> node) {
		return ExpressionCommons.newNonTerminal(null, grammar, "Any");
	}

	abstract public Expression toTObject(Tree<?> node);

	abstract public Expression toTClass(Tree<?> node);

	abstract public Expression toTArray(Tree<?> node);

	abstract public Expression toTEnum(Tree<?> node);

}
