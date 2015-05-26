package nez.vm;

import nez.lang.Expression;
import nez.lang.Production;

class ProductionCode {
	Production production;
	Expression localExpression;
	Instruction codePoint;
	int start;
	int end;
	int ref = 0;
	ProductionCode(Production p) {
		this.production = p;
		this.localExpression = p.getExpression();
	}
}