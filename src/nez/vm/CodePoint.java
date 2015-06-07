package nez.vm;

import nez.lang.Expression;
import nez.lang.Production;

public class CodePoint {
	Production production;
	Expression localExpression;
	Instruction nonmemoStart;
	int start;
	int end;
	int ref = 0;
	boolean     inlining = false;
	MemoPoint   memoPoint = null;
	Instruction memoStart = null;
	CodePoint(Production p, Expression local) {
		this.production = p;
		this.localExpression = local;
	}
}