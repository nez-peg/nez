package nez.lang;

import java.util.List;
import java.util.TreeMap;

import nez.ast.SourcePosition;
import nez.main.Verbose;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UFlag;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Production extends Expression {
	public final static int PublicProduction           = 1;
	public final static int TerminalProduction         = 1 << 1;
	public final static int InlineProduction           = 1 << 2;
	
	public final static int RecursiveChecked           = 1 << 3;
	public final static int RecursiveProduction        = 1 << 4;
	
	public final static int ConsumedChecked            = 1 << 5;
	public final static int ConsumedProduction         = 1 << 6;
	
	public final static int ASTChecked                 = 1 << 8;
	public final static int ObjectProduction           = 1 << 9;
	public final static int OperationalProduction      = 1 << 10;

	public final static int ConditionalChecked         = 1 << 16;
	public final static int ConditionalProduction      = 1 << 17;
	public final static int ContextualChecked          = 1 << 18;
	public final static int ContextualProduction       = 1 << 19;

	public final static void quickCheck(Production p) {
		p.flag = p.flag | ConsumedChecked | ConditionalChecked | ContextualChecked | RecursiveChecked /*| ASTChecked */;
		quickCheck(p, p.getExpression());
	}
	
	public final static void quickCheck(Production p, Expression e) {
		if(e instanceof Consumed) {
			p.flag = p.flag | ConsumedProduction | ConsumedChecked;
			return;
		}
		if(e instanceof NonTerminal) {
			NonTerminal n = (NonTerminal)e;
			if(n.getUniqueName().equals(p.getUniqueName())) {
				p.flag = RecursiveProduction | RecursiveChecked;
			}
			Production np = n.getProduction();
			if(!UFlag.is(p.flag, ConsumedProduction)) {
				if(np != null && UFlag.is(np.flag, ConsumedChecked)) {
					if(UFlag.is(p.flag, ConsumedProduction)) {
						p.flag = p.flag | ConsumedProduction | ConsumedChecked;						
					}
				}
				else {
					p.flag = UFlag.unsetFlag(p.flag, ConsumedChecked);					
				}				
			}
			if(!UFlag.is(p.flag, ConditionalProduction)) {
				if(np != null && UFlag.is(np.flag, ConditionalChecked)) {
					if(UFlag.is(p.flag, ConditionalProduction)) {
						p.flag = p.flag | ConditionalChecked | ConditionalProduction;
					}
				}
				else {
					p.flag = UFlag.unsetFlag(p.flag, ConditionalChecked);					
				}				
			}
			if(!UFlag.is(p.flag, ContextualProduction)) {
				if(np != null && UFlag.is(np.flag, ContextualChecked)) {
					if(UFlag.is(p.flag, ContextualProduction)) {
						p.flag = p.flag | ContextualChecked | ContextualProduction;
					}
				}
				else {
					p.flag = UFlag.unsetFlag(p.flag, ContextualChecked);					
				}				
			}
			if(!UFlag.is(p.flag, RecursiveProduction)) {
				p.flag = UFlag.unsetFlag(p.flag, RecursiveChecked);					
			}
			return;
		}
		if(e instanceof Sequence) {
			quickCheck(p, e.get(0));
			quickCheck(p, e.get(1));
			return;
		}
		if(e instanceof Repetition1) {
			quickCheck(p, e.get(0));
			return;
		}
		if(e instanceof Not || e instanceof Option || e instanceof Repetition || e instanceof And) {
			boolean consumed = UFlag.is(p.flag, ConsumedProduction);
			quickCheck(p, e.get(0));
			if(!consumed) {
				p.flag = UFlag.unsetFlag(p.flag, ConsumedProduction);
			}
			return;
		}
		if(e instanceof Choice) {
			boolean checkedConsumed = UFlag.is(p.flag, ConsumedProduction);
			if(checkedConsumed) {
				for(Expression sub: e) {
					quickCheck(p, sub);
					p.flag = p.flag | ConsumedProduction;
				}
			}
			else {
				boolean unconsumed = false;
				for(Expression sub: e) {
					p.flag = UFlag.unsetFlag(p.flag, ConsumedProduction);					
					quickCheck(p, sub);
					if(!UFlag.is(p.flag, ConsumedProduction)) {
						unconsumed = true;
					}
				}
				if(unconsumed) {
					p.flag = UFlag.unsetFlag(p.flag, ConsumedProduction);					
				}
				else {
					p.flag = p.flag | ConsumedProduction | ConsumedChecked;
				}
			}
			return;
		}
		if(e instanceof Conditional) {
			p.flag = p.flag | ConditionalChecked | ConditionalProduction;
		}
		if(e instanceof Contextual) {
			p.flag = p.flag | ContextualChecked | ContextualProduction;
		}
		for(Expression sub: e) {
			quickCheck(p, sub);
		}
	}
	
	public final List<String> flag() {
		UList<String> l = new UList<String>(new String[4]);
		if(UFlag.is(this.flag, RecursiveChecked)) {
			if(UFlag.is(this.flag, RecursiveProduction)) l.add("recursive");
			else l.add("nonrecursive");
		}
		if(UFlag.is(this.flag, ConsumedChecked)) {
			if(UFlag.is(this.flag, ConsumedProduction)) l.add("consumed");
			else l.add("unconsumed");
		}
		if(UFlag.is(this.flag, ConditionalChecked)) {
			if(UFlag.is(this.flag, ConditionalProduction)) l.add("conditional");
			else l.add("unconditional");
		}
		if(UFlag.is(this.flag, ContextualChecked)) {
			if(UFlag.is(this.flag, ContextualProduction)) l.add("contextual");
			else l.add("uncontextual");
		}
		return l;
	}

	public final static int quickConsumedCheck(Expression e) {
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

	
	int        flag;
	NameSpace  ns;
	String     name;
	String     uname;
	Expression body;
	
//	boolean isPublic      = false;
//	boolean isTerminal    = false;
//	boolean isInline      = false;
//	boolean isUnchecked   = true;
//	boolean isRecursive   = false;
//	boolean isContextual  = false;
//	boolean isConditional = false;
	
	public Production(SourcePosition s, int flag, NameSpace ns, String name, Expression body) {
		super(s);
		this.ns = ns;
		this.name = name;
		this.uname = ns.uniqueName(name);
		this.body = (body == null) ? GrammarFactory.newEmpty(s) : body;
		this.minlen = Production.quickConsumedCheck(body);
		this.flag = flag;
		Production.quickCheck(this);
	}

	private Production(String name, Production original, Expression body) {
		super(original.s);
		this.ns = original.getNameSpace();
		this.name = name;
		this.uname = ns.uniqueName(name);
		this.body = (body == null) ? GrammarFactory.newEmpty(s) : body;
		this.minlen = Production.quickConsumedCheck(body);
		Production.quickCheck(this);
	}

	Production newProduction(String localName) {
		return new Production(localName, this, this.getExpression());
	}
	
	@Override
	public final boolean equalsExpression(Expression o) {
		if(o instanceof Production) {
			return this.getExpression().equalsExpression(((Production) o).getExpression());
		}
		return false;
	}

	public final NameSpace getNameSpace() {
		return this.ns;
	}
	
	public final boolean isPublic() {
		return UFlag.is(this.flag, Production.PublicProduction);
	}

	public final boolean isInline() {
		return UFlag.is(this.flag, Production.InlineProduction);
	}

	public final boolean isTerminal() {
		return UFlag.is(this.flag, Production.TerminalProduction);
	}

	public final boolean isRecursive() {
		return UFlag.is(this.flag, Production.RecursiveProduction);
	}

	public final void setRecursive() {
		this.flag |= Production.RecursiveChecked | Production.RecursiveProduction;
	}

	public final boolean isConditional() {
		if(!UFlag.is(this.flag, Production.ConditionalChecked)) {
			checkConditional(this.getExpression(), null);
			this.flag |= Production.ConditionalChecked;
			//Verbose.debug("conditional? " + this.getLocalName() + " ? " + this.isConditional());
		}
		return UFlag.is(this.flag, Production.ConditionalProduction);
	}

	private void checkConditional(Expression e, Stacker stacker) {
		if(e instanceof Conditional) {
			this.flag |= Production.ConditionalChecked | Production.ConditionalProduction;
		}
		if(e instanceof NonTerminal) {
			Production p = ((NonTerminal) e).getProduction();
			if(!UFlag.is(p.flag, Production.ConditionalChecked)) {
				if(stacker != null && stacker.isVisited(p)) {
					p.flag |= Production.RecursiveChecked | Production.RecursiveProduction;
					return;
				}
				p.checkConditional(p.getExpression(), new Stacker(p, stacker));
				p.flag |= Production.ConditionalChecked;
				//Verbose.debug("conditional? " + p.getLocalName() + " ? " + p.isConditional());
			}
			if(UFlag.is(p.flag, Production.ConditionalProduction)) {
				this.flag |= Production.ConditionalChecked | Production.ConditionalProduction;
			}
		}
		for(Expression sub : e) {
			checkConditional(sub, stacker);
			if(UFlag.is(this.flag, Production.ConditionalProduction)) {
				break;
			}
		}
	}

	public final boolean isContextual() {
		if(!UFlag.is(this.flag, Production.ContextualChecked)) {
			checkContextual(this.getExpression(), null);
			this.flag |= Production.ContextualChecked;
		}
		return UFlag.is(this.flag, Production.ContextualProduction);
	}

	private void checkContextual(Expression e, Stacker stacker) {
		if(e instanceof Contextual) {
			this.flag |= Production.ContextualChecked | Production.ContextualProduction;
		}
		if(e instanceof NonTerminal) {
			Production p = ((NonTerminal) e).getProduction();
			if(!UFlag.is(p.flag, Production.ContextualChecked)) {
				if(stacker != null && stacker.isVisited(p)) {
					p.flag |= Production.RecursiveChecked | Production.RecursiveProduction;
					return;
				}
				p.checkContextual(p.getExpression(), new Stacker(p, stacker));
				p.flag |= Production.ContextualChecked;
			}
			if(UFlag.is(p.flag, Production.ContextualProduction)) {
				this.flag |= Production.ContextualChecked | Production.ContextualProduction;
			}
		}
		for(Expression sub : e) {
			checkContextual(sub, stacker);
			if(UFlag.is(this.flag, Production.ContextualProduction)) {
				break;
			}
		}
	}

	public final String getLocalName() {
		return this.name;
	}
	
	public final String getUniqueName() {
		return this.uname;
	}
	
	public final Expression getExpression() {
		return this.body;
	}

	public final void setExpression(Expression e) {
		this.body = e;
	}

	@Override
	public Expression get(int index) {
		return body;
	}
	
	@Override
	public int size() {
		return 1;
	}
	
	@Override
	protected final void format(StringBuilder sb) {
		sb.append(this.getLocalName());
		sb.append(" = ");
		this.getExpression().format(sb);
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
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
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

