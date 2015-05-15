package nez.generator;

import nez.lang.Grammar;
import nez.main.Command;
import nez.main.CommandConfigure;

public class GeneratorCommand extends Command {

	@Override
	public String getDesc() {
		return "parser generator";
	}

	@Override
	public void exec(CommandConfigure config) {
		Grammar p = config.getProduction();
//		int labelId = 0;
//		for(Instruction inst: cc.codeList) {
//			if(inst.label) {
//				inst.id = labelId;
//				labelId++;
//			}
//			else {
//				inst.id = -9999;
//			}
//		}
		ParserGenerator cc = new ParserGenerator(null);
		//cc.generate(p);
	}

}
