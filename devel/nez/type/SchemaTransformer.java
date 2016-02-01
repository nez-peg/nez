package nez.type;

import java.lang.reflect.Type;
import java.util.List;

import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.Expressions;
import nez.lang.Grammar;
import nez.lang.Nez;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.type.Schema.ObjectType;
import nez.type.Schema.Property;
import nez.type.Schema.UnaryType;
import nez.type.Schema.UnionType;
import nez.util.ConsoleUtils;

public class SchemaTransformer {
	ObjectType startType;
	Grammar grammar;
	Schema schema;

	public Grammar transform(Production start, Schema schema, ObjectType startType) {
		this.schema = schema == null ? new Schema() : schema;
		new TypeAnalysis(this.schema).typing(start.getGrammar());
		this.grammar = new Grammar();
		new TransformerVisitor(startType).check(start);
		return grammar;
	}

	class Task {
		Task prev;
		Type castType;

		Task(Type top, Task prev) {
			this.prev = prev;
			this.castType = top;
			this.replaceTag = null;
		}

		Task pop() {
			return this.prev;
		}

		ObjectType baseType(Type t) {
			if (t instanceof ObjectType) {
				return (ObjectType) t;
			}
			return baseType(((UnaryType) t).type);
		}

		public final boolean contain(Type castType, Type grammarType) {
			if (castType == null) {
				return true;
			}
			ObjectType objectType = baseType(castType);
			Type derefType = schema.derefType(grammarType);
			// System.out.println("CAST: " + castType + " base: " + objectType);
			// System.out.println("EXPR: " + t + " => " + derefType);
			if (objectType.isSingleType()) {
				return containSingleType(objectType, derefType);
			}
			if (objectType.isListType()) {
				return containListType(objectType, derefType);
			}
			return containObjectType(objectType, derefType);
		}

		private boolean containSingleType(ObjectType castType, Type grammarType) {
			if (grammarType instanceof UnionType) {
				UnionType u = (UnionType) grammarType;
				for (Type t2 : u.unions) {
					if (containSingleType(castType, t2)) {
						return true;
					}
				}
				return false;
			}
			if (grammarType instanceof ObjectType) {
				ObjectType t = (ObjectType) grammarType;
				return t.getTag() == castType.getTag() && t.isSingleType();
			}
			return false;
		}

		private boolean containListType(ObjectType castType, Type grammarType) {
			if (grammarType instanceof UnionType) {
				UnionType u = (UnionType) grammarType;
				for (Type t2 : u.unions) {
					if (containListType(castType, t2)) {
						return true;
					}
				}
				return false;
			}
			if (grammarType instanceof ObjectType) {
				ObjectType gt = (ObjectType) grammarType;
				if (gt.isListType()) {
					Type t1 = castType.getListElementType();
					Type t2 = gt.getListElementType();
					return contain(t1, t2);
				}
			}
			return false;
		}

		private boolean containObjectType(ObjectType castType, Type grammarType) {
			if (grammarType instanceof UnionType) {
				UnionType u = (UnionType) grammarType;
				for (Type t2 : u.unions) {
					if (containObjectType(castType, t2)) {
						this.recording = -1;
						return true;
					}
				}
				return false;
			}
			if (grammarType instanceof ObjectType) {
				if (((ObjectType) grammarType).isEmptyObjectType()) {
					return true;
				}
				if (((ObjectType) grammarType).isRecordType()) {
					this.recording = 0;
					return true;
				}
			}
			return false;
		}

		boolean isVoidType() {
			return castType == null;
		}

		String getNewProductionName(Production p) {
			if (isVoidType()) {
				return p.getLocalName();
			}
			StringBuilder sb = new StringBuilder();
			sb.append(p.getLocalName());
			sb.append("#");
			sb.append(baseType(castType).getTag());
			if (this.getProperty() != null) {
				sb.append("$");
				sb.append(this.getProperty().getLabel());
			}
			return sb.toString();
		}

		Symbol replaceTag = null;

