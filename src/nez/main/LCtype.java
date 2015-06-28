package nez.main;

import nez.lang.GrammarFile;
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
		GrammarFile peg = config.getGrammarFile(false);
		for(Production r : peg.getDefinedRuleList()) {
			if(r.inferTypestate() == Typestate.ObjectType) {
				Type t = Type.inferType(r, r.getExpression());
				System.out.println(r.getLocalName() + " : " + t);
			}
		}
	}
}
