package nez.vm;

import nez.NezOption;
import nez.lang.Grammar;
import nez.util.UFlag;
import nez.util.UList;

public abstract class NezCompiler extends NezEncoder {

	public NezCompiler(NezOption option) {
		super(option);
	}

	protected final boolean enablePackratParsing() {
		return this.option.enabledMemoization;
	}

	protected final boolean enableASTConstruction() {
		return this.option.enabledASTConstruction;
	}
	
	public abstract NezCode compile(Grammar grammar);

	
	public final void layoutCode(UList<Instruction> codeList, Instruction inst) {
		if(inst == null) {
			return;
		}
		if(inst.id == -1) {
			inst.id = codeList.size();
			codeList.add(inst);
			layoutCode(codeList, inst.next);
			if(inst.next != null && inst.id + 1 != inst.next.id) {
				Instruction.labeling(inst.next);
			}
			layoutCode(codeList, inst.branch());
			if(inst instanceof IDfaDispatch) {
				IDfaDispatch match = (IDfaDispatch)inst;
				for(int ch = 0; ch < match.jumpTable.length; ch ++) {
					layoutCode(codeList, match.jumpTable[ch]);
				}
			}
			//encode(inst.branch2());
		}
	}

}