		void beginReplace() {
			if (castType == null) {
				return;
			}
			replaceTag = baseType(castType).tag;
		}

		void endReplace() {
			replaceTag = null;
		}

		Property member = null;

		public void beginProperty(Property p) {
			this.member = p;
		}

		public void endProperty() {
			this.member = null;
		}

		public Property getProperty() {
			return this.member;
		}

		int recording = -1;

		boolean hasRecord() {
			if (recording == -1) {
				return false;
			}
			return recording < baseType(castType).size();
		}

		void nextRecord() {
			recording++;
		}

		Symbol getRecordLabel() {
			return baseType(castType).get(recording).getLabel();
		}

		Type getRecordType() {
			return baseType(castType).get(recording).getType();
		}
	}

	private void perror(Expression e, String msg) {
		ConsoleUtils.perror(this.grammar, ConsoleUtils.Red, e.formatSourceMessage("error", msg));
	}

	class TransformerVisitor extends Expression.DuplicateVisitor {
		Task task;

		TransformerVisitor(ObjectType top) {
			task = new Task(top, null);
		}

		Expression transform(Type t, Expression e) {
			if (t instanceof Schema.UnionType) {
				Schema.UnionType u = (Schema.UnionType) t;
				List<Expression> l = Expressions.newList(u.size());
				for (int i = 0; i < u.size(); i++) {
					Expressions.addChoice(l, transform(u.get(i), e));
				}
				return Expressions.newChoice(l);
			} else {
				task = new Task(t, task);
				e = transform(e);
				task = task.pop();
				return e;
			}
		}

		private final Expression transform(Expression e) {
			return (Expression) e.visit(this, null);
		}

		@Override
		public Expression visitNonTerminal(NonTerminal n, Object a) {
			Production p = check(n.getProduction());
			if (p != null) {
				return Expressions.newNonTerminal(grammar, p.getLocalName());
			}
			return Expressions.newFail();
		}

		final Production check(Production p) {
			String name = p.getLocalName();
			Type t = schema.getType(p.getLocalName());
			if (t == null) {
				return grammar.addProduction(name, p.getExpression());
			}
			if (task.getProperty() == null && !task.contain(task.castType, t)) {
				return null;
			}
			Expression expression = p.getExpression();
			name = task.getNewProductionName(p);
			p = grammar.getProduction(name);
			if (p != null) {
				return p; // Already Transformed
			}
			// System.out.println("new production: " + name);
			p = grammar.addProduction(name, null);
			task.beginReplace();
			p.setExpression(transform(expression));
			task.endReplace();
			return p;
		}

		@Override
		public Expression visitBeginTree(Nez.BeginTree p, Object a) {
			if (task.isVoidType() || task.getProperty() != null) {
				return Expressions.newEmpty();
			}
			return super.visitBeginTree(p, a);
		}

		@Override
		public Expression visitEndTree(Nez.EndTree p, Object a) {
			if (task.isVoidType() || task.getProperty() != null) {
				return Expressions.newEmpty();
			}
			return super.visitEndTree(p, a);
		}

		@Override
		public Expression visitTag(Nez.Tag p, Object a) {
			if (task.isVoidType() || task.getProperty() != null) {
				return Expressions.newEmpty();
			}
			Symbol tag = task.replaceTag != null ? task.replaceTag : p.tag;
			return Expressions.newTag(tag);
		}

		@Override
		public Expression visitReplace(Nez.Replace p, Object a) {
			if (task.isVoidType() || task.getProperty() != null) {
				return Expressions.newEmpty();
			}
			return super.visitReplace(p, a);
		}

		@Override
		public Expression visitOption(Nez.Option p, Object a) {
			if (task.hasRecord() && reachLinkTree(p.get(0), 0)) {
				return transform(p.get(0));
			}
			return super.visitOption(p, a);
		}

