package nez.lang.schema;

import java.util.ArrayList;
import java.util.List;

import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.NonTerminal;
import nez.lang.expr.Expressions;
import nez.util.UList;

public abstract class AbstractSchemaGrammarGenerator implements SchemaGrammarGenerator {
	protected Grammar grammar;
	private List<Element> requiredElementList;
	private List<Element> elementList;
	private int tableCounter = 0;

	public AbstractSchemaGrammarGenerator(Grammar grammar) {
		this.grammar = grammar;
	}

	public List<Element> getElementList() {
		return this.elementList;
	}

	public List<Element> getRequiredElementList() {
		return this.requiredElementList;
	}

	public List<Element> getOptionalElementList() {
		return extractOptionalMembers();
	}

	public void addRequired(Element element) {
		this.requiredElementList.add(element);
		this.elementList.add(element);
	}

	public void addElement(Element element) {
		this.elementList.add(element);
	}

	public int getTableCounter() {
		return this.tableCounter;
	}

	public String getTableName() {
		return "T" + getTableCounter();
	}

	public final void initMemberList() {
		requiredElementList = new ArrayList<Element>();
		elementList = new ArrayList<Element>();
		tableCounter++;
	}

	protected final List<Element> extractOptionalMembers() {
		List<Element> impliedList = new ArrayList<Element>();
		for (int i = 0; i < elementList.size(); i++) {
			if (!requiredElementList.contains(elementList.get(i))) {
				impliedList.add(elementList.get(i));
			}
		}
		return impliedList;
	}

	/* wrapper for nez grammar */

	protected final NonTerminal _NonTerminal(String nonterm) {
		return Expressions.newNonTerminal(null, grammar, nonterm);
	}

	protected final NonTerminal _NonTerminal(String format, Object... args) {
		return _NonTerminal(String.format(format, args));
	}

	protected final Expression _String(String text) {
		return Expressions.newString(null, text);
	}

	protected final Expression _String(String format, Object... args) {
		return _String(String.format(format, args));
	}

	protected final Expression _Char(int ch) {
		return Expressions.newCbyte(null, false, ch);
	}

	protected final Expression _ZeroMore(Expression... l) {
		return Expressions.newPzero(null, _Sequence(l));
	}

	protected final Expression _OneMore(Expression... l) {
		return Expressions.newPone(null, _Sequence(l));
	}

	protected final Expression _And(Expression... l) {
		return Expressions.newPand(null, _Sequence(l));
	}

	protected final Expression _Not(Expression... l) {
		return Expressions.newPnot(null, _Sequence(l));
	}

	protected final Expression _Option(Expression... l) {
		return Expressions.newPoption(null, _Sequence(l));
	}

	protected final Expression _Sequence(Expression... l) {
		UList<Expression> seq = new UList<Expression>(new Expression[8]);
		for (Expression p : l) {
			Expressions.addSequence(seq, p);
		}
		return Expressions.newPair(null, seq);
	}

	protected final Expression _Choice(Expression... l) {
		UList<Expression> seq = new UList<Expression>(new Expression[8]);
		for (Expression p : l) {
			Expressions.addChoice(seq, p);
		}
		return Expressions.newPchoice(null, seq);
	}

	protected final Expression _Detree(Expression e) {
		return Expressions.newTdetree(null, e);
	}

	protected final Expression _Link(Symbol label, Expression e) {
		return Expressions.newTlink(null, label, e);
	}

	protected final Expression _New(Expression... seq) {
		return Expressions.newNewCapture(null, false, null, _Sequence(seq));
	}

	protected final Expression _LeftFold(Symbol label, int shift) {
		return Expressions.newTlfold(null, label, shift);
	}

	protected final Expression _Capture(int shift) {
		return Expressions.newTcapture(null, shift);
	}

	protected final Expression _Tag(String tag) {
		return Expressions.newTtag(null, Symbol.tag(tag));
	}

	protected final Expression _Replace(String msg) {
		return Expressions.newTreplace(null, msg);
	}

	protected final Expression _DQuat() {
		return _Char('"');
	}

	protected final Expression _S() {
		return Expressions.newNonTerminal(null, grammar, "S");
	}

	protected final Expression _Empty() {
		return Expressions.newEmpty(null);
	}

	protected final Expression _Failure() {
		return Expressions.newFailure(null);
	}

	protected final Expression _Def(String tableName) {
		return Expressions.newXsymbol(null, _NonTerminal(tableName));
	}

	protected final Expression _Exists(String table, String name) {
		return Expressions.newXexists(null, Symbol.tag(table), name);
	}

	protected final Expression _Local(String table, Expression... seq) {
		return Expressions.newXlocal(null, Symbol.tag(table), _Sequence(seq));
	}

