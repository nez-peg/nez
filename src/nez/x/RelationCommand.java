package nez.x;

import nez.main.Command;
import nez.main.CommandConfigure;

public class RelationCommand extends Command {

	@Override
	public String getDesc() {
		return "relational extracker";
	}

	@Override
	public void exec(CommandConfigure config) {
//		Recorder rec = config.getRecorder();
//		Production p = config.getProduction();
//		p.record(rec);
//		RelationExtracker rel = new RelationExtracker(4096, 0.5, config.getOutputFileName());
//		while(config.hasInput()) {
//			SourceContext file = config.getInputSourceContext();
//			file.start(rec);
//			SyntaxTree node = p.parse(file);  // FIXMErel.newNode());
//			file.done(rec);
//			if(node == null) {
//				ConsoleUtils.exit(1, file.getSyntaxErrorMessage());
//				break;
//			}
//			if(rec != null) {
//				rel.record(rec);
//				rec.log();
//			}
//		}
//		rel.cleanUp();
	}
}