		@Override
		public Expression visitOneMore(Nez.OneMore p, Object a) {
			if (task.hasRecord() && reachLinkTree(p.get(0), 0)) {
				List<Expression> l = Expressions.newList(10);
				while (task.hasRecord()) {
					Expressions.addSequence(l, transform(p.get(0)));
				}
				return Expressions.newPair(l);
			}
			return super.visitOneMore(p, a);
		}

		@Override
		public Expression visitZeroMore(Nez.ZeroMore p, Object a) {
			if (task.hasRecord() && reachLinkTree(p.get(0), 0)) {
				List<Expression> l = Expressions.newList(10);
				while (task.hasRecord()) {
					Expressions.addSequence(l, transform(p.get(0)));
				}
				return Expressions.newPair(l);
			}
			return super.visitZeroMore(p, a);
		}

		private boolean reachLinkTree(Expression e, int nested) {
			if (e instanceof Nez.LinkTree) {
				return ((Nez.LinkTree) e).label == null;
			}
			if (e instanceof NonTerminal && nested < 8) {
				return reachLinkTree(((NonTerminal) e).deReference(), nested + 1);
			}
			for (Expression sub : e) {
				if (reachLinkTree(sub, nested)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Expression visitLinkTree(Nez.LinkTree e, Object a) {
			Property p = task.getProperty();
			if (p != null) {
				if (e.label == _key) {
					List<Expression> l = Expressions.newList(16);
					flattenKey(p, l, e.get(0), false);
					return Expressions.newPair(l);
				}
				if (e.label == _value) {
					Symbol label = p.getLabel();
					Type type = p.getType();
					Expression e2 = transform(type, e.get(0));
					if (e2 instanceof Nez.Fail) {
						perror(e.get(0), "cannot typecast to " + type);
						return e2;
					}
					return Expressions.newLinkTree(label, e2);
				}
			}
			if (task.isVoidType()) {
				return transform(e.get(0));
			}
			if (e.label == null && task.hasRecord()) {
				Type type = task.getRecordType();
				Symbol label = task.getRecordLabel();
				task.nextRecord();
				Expression e2 = transform(type, e.get(0));
				if (e2 instanceof Nez.Fail) {
					perror(e.get(0), "cannot typecast to " + type);
					return e2;
				}
				return Expressions.newLinkTree(label, e2);
			}
			ObjectType baseType = task.baseType(task.castType);
			if (baseType.isListType()) {
				Type type = baseType.getListElementType();
				Expression e2 = transform(type, e.get(0));
				if (e2 instanceof Nez.Fail) {
					perror(e.get(0), "cannot typecast to " + type);
					return e2;
				}
				return Expressions.newLinkTree(e.label, e2);
			}
			if (e.label == _member) {
				Expression other = Expressions.newLinkTree(e.label, transform(null, e.get(0)));
				List<Expression> l = Expressions.newList(8);
				for (Property p2 : baseType.members) {
					task.beginProperty(p2);
					Expression e2 = transform(e.get(0));
					Expressions.addChoice(l, e2);
					task.endProperty();
				}
				l.add(other);
				return Expressions.newChoice(l);
			}
			if (e.label == null) {
				return Expressions.newLinkTree(e.label, transform(task.getRecordType(), e.get(0)));
			}
			return super.visitLinkTree(e, a);
		}

		private boolean flattenKey(Property p, List<Expression> l, Expression e, boolean inside) {
			if (e instanceof NonTerminal) {
				return flattenKey(p, l, ((NonTerminal) e).deReference(), inside);
			}
			if (e instanceof Nez.Sequence || e instanceof Nez.Pair) {
				for (Expression sub : e) {
					inside = flattenKey(p, l, sub, inside);
				}
				return inside;
			}
			if (e instanceof Nez.BeginTree) {
				l.add(Expressions.newExpression(p.getKey()));
				return true;
			}
			if (e instanceof Nez.EndTree) {
				return false;
			}
			if (!inside) {
				Expressions.addSequence(l, e);
			}
			return inside;
		}
	}

	public final static Symbol _member = Symbol.unique("_");
	public final static Symbol _key = Symbol.unique("key");
	public final static Symbol _value = Symbol.unique("value");

}