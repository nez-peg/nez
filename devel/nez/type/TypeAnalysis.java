package nez.type;

import java.lang.reflect.Type;
import java.util.ArrayList;

import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Nez.And;
import nez.lang.Nez.Any;
import nez.lang.Nez.BeginTree;
import nez.lang.Nez.BlockScope;
import nez.lang.Nez.Byte;
import nez.lang.Nez.ByteSet;
import nez.lang.Nez.Choice;
import nez.lang.Nez.Detree;
import nez.lang.Nez.Empty;
import nez.lang.Nez.EndTree;
import nez.lang.Nez.Fail;
import nez.lang.Nez.FoldTree;
import nez.lang.Nez.IfCondition;
import nez.lang.Nez.Label;
import nez.lang.Nez.LinkTree;
import nez.lang.Nez.LocalScope;
import nez.lang.Nez.MultiByte;
import nez.lang.Nez.Not;
import nez.lang.Nez.OnCondition;
import nez.lang.Nez.OneMore;
import nez.lang.Nez.Option;
import nez.lang.Nez.Pair;
import nez.lang.Nez.Repeat;
import nez.lang.Nez.Replace;
import nez.lang.Nez.Scan;
import nez.lang.Nez.Sequence;
import nez.lang.Nez.SymbolAction;
import nez.lang.Nez.SymbolExists;
import nez.lang.Nez.SymbolMatch;
import nez.lang.Nez.SymbolPredicate;
import nez.lang.Nez.Tag;
import nez.lang.Nez.ZeroMore;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.lang.Typestate;
import nez.lang.Typestate.TypestateAnalyzer;
import nez.type.Schema.OptionType;
import nez.type.Schema.Property;
import nez.type.Schema.ZeroMoreType;
import nez.util.UList;

public class TypeAnalysis {

	final TypestateAnalyzer typeState = Typestate.newAnalyzer();
	TypingVisitor visitor = new TypingVisitor();
	Schema schema;

	public TypeAnalysis(Schema schema) {
		this.schema = schema;
	}

	public void typing(Grammar g) {
		for (Production p : g) {
			visitor.typing(p);
		}
	}

	public Type typing(Production p) {
		return visitor.typing(p);
	}

	public final static String nname(Production p) {
		return p.getLocalName();
	}

	static class TreeState {
		TreeState prev;
		boolean inOption;
		boolean inZeroMore;
		boolean inOneMore;
		Type left = null;
		ArrayList<String> tags = new ArrayList<>();
		UList<Property> fields = new UList<>(new Property[4]);

		TreeState(TreeState prev) {
			this.prev = prev;
		}

		void addTag(Symbol tag) {
			if (!tags.contains(tag)) {
				tags.add(tag.toString());
			}
		}

		Property tail() {
			if (fields.size() > 0) {
				return fields.get(fields.size() - 1);
			}
			return null;
		}

		void addProperty(Symbol label, Type t) {
			if (t == null) {
				return;
			}
			if (inOption) {
				t = Schema.newOptionType(t);
			}
			if (inOneMore) {
				t = Schema.newOneMoreType(t);
			}
			if (inZeroMore) {
				t = Schema.newZeroMoreType(t);
			}
			Property pt = tail();
			if (pt != null && pt.label == label) {
				Type merge = merge(pt.type, t);
				if (merge != null) {
					pt.type = merge;
					return;
				}
			}
			pt = new Property(label, t);
			fields.add(pt);
		}

		Type merge(Type t1, Type t2) {
			if (t2 instanceof ZeroMoreType) {
				if (t1 instanceof OptionType) {
					if (((OptionType) t1).type.equals(((ZeroMoreType) t2).type)) {
						return t2;
					}
					return null;
				}
				if (t1.equals(((ZeroMoreType) t2).type)) {
					return Schema.newOneMoreType(t1);
				}
			}
			return null;
		}

		Type newObjectType() {
			if (this.left == null && tags.size() == 1) {
				return new Schema.ObjectType(tags.get(0), fields.compactArray());
			}
			UList<Type> unionList = new UList<>(new Type[tags.size()]);
			if (left != null) {
				unionList.add(left);
			}
			for (String tag : tags) {
				unionList.add(new Schema.ObjectType(tag, fields.compactArray()));
			}
			return new Schema.UnionType(unionList.compactArray());
		}

		public Type newUnionType(Type[] compactArray) {
			return new Schema.UnionType(compactArray);
		}

	}

	class TypingVisitor extends Expression.Visitor {

		Type leftType = null;
		Production typingProduction;
		TreeState state = new TreeState(null);

		public Type typing(Production p) {
			if (typeState.isTree(p)) {
				this.leftType = null;
				this.typingProduction = p;
				typing(p.getExpression());
				schema.add(p.getUniqueName(), this.leftType);
				System.out.println(p.getUniqueName() + ": " + this.leftType);
				return this.leftType;
			}
			return null;
		}

		void addProperty(Symbol label, Expression e) {
			state.addProperty(label, typing(e));
		}

		Type typing(Expression e) {
			return (Type) e.visit(this, null);
		}

