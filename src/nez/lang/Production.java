package nez.lang;

import java.util.TreeMap;

import nez.ast.SourcePosition;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Instruction;
import nez.vm.NezCompiler;

public class Production extends Expression {
	NameSpace  ns;
	String     name;
	String     uname;
	Expression body;
	
	boolean isPublic  = false;
	boolean isInline  = false;
	boolean isRecursive = false;
	int     refCount  = 0;
	boolean isTerminal = false;
	
	Production original;

	public Production(SourcePosition s, NameSpace ns, String name, Expression body) {
		super(s);
		this.ns = ns;
		this.name = name;
		this.uname = ns.uniqueName(name);
		this.body = (body == null) ? GrammarFactory.newEmpty(s) : body;
		this.original = this;
		this.minlen = StructualAnalysis.quickConsumedCheck(body);
	}

	private Production(String name, Production original, Expression body) {
		super(original.s);
		this.ns = original.getNameSpace();
		this.name = name;
		this.uname = ns.uniqueName(name);
		this.body = (body == null) ? GrammarFactory.newEmpty(s) : body;
		this.original = original;
		this.minlen = StructualAnalysis.quickConsumedCheck(body);
	}

	Production newProduction(String localName) {
		return new Production(name, this, this.getExpression());
	}
	

	public final NameSpace getNameSpace() {
		return this.ns;
	}
	
	public final boolean isPublic() {
		return this.isPublic;
	}

	public final boolean isInline() {
		return this.isInline || (!isPublic && !isRecursive && this.refCount == 1);
	}

	public final boolean isRecursive() {
		return this.isRecursive;
	}


	
	@Override
	public Expression get(int index) {
		return body;
	}
	
	@Override
	public int size() {
		return 1;
	}
	
	public final String getLocalName() {
		return this.name;
	}
	
	public final String getUniqueName() {
		return this.uname;
	}

	public final String getOriginalLocalName() {
		return this.original.getLocalName();
	}
	
	public final Expression getExpression() {
		return this.body;
	}

	public final void setExpression(Expression e) {
		this.body = e;
	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeProduction(this);
	}
	
	int minlen = -1;

	@Override
	public boolean isConsumed(Stacker stacker) {
		if(minlen == -1) {
			this.minlen = this.getExpression().isConsumed(stacker) ? 1 : 0;
		}
		return minlen > 0;
	}
	
	@Override
	public final boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		if(stack != null && this.minlen != 0 && stack.size() > 0) {
			for(String n : stack) { // Check Unconsumed Recursion
				if(uname.equals(n)) {
					this.minlen = 0;
					break;
				}
			}
		}
		if(minlen == -1) {
			if(stack == null) {
				stack = new UList<String>(new String[4]);
			}
			if(startNonTerminal == null) {
				startNonTerminal = this.uname;
			}
			stack.add(this.uname);
			this.minlen = this.body.checkAlwaysConsumed(checker, startNonTerminal, stack) ? 1 : 0;
			stack.pop();
		}
		return minlen > 0;
	}
	
	public int transType = Typestate.Undefined;
	
	public final boolean isPurePEG() {
		return this.transType == Typestate.BooleanType;
	}

	@Override
	public int inferTypestate(UMap<String> visited) {
		if(this.transType != Typestate.Undefined) {
			return this.transType;
		}
		if(visited != null) {
			if(visited.hasKey(uname)) {
				this.transType = Typestate.BooleanType;
				return this.transType;
			}
		}
		else {
			visited = new UMap<String>();
		}
		visited.put(uname, uname);
		int t = body.inferTypestate(visited);
		assert(t != Typestate.Undefined);
		if(this.transType == Typestate.Undefined) {
			this.transType = t;
		}
		else {
			assert(transType == t);
		}
		return this.transType;
	}

//
//	public final void removeExpressionFlag(TreeMap<String, String> undefedFlags) {
//		this.body = this.body.removeFlag(undefedFlags).intern();
//	}
//	
//	@Override
//	public Expression removeFlag(TreeMap<String, String> undefedFlags) {
//		if(undefedFlags.size() > 0) {
//			StringBuilder sb = new StringBuilder();
//			int loc = name.indexOf('!');
//			if(loc > 0) {
//				sb.append(this.name.substring(0, loc));
//			}
//			else {
//				sb.append(this.name);
//			}
//			for(String flag: undefedFlags.keySet()) {
//				if(ConditionAnlysis.hasReachableFlag(this.body, flag)) {
//					sb.append("!");
//					sb.append(flag);
//				}
//			}
//			String rName = sb.toString();
//			Production rRule = ns.getProduction(rName);
//			if(rRule == null) {
//				rRule = ns.newRule(rName, Factory.newEmpty(null));
//				rRule.body = body.removeFlag(undefedFlags).intern();
//			}
//			return rRule;
//		}
//		return this;
//	}
	
	@Override
	public String key() {
		return this.getUniqueName() + "=";
	}
	
	public final void internRule() {
		this.body = this.body.intern();
	}

	@Override
	public String getPredicate() {
		return this.getUniqueName() + "=";
	}

	private int dfaOption = -1;
	private short[] dfaCache = null;

	@Override
	public short acceptByte(int ch, int option) {
		option = Grammar.mask(option);
		if(option != dfaOption) {
			if(dfaCache == null) {
				dfaCache = new short[257];
			}
			for(int c = 0; c < dfaCache.length; c++) {
				dfaCache[c] = this.body.acceptByte(c, option);
			}
			this.dfaOption = option;
		}
		return dfaCache[ch]; 
	}

	
	@Override
	public Instruction encode(NezCompiler bc, Instruction next, Instruction failjump) {
		return this.getExpression().encode(bc, next, failjump);
	}

	@Override
	protected int pattern(GEP gep) {
		return body.pattern(gep);
	}
	
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		body.examplfy(gep, sb, p);
	}

	public final void dump() {
		StringBuilder sb = new StringBuilder();
		if(this.isPublic) {
			sb.append("public ");
		}
		if(this.isInline()) {
			sb.append("inline ");
		}
		if(this.isRecursive) {
			sb.append("recursive ");
		}
		if(!this.isAlwaysConsumed()) {
			sb.append("unconsumed ");
		}
		boolean[] b = ByteMap.newMap(true);
		for(int c = 0; c < b.length; c++) {
			if(this.acceptByte(c, 0) == Prediction.Reject) {
				b[c] = false;
			}
		}
		sb.append("ref(" + this.refCount + ") ");
		sb.append("accept" + StringUtils.stringfyCharClass(b) + " ");
		
		ConsoleUtils.println(sb.toString());
		ConsoleUtils.println(this.getLocalName() + " = " + this.getExpression());
	}

}

class Stacker {
	Stacker prev;
	Production p;
	Stacker(Production p, Stacker prev) {
		this.prev = prev;
		this.p = p;
	}
	boolean isVisited(Production p) {
		Stacker d = this;
		while(d != null) {
			if(d.p == p) {
				return true;
			}
			d = d.prev;
		}
		return false;
	}
}

