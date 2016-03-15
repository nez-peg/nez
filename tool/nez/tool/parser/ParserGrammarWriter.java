package nez.tool.parser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Production;
import nez.parser.Parser;
import nez.parser.ParserStrategy;
import nez.util.ConsoleUtils;
import nez.util.FileBuilder;

public class ParserGrammarWriter {
	protected Parser parser;
	protected Grammar grammar;
	protected ParserStrategy strategy;
	protected String fileExt;

	public ParserGrammarWriter(String fileExt) {
		this.file = null;
		this.fileExt = fileExt;
	}

	protected String fileBase = "noname";
	protected FileBuilder file = null;

	public final void init(Parser parser) {
		this.parser = parser;
		this.grammar = parser.getGrammar();
		this.strategy = parser.getParserStrategy();
		String urn = parser.getGrammar().getURN();
		if (urn != null) {
			int l = urn.lastIndexOf("/");
			if (l == -1) {
				l = urn.lastIndexOf("\\");
			}
			if (l != -1) {
				urn = urn.substring(l + 1);
			}
			l = urn.indexOf(".");
			if (l > 0) {
				urn = urn.substring(0, l);
			}
			this.setFileBase(urn);
		}
		this.setFileBuilder(this.fileExt);
	}

	public void setFileBuilder(String fileExt) {
		if (this.file != null) {
			this.file.writeNewLine();
			this.file.close();
		}
		if (fileExt != null) {
			String filename = this.fileBase + fileExt;
			ConsoleUtils.println("generating %s ...", filename);
			this.file = new FileBuilder(filename);
		} else {
			this.file = null;
		}
	}

	public void setFileBase(String base) {
		this.fileBase = base;
	}

	public void generate() {
		for (Production p : this.grammar) {
			String name = p.getUniqueName();
			Expression e = p.getExpression();
			file.writeIndent("%s = %s", name, e);
		}
	}

	public final void importFileContent(String path) {
		importFileContent(path, null);
	}

	public final void importFileContent(String path, String[] re) {
		try {
			if (!path.startsWith("/")) {
				path = "/nez/include/" + path;
			}
			InputStream s = ParserGrammarWriter.class.getResourceAsStream(path);
			BufferedReader reader = new BufferedReader(new InputStreamReader(s));
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (re != null) {
					for (int i = 0; i < re.length; i += 2) {
						line = line.replace(re[i], re[i + 1]);
					}
				}
				file.writeIndent(line);
			}
			reader.close();
		} catch (Exception e) {
			ConsoleUtils.exit(1, "cannot load " + path + "; " + e);
		}
	}

	public final void showFileContent(String path) {
		importFileContent(path, null);
	}

	public final void showFileContent(String path, String[] re) {
		try {
			if (!path.startsWith("/")) {
				path = "/nez/include/" + path;
			}
			InputStream s = ParserGrammarWriter.class.getResourceAsStream(path);
			BufferedReader reader = new BufferedReader(new InputStreamReader(s));
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (re != null) {
					for (int i = 0; i < re.length; i += 2) {
						line = line.replace(re[i], re[i + 1]);
					}
				}
				ConsoleUtils.println(line);
			}
			reader.close();
		} catch (Exception e) {
			ConsoleUtils.exit(1, "cannot load " + path + "; " + e);
		}
	}

	public final void showManual(String path, String[] re) {
		ConsoleUtils.bold();
		ConsoleUtils.println("Here are some useful commands:");
		ConsoleUtils.end();
		showFileContent(path, re);
	}

	//
	protected final void Verbose(String stmt) {
		if (strategy.VerboseCode) {
			LineComment(stmt);
		}
	}

	protected void Line(String stmt) {
		file.writeIndent(stmt);
	}

	protected String _LineComment() {
		return "//";
	}

	protected void LineComment(String stmt) {
		file.writeIndent(_LineComment() + " " + stmt);
	}

}
