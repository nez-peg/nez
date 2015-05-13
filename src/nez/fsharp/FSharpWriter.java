package nez.fsharp;

import nez.ast.CommonTree;

public class FSharpWriter extends ParsingWriter {
	
	static {
		ParsingWriter.registerExtension("fs", FSharpWriter.class);
	}
	
	@Override
	protected void write(CommonTree po) {
		SourceGenerator generator = new FSharpGenerator();
		generator.visit(po);
		this.out.println(generator.toString());
	}

}
