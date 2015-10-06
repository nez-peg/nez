package nez.lang.schema;

import java.util.ArrayList;
import java.util.List;

import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.GrammarFile;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.NonTerminal;
import nez.util.UList;

public abstract class SchemaGrammarGenerator extends AbstractSchemaGrammarGenerator {
	protected GrammarFile gfile;
	private List<String> requiredList;
	private List<String> membersList;
	private int tableCounter = 0;

	public SchemaGrammarGenerator(GrammarFile gfile) {
		this.gfile = gfile;
	}

	public void addRequired(String name) {
		this.requiredList.add(name);
		this.membersList.add(name);
	}

	public void addMember(String name) {
		this.membersList.add(name);
	}

	public List<String> getMembers() {
		return this.membersList;
	}

	public List<String> getRequiredList() {
		return this.requiredList;
	}

	public int getTableCounter() {
		return this.tableCounter;
	}

	public String getTableName() {
		return "T" + getTableCounter();
	}

	public final void initMemberList() {
		requiredList = new ArrayList<String>();
		membersList = new ArrayList<String>();
		tableCounter++;
	}

	protected final List<String> extractImpliedMembers() {
		List<String> impliedList = new ArrayList<String>();
		for (int i = 0; i < membersList.size(); i++) {
			if (!requiredList.contains(membersList.get(i))) {
				impliedList.add(membersList.get(i));
			}
		}
		return impliedList;
	}

	/* wrapper for nez grammar */

	protected final NonTerminal _NonTerminal(String nonterm) {
		return ExpressionCommons.newNonTerminal(null, gfile, nonterm);
	}

	protected final Expression _String(String text) {
		return ExpressionCommons.newString(null, text);
	}

	protected final Expression _Char(int ch) {
		return ExpressionCommons.newCbyte(null, false, ch);
	}

	protected final Expression _ZeroMore(Expression... l) {
		return ExpressionCommons.newPzero(null, _Sequence(l));
	}

	protected final Expression _OneMore(Expression... l) {
		return ExpressionCommons.newPone(null, _Sequence(l));
	}

	protected final Expression _And(Expression... l) {
		return ExpressionCommons.newPand(null, _Sequence(l));
	}

	protected final Expression _Not(Expression... l) {
		return ExpressionCommons.newPnot(null, _Sequence(l));
	}

	protected final Expression _Option(Expression... l) {
		return ExpressionCommons.newPoption(null, _Sequence(l));
	}

	protected final Expression _Sequence(Expression... l) {
		return ExpressionCommons.newPsequence(null, new UList<Expression>(l));
	}

	protected final Expression _Choice(Expression... l) {
		return ExpressionCommons.newPchoice(null, new UList<Expression>(l));
	}

	protected final Expression _DQuat() {
		return _Char('"');
	}

	protected final Expression _S() {
		return ExpressionCommons.newNonTerminal(null, gfile, "SPACING");
	}

	protected final Expression _Empty() {
		return ExpressionCommons.newEmpty(null);
	}

	protected final Expression _Failure() {
		return ExpressionCommons.newFailure(null);
	}

	protected final Expression _Def(String tableName) {
		return ExpressionCommons.newXsymbol(null, _NonTerminal(tableName));
	}

	protected final Expression _Exists(String table, String name) {
		return ExpressionCommons.newXexists(null, Symbol.tag(table), name);
	}

	protected final Expression _Local(String table, Expression... seq) {
		return ExpressionCommons.newXlocal(null, Symbol.tag(table), _Sequence(seq));
	}

	/* common methods of schema grammar */

	@Override
	public void newMembers(String structName, Type... types) {
		Expression[] l = new Expression[types.length];
		int index = 0;
		for (Type type : types) {
			l[index++] = type.getTypeExpression();
		}
		gfile.addProduction(null, structName + "_SMembers", _Choice(l));
	}

