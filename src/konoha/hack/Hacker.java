package konoha.hack;

import nez.ast.script.ScriptContext;
import nez.ast.script.TypeSystem;

public abstract class Hacker {
	public abstract void perform(ScriptContext context, TypeSystem typeSystem);
}
