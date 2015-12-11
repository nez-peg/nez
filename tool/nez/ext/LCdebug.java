package nez.ext;
//package nez.main;
//
//import java.io.IOException;
//
//import nez.Parser;
//import nez.SourceContext;
//
//public class LCdebug extends Command {
//	@Override
//	public String getDesc() {
//		return "Nez debugger";
//	}
//
//	@Override
//	public void exec(CommandContext config) throws IOException {
//		// config.setNezOption(NezOption.DebugOption);
//		Command.displayVersion();
//		Parser peg = config.newParser();
//		while (config.hasInput()) {
//			SourceContext file = config.nextInputSource();
//			// peg.debug(file);
//			file = null;
//		}
//	}
// }