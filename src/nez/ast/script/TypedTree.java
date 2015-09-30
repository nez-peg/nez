package nez.ast.script;

import java.lang.reflect.Method;

import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.Tree;

public class TypedTree extends Tree<TypedTree> {
	Class<?> type;
	Method resolvedMethod;

	TypedTree() {
		super();
	}

	public TypedTree(Symbol tag, Source source, long pos, int len, int size, Object value) {
		super(tag, source, pos, len, size > 0 ? new TypedTree[size] : null, value);
	}

	@Override
	protected TypedTree newInstance(Symbol tag, Source source, long pos, int len, int objectsize, Object value) {
		return new TypedTree(tag, source, pos, len, objectsize, value);
	}

	@Override
	protected void link(int n, Symbol label, Object child) {
		this.set(n, label, (TypedTree) child);
	}

	@Override
	public TypedTree newInstance(Symbol tag, int size, Object value) {
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
	protected void stringfy(String indent, Symbol label, StringBuilder sb) {
		super.stringfy(indent, label, sb);
		if (type != null) {
			sb.append(" :");
			sb.append(type.getSimpleName());
		}
	}

}
