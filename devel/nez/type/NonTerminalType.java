package nez.type;

import java.lang.reflect.Type;

import nez.lang.NonTerminal;
import nez.lang.Production;

public class NonTerminalType implements Type {
	String uname;

	public NonTerminalType(NonTerminal e) {
		this.uname = e.getUniqueName();
	}

	public NonTerminalType(Production p) {
		this.uname = p.getUniqueName();
	}

	@Override
	public final String toString() {
		return uname;
	}

}
