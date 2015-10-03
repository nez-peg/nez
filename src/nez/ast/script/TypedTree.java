package nez.ast.script;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.Tree;

public class TypedTree extends Tree<TypedTree> {
	public Hint hint = Hint.Unique;
	Type type;
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

	public TypedTree newStringConst(String s) {
		TypedTree t = new TypedTree(CommonSymbols._String, this.getSource(), this.getSourcePosition(), 0, 0, s);
		t.setConst(String.class, s);
		return t;
	}

	@Override
	protected TypedTree dupImpl() {
		return new TypedTree(this.getTag(), this.getSource(), this.getSourcePosition(), this.getLength(), this.size(), getValue());
	}

	public void changed(Symbol tag, int n, Object v) {
		this.tag = tag;
		this.subTree = new TypedTree[n];
		this.value = v;
	}

	public final Class<?> getClassType() {
		return TypeSystem.toClass(this.type);
	}

	public Type getType() {
		return this.type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Type setConst(Type type, Object value) {
		this.hint = Hint.Constant;
		this.setValue(value);
		this.type = type;
		return this.type;
	}

	public void setClass(Hint hint, Class<?> c) {
		this.hint = hint;
		this.setValue(c);
	}

	public Type setMethod(Hint hint, Method m) {
		this.hint = hint;
		this.setValue(m);
		this.type = m.getReturnType();
		return this.type;
	}

	public final Method getMethod() {
		return (Method) this.getValue();
	}

	public final Field getField() {
		return (Field) this.getValue();
	}

	public Type setField(Hint hint, Field f) {
		this.hint = hint;
		this.setValue(f);
		this.type = f.getType();
		return this.type;
	}

	@Override
	protected void stringfy(String indent, Symbol label, StringBuilder sb) {
		super.stringfy(indent, label, sb);
		if (type != null) {
			sb.append(" :");
			sb.append(type.toString());
		}
	}

	public void done() {
		this.setTag(CommonSymbols._Empty);
	}

	public Hint hint() {
		return this.hint;
	}

}
