package nez.lang;

import nez.util.StringUtils;

public class NameAnalysis extends GrammarReshaper {
	// <applay Statement Expr Expr>
	// <any ![e]> i'hello'
	
	public Expression reshapeProduction(Production p) {
		if(p.isTerminal()) {
			return p;
		}
		p.setExpression(p.getExpression().reshape(this));
		return p;
	}
	
	public Expression reshapeNonTerminal(NonTerminal p) {
		Production r = p.getProduction();
		if(r == null) {
			if(p.isTerminal()) {
				p.getNameSpace().reportNotice(p, "undefined terminal: " + p.getLocalName());
				return GrammarFactory.newString(p.s, StringUtils.unquoteString(p.getLocalName()));
			}
			p.getNameSpace().reportWarning(p, "undefined rule: " + p.getLocalName());
			r = p.getNameSpace().defineProduction(p.s, p.getLocalName(), GrammarFactory.newEmpty(p.s));
		}
		if(p.isTerminal()) {
			return r.getExpression().reshape(this);  // inlining terminal
		}
		return p;
	}
}

class StructualAnalysis extends GrammarReshaper {
}
