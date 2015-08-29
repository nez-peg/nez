package nez.generator;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import nez.NezOption;
import nez.lang.And;
import nez.lang.AnyChar;
import nez.lang.Block;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.Capture;
import nez.lang.MatchSymbol;
import nez.lang.MultiChar;
import nez.lang.Choice;
import nez.lang.DefIndent;
import nez.lang.DefSymbol;
import nez.lang.ExistsSymbol;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.IsIndent;
import nez.lang.IsSymbol;
import nez.lang.Link;
import nez.lang.LocalTable;
import nez.lang.New;
import nez.lang.NonTerminal;
import nez.lang.Not;
import nez.lang.Option;
import nez.lang.Production;
import nez.lang.Repetition;
import nez.lang.Repetition1;
import nez.lang.Replace;
import nez.lang.Sequence;
import nez.lang.Tagging;

public class SimpleCParserGenerator extends ParserGenerator {
	private int fid = 0;
	private int memoSize = 0;
	private ArrayList<String> memorizedTerminalList = new ArrayList<String>();
	private int tableNameCount = 0;
	private ArrayList<String> tableNameList = new ArrayList<String>();
	private FailureOp currentFailureOp = null;

	public static int AstMachineLogSize = 128;
	public static int MemoWindowSize    = 32;

	class FailureOp {
		String blockTerminal;
		String logMarkPoint;
		String symtableMarkPoint;
		String extraOp;
		FailureOp prev;

		FailureOp(String blockTerminal, FailureOp prev) {
			this.blockTerminal     = blockTerminal;
			this.logMarkPoint      = null;
			this.symtableMarkPoint = null;
			this.extraOp           = null;
			this.prev              = prev;
		}
	}

	@Override
	public String getDesc() {
		return "C Parser Generator";
	}

	@Override
	public void generate(Grammar grammar, NezOption option, String fileName) {
		this.setOption(option);
		this.setOutputFile(fileName);
		makeHeader(grammar);
		for(Production p : grammar.getProductionList()) {
			visitProduction(p);
		}
		makeFooter(grammar);
		file.writeNewLine();
		file.flush();
	}

	@Override
	public void makeHeader(Grammar g) {
		this.file.write("#include <stdio.h>");
		this.file.writeIndent("#include <stdlib.h>");
		this.file.writeIndent("#include <string.h>");
		this.file.writeIndent("#include \"node.h\"");
		this.file.writeIndent("#include \"ast.h\"");
		this.file.writeIndent("#include \"memo.h\"");
		this.file.writeIndent("#include \"token.h\"");
		this.file.writeIndent("#include \"symtable.h\"");
		this.file.writeNewLine();

		this.file.writeIndent("#ifdef MOZVM_USE_POINTER_AS_POS_REGISTER");
		this.file.writeIndent("#define MOZ_POS(POS) (POS)");
		this.file.writeIndent("#else");
		this.file.writeIndent("#define MOZ_POS(POS) ((POS) - ctx->source)");
		this.file.writeIndent("#endif");
		this.file.writeNewLine();

		this.startBlock("typedef struct {");
		this.file.writeIndent("char *source;");
		this.file.writeIndent("char *cur;");
		this.file.writeIndent("size_t source_size;");
		this.file.writeIndent("AstMachine *ast;");
		this.file.writeIndent("memo_t *memo;");
		this.file.writeIndent("symtable_t *symtable;");
		this.endBlock("} Context;");
		this.file.writeNewLine();

		for(Production r : g.getProductionList()) {
			if(!r.getLocalName().startsWith("\"")) {
				this.file.writeIndent("int production_" + this.normalizeName(r.getLocalName()) + "(Context *ctx);");
			}
		}
		this.file.writeNewLine();
	}