	/* common methods of schema grammar */

	@Override
	public void newMembers(String memberListName) {
		Expression[] l = new Expression[elementList.size()];
		int index = 0;
		for (Element element : elementList) {
			l[index++] = _Link(null, _NonTerminal(element.getUniqueName()));
		}

		// l[index] = _Link(null, newOthers().getSchemaExpression());
		grammar.addProduction(null, memberListName, _ZeroMore(_Choice(l)));
	}

	@Override
	public void newSymbols() {
		Expression[] l = new Expression[getElementList().size()];
		int index = 0;
		for (Element element : elementList) {
			l[index++] = _String(element.getElementName());
		}
		grammar.addProduction(null, getTableName(), _Choice(l));
	}

	// FIXME
	@Override
	public Schema newSet(String structMemberName) {
		Expression[] l = new Expression[getRequiredElementList().size() + 1];
		int index = 1;
		l[0] = _NonTerminal(structMemberName);
		for (Element required : requiredElementList) {
			l[index++] = _Exists(getTableName(), required.getElementName());
		}
		return new Schema(_Local(getTableName(), l));
	}

	@Override
	public Schema newUniq(String elementName, Schema t) {
		Expression expr = _Sequence(_And(_String(elementName)), _Not(_Exists(getTableName(), elementName)), _Def(getTableName()));
		return new Schema(expr, t);
	}

	@Override
	public Schema newAlt(String name) {
		return new Schema(_NonTerminal(name));
	}

	@Override
	public Schema newPermutation() {
		genImpliedChoice();
		if (getRequiredElementList().isEmpty()) {
			return newCompletePerm();
		} else {
			return newAproximatePerm();
		}
	}

	// FIXME
	protected Schema newCompletePerm() {
		int listLength = getRequiredElementList().size();
		if (listLength == 1) {
			return new Schema(_NonTerminal(requiredElementList.get(0).getUniqueName()));
		} else {
			PermutationGenerator permGen = new PermutationGenerator(listLength);
			int[][] permedList = permGen.getPermList();
			Expression[] choiceList = new Expression[permedList.length];
			int choiceCount = 0;
			for (int[] targetLine : permedList) {
				Expression[] seqList = new Expression[listLength];
				for (int index = 0; index < targetLine.length; index++) {
					seqList[index] = _NonTerminal(getRequiredElementList().get(targetLine[index]).getUniqueName());
				}
				choiceList[choiceCount++] = _Sequence(seqList);
			}
			return new Schema(_Choice(choiceList));
		}
	}

	// FIXME
	protected Schema newAproximatePerm() {
		int listLength = getRequiredElementList().size();
		if (listLength == 0) {
			return new Schema(_NonTerminal("%s_implied", getTableName()));
		} else {
			PermutationGenerator permGen = new PermutationGenerator(listLength);
			int[][] permutedList = permGen.getPermList();
			Expression[] choiceList = new Expression[permutedList.length];
			Expression impliedChoiceRule = _NonTerminal("%s_implied", getTableName());
			int choiceCount = 0;
			for (int[] targetLine : permutedList) {
				int seqCount = 0;
				Expression[] seqList = new Expression[listLength * 2 + 1];
				seqList[seqCount++] = _ZeroMore(impliedChoiceRule);
				for (int index = 0; index < targetLine.length; index++) {
					seqList[seqCount++] = _NonTerminal(getRequiredElementList().get(targetLine[index]).getUniqueName());
					seqList[seqCount++] = _ZeroMore(impliedChoiceRule);
				}
				choiceList[choiceCount++] = _Sequence(seqList);
			}
			return new Schema(_Choice(choiceList));
		}
	}

	protected void genImpliedChoice() {
		List<Element> impliedList = extractOptionalMembers();
		Expression[] l = new Expression[impliedList.size()];
		int choiceCount = 0;
		for (Element element : impliedList) {
			l[choiceCount++] = _NonTerminal(element.getUniqueName());
		}
		grammar.addProduction(null, String.format("%s_implied", getTableName()), grammar.newChoice(l));
	}

	@Override
	public Schema newTInteger() {
		return new Schema(_NonTerminal("Integer"));
	}

	@Override
	public Schema newTInteger(int min, int max) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Schema newTFloat() {
		return new Schema(_NonTerminal("Float"));
	}

	@Override
	public Schema newTFloat(int min, int max) {
		// TODO Auto-generated method stub
		return null;
	};

	@Override
	public Schema newTString() {
		return new Schema(_NonTerminal("String"));
	}

	@Override
	public Schema newTString(int minLength, int maxLength) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Schema newTAny() {
		return new Schema(_NonTerminal("Any"));
	}

}
