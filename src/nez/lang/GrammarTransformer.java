package nez.lang;

import nez.lang.Nez.And;
import nez.lang.Nez.Any;
import nez.lang.Nez.BlockScope;
import nez.lang.Nez.Byte;
import nez.lang.Nez.ByteSet;
import nez.lang.Nez.Choice;
import nez.lang.Nez.Detree;
import nez.lang.Nez.Empty;
import nez.lang.Nez.Fail;
import nez.lang.Nez.If;
import nez.lang.Nez.LeftFold;
import nez.lang.Nez.Link;
import nez.lang.Nez.LocalScope;
import nez.lang.Nez.MultiByte;
import nez.lang.Nez.New;
import nez.lang.Nez.Not;
import nez.lang.Nez.On;
import nez.lang.Nez.OneMore;
import nez.lang.Nez.Option;
import nez.lang.Nez.Pair;
import nez.lang.Nez.PreNew;
import nez.lang.Nez.Replace;
import nez.lang.Nez.Sequence;
import nez.lang.Nez.SymbolAction;
import nez.lang.Nez.SymbolExists;
import nez.lang.Nez.SymbolMatch;
import nez.lang.Nez.SymbolPredicate;
import nez.lang.Nez.Tag;
import nez.lang.Nez.ZeroMore;

public abstract class GrammarTransformer extends Expression.Visitor {

	private Expression inner(Expression e) {
		return (Expression) e.get(0).visit(this, null);
	}

	private Expression sub(Expression e, int i) {
		return (Expression) e.get(i).visit(this, null);
	}

	// @Override
	// public Expression visitNonTerminal(NonTerminal e, Object a) {
	// NonTerminal e2 = null; // FIXME
	// e2.setSourceLocation(e.getSourceLocation());
	// return e2;
	// }

	@Override
	public Expression visitEmpty(Empty e, Object a) {
		if (e.getSourceLocation() == null) {
			return e;
		}
		Empty e2 = new Empty();
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitFail(Fail e, Object a) {
		if (e.getSourceLocation() == null) {
			return e;
		}
		Fail e2 = new Fail();
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitByte(Byte e, Object a) {
		if (e.getSourceLocation() == null) {
			return e;
		}
		Byte e2 = new Byte(e.byteChar);
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitByteSet(ByteSet e, Object a) {
		if (e.getSourceLocation() == null) {
			return e;
		}
		ByteSet e2 = new ByteSet(e.byteMap);
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitAny(Any e, Object a) {
		if (e.getSourceLocation() == null) {
			return e;
		}
		Any e2 = new Any();
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitMultiByte(MultiByte e, Object a) {
		if (e.getSourceLocation() == null) {
			return e;
		}
		MultiByte e2 = new MultiByte(e.byteSeq);
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitPair(Pair e, Object a) {
		Pair e2 = new Pair(sub(e, 0), sub(e, 1));
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitSequence(Sequence e, Object a) {
		Expression[] inners = new Expression[e.size()];
		for (int i = 0; i < inners.length; i++) {
			inners[i] = sub(e, i);
		}
		Sequence e2 = new Sequence(inners);
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitChoice(Choice e, Object a) {
		Expression[] inners = new Expression[e.size()];
		for (int i = 0; i < inners.length; i++) {
			inners[i] = sub(e, i);
		}
		Choice e2 = new Choice(inners);
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitOption(Option e, Object a) {
		Option e2 = new Option(inner(e));
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitZeroMore(ZeroMore e, Object a) {
		ZeroMore e2 = new ZeroMore(inner(e));
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitOneMore(OneMore e, Object a) {
		OneMore e2 = new OneMore(inner(e));
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitAnd(And e, Object a) {
		And e2 = new And(inner(e));
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitNot(Not e, Object a) {
		Not e2 = new Not(inner(e));
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitPreNew(PreNew e, Object a) {
		PreNew e2 = new PreNew(e.shift);
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitLeftFold(LeftFold e, Object a) {
		LeftFold e2 = new LeftFold(e.shift, e.label);
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitLink(Link e, Object a) {
		Link e2 = new Link(e.label, inner(e));
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitTag(Tag e, Object a) {
		if (e.getSourceLocation() == null) {
			return e;
		}
		Tag e2 = new Tag(e.tag);
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitReplace(Replace e, Object a) {
		if (e.getSourceLocation() == null) {
			return e;
		}
		Replace e2 = new Replace(e.value);
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitNew(New e, Object a) {
		New e2 = new New(e.shift);
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitDetree(Detree e, Object a) {
		Detree e2 = new Detree(inner(e));
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitBlockScope(BlockScope e, Object a) {
		BlockScope e2 = new BlockScope(inner(e));
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitLocalScope(LocalScope e, Object a) {
		LocalScope e2 = new LocalScope(e.tableName, inner(e));
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitSymbolAction(SymbolAction e, Object a) {
		SymbolAction e2 = new SymbolAction(e.op, (NonTerminal) inner(e));
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitSymbolPredicate(SymbolPredicate e, Object a) {
		SymbolPredicate e2 = new SymbolPredicate(e.op, e.tableName, inner(e));
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitSymbolMatch(SymbolMatch e, Object a) {
		SymbolMatch e2 = new SymbolMatch(e.op, e.tableName);
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitSymbolExists(SymbolExists e, Object a) {
		SymbolExists e2 = new SymbolExists(e.tableName, e.symbol);
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitIf(If e, Object a) {
		If e2 = new If(e.predicate, e.flagName);
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitOn(On e, Object a) {
		On e2 = new On(e.predicate, e.flagName, inner(e));
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

}
