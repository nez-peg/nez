package nez.x;

import nez.lang.NameSpace;
import nez.lang.Production;
import nez.lang.Typestate;
import nez.main.Command;
import nez.main.CommandConfigure;

public class TypeCommand extends Command {

	@Override
	public String getDesc() {
		return "grammar type checker";
	}

	@Override
	public void exec(CommandConfigure config) {
		NameSpace peg = config.getNameSpace(false);
		for(Production r : peg.getDefinedRuleList()) {
			if(r.inferTypestate() == Typestate.ObjectType) {
				Type t = Type.inferType(r, r.getExpression());
				System.out.println(r.getLocalName() + " : " + t);
			}
		}
	}
}