	@Override
	public void newSymbols() {
		Expression[] l = new Expression[getMembers().size()];
		int index = 0;
		for (String name : getMembers()) {
			l[index++] = _String(name);
		}
		gfile.addProduction(null, getTableName(), _Choice(l));
	}

	// FIXME
	@Override
	public Type newSet(String structName) {
		Expression[] l = new Expression[getRequiredList().size() + 1];
		int index = 1;
		l[0] = _ZeroMore(_NonTerminal(structName + "_SMembers"));
		for (String required : getRequiredList()) {
			l[index++] = _Exists(getTableName(), required);
		}
		return new Type(_Local(getTableName(), l));
	}

	@Override
	public Type newUniq(String elementName, Type t) {
		Expression expr = _Sequence(_And(_String(elementName)), _Not(_Exists(getTableName(), elementName)), _Def(getTableName()));
		return new Type(elementName, expr, t);
	}

	@Override
	public Type newAlt(String name) {
		return new Type(_NonTerminal(name));
	}

	@Override
	public Type newPermutation() {
		genImpliedChoice();
		if (getRequiredList().isEmpty()) {
			return newCompletePerm();
		} else {
			return newAproximatePerm();
		}
	}

	// FIXME
	protected Type newCompletePerm() {
		int listLength = getRequiredList().size();

		if (listLength == 1) {
			return new Type(_NonTerminal(getRequiredList().get(0)));
		} else {
			PermutationGenerator permGen = new PermutationGenerator(listLength);
			int[][] permedList = permGen.getPermList();
			Expression[] choiceList = new Expression[permedList.length];
			int choiceCount = 0;
			for (int[] targetLine : permedList) {
				Expression[] seqList = new Expression[listLength];
				for (int index = 0; index < targetLine.length; index++) {
					seqList[index] = ExpressionCommons.newNonTerminal(null, gfile, getRequiredList().get(targetLine[index]));
				}
				choiceList[choiceCount++] = _Sequence(seqList);
			}
			return new Type(_Choice(choiceList));
		}
	}

	// FIXME
	protected Type newAproximatePerm() {
		int listLength = getRequiredList().size();
		if (listLength == 0) {
			return new Type(_NonTerminal(getTableName() + "_implied"));
		} else {
			PermutationGenerator permGen = new PermutationGenerator(listLength);
			int[][] permutedList = permGen.getPermList();
			Expression[] choiceList = new Expression[permutedList.length];
			Expression impliedChoiceRule = _NonTerminal(getTableName() + "_implied");
			int choiceCount = 0;
			for (int[] targetLine : permutedList) {
				int seqCount = 0;
				Expression[] seqList = new Expression[listLength * 2 + 1];
				seqList[seqCount++] = _ZeroMore(impliedChoiceRule);
				for (int index = 0; index < targetLine.length; index++) {
					seqList[seqCount++] = ExpressionCommons.newNonTerminal(null, gfile, getRequiredList().get(targetLine[index]));
					seqList[seqCount++] = _ZeroMore(impliedChoiceRule);
				}
				choiceList[choiceCount++] = _Sequence(seqList);
			}
			return new Type(_Choice(choiceList));
		}

	}

	protected void genImpliedChoice() {
		List<String> impliedList = extractImpliedMembers();
		Expression[] l = new Expression[impliedList.size()];
		int choiceCount = 0;
		for (String nonTerminalSymbol : impliedList) {
			l[choiceCount++] = _NonTerminal(nonTerminalSymbol);
		}
		gfile.addProduction(null, getTableName() + "_implied", gfile.newChoice(l));
	}

	@Override
	public Type newTInteger() {
		return new Type(_NonTerminal("INT"));
	}

	@Override
	public Type newTFloat() {
		return new Type(_NonTerminal("Number"));
	}

	@Override
	public Type newTString() {
		return new Type(_NonTerminal("String"));
	}

	@Override
	public Type newTAny() {
		return new Type(_NonTerminal("Any"));
	}

}
