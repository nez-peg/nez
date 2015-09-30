package nez.ast.script;

import java.lang.reflect.Method;

import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.Tree;

public class TypedTree extends Tree<TypedTree> {
	Class<?> type;
	Method resolvedMethod;

	public TypedTree(Symbol tag, Source source, long pos, int len, int size, Object value) {
		super(tag, source, pos, len, size > 0 ? new TypedTree[size] : null, value);
	}

	@Override
	protected TypedTree newInstance(Symbol tag, int size, Object value) {
		return new TypedTree(tag, this.getSource(), this.getSourcePosition(), 0, size, value);
	}

	@Override
	protected TypedTree dupImpl() {
		return new TypedTree(this.getTag(), this.getSource(), this.getSourcePosition(), this.getLength(), this.size(), getValue());
	}

	public final Class<?> getType() {
		return this.type;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}

	public Method getMethod() {
		return this.resolvedMethod;
	}

	public void setMethod(Method m) {
		this.resolvedMethod = m;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		this.stringfy("", null, sb);
		return sb.toString();
	}

	@Override
	public void stringfy(String indent, Symbol label, StringBuilder sb) {
		super.stringfy(indent, label, sb);
		if (this.type != null) {
			sb.append(" :");
			sb.append(this.type.toString());
		}
	}

}
