package nez.lang;

import nez.lang.Nez.Byte;

public class ByteConsumption extends Expression.Visitor {

	static enum Result {
		Unconsumed, //
		Consumed, //
		Undecided;
	}

	public final boolean isConsumed(Production p) {
		Result c = deepCheck(p);
		return c == Result.Consumed;
	}

	public final boolean isConsumed(Expression p) {
		Result c = deepCheck(p);
		return c == Result.Consumed;
	}

	public Result quickCheck(Production p) {
		String uname = p.getUniqueName();
		if (this.isVisited(uname)) {
			return (Result) lookup(uname);
		}
		Result c = quickCheck(p.getExpression());
		if (c != Result.Undecided) {
			this.memo(uname, c);
		}
		return c;
	}

	public final Result quickCheck(Expression e) {
		return (Result) e.visit(this, true);
	}

	public Result deepCheck(Production p) {
		String uname = p.getUniqueName();
		if (this.isVisited(uname)) {
			return (Result) lookup(uname);
		}
		Result c = quickCheck(p.getExpression());
		this.memo(uname, c);
		if (c == Result.Undecided) {
			c = deepCheck(p.getExpression());
			this.memo(uname, c);
		}
		return c;
	}

	public final Result deepCheck(Expression e) {
		return (Result) e.visit(this, false);
	}

	private Result check(Expression e, Object a) {
		return (Result) e.visit(this, a);
	}

	@Override
	public Object visitNonTerminal(NonTerminal e, Object a) {
		if ((Boolean) a) {
			return Result.Undecided;
		}
		return this.deepCheck(e.getProduction());
	}

	@Override
	public Object visitEmpty(Nez.Empty e, Object a) {
		return Result.Unconsumed;
	}

	@Override
	public Object visitFail(Nez.Fail e, Object a) {
		return Result.Unconsumed;
	}

	@Override
	public Object visitByte(Byte e, Object a) {
		return Result.Consumed;
	}

	@Override
	public Object visitByteSet(Nez.ByteSet e, Object a) {
		return Result.Consumed;
	}

	@Override
	public Object visitAny(Nez.Any e, Object a) {
		return Result.Consumed;
	}

	@Override
	public Object visitMultiByte(Nez.MultiByte e, Object a) {
		return Result.Consumed;
	}

	@Override
	public Object visitPair(Nez.Pair e, Object a) {
		if (check(e.get(0), a) == Result.Consumed) {
			return Result.Consumed;
		}
		return check(e.get(1), a);
	}

	@Override
	public Object visitSequence(Nez.Sequence e, Object a) {
		boolean undecided = false;
		for (Expression sub : e) {
			Result c = check(sub, a);
			if (c == Result.Consumed) {
				return Result.Consumed;
			}
			if (c == Result.Undecided) {
				undecided = true;
			}
		}
		return undecided ? Result.Undecided : Result.Unconsumed;
	}

	@Override
	public Object visitChoice(Nez.Choice e, Object a) {
		boolean unconsumed = false;
		boolean undecided = false;
		for (Expression sub : e) {
			Result c = check(sub, a);
			if (c == Result.Consumed) {
				continue;
			}
			unconsumed = true;
			if (c == Result.Undecided) {
				undecided = true;
			}
		}
		if (!unconsumed) {
			return Result.Consumed;
		}
		return undecided ? Result.Undecided : Result.Unconsumed;
	}

	@Override
	public Object visitOption(Nez.Option e, Object a) {
		return Result.Unconsumed;
	}

	@Override
	public Object visitZeroMore(Nez.ZeroMore e, Object a) {
		return Result.Unconsumed;
	}

	@Override
	public Object visitOneMore(Nez.OneMore e, Object a) {
		return check(e.get(0), a);
	}

	@Override
	public Object visitAnd(Nez.And e, Object a) {
		return Result.Unconsumed;
	}

	@Override
	public Object visitNot(Nez.Not e, Object a) {
		return Result.Unconsumed;
	}

	@Override
	public Object visitBeginTree(Nez.BeginTree e, Object a) {
		return Result.Unconsumed;
	}

	@Override
	public Object visitFoldTree(Nez.FoldTree e, Object a) {
		return Result.Unconsumed;
	}

	@Override
	public Object visitLink(Nez.LinkTree e, Object a) {
		return check(e.get(0), a);
	}

	@Override
	public Object visitTag(Nez.Tag e, Object a) {
		return Result.Unconsumed;
	}

	@Override
	public Object visitReplace(Nez.Replace e, Object a) {
		return Result.Unconsumed;
	}

	@Override
	public Object visitEndTree(Nez.EndTree e, Object a) {
		return Result.Unconsumed;
	}

	@Override
	public Object visitDetree(Nez.Detree e, Object a) {
		return check(e.get(0), a);
	}

	@Override
	public Object visitBlockScope(Nez.BlockScope e, Object a) {
		return check(e.get(0), a);
	}

	@Override
	public Object visitLocalScope(Nez.LocalScope e, Object a) {
		return check(e.get(0), a);
	}

	@Override
	public Object visitSymbolAction(Nez.SymbolAction e, Object a) {
		return check(e.get(0), a);
	}

	@Override
	public Object visitSymbolMatch(Nez.SymbolMatch e, Object a) {
		return Result.Undecided;
	}

	@Override
	public Object visitSymbolPredicate(Nez.SymbolPredicate e, Object a) {
		return check(e.get(0), a);
	}

	@Override
	public Object visitSymbolExists(Nez.SymbolExists e, Object a) {
		return check(e.get(0), a);
	}

	@Override
	public Object visitIf(Nez.IfCondition e, Object a) {
		return Result.Unconsumed;
	}

	@Override
	public Object visitOn(Nez.OnCondition e, Object a) {
		return check(e.get(0), a);
	}
}
