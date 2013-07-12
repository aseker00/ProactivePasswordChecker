package project.lm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;

public class Test {
	public static void main(String[] args) {
		File vocabularyFile = new File(args[0]);
		File trainingSetFile = new File(args[1]);
		File testSetFile = new File(args[2]);
		File corpusFile = new File(args[3]);
		//LanguageModel lm = new KatzBackoffModel(3, 0.1);
		LanguageModel lm = new GoodTuringModel(3);
		try {
			lm.vocabulary(loadVocabulary(vocabularyFile));
			lm.trainingSet(trainingSetFile);
			//double p1 = lm.stringProbability("aaaa");
			//double p2 = lm.stringProbability("a%%%");
			Perplexity p = new Perplexity(lm);
			double evaluation = p.test(testSetFile);
			//lm.trainingSet(corpusFile);
			double p1 = lm.test("aaaa");
			double p2 = lm.test("a%%%");
			double p3 = lm.test("aa%%");
			double p4 = lm.test("aaa%");
			System.out.println("done.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static HashSet<Gram> loadVocabulary(File f) throws IOException {
		HashSet<Gram> v = new HashSet<Gram>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
		String line = null;
		while ((line = br.readLine()) != null) {
			for (int i = 0; i < line.length(); i++) {
				Gram g = new Gram(line.charAt(i));
				v.add(g);
			}
		}
		br.close();
		return v;
	}
}
