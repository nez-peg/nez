package nez.x;

import java.lang.reflect.Constructor;
import java.util.TreeMap;

import nez.Grammar;
import nez.Production;
import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeWriter;
import nez.main.Command;
import nez.main.CommandConfigure;
import nez.main.Recorder;
import nez.main.Verbose;
import nez.util.ConsoleUtils;

public class ConverterCommand extends Command {
	@Override
	public String getDesc() {
		return "converter? FIXME";
	}

	@Override
	public void exec(CommandConfigure config) {
		Recorder rec = config.getRecorder();
		Production p = config.getProduction(config.StartingPoint);
		p.record(rec);
		while(config.hasInput()) {
			SourceContext file = config.getInputSourceContext();
			file.start(rec);
			CommonTree node = p.parse(file);
			file.done(rec);
			if(node == null) {
				ConsoleUtils.println(file.getSyntaxErrorMessage());
				continue;
			}
			if(file.hasUnconsumed()) {
				ConsoleUtils.println(file.getUnconsumedMessage());
			}
			if(rec != null) {
				rec.log();
			}
			new CommonTreeWriter().transform(config.getOutputFileName(file), node);
			String outputfile = config.getOutputFileName();
			if (outputfile == null) {
				outputfile = file.getResourceName() + ".nez";
				int index = outputfile.indexOf("/");
				while(index > -1) {
					outputfile = outputfile.substring(index+1);
					index = outputfile.indexOf("/");
				}
				outputfile = "gen/" + outputfile;
			}
			GrammarConverter conv = loadConverter(new Grammar(file.getResourceName()), outputfile);
			conv.convert(node);
		}
	}
	
	static private TreeMap<String, Class<?>> classMap = new TreeMap<String, Class<?>>();
	static void regist(String type, String className) {
		try {
			Class<?> c = Class.forName(className);
			classMap.put(type, c);
		} catch (ClassNotFoundException e) {
			Verbose.println("unfound class: " + className);
		}
	}
	
	static {
		regist("regex", "nez.x.RegexConverter");
		regist("dtd", "nez.x.DTDConverter");
	}
	
	final GrammarConverter loadConverter(Grammar peg, String name) {
		String input = peg.getResourceName();
		if(input != null) {
			GrammarConverter conv = null;
			String type = input;
			int loc = input.lastIndexOf('.');
			if(loc > 0) {
				type = input.substring(loc+1);
			}
			Class<?> c = classMap.get(type);
			if(c == null) {
				try {
					c = Class.forName(input);
				} catch (ClassNotFoundException e) {
					showInputType(input);
				}
			}
			try {
				Constructor<?> ct = c.getConstructor(Grammar.class, String.class);
				conv = (GrammarConverter)ct.newInstance(peg, name);
			}
			catch(Exception e) {
				ConsoleUtils.exit(1, "unable to load: " + input + " due to " + e);
			}
			return conv;
		}
		ConsoleUtils.exit(1, "Error: input file not found");
		return null;
	}
	
	void showInputType(String input) {
		ConsoleUtils.println("Nez Grammar Generator");
		try {
			for(String n : this.classMap.keySet()) {
				Class<?> c = this.classMap.get(n);
				Constructor<?> ct = c.getConstructor(Grammar.class, String.class);
				GrammarConverter g = (GrammarConverter)ct.newInstance(null, null);
				String s = String.format("%8s - %s", n, g.getDesc());
				ConsoleUtils.println(s);
			}
			ConsoleUtils.exit(1, "Unknown output type ("+ input + ") => Try the above !!");
		}
		catch(Exception e) {
			e.printStackTrace();
			ConsoleUtils.exit(1, "killed");
		}
	}
}
