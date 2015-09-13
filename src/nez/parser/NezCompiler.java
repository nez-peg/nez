package nez.parser;

import nez.NezOption;
import nez.lang.Production;
import nez.main.Verbose;
import nez.util.UList;

public abstract class NezCompiler extends AbstractGenerator {

	public final static NezCompiler newCompiler(NezOption option) {
		return new PackratCompiler(option);
	}

	protected NezCompiler(NezOption option) {
		super(option);
	}

	public final NezCode compile(GenerativeGrammar g) {
		return this.compile(g, null);
	}

	public NezCode compile(GenerativeGrammar gg, ByteCoder coder) {
		this.setGenerativeGrammar(gg);
		long t = System.nanoTime();
		UList<Instruction> codeList = new UList<Instruction>(new Instruction[64]);
		for (Production p : gg) {
			this.encodeProduction(codeList, p, new IRet(p));
		}
		for (Instruction inst : codeList) {
			if (inst instanceof ICall) {
				ParseFunc deref = this.getParseFunc(((ICall) inst).prod);
				if (deref == null) {
					Verbose.debug("no parse func: " + ((ICall) inst).prod.getLocalName());
				}
				((ICall) inst).setResolvedJump(deref.compiled);
			}
			// Verbose.debug("\t" + inst.id + "\t" + inst);
		}
		long t2 = System.nanoTime();
		Verbose.printElapsedTime("CompilingTime", t, t2);
		if (coder != null) {
			coder.setHeader(codeList.size(), this.getParseFuncSize(), gg.memoPointList == null ? 0 : gg.memoPointList.size());
			coder.setInstructions(codeList.ArrayValues, codeList.size());
		}
		return new NezCode(codeList.ArrayValues[0], codeList.size(), gg.memoPointList);
	}

	// public final NezCode compile(Parser grammar) {
	// return this.compile(grammar, null);
	// }
	//
	// public NezCode compile(Parser grammar, ByteCoder coder) {
	// long t = System.nanoTime();
	// List<MemoPoint> memoPointList = null;
	// if (option.enabledMemoization || option.enabledPackratParsing) {
	// memoPointList = new UList<MemoPoint>(new MemoPoint[4]);
	// }
	// initParseFuncMap(grammar, memoPointList);
	// UList<Instruction> codeList = new UList<Instruction>(new
	// Instruction[64]);
	// // Production start = grammar.getStartProduction();
	// // this.encodeProduction(codeList, start, new IRet(start));
	// // for(Production p : grammar.getProductionList()) {
	// // if(p != start) {
	// // this.encodeProduction(codeList, p, new IRet(p));
	// // }
	// // }
	// for (Production p : grammar.getProductionList()) {
	// this.encodeProduction(codeList, p, new IRet(p));
	// }
	// for (Instruction inst : codeList) {
	// if (inst instanceof ICall) {
	// ParseFunc deref = this.getParseFunc(((ICall) inst).prod);
	// if (deref == null) {
	// Verbose.debug("no deref: " + ((ICall) inst).prod.getUniqueName());
	// }
	// ((ICall) inst).setResolvedJump(deref.compiled);
	// }
	// // Verbose.debug("\t" + inst.id + "\t" + inst);
	// }
	// long t2 = System.nanoTime();
	// Verbose.printElapsedTime("CompilingTime", t, t2);
	// if (coder != null) {
	// coder.setHeader(codeList.size(), this.getParseFuncSize(), memoPointList
	// == null ? 0 : memoPointList.size());
	// coder.setInstructions(codeList.ArrayValues, codeList.size());
	// }
	// return new NezCode(codeList.ArrayValues[0], codeList.size(),
	// memoPointList);
	// }

	private Production encodingProduction;

	protected final Production getEncodingProduction() {
		return this.encodingProduction;
	}

	protected void encodeProduction(UList<Instruction> codeList, Production p, Instruction next) {
		ParseFunc f = this.getParseFunc(p);
		encodingProduction = p;
		f.compiled = encode(f.e, next, null/* failjump */);
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
