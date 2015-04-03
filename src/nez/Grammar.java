package nez;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.expr.Expression;
import nez.expr.Factory;
import nez.expr.Rule;
import nez.util.UList;
import nez.util.UMap;

public class Grammar {
	String               resourceName;
	String               ns;
	UMap<Rule>           ruleMap;
	UList<String>        nameList;

	public Grammar(String name) {
		this.resourceName = name;
		if(name != null) {
			int loc = name.lastIndexOf('/');
			if(loc != -1) {
				name = name.substring(loc+1);
			}
			this.ns = name.replace(".nez", "");
		}
		this.ruleMap = new UMap<Rule>();
		this.nameList = new UList<String>(new String[8]);
	}

	public String getResourceName() {
		return this.resourceName;
	}
	
	public String uniqueName(String rulename) {
		return this.ns + ":" + rulename;
	}
	
	public final Rule newRule(String name, Expression e) {
		Rule r = new Rule(null, this, name, e);
		this.ruleMap.put(name, r);
		return r;
	}

	public final Rule defineRule(SourcePosition s, String name, Expression e) {
		if(!hasRule(name)) {
			nameList.add(name);
		}
		Rule r = new Rule(s, this, name, e);
		this.ruleMap.put(name, r);
		return r;
	}
		
//	public int getRuleSize() {
//		return this.ruleMap.size();
//	}

	public final boolean hasRule(String ruleName) {
		return this.ruleMap.get(ruleName) != null;
	}

	public final Rule getRule(String ruleName) {
		return this.ruleMap.get(ruleName);
	}

	public final UList<Rule> getDefinedRuleList() {
		UList<Rule> ruleList = new UList<Rule>(new Rule[this.nameList.size()]);
		for(String n : nameList) {
			ruleList.add(this.getRule(n));
		}
		return ruleList;
	}

	public final UList<Rule> getRuleList() {
		UList<Rule> ruleList = new UList<Rule>(new Rule[this.ruleMap.size()]);
		for(String n : this.ruleMap.keys()) {
			ruleList.add(this.getRule(n));
		}
		return ruleList;
	}

	public final Production newProduction(String name, int option) {
		Rule r = this.getRule(name);
		if(r != null) {
			return new Production(r, option);
		}
		//System.out.println("** " + this.ruleMap.keys());
		return null;
	}

	public final Production newProduction(String name) {
		return this.newProduction(name, Production.DefaultOption);
	}

	public void dump() {
		for(Rule r : this.getRuleList()) {
			System.out.println(r);
		}
	}
	
	// Grammar
	
	private SourcePosition src() {
		return null; // TODO
	}
	
	public final Expression newNonTerminal(String name) {
		return Factory.newNonTerminal(src(), this, name);
	}
	
	public final Expression newEmpty() {
		return Factory.newEmpty(src());
	}

	public final Expression newFailure() {
		return Factory.newFailure(src());
	}

	public final Expression newByteChar(int ch) {
		return Factory.newByteChar(src(), ch);
	}
	
	public final Expression newAnyChar() {
		return Factory.newAnyChar(src());
	}
	
	public final Expression newString(String text) {
		return Factory.newString(src(), text);
	}
	
	public final Expression newCharSet(SourcePosition s, String text) {
		return Factory.newCharSet(src(), text);
	}

	public final Expression newByteMap(boolean[] byteMap) {
		return Factory.newByteMap(src(), byteMap);
	}
	
	public final Expression newSequence(Expression ... seq) {
		UList<Expression> l = new UList<Expression>(new Expression[8]);
		for(Expression p: seq) {
			Factory.addSequence(l, p);
		}
		return Factory.newSequence(src(), l);
	}

	public final Expression newChoice(Expression ... seq) {
		UList<Expression> l = new UList<Expression>(new Expression[8]);
		for(Expression p: seq) {
			Factory.addChoice(l, p);
		}
		return Factory.newChoice(src(), l);
	}

	public final Expression newOption(Expression ... seq) {
		return Factory.newOption(src(), newSequence(seq));
	}
		
	public final Expression newRepetition(Expression ... seq) {
		return Factory.newRepetition(src(), newSequence(seq));
	}

	public final Expression newRepetition1(Expression ... seq) {
		return Factory.newRepetition1(src(), newSequence(seq));
	}

	public final Expression newAnd(Expression ... seq) {
		return Factory.newAnd(src(), newSequence(seq));
	}

	public final Expression newNot(Expression ... seq) {
		return Factory.newNot(src(), newSequence(seq));
	}
	
//	public final Expression newByteRange(int c, int c2) {
//		if(c == c2) {
//			return newByteChar(s, c);
//		}
//		return internImpl(s, new ByteMap(s, c, c2));
//	}
	
	// PEG4d
	public final Expression newMatch(Expression ... seq) {
		return Factory.newMatch(src(), newSequence(seq));
	}
	
	public final Expression newLink(Expression ... seq) {
		return Factory.newLink(src(), newSequence(seq), -1);
	}

	public final Expression newLink(int index, Expression ... seq) {
		return Factory.newLink(src(), newSequence(seq), index);
	}

	public final Expression newNew(Expression ... seq) {
		return Factory.newNew(src(), false, newSequence(seq));
	}

	public final Expression newLeftNew(Expression ... seq) {
		return Factory.newNew(src(), true, newSequence(seq));
	}

	public final Expression newTagging(String tag) {
		return Factory.newTagging(src(), Tag.tag(tag));
	}

	public final Expression newReplace(String msg) {
		return Factory.newReplace(src(), msg);
	}
	
	// Conditional Parsing
	// <if FLAG>
	// <with FLAG e>
	// <without FLAG e>
	
	public final Expression newIfFlag(String flagName) {
		return Factory.newIfFlag(src(), flagName);
	}
	
	public final Expression newWithFlag(String flagName, Expression ... seq) {
		return Factory.newWithFlag(src(), flagName, newSequence(seq));
	}

	public final Expression newWithoutFlag(String flagName, Expression ... seq) {
		return Factory.newWithoutFlag(src(), flagName, newSequence(seq));
	}

	public final Expression newScan(SourcePosition s, int number, Expression scan, Expression repeat) {
		return null;
	}
	
	public final Expression newRepeat(SourcePosition s, Expression e) {
		return null;
	}
	
	public final Expression newBlock(Expression ... seq) {
		return Factory.newBlock(src(), newSequence(seq));
	}

	public final Expression newDefSymbol(SourcePosition s, String table, Expression ... seq) {
		return Factory.newDefSymbol(src(), Tag.tag(table), newSequence(seq));
	}

	public final Expression newIsSymbol(SourcePosition s, String table) {
		return Factory.newIsSymbol(src(), Tag.tag(table));
	}
	
	public final Expression newIsaSymbol(SourcePosition s, String table) {
		return Factory.newIsaSymbol(src(), Tag.tag(table));
	}

	public final Expression newDefIndent(SourcePosition s) {
		return Factory.newDefIndent(src());
	}

	public final Expression newIndent(SourcePosition s) {
		return Factory.newIndent(src());
	}



}
