package nez.ast.script;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.Tree;

public class TypedTree extends Tree<TypedTree> {
	Type type;
	Method resolvedMethod;
	boolean isStaticNormalMethod;

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

	public final Class<?> getClassType() {
		return TypeSystem.toClass(this.type);
	}

	public void setType(Type type) {
		this.type = type;
	}

	public final Method getMethod() {
		return this.resolvedMethod;
	}

	public void setMethod(boolean isStaticNormalMethod, Method m) {
		this.isStaticNormalMethod = isStaticNormalMethod;
		this.resolvedMethod = m;
	}

	@Override
	protected void stringfy(String indent, Symbol label, StringBuilder sb) {
		super.stringfy(indent, label, sb);
		if (type != null) {
			sb.append(" :");
			sb.append(type.toString());
		}
	}

}
