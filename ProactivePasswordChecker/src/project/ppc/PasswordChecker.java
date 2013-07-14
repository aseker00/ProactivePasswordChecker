package project.ppc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

import project.lm.FrequencyMatrix;
import project.lm.GoodTuringModel;
import project.lm.Gram;
import project.lm.KatzBackoffModel;
import project.lm.LanguageModel;
import project.lm.NGram;
import project.lm.Perplexity;

public class PasswordChecker {
	
	private LanguageModel lm;
	private double threshold;
	private Properties configuration;
	
	public PasswordChecker(File configurationFile) throws Exception {
		this.configuration = new Properties();
		FileInputStream fis = new FileInputStream(configurationFile);
		configuration.load(fis);
		fis.close();
		load();
	}
	
	public LanguageModel getLanguageModel() {
		return this.lm;
	}
	
	public double getThreshold() {
		return this.threshold;
	}
	
	public void load() throws Exception {
		String model = this.configuration.getProperty("model");
		int order = Integer.parseInt(this.configuration.getProperty("order"));
		double mu = Double.parseDouble(this.configuration.getProperty("mu"));
		double sigma = Double.parseDouble(this.configuration.getProperty("sigma"));
		this.threshold = Double.parseDouble(this.configuration.getProperty("threshold"));
		if (model.equals("gt"))
			this.lm = new GoodTuringModel(order);
		else if (model.equals("kb")) {
			double discount = Double.parseDouble(this.configuration.getProperty("kb.discount"));
			this.lm = new KatzBackoffModel(order, discount);
		}
		else
			throw new Exception("invalid language model: " + model);
		this.lm.setMu(mu);
		this.lm.setSigma(sigma);
	}
	
	public void save() {
		this.configuration.setProperty("mu", String.valueOf(this.lm.getMu()));
		this.configuration.setProperty("sigma", String.valueOf(this.lm.getSigma()));
	}
	
	public void saveTransitionMatrix(File outputFile) throws FileNotFoundException {
		PrintStream ps = new PrintStream(outputFile);
		Iterator<NGram> tIter = lm.getTransitionProbabilityMatrix().iterator();
		while (tIter.hasNext()) {
			NGram ngram = tIter.next();
			ps.println(ngram + "\t" + lm.getTransitionProbabilityMatrix().ngramFrequency(ngram));
		}
		ps.close();
	}
	
	public void loadTransitionMatrix(File f) throws IOException {
		FrequencyMatrix tm = new FrequencyMatrix();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
		String line = null;
		while ((line = br.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line, "\t");
			String s = st.nextToken();
			double d = Double.parseDouble(st.nextToken());
			NGram ngram = new NGram(lm.getOrder());
			for (int i = 0; i < lm.getOrder(); i++) {
				ngram.gram(i, new Gram(s.charAt(i)));
			}
			tm.ngramFrequency(ngram, d);
		}
		br.close();
		this.lm.setTransitionProbabilityMatrix(tm);
	}
	
	public static void main(String[] args) {
		PasswordChecker pc = null;
		try {
			pc = new PasswordChecker(new File("project.properties"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
		try {
			String action = args[0];
			if (action.equals("train")) {
				File trainingSetFile = new File(args[1]);
				pc.getLanguageModel().trainingSet(trainingSetFile);
				File transitionMatrixFile = new File(args[2]);
				pc.saveTransitionMatrix(transitionMatrixFile);
				if (args.length > 3) {
					File testSetFile = new File(args[args.length-1]);
					Perplexity perplexity = new Perplexity(pc.getLanguageModel());
					double eval = perplexity.test(testSetFile);
					System.out.println("Perplexity = " + eval);
				}
				pc.save();
			}
			else if (action.equals("test")) {
				File transitionMatrixFile = new File(args[1]);
				File testSetFile = new File(args[2]);
				pc.loadTransitionMatrix(transitionMatrixFile);
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(testSetFile)));
				String line = null;
				while ((line = br.readLine()) != null) {
					double p = pc.getLanguageModel().test(line);
					System.out.println(p);
				}
				br.close();
			}
			else if (action.equals("pwd")) {
				boolean scoreFlag = false;
				if (args.length > 4) {
					String flag = args[2];
					if (flag.equals("-s"))
						scoreFlag = true;
					else {
						System.out.println("invalid pwd flag: " + flag);
						System.out.println("usage: " + usage());
					}
				}
				File transitionMatrixFile = new File(args[args.length-2]);
				pc.loadTransitionMatrix(transitionMatrixFile);
				String password = args[args.length-1];
				double p = pc.getLanguageModel().test(password);
				if (scoreFlag) {
					System.out.println(p);
				}
				else {
					boolean good = p < pc.getThreshold();
					System.out.println(good);
				}
			}
			else {
				System.out.println("invalid action: " + action);
				System.out.println("usage: " + usage());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static String usage() {
		String str1 = "java PasswordChecker train <training_set_file_path> <transition_matrix_file> [<test_set_file_path>]	;	train bad password language model and save transition matrix file [use test set file to evaluate the model]";
		String str2 = "java PasswordChecker test <transition_matrix_file> <test_set_file_path>	;	load transition matrix and test all the passwords in test set file";
		String str3 = "java PasswordChecker pwd [-s] <transition_matrix_file> <password>	;	load transition matrix and check password [-s for raw probability score]";
		return str1 + "\n" + str2 + "\n" + str3 + "\n";
	}
}