package nez.junks;

import nez.lang.Expression;
import nez.lang.Production;
import nez.parser.MemoPoint;
import nez.parser.ParserCode.ProductionCode;
import nez.parser.moz.MozInst;

public class ParserGrammarFunc extends ProductionCode<MozInst> {
	String name;
	// Production grammarProduction;
	Production parserProduction;

	int refcount;
	boolean inlining;
	boolean state;
	MemoPoint memoPoint = null;

	ParserGrammarFunc(String uname, Production p, Production pp, int init) {
		super(null);
		this.name = uname;
		this.refcount = 0;
		// this.grammarProduction = p;
		this.parserProduction = pp;
		this.refcount = init;
	}

	public Expression getExpression() {
		return this.parserProduction.getExpression();
	}

	public void incCount() {
		this.refcount++;
	}

	public void resetCount() {
		this.refcount = 0;
	}

	public final int getCount() {
		return this.refcount;
	}

	public final MemoPoint getMemoPoint() {
		return this.memoPoint;
	}

	public final boolean isStateful() {
		return this.state;
	}

	public final boolean isInlined() {
		return this.inlining;
	}
}