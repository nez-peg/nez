package nez.lang.schema;

import java.util.ArrayList;
import java.util.List;

import nez.lang.Expression;
import nez.lang.GrammarFile;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.NonTerminal;

public abstract class SchemaGrammarGenerator extends AbstractSchemaGrammarGenerator {
	protected GrammarFile gfile;
	private List<String> requiredList;
	private List<String> membersList;
	private int tableCounter = 0;

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

	protected final NonTerminal _NonTerminal(String nonterm) {
		return ExpressionCommons.newNonTerminal(null, gfile, nonterm);
	}

	protected final Expression _String(String text) {
		return ExpressionCommons.newString(null, text);
	}

	protected final Expression _DQuat() {
		return gfile.newByteChar('"');
	}

	protected final Expression _S() {
		return ExpressionCommons.newNonTerminal(null, gfile, "SPACING");
	}

	protected final Expression _Def(String table, String symbol) {
		return ExpressionCommons.newXsymbol(null, _NonTerminal(symbol));
	}
}
