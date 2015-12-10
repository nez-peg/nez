package nez.lang.schema;

import nez.lang.Expression;
import nez.lang.Grammar;

public class JSONSchemaGrammarGenerator extends SchemaGrammarGenerator {

	public JSONSchemaGrammarGenerator(Grammar grammar) {
		super(grammar);
	}

	@Override
	public void loadPredefinedRules() {
		JSONPredefinedGrammar pre = new JSONPredefinedGrammar(grammar);
		pre.define();
	}

	@Override
	public void newRoot(String structName) {
		Expression root = _NonTerminal(structName);
		Expression[] seq = { _NonTerminal("VALUESEP"), root };
		Expression[] array = { _OpenSquare(), _S(), root, _S(), _OneMore(seq), _S(), _CloseSquare() };
		grammar.addProduction(null, "Root", _Choice(_Sequence(root), _Sequence(array)));
	}

	@Override
	public void newElement(String elementName, Type t) {
		Expression[] l = { _DQuat(), t.getTypeExpression(), _DQuat(), _S(), _NonTerminal("NAMESEP"), t.next().getTypeExpression(), _Option(_NonTerminal("VALUESEP")), _S() };
		grammar.addProduction(null, elementName, _Sequence(l));
	}

	@Override
	public void newStruct(String structName, Type t) {
		Expression[] l = { _OpenWave(), _S(), t.getTypeExpression(), _S(), _CloseWave(), _S() };
		grammar.addProduction(null, structName, _Sequence(l));
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
		Expression[] array = { _OpenSquare(), _S(), tExpr, _ZeroMore(_NonTerminal("VALUESEP"), tExpr), _CloseSquare() };
		return new Type(_Sequence(array));
	}

	@Override
	public Type newTEnum(String[] candidates) {
		Expression[] choice = new Expression[candidates.length];
		int index = 0;
		for (String cand : candidates) {
			choice[index++] = _String(cand);
		}
		return new Type(_Choice(choice));
	}

	@Override
	public Type newOthers() {
		return new Type(_NonTerminal("Member"));
	}

	private final Expression _OpenSquare() {
		return _Char('[');
	}

	private final Expression _CloseSquare() {
		return _Char(']');
	}

	private final Expression _OpenWave() {
		return _Char('{');
	}

	private final Expression _CloseWave() {
		return _Char('}');
	}

}
