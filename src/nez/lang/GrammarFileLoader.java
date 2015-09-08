package nez.lang;

import java.util.HashMap;

public class GrammarFileLoader {

	static HashMap<String, GrammarFileLoader> loaderMap = new HashMap<>();

	// public final static GrammarFile loadGrammarFile(String urn, NezOption
	// option) throws IOException {
	// String ext = StringUtils.extractFileExtension(urn);
	// if (ext == null) {
	// NezGrammarLoader loader = loaderMap.get(ext);
	// }
	// if (urn.endsWith(".dtd")) {
	// return DTDConverter.loadGrammar(urn, option);
	// }
	// if (urn.endsWith(".celery")) {
	// return Celery.loadGrammar(urn, option);
	// }
	// return GrammarFile.loadNezFile(urn, option);
	// }

}
