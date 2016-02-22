package nez.infer;

import java.util.List;

import nez.lang.Expression;
import nez.lang.Expressions;
import nez.lang.Grammar;
import nez.util.UList;

public abstract class StructureType {
	protected int maxTokenCount = 0;

	abstract public Expression getExpression(Grammar g);
}

class BaseType extends StructureType {
	private Token baseToken;

	public BaseType(Token singleToken) {
		this.baseToken = singleToken;
	}

	@Override
	public String toString() {
		return this.baseToken.toString();
	}

	@Override
	public Expression getExpression(Grammar g) {
		return this.baseToken.getExpression(g);
	}
}

class MetaTokenType extends StructureType {
	private Token metaToken;

	public MetaTokenType(Token singleToken) {
		this.metaToken = singleToken;
	}

	@Override
	public String toString() {
		return this.metaToken.toString();
	}

	@Override
	public Expression getExpression(Grammar g) {
		return this.metaToken.getExpression(g);
	}

}

class Struct extends StructureType {
	protected StructureType[] inner;

	public Struct() {

	}

	public Struct(StructureType[] structList) {
		this.inner = structList;
	}

	@Override
	public Expression getExpression(Grammar g) {
		UList<Expression> l = new UList<Expression>(new Expression[5]);
		for (StructureType element : inner) {
			l.add(element.getExpression(g));
		}
		return Expressions.newSequence(l);
	}
}

class Sequence extends StructureType {
	List<Token> tokenList;

	public Sequence(List<Token> tokenList, int maxTokenCount) {
		this.tokenList = tokenList;
		this.maxTokenCount = maxTokenCount;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Token token : this.tokenList) {
			builder.append(token.toString()).append(" ");
		}
		return builder.toString();
	}

	@Override
	public Expression getExpression(Grammar g) {
		Expression[] l = new Expression[this.maxTokenCount];

		for (Token element : tokenList) {
			for (int index : element.getHistogram().getOrderIdList()) {
				l[index] = element.getExpression(g);
			}
		}
		return Expressions.newSequence(l);
	}
}

class Choice extends StructureType {
	List<Token> tokenList;

	public Choice(List<Token> tokenList, int maxTokenCount) {
		this.tokenList = tokenList;
		this.maxTokenCount = maxTokenCount;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Token token : this.tokenList) {
			builder.append(token.toString()).append(" / ");
		}
		builder.deleteCharAt(builder.lastIndexOf("/"));
		return builder.toString();
	}

	@Override
	public Expression getExpression(Grammar g) {
		UList<Expression> l = new UList<Expression>(new Expression[this.maxTokenCount]);

		for (Token element : tokenList) {
			for (int index : element.getHistogram().getOrderIdList()) {
				l.add(index, element.getExpression(g));
			}
		}
		return Expressions.newChoice(l);
	}
}

class Array extends Sequence {

	public Array(List<Token> tokenList, int maxTokenCount) {
		super(tokenList, maxTokenCount);
		// TODO Auto-generated constructor stub
	}
}

class Union extends Struct {

	public Union(StructureType[] structList) {
		super(structList);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Expression getExpression(Grammar g) {
		UList<Expression> l = new UList<Expression>(new Expression[5]);
		for (StructureType element : inner) {
			l.add(element.getExpression(g));
		}
		return Expressions.newChoice(l);
	}

}
