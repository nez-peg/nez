package nez.x;

import nez.NameSpace;
import nez.expr.Production;
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
		NameSpace peg = config.getGrammar(false);
		for(Production r : peg.getDefinedRuleList()) {
			if(r.inferTypestate() == Typestate.ObjectType) {
				Type t = Type.inferType(r, r.getExpression());
				System.out.println(r.getLocalName() + " : " + t);
			}
		}
	}
}
