package project.ppc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import project.lm.GoodTuringModel;
import project.lm.KatzBackoffModel;
import project.lm.LanguageModel;
import project.lm.Perplexity;

public class PasswordChecker {
	
	private LanguageModel lm;
	private double threshold;
	private Properties configuration;
	
	public PasswordChecker(File configurationFile) throws Exception {
		this.configuration = new Properties();
		loadConfiguration(configurationFile);
	}
	
	public LanguageModel getLanguageModel() {
		return this.lm;
	}
	
	public double getThreshold() {
		return this.threshold;
	}
	
	public void loadConfiguration(File configurationFile) throws Exception {
		this.configuration.load(new FileInputStream(configurationFile));
		String model = this.configuration.getProperty("model");
		int order = Integer.parseInt(this.configuration.getProperty("ngram"));
		double mu = Double.parseDouble(this.configuration.getProperty("mu"));
		double sigma = Double.parseDouble(this.configuration.getProperty("sigma"));
		this.threshold = Double.parseDouble(this.configuration.getProperty("threshold"));
		if (model.equals("gt")) {
			double zero = Double.parseDouble(this.configuration.getProperty("gt.zero"));
			this.lm = new GoodTuringModel(order);
			((GoodTuringModel)this.lm).setZero(zero);
		}
		else if (model.equals("kb")) {
			double discount = Double.parseDouble(this.configuration.getProperty("kb.discount"));
			this.lm = new KatzBackoffModel(order, discount);
		}
		else
			throw new Exception("invalid language model: " + model);
		this.lm.setMu(mu);
		this.lm.setSigma(sigma);
	}
	
	public void saveConfiguration(File configurationFile) throws IOException {
		this.configuration.setProperty("mu", String.valueOf(this.lm.getMu()));
		this.configuration.setProperty("sigma", String.valueOf(this.lm.getSigma()));
		if (this.lm instanceof GoodTuringModel) {
			this.configuration.setProperty("gt.zero", String.valueOf(((GoodTuringModel)this.lm).getZero()));
		}
		this.configuration.store(new FileOutputStream(configurationFile), null);
	}
	
	public void saveModel(File modelFile) throws IOException {
		this.lm.getTransitionProbabilityMatrix().save(modelFile);
		if (this.lm instanceof KatzBackoffModel) {
			File alphaFile = new File(modelFile.getParent() + "\\kb.alpha.dat");
			((KatzBackoffModel)this.lm).getAlpha().save(alphaFile);
			File countsFile = new File(modelFile.getParent() + "\\kb.counts.dat");
			((KatzBackoffModel)this.lm).getCounts().save(countsFile);
			File countsStarFile = new File(modelFile.getParent() + "\\kb.cstar.dat");
			((KatzBackoffModel)this.lm).getCountsStar().save(countsStarFile);
		}
	}
	
	public void loadModel(File modelFile) throws IOException {
		this.lm.getTransitionProbabilityMatrix().load(modelFile);
		if (this.lm instanceof KatzBackoffModel) {
			File alphaFile = new File(modelFile.getParent() + "\\kb.alpha.dat");
			((KatzBackoffModel)this.lm).getAlpha().load(alphaFile);
			File countsFile = new File(modelFile.getParent() + "\\kb.counts.dat");
			((KatzBackoffModel)this.lm).getCounts().load(countsFile);
			File countsStarFile = new File(modelFile.getParent() + "\\kb.cstar.dat");
			((KatzBackoffModel)this.lm).getCountsStar().load(countsStarFile);
		}
	}
	
	public static void main(String[] args) {
		if (args.length < 4) {
			System.out.println("not enough arguments");
			System.out.println("usage:\n" + usage());
			System.exit(-1);
		}
		try {
			String action = args[0];
			File configurationFile = new File(args[1]);
			PasswordChecker pc = new PasswordChecker(configurationFile);
			File transitionMatrixFile = new File(args[2]);
			if (action.equals("train")) {
				File trainingSetFile = new File(args[3]);
				pc.getLanguageModel().trainingSet(trainingSetFile);
				pc.saveModel(transitionMatrixFile);
				if (args.length > 4) {
					File testSetFile = new File(args[args.length-1]);
					Perplexity perplexity = new Perplexity(pc.getLanguageModel());
					double eval = perplexity.test(testSetFile);
					System.out.println("Perplexity = " + eval);
				}
				pc.saveConfiguration(configurationFile);
			}
			else if (action.equals("test")) {
				File testSetFile = new File(args[3]);
				pc.loadModel(transitionMatrixFile);
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
					String flag = args[3];
					if (flag.equals("-s"))
						scoreFlag = true;
					else {
						System.out.println("invalid pwd flag: " + flag);
						System.out.println("usage: " + usage());
						System.exit(-1);
					}
				}
				pc.loadModel(transitionMatrixFile);
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
				System.exit(-1);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(0);
	}
	
	private static String usage() {
		String str1 = "java PasswordChecker train <config_properties_file_path> <transition_matrix_file> <training_set_file_path> [<test_set_file_path>]	;	train bad password language model and save transition matrix file [use test set file to evaluate the model]";
		String str2 = "java PasswordChecker test <config_properties_file_path> <transition_matrix_file> <test_set_file_path>	;	load transition matrix and test all the passwords in test set file";
		String str3 = "java PasswordChecker pwd <config_properties_file_path> <transition_matrix_file> [-s] <password>	;	load transition matrix and check password [-s for raw probability score]";
		return str1 + "\n" + str2 + "\n" + str3 + "\n";
	}
}