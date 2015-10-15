package konoha.hack;

import konoha.script.ScriptContext;
import konoha.script.TypeSystem;

public abstract class Hacker {
	public abstract void perform(ScriptContext context, TypeSystem typeSystem);
}