	@Override
	public void makeFooter(Grammar g) {
		this.startBlock("int main(int argc, char* const argv[]) {");
		this.file.writeIndent("static Context context;");
		this.file.writeIndent("Context *ctx = &context;");
			/*
			this.startBlock("if(argc >= 2) {");
			this.file.writeIndent("ctx->source_size = strlen(argv[1]);");
			this.file.writeIndent("ctx->source = (char *)malloc(ctx->source_size + 1);");
			this.file.writeIndent("strcpy(ctx->source, argv[1]);");
			this.file.writeIndent("ctx->source[ctx->source_size] = '\\0';");
			this.endAndStartBlock("} else {");
			this.file.writeIndent("fprintf(stderr, \"input error: input text is required\\n\");");
			this.file.writeIndent("return 1;");
			this.endBlock("}");
			 */
		this.startBlock("do {", null);
		this.file.writeIndent("FILE *fp = fopen(argv[1], \"rb\");");
		this.startBlock("if(!fp) {");
		this.file.writeIndent("fprintf(stderr, \"fopen error: cannot open file\\n\");");
		this.file.writeIndent("return 1;");
		this.endBlock("}");
		this.file.writeIndent("fseek(fp, 0, SEEK_END);");
		this.file.writeIndent("ctx->source_size = (size_t)ftell(fp);");
		this.file.writeIndent("fseek(fp, 0, SEEK_SET);");
		this.file.writeIndent("ctx->source = (char *)malloc(ctx->source_size + 1);");
		this.startBlock("if(ctx->source_size != fread(ctx->source, 1, ctx->source_size, fp)) {");
		this.file.writeIndent("fprintf(stderr, \"fread error: cannot read file collectly\\n\");");
		this.file.writeIndent("fclose(fp);");
		this.file.writeIndent("free(ctx->source);");
		this.file.writeIndent("return 1;");
		this.endBlock("}");
		this.file.writeIndent("ctx->source[ctx->source_size] = '\\0';");
		this.file.writeIndent("fclose(fp);");
		this.endBlock("} while(0);");
		this.file.writeIndent("ctx->cur = ctx->source;");

		if(this.option.enabledASTConstruction) {
			this.file.writeIndent("NodeManager_init();");
			this.file.writeIndent("ctx->ast = AstMachine_init(" + AstMachineLogSize + ", ctx->source);");
		}
		if(this.option.enabledPackratParsing && this.memoSize > 0) {
			this.file.writeIndent("ctx->memo = memo_init(" + MemoWindowSize + ", " + memoSize + ");");
		}
		if(this.tableNameCount > 0) {
			this.file.writeIndent("ctx->symtable = symtable_init();");
		}

		for(Production r : g.getProductionList()) {
			if(r.getLocalName().replaceAll("&[^&!]+", "").equals("File")) {
				this.startBlock("if(production_" + this.normalizeName(r.getLocalName()) + "(ctx)) {");
				break;
			}
		}
		this.file.writeIndent("fprintf(stderr, \"parse error\\n\");");
		this.file.writeIndent("return 1;");
		this.endAndStartBlock("} else if((ctx->cur - ctx->source) != ctx->source_size) {");
		this.file.writeIndent("fprintf(stderr, \"unconsume\\n\");");
		this.file.writeIndent("return 1;");
		this.endAndStartBlock("} else {");
		if(this.option.enabledASTConstruction) {
			this.file.writeIndent("Node_print(ast_get_parsed_node(ctx->ast));");
		}
		else {
			this.file.writeIndent("fprintf(stderr, \"consume\\n\");");
		}
		this.endBlock("}");

		if(this.tableNameCount > 0) {
			this.file.writeIndent("symtable_dispose(ctx->symtable);");
		}
		if(this.option.enabledPackratParsing && this.memoSize > 0) {
			this.file.writeIndent("memo_dispose(ctx->memo);");
		}
		if(this.option.enabledASTConstruction) {
			this.file.writeIndent("AstMachine_dispose(ctx->ast);");
			this.file.writeIndent("NodeManager_dispose();");
		}
		this.file.writeIndent("free(ctx->source);");
		this.file.writeIndent("return 0;");
		this.endBlock("}");

	}

	private String normalizeName(String name) {
		return name.replaceAll("(_++)", "_$1").replaceAll("&", "_with_").replaceAll("!", "_without_");
	}

