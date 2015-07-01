package nez;

import nez.main.Verbose;

public class NezOption {
		
	public final static NezOption newDefaultOption() {
		return new NezOption();
	}

	public final static NezOption newSafeOption() {
		return new NezOption("-memo:-predict");
	}
	
	public boolean enabledSafeMode        = true;
	public boolean enabledExperimental    = false;
	
	/* grammar */
	public boolean enabledASTConstruction = true;      // ast
	public boolean enabledSymbolTable     = true;      // symbol
	
	/* optimization */
	public boolean enabledAsIsGrammar     = false; // asis
	public boolean enabledInlining        = true;      // inline

	public boolean enabledLexicalOptimization = true;  // lex
	//public boolean enabledCommonFactored  = true;      // common
	
	public boolean enabledPrediction      = true;      // predict
	public boolean enabledDFAConversion   = false;     // dfa

	/* runtime option */
	public boolean enabledMemoization     = true;  // memo
	public boolean enabledPackratParsing  = false; // packrat

	/* misc */
	public boolean enabledInterning            = true;
	public boolean enabledExampleVerification  = false;
	public boolean enabledProfiling            = false;
	
	public boolean enabledNoticeReport = true;     // notice
	
	public NezOption() {
	}

	public NezOption(String arguments) {
		this.setOption(arguments);
	}

	public NezOption clone() {
		NezOption o = new NezOption();
		o.enabledSafeMode = this.enabledSafeMode;
		o.enabledExperimental = this.enabledExperimental;
		o.enabledASTConstruction = this.enabledASTConstruction;
		o.enabledSymbolTable = this.enabledSymbolTable;
		o.enabledAsIsGrammar = this.enabledAsIsGrammar;
		o.enabledLexicalOptimization = this.enabledLexicalOptimization;
		o.enabledInlining = this.enabledInlining;
//		o.enabledCommonFactored = this.enabledCommonFactored;
		o.enabledPrediction = this.enabledPrediction;
		o.enabledMemoization = this.enabledMemoization;
		o.enabledPackratParsing = this.enabledPackratParsing;
		o.enabledInterning = this.enabledInterning;
		o.enabledExampleVerification = this.enabledExampleVerification;
		o.enabledProfiling = this.enabledProfiling;
		o.enabledNoticeReport = this.enabledNoticeReport;
		return o;
	}
	
	public final void setOption(String args) {
		for(String s : args.split(":")) {
			if(s.startsWith("+")) {
				setOption(s.substring(1),true);
			}
			else if(s.startsWith("-")) {
				setOption(s.substring(1),false);
			}
			else {
				setOption(s,true);
			}
		}
	}
	
	public final void setOption(String key, boolean value) {
		switch(key) {
		case "asis":
			this.enabledAsIsGrammar = value;
			break;
		case "ast":
			this.enabledASTConstruction = value;
			break;
		case "dfa":
			this.enabledDFAConversion = value;
			break;
		case "example":
			this.enabledExampleVerification = value;
			break;
		case "inline":
			this.enabledInlining = value;
			break;
		case "intern":
			this.enabledInterning = value;
			break;
		case "lex" : case "lexer":
			this.enabledLexicalOptimization = value;
			break;
		case "memo":
			this.enabledMemoization = value;
			break;
		case "notice":
			this.enabledNoticeReport = value;
			break;
		case "packrat":
			this.enabledPackratParsing = value;
			break;
		case "predict":
			this.enabledPrediction = value;
			break;
		case "prof":
			this.enabledProfiling = value;
			break;
		case "safe":
			this.enabledSafeMode = value;
			break;
		case "symbol":
			this.enabledSymbolTable = value;
			break;
		default:
			Verbose.debug("undefined option:" + key + " " + value);
		}
	}
	
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		if(this.enabledSafeMode) {
			sb.append(":safe");
		}
		if(this.enabledASTConstruction) {
			sb.append(":ast");
		}
		if(this.enabledSymbolTable) {
			sb.append(":symbol");
		}
		if(this.enabledAsIsGrammar) {
			sb.append(":asis");
		}
		if(this.enabledLexicalOptimization) {
			sb.append(":lexer");
		}
		if(this.enabledPrediction) {
			sb.append(":predict");
		}
		if(this.enabledMemoization) {
			sb.append(":memo");
		}
		if(this.enabledPackratParsing) {
			sb.append(":packrat");
		}
		if(this.enabledExampleVerification) {
			sb.append(":example");
		}
		String s = sb.toString();
		if(s.length() > 0) {
			s = s.substring(1);
		}
		return s;
	}
	
}
