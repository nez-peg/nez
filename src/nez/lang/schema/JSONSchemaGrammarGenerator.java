package nez.lang.schema;

import nez.lang.Expression;
import nez.lang.GrammarFile;

public class JSONSchemaGrammarGenerator extends SchemaGrammarGenerator {

	public JSONSchemaGrammarGenerator(GrammarFile gfile) {
		this.gfile = gfile;
	}

	@Override
	public void loadPredefinedRules() {
		JSONPredefinedGrammar pre = new JSONPredefinedGrammar(gfile);
		pre.define();
	}

	@Override
	public void newRoot(String structName) {
		Expression root = _NonTerminal(structName);
		Expression[] seq = { _NonTerminal("VALUESEP"), root };
		Expression[] array = { _OpenSquare(), _S(), root, _S(), gfile.newRepetition1(seq), _S(), _CloseSquare() };
		gfile.addProduction(null, "Root", gfile.newChoice(gfile.newSequence(root), gfile.newSequence(array)));
	}

	@Override
	public void newElement(Type t) {
		Expression[] l = { _DQuat(), t.getTypeExpression(), _DQuat(), _NonTerminal("NAMESEP"), t.next().getTypeExpression(), gfile.newOption(_NonTerminal("VALUESEP")) };
		gfile.addProduction(null, t.getElementName(), gfile.newSequence(l));
	}

	@Override
	public void newStruct(String structName, Type t) {
		Expression[] l = { _OpenWave(), t.getTypeExpression(), _CloseWave() };
		gfile.addProduction(null, structName, gfile.newSequence(l));
	}

	@Override
	public void newMembers(Type... types) {
		Expression[] l = new Expression[types.length];
		int index = 0;
		for (Type type : types) {
			l[index++] = type.getTypeExpression();
		}
		gfile.addProduction(null, getTableName() + "_Members", gfile.newChoice(l));
	}

	@Override
	public Type newRequired(String name, Type t) {
		Expression def = _Def(getTableName(), name);
		return new Type(name, def, t);
	}

	@Override
	public Type newOption(String name, Type t) {
		Expression expr = _String(name);
		return new Type(name, expr, t);
	}

	@Override
	public Type newTObject() {
		return new Type(_NonTerminal("JSONObject"));
	}

	@Override
	public Type newTStruct(String structName) {
		return new Type(_NonTerminal(structName));
	}

	@Override
	public Type newTArray(Type t) {
		Expression tExpr = t.getTypeExpression();
		Expression[] array = { _OpenSquare(), tExpr, gfile.newRepetition(_NonTerminal("VALUESEP"), tExpr), _CloseSquare() };
		return new Type(gfile.newSequence(array));
	}

	@Override
	public Type newTEnum(String[] candidates) {
		Expression[] choice = new Expression[candidates.length];
		int index = 0;
		for (String cand : candidates) {
			choice[index++] = _String(cand);
		}
		return new Type(gfile.newChoice(choice));
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

	// FIXME
	@Override
	public Type newSet() {
		String tableName = getTableName();
		Expression[] l = new Expression[getRequiredList().size() + 1];
		int index = 1;
		l[0] = gfile.newRepetition(_NonTerminal(getTableName() + "_Members"));
		for (String required : getRequiredList()) {
			l[index++] = gfile.newExists(tableName, required);
		}
		return new Type(gfile.newLocal(tableName, gfile.newSequence(l)));
	}

	@Override
	public Type newUniq(String elementName) {
		Expression expr = gfile.newSequence(gfile.newNot(gfile.newExists("T" + getTableCounter(), elementName)), _NonTerminal(elementName));
		return new Type(expr);
	}

	@Override
	public Type newOthers() {
		return new Type(_NonTerminal("Member"));
	}

	@Override
	public Type newPermutation() {
		// TODO Auto-generated method stub
		return null;
	}

	private final Expression _OpenSquare() {
		return gfile.newByteChar('[');
	}

	private final Expression _CloseSquare() {
		return gfile.newByteChar(']');
	}

	private final Expression _OpenWave() {
		return gfile.newByteChar('{');
	}

	private final Expression _CloseWave() {
		return gfile.newByteChar('}');
	}

}
