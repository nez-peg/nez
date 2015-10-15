package konoha.hack;

import nez.ast.script.ScriptContext;
import nez.ast.script.TypeSystem;
import nez.util.ConsoleUtils;

public class AST extends Hacker {

	@Override
	public void perform(ScriptContext context, TypeSystem typeSystem) {
		context.enableASTDump = !context.enableASTDump;
		ConsoleUtils.println("turning AST dump: " + context.enableASTDump);
	}
}