		@Override
		public Object visitNonTerminal(NonTerminal e, Object a) {
			if (typeState.isUnit(e)) {
				return null; // unit
			}
			if (typeState.isTree(e)) {
				leftType = new Schema.ReferenceType(schema, nname(e.getProduction()));
				return leftType;
			}
			typing(e.deReference());
			return null;
		}

		@Override
		public Object visitEmpty(Empty e, Object a) {
			return null;
		}

		@Override
		public Object visitFail(Fail e, Object a) {
			return null;
		}

		@Override
		public Object visitByte(Byte e, Object a) {
			return null;
		}

		@Override
		public Object visitByteSet(ByteSet e, Object a) {
			return null;
		}

		@Override
		public Object visitAny(Any e, Object a) {
			return null;
		}

		@Override
		public Object visitMultiByte(MultiByte e, Object a) {
			return null;
		}

		@Override
		public Object visitPair(Pair e, Object a) {
			for (Expression sub : e) {
				typing(sub);
			}
			return null;
		}

		@Override
		public Object visitSequence(Sequence e, Object a) {
			for (Expression sub : e) {
				typing(sub);
			}
			return null;
		}

		@Override
		public Object visitChoice(Choice e, Object a) {
			if (typeState.isUnit(e)) {
				return null;
			}
			if (typeState.isTree(e)) {
				UList<Type> unionList = new UList<>(new Type[e.size()]);
				for (Expression sub : e) {
					Type t = this.leftType;
					typing(sub);
					if (t != this.leftType) {
						unionList.add(this.leftType);
						this.leftType = t;
					}
				}
				if (unionList.size() == 1) {
					this.leftType = unionList.get(0);
				} else if (unionList.size() > 1) {
					this.leftType = state.newUnionType(unionList.compactArray());
				}
				return null;
			}
			state.inOption = true;
			for (Expression sub : e) {
				typing(sub);
			}
			state.inOption = false;
			return null;
		}

		@Override
		public Object visitOption(Option e, Object a) {
			state.inOption = true;
			Object o = typing(e.get(0));
			state.inOption = false;
			return o;
		}

		boolean inRepetition = false;

		@Override
		public Object visitZeroMore(ZeroMore e, Object a) {
			state.inZeroMore = true;
			inRepetition = true;
			Object o = typing(e.get(0));
			state.inZeroMore = false;
			inRepetition = false;
			return o;
		}

		@Override
		public Object visitOneMore(OneMore e, Object a) {
			state.inOneMore = true;
			inRepetition = true;
			Object o = typing(e.get(0));
			state.inOneMore = false;
			inRepetition = false;
			return o;
		}

		@Override
		public Object visitAnd(And e, Object a) {
			typing(e.get(0));
			return null;
		}

		@Override
		public Object visitNot(Not e, Object a) {
			return null;
		}

		/* Tree */

		@Override
		public Object visitBeginTree(BeginTree e, Object a) {
			state = new TreeState(state);
			return null;
		}

		@Override
		public Object visitEndTree(EndTree e, Object a) {
			if (e.tag != null) {
				state.addTag(e.tag);
			}
			TreeState ts = state;
			state = state.prev;
			this.leftType = ts.newObjectType();
			return null;
		}

		@Override
		public Object visitLinkTree(LinkTree e, Object a) {
			addProperty(e.label, e.get(0));
			return null;
		}

		@Override
		public Object visitFoldTree(FoldTree e, Object a) {
			state = new TreeState(state);
			state.left = this.leftType;
			state.addProperty(e.label, this.inRepetition ? new Schema.ReferenceType(schema, nname(this.typingProduction)) : leftType);
			return null;
		}

		@Override
		public Object visitTag(Tag e, Object a) {
			if (state != null) {
				state.addTag(e.tag);
			}
			return null;
		}

		@Override
		public Object visitReplace(Replace e, Object a) {
			return null;
		}

		@Override
		public Object visitDetree(Detree e, Object a) {
			return null;
		}

		@Override
		public Object visitBlockScope(BlockScope e, Object a) {
			typing(e.get(0));
			return null;
		}

		@Override
		public Object visitLocalScope(LocalScope e, Object a) {
			typing(e.get(0));
			return null;
		}

		@Override
		public Object visitSymbolAction(SymbolAction e, Object a) {
			typing(e.get(0));
			return null;
		}

		@Override
		public Object visitSymbolPredicate(SymbolPredicate e, Object a) {
			typing(e.get(0));
			return null;
		}

		@Override
		public Object visitSymbolMatch(SymbolMatch e, Object a) {
			return null;
		}

		@Override
		public Object visitSymbolExists(SymbolExists e, Object a) {
			return null;
		}

		@Override
		public Object visitIf(IfCondition e, Object a) {
			return null;
		}

		@Override
		public Object visitOn(OnCondition e, Object a) {
			return null;
		}

		@Override
		public Object visitScan(Scan e, Object a) {
			typing(e.get(0));
			return null;
		}

		@Override
		public Object visitRepeat(Repeat e, Object a) {
			state.inZeroMore = true;
			typing(e.get(0));
			state.inZeroMore = false;
			return null;
		}

		@Override
		public Object visitLabel(Label e, Object a) {
			return null;
		}
	}
}
