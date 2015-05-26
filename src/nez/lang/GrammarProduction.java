package nez.lang;

class GrammarProduction {
	int ref = 1;
	Production p;
	Expression e;
	GrammarProduction(Production p) {
		this.p = p;
		this.e = p.getExpression();
	}
}