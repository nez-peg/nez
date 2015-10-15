package konoha.script;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import konoha.message.Message;
import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.Tree;
import nez.util.UList;

public class TypedTree extends Tree<TypedTree> {
	public Hint hint = Hint.Unique;
	Type type;

	// Method resolvedMethod;

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

	public TypedTree newIntConst(int n) {
		TypedTree t = new TypedTree(CommonSymbols._Integer, this.getSource(), this.getSourcePosition(), 0, 0, n);
		t.setConst(int.class, n);
		return t;
	}

	@Override
	protected TypedTree dupImpl() {
		TypedTree t = new TypedTree(this.getTag(), this.getSource(), this.getSourcePosition(), this.getLength(), this.size(), getValue());
		t.hint = this.hint;
		t.type = this.type;
		return t;
	}

	@Override
	protected RuntimeException newNoSuchLabel(Symbol label) {
		return new TypeCheckerException(this, Message.SyntaxError_Expected, label);
	}

	// public void changed(Symbol tag, int n, Object v) {
	// this.tag = tag;
	// this.subTree = new TypedTree[n];
	// this.value = v;
	// }

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

	// public Type setMethod(Hint hint, Method m, TypeVarMatcher matcher) {
	// this.hint = hint;
	// this.setValue(m);
	// this.type = matcher == null ? m.getReturnType() :
	// matcher.resolve(m.getGenericReturnType(), Object.class);
	// return this.type;
	// }

	public final Field getField() {
		return (Field) this.getValue();
	}

	public Type setField(Hint hint, Field f) {
		this.hint = hint;
		this.setValue(f);
		this.type = f.getType();
		return this.type;
	}

	public Type setInterface(Hint hint, Functor inf) {
		this.hint = hint;
		this.setValue(inf);
		this.type = inf.getReturnType();
		return this.type;
	}

	public Type setInterface(Hint hint, Functor inf, TypeMatcher matcher) {
		this.hint = hint;
		this.setValue(inf);
		this.type = matcher != null ? matcher.resolve(inf.getReturnType(), Object.class) : inf.getReturnType();
		return this.type;
	}

	//
	// public final Method getMethod() {
	// return (Method) this.getValue();
	// }

	@Override
	protected void stringfy(String indent, Symbol label, StringBuilder sb) {
		super.stringfy(indent, label, sb);
		if (type != null) {
			sb.append(" :");
			sb.append(TypeSystem.name(type));
		}
	}

	public void done() {
		this.setTag(CommonSymbols._Empty);
	}

	public Hint hint() {
		return this.hint;
	}

	public void setHint(Hint hint, Type type) {
		this.hint = hint;
		this.type = type;
	}

	/* Tree Manipulation */

	public void makeFlattenedList(TypedTree... args) {
		UList<TypedTree> l = new UList<TypedTree>(new TypedTree[4]);
		for (TypedTree t : args) {
			if (t.size() == 0) {
				l.add(t);
			} else {
				for (TypedTree sub : t) {
					l.add(sub);
				}
			}
		}
		this.subTree = l.compactArray();
		this.labels = new Symbol[l.size()];
	}

	public void removeSubtree() {
		this.subTree = null;
		this.labels = EmptyLabels;
	}

	public void make(Symbol l1, TypedTree t1) {
		this.subTree = new TypedTree[] { t1 };
		this.labels = new Symbol[] { l1 };
	}

	public void make(Symbol l1, TypedTree t1, Symbol l2, TypedTree t2) {
		this.subTree = new TypedTree[] { t1, t2 };
		this.labels = new Symbol[] { l1, l2 };
	}

	public void make(Symbol l1, TypedTree t1, Symbol l2, TypedTree t2, Symbol l3, TypedTree t3) {
		this.subTree = new TypedTree[] { t1, t2, t3 };
		this.labels = new Symbol[] { l1, l2, l3 };
	}

	public Functor getInterface() {
		return (Functor) this.value;
	}

}
