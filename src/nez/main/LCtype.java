package nez.main;

import nez.Grammar;
import nez.lang.Production;
import nez.lang.Typestate;
import nez.x.Type;

public class LCtype extends Command {

	@Override
	public String getDesc() {
		return "grammar type checker";
	}

	@Override
	public void exec(CommandContext config) {
		Grammar peg = config.getGrammar(false);
		for (Production r : peg.getProductionList()/* getDefinedRuleList() */) {
			if (r.inferTypestate(null) == Typestate.ObjectType) {
				Type t = Type.inferType(r, r.getExpression());
				System.out.println(r.getLocalName() + " : " + t);
			}
		}
	}
}
