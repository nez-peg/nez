package nez.lang;

import java.util.List;

import nez.ast.SourceLocation;
import nez.lang.Nez.Detree;
import nez.lang.Nez.EndTree;
import nez.lang.Nez.Replace;
import nez.util.UList;

public abstract class ExpressionDuplicationVisitor extends Expression.Visitor {

	protected boolean enforcedSequence = false;
	protected boolean enforcedPair = false;

	protected boolean enableImmutableDuplication = false;

	public Expression visit(Expression e) {
		SourceLocation s = e.getSourceLocation();
		e = (Expression) e.visit(this, null);
		if (s != null) {
			e.setSourceLocation(s);
		}
		return e;
	}

	private Expression inner(Expression e) {
		return visit(e.get(0));
	}

	private Expression sub(Expression e, int i) {
		return visit(e.get(i));
	}

	// @Override
	// public Expression visitNonTerminal(NonTerminal e, Object a) {
	// NonTerminal e2 = null; // FIXME
	// e2.setSourceLocation(e.getSourceLocation());
	// return e2;
	// }

	@Override
	public Expression visitEmpty(Nez.Empty e, Object a) {
		if (!enableImmutableDuplication && e.getSourceLocation() == null) {
			return e;
		}
		return new Nez.Empty();
	}

	@Override
	public Expression visitFail(Nez.Fail e, Object a) {
		if (!enableImmutableDuplication && e.getSourceLocation() == null) {
			return e;
		}
		return new Nez.Fail();
	}

	@Override
	public Expression visitByte(Nez.Byte e, Object a) {
		if (!enableImmutableDuplication && e.getSourceLocation() == null) {
			return e;
		}
		return new Nez.Byte(e.byteChar);
	}

	@Override
	public Expression visitByteSet(Nez.ByteSet e, Object a) {
		if (!enableImmutableDuplication && e.getSourceLocation() == null) {
			return e;
		}
		return new Nez.ByteSet(e.byteMap);
	}

	@Override
	public Expression visitAny(Nez.Any e, Object a) {
		if (!enableImmutableDuplication && e.getSourceLocation() == null) {
			return e;
		}
		return new Nez.Any();
	}

	@Override
	public Expression visitMultiByte(Nez.MultiByte e, Object a) {
		if (!enableImmutableDuplication && e.getSourceLocation() == null) {
			return e;
		}
		return new Nez.MultiByte(e.byteSeq);
	}

	@Override
	public Expression visitPair(Nez.Pair e, Object a) {
		if (enforcedSequence) {
			List<Expression> l = Expressions.flatten(e);
			UList<Expression> l2 = Expressions.newUList(l.size());
			for (int i = 0; i < l.size(); i++) {
				Expressions.addSequence(l2, visit(l.get(i)));
			}
			return new Nez.Sequence(l2.compactArray());
		}
		return new Nez.Pair(sub(e, 0), sub(e, 1));
	}

	@Override
	public Expression visitSequence(Nez.Sequence e, Object a) {
		if (enforcedPair) {
			List<Expression> l = Expressions.flatten(e);
			UList<Expression> l2 = Expressions.newUList(l.size());
			for (int i = 0; i < l.size(); i++) {
				Expressions.addSequence(l2, visit(l.get(i)));
			}
			return Expressions.newPair(l2);
		}
		UList<Expression> l = Expressions.newUList(e.size());
		for (Expression sub : e) {
			Expressions.addSequence(l, visit(sub));
		}
		return new Nez.Sequence(l.compactArray());

	}

	@Override
	public Expression visitChoice(Nez.Choice e, Object a) {
		UList<Expression> l = Expressions.newUList(e.size());
		for (Expression sub : e) {
			Expressions.addChoice(l, visit(sub));
		}
		return new Nez.Choice(l.compactArray());
	}

	@Override
	public Expression visitOption(Nez.Option e, Object a) {
		return new Nez.Option(inner(e));
	}

	@Override
	public Expression visitZeroMore(Nez.ZeroMore e, Object a) {
		return new Nez.ZeroMore(inner(e));
	}

	@Override
	public Expression visitOneMore(Nez.OneMore e, Object a) {
		return new Nez.OneMore(inner(e));
	}

	@Override
	public Expression visitAnd(Nez.And e, Object a) {
		return new Nez.And(inner(e));
	}

	@Override
	public Expression visitNot(Nez.Not e, Object a) {
		return new Nez.Not(inner(e));
	}

	@Override
	public Expression visitBeginTree(Nez.BeginTree e, Object a) {
		return new Nez.BeginTree(e.shift);
	}

	@Override
	public Expression visitEndTree(EndTree e, Object a) {
		return new Nez.EndTree(e.shift);
	}

	@Override
	public Expression visitFoldTree(Nez.FoldTree e, Object a) {
		return new Nez.FoldTree(e.shift, e.label);
	}

	@Override
	public Expression visitLinkTree(Nez.LinkTree e, Object a) {
		return new Nez.LinkTree(e.label, inner(e));
	}

	@Override
	public Expression visitTag(Nez.Tag e, Object a) {
		if (!enableImmutableDuplication && e.getSourceLocation() == null) {
			return e;
		}
		return new Nez.Tag(e.tag);
	}

	@Override
	public Expression visitReplace(Replace e, Object a) {
		if (!enableImmutableDuplication && e.getSourceLocation() == null) {
			return e;
		}
		return new Nez.Replace(e.value);
	}

	@Override
	public Expression visitDetree(Detree e, Object a) {
		return new Nez.Detree(inner(e));
	}

	@Override
	public Expression visitBlockScope(Nez.BlockScope e, Object a) {
		return new Nez.BlockScope(inner(e));
	}

	@Override
	public Expression visitLocalScope(Nez.LocalScope e, Object a) {
		return new Nez.LocalScope(e.tableName, inner(e));
	}

	@Override
	public Expression visitSymbolAction(Nez.SymbolAction e, Object a) {
		return new Nez.SymbolAction(e.op, (NonTerminal) inner(e));
	}

	@Override
	public Expression visitSymbolPredicate(Nez.SymbolPredicate e, Object a) {
		return new Nez.SymbolPredicate(e.op, (NonTerminal) e.get(0), e.tableName);
	}

	@Override
	public Expression visitSymbolMatch(Nez.SymbolMatch e, Object a) {
		return new Nez.SymbolMatch(e.op, (NonTerminal) e.get(0), e.tableName);
	}

	@Override
	public Expression visitSymbolExists(Nez.SymbolExists e, Object a) {
		return new Nez.SymbolExists(e.tableName, e.symbol);
	}

	@Override
	public Expression visitIf(Nez.IfCondition e, Object a) {
		return new Nez.IfCondition(e.predicate, e.flagName);
	}

	@Override
	public Expression visitOn(Nez.OnCondition e, Object a) {
		return new Nez.OnCondition(e.predicate, e.flagName, inner(e));
	}

}
