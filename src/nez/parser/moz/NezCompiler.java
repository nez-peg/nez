package nez.parser.moz;

import nez.lang.Production;
import nez.parser.AbstractGenerator;
import nez.parser.Coverage;
import nez.parser.ParserGrammar;
import nez.parser.ParseFunc;
import nez.parser.ParserStrategy;
import nez.util.UList;
import nez.util.Verbose;

public abstract class NezCompiler extends AbstractGenerator {

	public final static NezCompiler newCompiler(ParserStrategy option) {
		return new PackratCompiler(option);
	}

	protected NezCompiler(ParserStrategy option) {
		super(option);
	}

	public MozCode compile(ParserGrammar gg) {
		this.setGenerativeGrammar(gg);
		long t = System.nanoTime();
		UList<MozInst> codeList = new UList<MozInst>(new MozInst[64]);
		for (Production p : gg) {
			if (!p.isSymbolTable()) {
				this.encodeProduction(codeList, p, new Moz.Ret(p));
			}
		}
		this.layoutCachedInstruction(codeList);
		for (MozInst inst : codeList) {
			if (inst instanceof Moz.Call) {
				((Moz.Call) inst).sync();
			}
			// Verbose.debug("\t" + inst.id + "\t" + inst);
		}
		long t2 = System.nanoTime();
		Verbose.printElapsedTime("CompilingTime", t, t2);
		return new MozCode(gg, codeList, gg.memoPointList);
	}

	private Production encodingProduction;

	protected final Production getEncodingProduction() {
		return this.encodingProduction;
	}

	private UList<MozInst> cachedInstruction;

	protected void addCachedInstruction(MozInst inst) {
		if (this.cachedInstruction == null) {
			this.cachedInstruction = new UList<MozInst>(new MozInst[32]);
		}
		this.cachedInstruction.add(inst);
	}

	private void layoutCachedInstruction(UList<MozInst> codeList) {
		if (this.cachedInstruction != null) {
			for (MozInst inst : this.cachedInstruction) {
				layoutCode(codeList, inst);
			}
		}
	}

	protected void encodeProduction(UList<MozInst> codeList, Production p, MozInst next) {
		ParseFunc f = this.getParseFunc(p);
		// System.out.println("inline: " + f.inlining + " name: " +
		// p.getLocalName());
		encodingProduction = p;
		if (!f.isInlined()) {
			next = Coverage.encodeExitCoverage(p, next);
		}
		f.setCompiled(encode(f.getExpression(), next, null/* failjump */));
		if (!f.isInlined()) {
			f.setCompiled(Coverage.encodeEnterCoverage(p, (MozInst) f.getCompiled()));
		}
		MozInst block = new Moz.Label(p, (MozInst) f.getCompiled());
		this.layoutCode(codeList, block);
	}

	public final void layoutCode(UList<MozInst> codeList, MozInst inst) {
		if (inst == null) {
			return;
		}
		if (inst.id == -1) {
			inst.id = codeList.size();
			codeList.add(inst);
			layoutCode(codeList, inst.next);
			if (inst.next != null && inst.id + 1 != inst.next.id) {
				MozInst.labeling(inst.next);
			}
			layoutCode(codeList, inst.branch());
			if (inst instanceof Moz.First) {
				Moz.First match = (Moz.First) inst;
				for (int ch = 0; ch < match.jumpTable.length; ch++) {
					layoutCode(codeList, match.jumpTable[ch]);
				}
			}
			// encode(inst.branch2());
		}
	}

}
