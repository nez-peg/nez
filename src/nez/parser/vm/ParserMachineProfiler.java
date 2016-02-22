package nez.parser.vm;

import nez.ast.Source;
import nez.ast.Tree;

public class ParserMachineProfiler<T extends Tree<T>> extends ParserMachineContext<T> {

	public ParserMachineProfiler(Source source, T proto) {
		super(source, proto);
	}

}
