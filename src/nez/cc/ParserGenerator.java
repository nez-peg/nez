package nez.cc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import nez.runtime.Instruction;

public abstract class ParserGenerator extends GrammarGenerator {
	ParserGenerator(String fileName) {
		super(fileName);
	}

	public final void visit(Instruction inst) {
		Method m = lookupMethod("visit", inst.getClass());
		if(m != null) {
			try {
				m.invoke(this, inst);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		else {
			visitUndefined(inst);
		}
	}
	
	void visitUndefined(Instruction inst) {
		System.out.println("undefined: " + inst.getClass());
	}

}