	private int getMemoId(String name) {
		if(this.memorizedTerminalList.contains(name)) {
			return this.memorizedTerminalList.indexOf(name);
		}
		else {
			this.memorizedTerminalList.add(name);
			return this.memoSize++;
		}
	}

	private void lookupMemo(int memoId, Consumer<String> ifMemoHit, Supplier<String> ifMemoMiss) {
		int varId = this.fid++;
		String memoEntry = "memo" + varId;
		this.file.writeIndent("MemoEntry_t *" + memoEntry + " = memo_get(ctx->memo, MOZ_POS(ctx->cur), " + memoId + ", 0);"); //FIXME what is state?
		this.startBlock("if(" + memoEntry + " != NULL) {");
		this.startBlock("if(" + memoEntry + "->failed == MEMO_ENTRY_FAILED) {");
		this.failure();
		this.endAndStartBlock("} else {");
		ifMemoHit.accept(memoEntry);
		this.endBlock("}");

		this.endAndStartBlock("} else {");
		this.file.writeIndent("char *c" + varId + " = ctx->cur;");
		this.setExtraOp("memo_fail(ctx->memo, MOZ_POS(c" + varId + "), " + memoId + ");");
		String resultNode = ifMemoMiss.get();
		this.file.writeIndent("memo_set(ctx->memo, MOZ_POS(c" + varId + "), " + memoId + ", " + resultNode + ", ctx->cur - c" + varId + ", 0);"); //FIXME what is state?
		this.endBlock("}");
	}

	private int getTableId(String name) {
		if(this.tableNameList.contains(name)) {
			return this.tableNameList.indexOf(name);
		}
		else {
			this.tableNameList.add(name);
			return this.tableNameCount++;
		}
	}

	private void pushOpFailure(String blockTerminal) {
		this.currentFailureOp = new FailureOp(blockTerminal, this.currentFailureOp);
	}

	private void popOpFailure() {
		this.currentFailureOp = this.currentFailureOp.prev;
	}

	private void setLogMarkPoint(String logMarkPoint) {
		if(this.currentFailureOp.logMarkPoint == null) {
			this.currentFailureOp.logMarkPoint = logMarkPoint;
		}
	}

	private void setSymTableMarkPoint(String symbolMarkPoint) {
		if(this.currentFailureOp.symtableMarkPoint != null) {
			this.currentFailureOp.symtableMarkPoint = symbolMarkPoint;
		}
	}

	private void setExtraOp(String extraOp) {
		if(this.currentFailureOp.extraOp != null) {
			this.currentFailureOp.extraOp = extraOp;
		}
	}

	private void failure() {
		boolean logRollbacked = false;
		boolean symtableRollbacked = false;
		for(FailureOp cur = this.currentFailureOp; cur != null; cur = cur.prev) {
			if(cur.extraOp != null) {
				this.file.writeIndent(cur.extraOp);
			}
			if(!symtableRollbacked && cur.symtableMarkPoint != null) {
				this.file.writeIndent("symtable_rollback(ctx->symtable, " + cur.symtableMarkPoint + ");");
				symtableRollbacked = true;
			}
			if(!logRollbacked && cur.logMarkPoint != null) {
				this.file.writeIndent("ast_rollback_tx(ctx->ast, " + cur.logMarkPoint + ");");
				logRollbacked = true;
			}
			if(cur.blockTerminal != null) {
				this.file.writeIndent(cur.blockTerminal);
				break;
			}
		}
	}

	private void startBlock(String text) {
		this.startBlock(text, null);
	}

	private void startBlock(String text, String blockTerminal) {
		this.pushOpFailure(blockTerminal);
		this.file.writeIndent(text);
		this.file.incIndent();
	}

	private void endBlock(String text) {
		this.file.decIndent();
		this.file.writeIndent(text);
		this.popOpFailure();
	}

