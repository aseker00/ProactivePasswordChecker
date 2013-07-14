package project.ppc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import project.lm.GoodTuringModel;
import project.lm.Gram;
import project.lm.LanguageModel;
import project.lm.Perplexity;
import project.lm.Vocabulary;

public class PasswordChecker {
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
			double p5 = lm.test("a%b%");
			double p6 = lm.test("8shmone8");
			double p7 = lm.test("%a$m^!#t8");
			System.out.println("perplexity = " + evaluation);
			System.out.println("p1 = " + p1);
			System.out.println("p2 = " + p2);
			System.out.println("p3 = " + p3);
			System.out.println("p4 = " + p4);
			System.out.println("p5 = " + p5);
			System.out.println("p6 = " + p6);
			System.out.println("p7 = " + p7);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static Vocabulary loadVocabulary(File f) throws IOException {
		Vocabulary v = new Vocabulary();
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