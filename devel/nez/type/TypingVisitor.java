package nez.type;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.TreeSet;

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
import nez.util.UList;

public class TypingVisitor extends Expression.Visitor {

	final TypestateAnalyzer typeState = Typestate.newAnalyzer();

	HashMap<String, Type> typeMap = new HashMap<>();
	Type leftType = null;
	Production typingProduction;

	public void typing(Grammar g) {
		for (Production p : g) {
			typing(p);
		}
	}

	public void typing(Production p) {
		if (typeState.isTree(p)) {
			this.leftType = null;
			this.typingProduction = p;
			visit(p.getExpression());
			typeMap.put(p.getUniqueName(), this.leftType);
			System.out.println(p.getUniqueName() + ": " + this.leftType);
		}
	}

	static class TreeState {
		TreeState prev;
		boolean inOption;
		boolean inZeroMore;
		boolean inOneMore;
		Type left = null;
		TreeSet<String> tags = new TreeSet<>();
		UList<Property> fields = new UList<>(new Property[2]);

		TreeState(TreeState prev) {
			this.prev = prev;
		}

		void addTag(Symbol tag) {
			tags.add(tag.toString());
		}

		void addProperty(Symbol label, Type t) {
			if (t == null) {
				return;
			}
			Property pt = new Property(label, t);
			if (inOption) {
				pt.type = OptionType.enforce(pt.type);
			}
			if (inOneMore) {
				pt.type = OneMoreType.enforce(pt.type);
			}
			if (inZeroMore) {
				pt.type = ZeroMoreType.enforce(pt.type);
			}
			fields.add(pt);
		}

		Type newType() {
			if (this.left == null && tags.size() == 1) {
				return new TreeType(tags.first(), fields.compactArray());
			}
			UList<Type> unionList = new UList<>(new Type[tags.size()]);
			if (left != null) {
				unionList.add(left);
			}
			for (String tag : tags) {
				unionList.add(new TreeType(tag, fields.compactArray()));
			}
			return new UnionType(unionList.compactArray());
		}
	}

	void addProperty(Symbol label, Expression e) {
		if (state != null) {
			state.addProperty(label, typing(e));
		}
	}

	TreeState state = null;

	Type typing(Expression e) {
		return (Type) e.visit(this, null);
	}

	private void visit(Expression e) {
		e.visit(this, null);
	}

	@Override
	public Object visitNonTerminal(NonTerminal e, Object a) {
		if (typeState.isUnit(e)) {
			return null; // unit
		}
		if (typeState.isTree(e)) {
			leftType = new NonTerminalType(e);
			return leftType;
		}
		visit(e.deReference());
		return null;
	}

	@Override
	public Object visitEmpty(Empty e, Object a) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitFail(Fail e, Object a) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitByte(Byte e, Object a) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitByteSet(ByteSet e, Object a) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitAny(Any e, Object a) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitMultiByte(MultiByte e, Object a) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitPair(Pair e, Object a) {
		for (Expression sub : e) {
			visit(sub);
		}
		return null;
	}

	@Override
	public Object visitSequence(Sequence e, Object a) {
		for (Expression sub : e) {
			visit(sub);
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
				visit(sub);
				if (t != this.leftType) {
					unionList.add(this.leftType);
					this.leftType = t;
				}
			}
			if (unionList.size() == 1) {
				this.leftType = unionList.get(0);
			} else if (unionList.size() > 1) {
				this.leftType = new UnionType(unionList.compactArray());
			}
			return null;
		}
		if (state != null) {
			state.inOption = true;
		}
		for (Expression sub : e) {
			visit(sub);
		}
		if (state != null) {
			state.inOption = false;
		}
		return null;
	}

	@Override
	public Object visitOption(Option e, Object a) {
		if (state != null) {
			state.inOption = true;
			visit(e.get(0));
			state.inOption = false;
		} else {
			visit(e.get(0));
		}
		return null;
	}

	boolean inRepetition = false;

	@Override
	public Object visitZeroMore(ZeroMore e, Object a) {
		if (state != null) {
			state.inZeroMore = true;
			visit(e.get(0));
			state.inZeroMore = false;
		} else {
			inRepetition = true;
			visit(e.get(0));
			inRepetition = false;
		}
		return null;
	}

	@Override
	public Object visitOneMore(OneMore e, Object a) {
		if (state != null) {
			state.inZeroMore = true;
			visit(e.get(0));
			state.inZeroMore = false;
		} else {
			inRepetition = true;
			visit(e.get(0));
			inRepetition = false;
		}
		return null;
	}

	@Override
	public Object visitAnd(And e, Object a) {
		visit(e.get(0));
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
		this.leftType = ts.newType();
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
		state.addProperty(e.label, this.inRepetition ? new NonTerminalType(this.typingProduction) : leftType);
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
		visit(e.get(0));
		return null;
	}

	@Override
	public Object visitLocalScope(LocalScope e, Object a) {
		visit(e.get(0));
		return null;
	}

	@Override
	public Object visitSymbolAction(SymbolAction e, Object a) {
		visit(e.get(0));
		return null;
	}

	@Override
	public Object visitSymbolPredicate(SymbolPredicate e, Object a) {
		visit(e.get(0));
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
		visit(e.get(0));
		return null;
	}

	@Override
	public Object visitRepeat(Repeat e, Object a) {
		if (state != null) {
			state.inZeroMore = true;
			visit(e.get(0));
			state.inZeroMore = false;
		} else {
			visit(e.get(0));
		}
		return null;
	}

	@Override
	public Object visitLabel(Label e, Object a) {
		return null;
	}

}