	private void endAndStartBlock(String text) {
		this.file.decIndent();
		this.file.writeIndent(text);
		this.currentFailureOp.blockTerminal     = null;
		this.currentFailureOp.logMarkPoint      = null;
		this.currentFailureOp.symtableMarkPoint = null;
		this.currentFailureOp.extraOp           = null;
		this.file.incIndent();
	}

	@Override
	public void visitEmpty(Expression p) {
	}

	@Override
	public void visitFailure(Expression p) {
		throw new RuntimeException("Failure Expression is not implemented");
	}

	@Override
	public void visitAnyChar(AnyChar p) {
		this.startBlock("if(*ctx->cur == 0) {");
		this.failure();
		this.endBlock("}");
		this.file.writeIndent("ctx->cur++;");
	}

	@Override
	public void visitByteChar(ByteChar p) {
		this.startBlock("if((int)*ctx->cur != " + p.byteChar + ") {");
		this.failure();
		this.endBlock("}");
		this.file.writeIndent("ctx->cur++;");
	}

	@Override
	public void visitByteMap(ByteMap p) {
		StringBuilder cond = new StringBuilder();
		boolean b[] = p.byteMap;
		for(int start = 0; start < 256; ++start) {
			if(b[start]) {
				if(cond.length() > 0) {
					cond.append(" && ");
				}
				int end;
				for(end = start; end < 255; ++end) {
					if(!b[end+1]){
						break;
					}
				}
				if(end - start <= 1) {
					cond.append("(int)*ctx->cur != " + start);
				}
				else {
					cond.append("((int)*ctx->cur < " + start + " || " + end + " < (int)*ctx->cur)");
					start = end;
				}
			}
		}
		this.startBlock("if(" + cond.toString() + ") {");
		this.failure();
		this.endBlock("}");
		this.file.writeIndent("ctx->cur++;");
	}

	@Override
	public void visitOption(Option p) {
		int id = this.fid++;
		this.file.writeIndent("char *c" + id + " = ctx->cur;");
		this.startBlock("do {", "break;");
		visitExpression(p.get(0));
		this.file.writeIndent("c" + id + " = ctx->cur;");
		this.endBlock("} while(0);");
		this.file.writeIndent("ctx->cur = c" + id + ";");
	}

	@Override
	public void visitRepetition(Repetition p) {
		int id = this.fid++;
		this.file.writeIndent("char *c" + id + " = ctx->cur;");
		this.startBlock("do {", "break;");
		visitExpression(p.get(0));
		this.file.writeIndent("c" + id + " = ctx->cur;");
		this.endBlock("} while(1);");
		this.file.writeIndent("ctx->cur = c" + id + ";");
	}

	@Override
	public void visitRepetition1(Repetition1 p) {
		visitExpression(p.get(0));
		int id = this.fid++;
		this.file.writeIndent("char *c" + id + " = ctx->cur;");
		this.startBlock("do {", "break;");
		visitExpression(p.get(0));
		this.file.writeIndent("c" + id + " = ctx->cur;");
		this.endBlock("} while(1);");
		this.file.writeIndent("ctx->cur = c" + id + ";");
	}

	@Override
	public void visitAnd(And p) {
		int id = this.fid++;
		this.file.writeIndent("char *c" + id + " = ctx->cur;");
		visitExpression(p.get(0));
		this.file.writeIndent("ctx->cur = c" + id + ";");
	}

	@Override
	public void visitNot(Not p) {
		int id = this.fid++;
		this.file.writeIndent("char *c" + id + " = ctx->cur;");
		this.file.writeIndent("int f" + id + " = 0;");
		this.startBlock("do {", "break;");
		visitExpression(p.get(0));
		this.file.writeIndent("f" + id + " = 1;");
		this.endBlock("} while(0);");
		this.startBlock("if(f" + id + ") {");
		this.failure();
		this.endAndStartBlock("} else {");
		this.file.writeIndent("ctx->cur = c" + id + ";");
		this.endBlock("}");
	}

	@Override
	public void visitSequence(Sequence p) {
		for(int i = 0; i < p.size(); ++i) {
			visitExpression(p.get(i));
		}
	}

