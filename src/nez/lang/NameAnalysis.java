package nez.lang;

import nez.util.StringUtils;

public class NameAnalysis extends Manipulator {
	// <applay Statement Expr Expr>
	// <any ![e]> i'hello'
	
	public Expression reshapeProduction(Production p) {
		if(p.isTerminal) {
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
				return Factory.newString(p.s, StringUtils.unquoteString(p.getLocalName()));
			}
			p.getNameSpace().reportWarning(p, "undefined rule: " + p.getLocalName());
			r = p.getNameSpace().defineProduction(p.s, p.getLocalName(), Factory.newEmpty(p.s));
		}
		r.refCount += 1;
		if(p.isTerminal()) {
			return r.getExpression().reshape(this);  // inlining terminal
		}
		return p;
//		if(!r.isRecursive) {
//			String u = r.getUniqueName();
//			if(u.equals(ruleName)) {
//				r.isRecursive = true;
//				if(r.isInline) {
//					checker.reportError(s, "recursion disallows inlining " + r.getLocalName());
//					r.isInline = false;
//				}
//			}
//			if(!visited.hasKey(u)) {
//				visited.put(u, ruleName);
//				checker.checkPhase1(r.getExpression(), ruleName, visited, depth+1);
//			}
//		}
	}
}

class StructualAnalysis extends Manipulator {

	//New
//	@Override void checkPhase2(GrammarChecker checker) {
//		if(this.lefted && this.outer == null) {
//			checker.reportError(s, "expected repetition for " + this);
//		}
//	}

	//Repetition
//	@Override void checkPhase2(GrammarChecker checker) {
//		if(!this.inner.checkAlwaysConsumed(checker, null, null)) {
//			checker.reportError(s, "unconsumed repetition");
//			this.possibleInfiniteLoop = true;
//		}
//	}

	public static int quickConsumedCheck(Expression e) {
		if(e == null) {
			return -1;
		}
		if(e instanceof NonTerminal ) {
			NonTerminal n = (NonTerminal)e;
			Production p = n.getProduction();
			if(p != null && p.minlen != -1) {
				return p.minlen;
			}
			return -1;  // unknown
		}
		if(e instanceof ByteChar || e instanceof AnyChar || e instanceof ByteMap) {
			return 1; /* next*/
		}
		if(e instanceof Choice) {
			int r = 1;
			for(Expression sub: e) {
				int minlen = quickConsumedCheck(sub);
				if(minlen == 0) {
					return 0;
				}
				if(minlen == -1) {
					r = -1;
				}
			}
			return r;
		}
		if(e instanceof Not || e instanceof Option || (e instanceof Repetition && !(e instanceof Repetition1)) || e instanceof And) {
			return 0;
		}
		int r = 0;
		for(Expression sub: e) {
			int minlen = quickConsumedCheck(sub);
			if(minlen > 0) {
				return minlen;
			}
			if(minlen == -1) {
				r = -1;
			}
		}
		return r;
	}
}
