package nez.x;

import nez.Grammar;
import nez.expr.Rule;
import nez.expr.Typestate;
import nez.main.Command;
import nez.main.CommandConfigure;

public class TypeCommand extends Command {

	@Override
	public String getDesc() {
		return "grammar type checker";
	}

	@Override
	public void exec(CommandConfigure config) {
		Grammar peg = config.getGrammar();
		for(Rule r : peg.getDefinedRuleList()) {
			if(r.inferTypestate() == Typestate.ObjectType) {
				Type t = Type.inferType(r, r.getExpression());
				System.out.println(r.getLocalName() + " : " + t);
			}
		}
	}
}