	@Override
	public void visitChoice(Choice p) {
		int id = this.fid++;
		this.file.writeIndent("char *c" + id + " = ctx->cur;");

		this.file.writeIndent("int i" + id + ";");
		this.startBlock("for(i" + id + " = 0; i" + id + " < " + p.size() + "; ++i" + id + ") {", "continue;");
		this.file.writeIndent("ctx->cur = c" + id + ";");
		//this.file.writeIndent("ctx->choiceCount++;");

		this.startBlock("switch(i" + id + ") {");
		for(int i = 0; i < p.size(); ++i) {
			this.endAndStartBlock("case " + i + ":");
			this.file.writeIndent(";");
			visitExpression(p.get(i));
			this.file.writeIndent("break;"); //switch
		}
		this.endBlock("}");

		this.file.writeIndent("break;"); //for
		this.endBlock("}");

		this.startBlock("if(i" + id + " == " + p.size() + ") {");
		this.failure();
		this.endBlock("}");
	}

	@Override
	public void visitNonTerminal(NonTerminal p) {
		this.startBlock("if(production_" + this.normalizeName(p.getLocalName()) + "(ctx)) {");
		this.failure();
		this.endBlock("}");
	}

	@Override
	public void visitCharMultiByte(MultiChar p) {
		StringBuilder cond = new StringBuilder();
		for(int i = 0; i < p.byteSeq.length; ++i) {
			if(cond.length() > 0) {
				cond.append(" || ");
			}
			cond.append("(int)(ctx->cur[" + i + "]) != " + p.byteSeq[i]);
		}
		this.startBlock("if(" + cond.toString() + ") {");
		this.failure();
		this.endBlock("}");
		this.file.writeIndent("ctx->cur += " + p.byteSeq.length + ";");
	}

	@Override
	public void visitLink(Link p) {
		if(this.option.enabledASTConstruction) {
			if(this.option.enabledPackratParsing && p.get(0) instanceof NonTerminal) {
				NonTerminal n = (NonTerminal)p.get(0);
				this.lookupMemo(this.getMemoId(n.getLocalName()),
						(String memoEntry) -> {
							this.file.writeIndent("ctx->cur += " + memoEntry + "->consumed;");
							this.file.writeIndent("ast_log_link(ctx->ast, -1, " + memoEntry + "->result);");
						},
						() -> {
							int id = this.fid++;
							this.file.writeIndent("long mark" + id + " = ast_save_tx(ctx->ast);");
							this.setLogMarkPoint("mark" + id);
							this.visitExpression(n);
							this.file.writeIndent("ast_commit_tx(ctx->ast, -1, mark" + id + ");");
							this.file.writeIndent("Node r" + id + " = ast_get_last_linked_node(ctx->ast);");
							return "r" + id;
						}
				);
			}
			else {
				int id = this.fid++;
				this.file.writeIndent("long mark" + id + " = ast_save_tx(ctx->ast);");
				this.setLogMarkPoint("mark" + id);
				visitExpression(p.get(0));
				this.file.writeIndent("ast_commit_tx(ctx->ast, -1, mark" + id + ");");
			}
		}
		else {
			visitExpression(p.get(0));
		}
	}

	@Override
	public void visitNew(New p) {
		if(this.option.enabledASTConstruction) {
			int id = this.fid++;
			this.file.writeIndent("long mark" + id + " = ast_save_tx(ctx->ast);");
			this.setLogMarkPoint("mark" + id);
			if(p.leftFold) {
				this.file.writeIndent("ast_log_swap(ctx->ast, MOZ_POS(ctx->cur));");
			}
			else {
				this.file.writeIndent("ast_log_new(ctx->ast, MOZ_POS(ctx->cur));");
			}
		}
	}

	@Override
	public void visitCapture(Capture p) {
		if(this.option.enabledASTConstruction) {
			this.file.writeIndent("ast_log_capture(ctx->ast, MOZ_POS(ctx->cur));");
		}
	}

