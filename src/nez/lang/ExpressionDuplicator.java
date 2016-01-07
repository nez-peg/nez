package nez.lang;

import java.util.List;

import nez.lang.Nez.And;
import nez.lang.Nez.BeginTree;
import nez.lang.Nez.BlockScope;
import nez.lang.Nez.Choice;
import nez.lang.Nez.Detree;
import nez.lang.Nez.EndTree;
import nez.lang.Nez.FoldTree;
import nez.lang.Nez.IfCondition;
import nez.lang.Nez.LinkTree;
import nez.lang.Nez.LocalScope;
import nez.lang.Nez.Not;
import nez.lang.Nez.OnCondition;
import nez.lang.Nez.OneMore;
import nez.lang.Nez.Option;
import nez.lang.Nez.Pair;
import nez.lang.Nez.Replace;
import nez.lang.Nez.SymbolAction;
import nez.lang.Nez.SymbolExists;
import nez.lang.Nez.SymbolMatch;
import nez.lang.Nez.SymbolPredicate;
import nez.lang.Nez.Tag;
import nez.lang.Nez.ZeroMore;

public abstract class ExpressionDuplicator extends Expression.Visitor {

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
	public Expression visitEmpty(Nez.Empty e, Object a) {
		if (e.getSourceLocation() == null) {
			return e;
		}
		return Expressions.newEmpty(e.getSourceLocation());
	}

	@Override
	public Expression visitFail(Nez.Fail e, Object a) {
		if (e.getSourceLocation() == null) {
			return e;
		}
		return Expressions.newFailure(e.getSourceLocation());
	}

	@Override
	public Expression visitByte(Nez.Byte e, Object a) {
		if (e.getSourceLocation() == null) {
			return e;
		}
		return Expressions.newByte(e.getSourceLocation(), e.byteChar);
	}

	@Override
	public Expression visitByteSet(Nez.ByteSet e, Object a) {
		if (e.getSourceLocation() == null) {
			return e;
		}
		return Expressions.newByteSet(e.getSourceLocation(), e.byteMap);
	}

	@Override
	public Expression visitAny(Nez.Any e, Object a) {
		if (e.getSourceLocation() == null) {
			return e;
		}
		return Expressions.newAny(e.getSourceLocation());
	}

	@Override
	public Expression visitMultiByte(Nez.MultiByte e, Object a) {
		if (e.getSourceLocation() == null) {
			return e;
		}
		return Expressions.newMultiByte(e.getSourceLocation(), e.byteSeq);
	}

	@Override
	public Expression visitPair(Pair e, Object a) {
		return Expressions.newPair(e.getSourceLocation(), sub(e, 0), sub(e, 1));
	}

	@Override
	public Expression visitSequence(Nez.Sequence e, Object a) {
		List<Expression> l = Expressions.newList2(e.size());
		Expression[] inners = new Expression[e.size()];
		for (int i = 0; i < inners.length; i++) {
			Expressions.addSequence(l, sub(e, i));
		}
		return Expressions.newSequence(e.getSourceLocation(), l);
	}

	@Override
	public Expression visitChoice(Choice e, Object a) {
		List<Expression> l = Expressions.newList2(e.size());
		Expression[] inners = new Expression[e.size()];
		for (int i = 0; i < inners.length; i++) {
			Expressions.addChoice(l, sub(e, i));
		}
		return Expressions.newChoice(e.getSourceLocation(), l);
	}

	@Override
	public Expression visitOption(Option e, Object a) {
		return Expressions.newOption(e.getSourceLocation(), inner(e));
	}

	@Override
	public Expression visitZeroMore(ZeroMore e, Object a) {
		return Expressions.newZeroMore(e.getSourceLocation(), inner(e));
	}

	@Override
	public Expression visitOneMore(OneMore e, Object a) {
		return Expressions.newOneMore(e.getSourceLocation(), inner(e));
	}

	@Override
	public Expression visitAnd(And e, Object a) {
		return Expressions.newAnd(e.getSourceLocation(), inner(e));
	}

	@Override
	public Expression visitNot(Not e, Object a) {
		return Expressions.newNot(e.getSourceLocation(), inner(e));
	}

	@Override
	public Expression visitBeginTree(BeginTree e, Object a) {
		return Expressions.newBeginTree(e.getSourceLocation(), e.shift);
	}

	@Override
	public Expression visitFoldTree(FoldTree e, Object a) {
		return Expressions.newFoldTree(e.getSourceLocation(), e.label, e.shift);
	}

	@Override
	public Expression visitLink(LinkTree e, Object a) {
		return Expressions.newLinkTree(e.getSourceLocation(), e.label, inner(e));
	}

	@Override
	public Expression visitTag(Tag e, Object a) {
		if (e.getSourceLocation() == null) {
			return e;
		}
		return Expressions.newTag(e.getSourceLocation(), e.tag);
	}

	@Override
	public Expression visitReplace(Replace e, Object a) {
		if (e.getSourceLocation() == null) {
			return e;
		}
		return Expressions.newReplace(e.getSourceLocation(), e.value);
	}

	@Override
	public Expression visitEndTree(EndTree e, Object a) {
		return Expressions.newEndTree(e.getSourceLocation(), e.shift);
	}

	@Override
	public Expression visitDetree(Detree e, Object a) {
		return Expressions.newDetree(e.getSourceLocation(), inner(e));
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
	public Expression visitIf(IfCondition e, Object a) {
		IfCondition e2 = new IfCondition(e.predicate, e.flagName);
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

	@Override
	public Expression visitOn(OnCondition e, Object a) {
		OnCondition e2 = new OnCondition(e.predicate, e.flagName, inner(e));
		e2.setSourceLocation(e.getSourceLocation());
		return e2;
	}

}
