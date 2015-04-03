package nez.cc;

import java.lang.reflect.Constructor;
import java.util.TreeMap;

import nez.Grammar;
import nez.main.Command;
import nez.main.CommandConfigure;
import nez.main.Verbose;
import nez.util.ConsoleUtils;

public class GrammarCommand extends Command {

	@Override
	public String getDesc() {
		return "grammar converter";
	}

	@Override
	public void exec(CommandConfigure config) {
		Grammar peg = config.getGrammar();
		GrammarGenerator gen = loadGenerator(config.getOutputFileName());
		gen.generate(peg);
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
		regist("nez", "nez.cc.NezGrammarGenerator");
		regist("mouse", "nez.cc.MouseGrammarGenerator");
		regist("lua",   "nez.cc.LPegGrammarGenerator");
		regist("lpeg",  "nez.cc.LPegGrammarGenerator");
	}
	
	final GrammarGenerator loadGenerator(String output) {
		if(output != null) {
			GrammarGenerator gen = null;
			String type = output;
			String fileName = null; // stdout
			int loc = output.lastIndexOf('.');
			if(loc > 0) {
				type = output.substring(loc+1);
				fileName = output;
			}
			Class<?> c = classMap.get(type);
			if(c == null) {
				fileName = null;
				try {
					c = Class.forName(output);
				} catch (ClassNotFoundException e) {
					showOutputType(output);
				}
			}
			try {
				Constructor<?> ct = c.getConstructor(String.class);
				gen = (GrammarGenerator)ct.newInstance(fileName);
			}
			catch(Exception e) {
				ConsoleUtils.exit(1, "unable to load: " + output + " due to " + e);
			}
			return gen;
		}
		return new NezGrammarGenerator(null);
	}
	
	void showOutputType(String output) {
		ConsoleUtils.println("Nez Grammar Generator");
		try {
			for(String n : this.classMap.keySet()) {
				String dummy = null;
				Class<?> c = this.classMap.get(n);
				Constructor<?> ct = c.getConstructor(String.class);
				GrammarGenerator g = (GrammarGenerator)ct.newInstance(dummy);
				String s = String.format("%8s - %s", n, g.getDesc());
				ConsoleUtils.println(s);
			}
			ConsoleUtils.exit(1, "Unknown output type ("+ output + ") => Try the above !!");
		}
		catch(Exception e) {
			e.printStackTrace();
			ConsoleUtils.exit(1, "killed");
		}
	}

}

