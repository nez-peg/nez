package nez.expr;

import nez.util.UList;

public class Tracer {
	class Trace {
		Expression e;
		int pos;
		int count = 0;
		boolean redundant = false;
		Trace(Expression e, int pos) {
			this.e = e;
			this.pos = pos;
		}
	}
	UList<Trace> path = new UList<Trace>(new Trace[128]);
	Tracer() {
		
	}

	boolean checkRedundantCall(Expression e, int pos) {
		boolean r = false;
		for(Trace t : this.path) {
			if(t.e == e && t.pos <= pos) {
				t.redundant = true;
				r = true;
			}
		}
		return r;
	}
	
	boolean isRecursivelyVisited(NonTerminal e) {
		for(int i = path.size() - 1; i >= 0; i--) {
			if(path.ArrayValues[i].e == e) {
				path.ArrayValues[i].count += 1;
				return true;
			}
		}
		return false;
	}
	
	void push(Expression e, int pos) {
		path.add(new Trace(e, pos));
	}
	
	int count(Expression e) {
		return (e.isAlwaysConsumed()) ? 1 : 0;
	}

	void checkBacktrack(Expression e, int pos) {
	}
	
}
