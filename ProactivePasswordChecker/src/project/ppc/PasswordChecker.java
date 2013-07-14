package project.ppc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import project.lm.GoodTuringModel;
import project.lm.Gram;
import project.lm.KatzBackoffModel;
import project.lm.LanguageModel;
import project.lm.Perplexity;
import project.lm.Vocabulary;

public class PasswordChecker {
	public static void main(String[] args) {
		File trainingSetFile = new File(args[0]);
		File testSetFile = new File(args[1]);
		LanguageModel lm = new KatzBackoffModel(3, 0.1);
		//LanguageModel lm = new GoodTuringModel(3);
		try {
			lm.vocabulary(new Vocabulary());
			lm.trainingSet(trainingSetFile);
			Perplexity p = new Perplexity(lm);
			double evaluation = p.test(testSetFile);
			double p1 = lm.test("aaaa");
			double p2 = lm.test("a%%%");
			double p3 = lm.test("aa%%");
			double p4 = lm.test("aaa%");
			double p5 = lm.test("a%b%");
			double p6 = lm.test("8shmone8");
			double p7 = lm.test("%a$m^!#t8");
			double p8 = lm.test("amit");
			double p9 = lm.test("conner");
			System.out.println("perplexity = " + evaluation);
			System.out.println("p1 = " + p1);
			System.out.println("p2 = " + p2);
			System.out.println("p3 = " + p3);
			System.out.println("p4 = " + p4);
			System.out.println("p5 = " + p5);
			System.out.println("p6 = " + p6);
			System.out.println("p7 = " + p7);
			System.out.println("p8 = " + p8);
			System.out.println("p9 = " + p9);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
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