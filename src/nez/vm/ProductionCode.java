package nez.vm;

import nez.lang.Expression;
import nez.lang.Production;

public class ProductionCode {
	Production production;
	Expression localExpression;
	Instruction compiled;

	int ref = 0;
	boolean     inlining = false;
	MemoPoint   memoPoint = null;
	boolean state;
	
	int start;
	int end;
	Instruction memoStart = null;

	ProductionCode(Production p, Expression local) {
		this.production = p;
		this.localExpression = local;
		this.state = p.isContextual();
	}

	public final String getLocalName() {
		return this.production.getLocalName();
	}

}