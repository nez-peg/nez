package nez.vm;

import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.GrammarReshaper;
import nez.lang.Production;
import nez.main.Verbose;
import nez.util.UFlag;
import nez.util.UList;

public abstract class NezCompiler2 extends NezCompiler {

	public NezCompiler2(int option) {
		super(option);
	}

//	public final Instruction encode(Grammar grammar) {
//		long t = System.nanoTime();
//		for(GProduction gp : grammar.list()) {
//
//			
//			String uname = r.getUniqueName();
//			if(Verbose.Debug) {
//				Verbose.debug("compiling .. " + r);
//			}
//			Expression e = r.getExpression();
//			if(UFlag.is(option, Grammar.Inlining)  && this.ruleMap.size() > 0 && r.isInline() ) {
//				//System.out.println("skip .. " + r.getLocalName() + "=" + e);
//				continue;
//			}
//			if(!UFlag.is(option, Grammar.ASTConstruction)) {
//				e = e.reshape(Manipulator.RemoveAST);
//			}
//			CodeBlock block = new CodeBlock();
//			block.head = encodeExpression(e, new IRet(r));
//			block.start = codeList.size();
//			this.ruleMap.put(uname, block);
//			verify(block.head);
//			block.end = codeList.size();
//		}
//		for(Instruction inst : codeList) {
//			if(inst instanceof ICallPush) {
//				CodeBlock deref = this.ruleMap.get(((ICallPush) inst).rule.getUniqueName());
//				((ICallPush) inst).setResolvedJump(deref.head);
//			}
//		}
//		long t2 = System.nanoTime();
//		//Verbose.printElapsedTime("CompilingTime", t, t2);
//		return this.codeList.ArrayValues[0];
//	}

}