	@Override
	public void visitTagging(Tagging p) {
		if(this.option.enabledASTConstruction) {
			this.file.writeIndent("ast_log_tag(ctx->ast, \"" + p.tag.getName() + "\");");
		}
	}

	@Override
	public void visitReplace(Replace p) {
		if(this.option.enabledASTConstruction) {
			this.file.writeIndent("ast_log_replace(ctx->ast, \"" + p.value + "\");");
		}
	}

	@Override
	public void visitBlock(Block p) {
		int id = this.fid++;
		this.file.writeIndent("long mark" + id + " = symtable_savepoint(ctx->symtable);");
		this.setSymTableMarkPoint("mark" + id);
		this.visitExpression(p.get(0));
		this.file.writeIndent("symtable_rollback(ctx->symtable, mark" + id + ");");
		this.currentFailureOp.symtableMarkPoint = null;
	}

	@Override
	public void visitDefSymbol(DefSymbol p) {
		int id = this.fid++;
		int tableId = this.getTableId(p.getTableName());
		this.file.writeIndent("long mark" + id + " = symtable_savepoint(ctx->symtable);");
		this.setSymTableMarkPoint("mark" + id);
		this.file.writeIndent("char *c" + id + " = ctx->cur;");
		this.visitExpression(p.get(0));
		this.file.writeIndent("token_t t" + id + ";");
		this.file.writeIndent("token_init(&t" + id + ", c" + id + ", ctx->cur);");
		this.file.writeIndent("symtable_add_symbol(ctx->symtable, (uintptr_t)" + tableId + ", &t" + id + ");");
	}

	@Override
	public void visitMatchSymbol(MatchSymbol p) {
		throw new RuntimeException("MatchSymbol Expression is not implemented");
	}

	@Override
	public void visitIsSymbol(IsSymbol p) {
		int id = this.fid++;
		int tableId = this.getTableId(p.getTableName());
		this.file.writeIndent("token_t t" + id + ";");
		if(p.is) {
			this.startBlock("if(!symtable_get_symbol(ctx->symtable, (uintptr_t)" + tableId + ", &t" + id + ") || !token_equal_string(&t" + id + ", ctx->cur)) {");
			this.failure();
			this.endBlock("}");
		}
		else {
			throw new RuntimeException("IsaSymbol Expression is not implemented");
			//this.file.writeIndent("char *c" + id + " = nez_isaSymbol(ctx, " + tableId + ", ctx->cur);");
		}
		this.file.writeIndent("ctx->cur += token_length(&t" + id + ");");
	}

	@Override
	public void visitDefIndent(DefIndent p) {
		throw new RuntimeException("DefIndent Expression is not implemented");
	}

	@Override
	public void visitIsIndent(IsIndent p) {
		throw new RuntimeException("IsIndent Expression is not implemented");
	}

	@Override
	public void visitExistsSymbol(ExistsSymbol p) {
		int tableId = this.getTableId(p.getTableName());
		this.startBlock("if(!symtable_has_symbol(ctx->symtable, (uintptr_t)" + tableId + ")) {");
		this.failure();
		this.endBlock("}");
	}

	@Override
	public void visitLocalTable(LocalTable p) {
		throw new RuntimeException("LocalTable Expression is not implemented");
	}

	@Override
	public void visitProduction(Production r) {
		this.fid = 0;
		this.startBlock("int production_" + this.normalizeName(r.getLocalName()) + "(Context *ctx) {", "return 1;");
		if(this.option.enabledPackratParsing && (!this.option.enabledASTConstruction || r.isNoNTreeConstruction())) {
			this.lookupMemo(this.getMemoId(r.getLocalName()),
					(String memoEntry) -> this.file.writeIndent("ctx->cur += " + memoEntry + "->consumed;"),
					() -> {
						this.visitExpression(r.getExpression());
						return "NULL";
					}
			);
		}
		else {
			this.visitExpression(r.getExpression());
		}
		this.file.writeIndent("return 0;");
		this.endBlock("}");
		this.file.writeNewLine();
	}

}
