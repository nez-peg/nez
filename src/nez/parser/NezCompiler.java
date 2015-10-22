package nez.parser;

import nez.Strategy;
import nez.lang.Production;
import nez.main.Verbose;
import nez.util.UList;

public abstract class NezCompiler extends AbstractGenerator {

	public final static NezCompiler newCompiler(Strategy option) {
		return new PackratCompiler(option);
	}

	protected NezCompiler(Strategy option) {
		super(option);
	}

	public NezCode compile(GenerativeGrammar gg) {
		this.setGenerativeGrammar(gg);
		long t = System.nanoTime();
		UList<Instruction> codeList = new UList<Instruction>(new Instruction[64]);
		for (Production p : gg) {
			if (!p.isSymbolTable()) {
				this.encodeProduction(codeList, p, new IRet(p));
			}
		}
		this.layoutCachedInstruction(codeList);
		for (Instruction inst : codeList) {
			if (inst instanceof ICall) {
				((ICall) inst).sync();
			}

			// add for left recursion supporter
			if (inst instanceof ILRCall) {
				((ILRCall) inst).sync();
			}
			if (inst instanceof ILRGrow) {
				((ILRGrow) inst).sync();
			}

			// Verbose.debug("\t" + inst.id + "\t" + inst);
		}
		long t2 = System.nanoTime();
		Verbose.printElapsedTime("CompilingTime", t, t2);
		return new NezCode(gg, codeList, gg.memoPointList);
	}

	private Production encodingProduction;

	protected final Production getEncodingProduction() {
		return this.encodingProduction;
	}

	private UList<Instruction> cachedInstruction;

	protected void addCachedInstruction(Instruction inst) {
		if (this.cachedInstruction == null) {
			this.cachedInstruction = new UList<Instruction>(new Instruction[32]);
		}
		this.cachedInstruction.add(inst);
	}

	private void layoutCachedInstruction(UList<Instruction> codeList) {
		if (this.cachedInstruction != null) {
			for (Instruction inst : this.cachedInstruction) {
				layoutCode(codeList, inst);
			}
		}
	}

	protected void encodeProduction(UList<Instruction> codeList, Production p, Instruction next) {
		ParseFunc f = this.getParseFunc(p);
		// System.out.println("inline: " + f.inlining + " name: " +
		// p.getLocalName());
		encodingProduction = p;
		if (!f.inlining) {
			next = Coverage.encodeExitCoverage(p, next);
		}
		f.compiled = encode(f.getExpression(), next, null/* failjump */);
		if (!f.inlining) {
			f.compiled = Coverage.encodeEnterCoverage(p, f.compiled);
		}
		Instruction block = new ILabel(p, f.compiled);
		this.layoutCode(codeList, block);
	}

	public final void layoutCode(UList<Instruction> codeList, Instruction inst) {
		if (inst == null) {
			return;
		}
		if (inst.id == -1) {
			inst.id = codeList.size();
			codeList.add(inst);
			layoutCode(codeList, inst.next);
			if (inst.next != null && inst.id + 1 != inst.next.id) {
				Instruction.labeling(inst.next);
			}
			layoutCode(codeList, inst.branch());
			if (inst instanceof IFirst) {
				IFirst match = (IFirst) inst;
				for (int ch = 0; ch < match.jumpTable.length; ch++) {
					layoutCode(codeList, match.jumpTable[ch]);
				}
			}
			// encode(inst.branch2());
		}
	}

}
