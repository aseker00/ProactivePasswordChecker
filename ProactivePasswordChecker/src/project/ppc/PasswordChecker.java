package project.ppc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import project.lm.GoodTuringModel;
import project.lm.KatzBackoffModel;
import project.lm.LanguageModel;
import project.lm.Perplexity;

/*
 * The main API
 */
public class PasswordChecker {
	
	private LanguageModel lm;
	private double threshold;
	private Properties configuration;
	private File modelDir;
	
	public PasswordChecker(String modelType, File modelDir) throws Exception {
		this.modelDir = modelDir;
		this.configuration = new Properties();
		load(modelType);
	}
	
	/*
	 * Save language model and threshold as well as the language model parameters (mean and standard deviation)
	 */
	public void save() throws Exception {
		if (this.modelDir == null)
			throw new Exception("modelDir = null");
		File configurationFile = new File(this.modelDir.getPath() + "\\" + getModelType() + ".properties");
		saveConfiguration(configurationFile);
		saveModel(this.modelDir);
	}
	
	public LanguageModel getLanguageModel() {
		return this.lm;
	}
	
	public double getThreshold() {
		return this.threshold;
	}
	
	private LanguageModel initLanguageModel(File configurationFile) throws Exception {
		return initLanguageModel(new FileInputStream(configurationFile));
	}
	
	/*
	 * Read the configuration properties and create the language model
	 */
	private LanguageModel initLanguageModel(InputStream is) throws Exception {
		LanguageModel lm = null;
		this.configuration.load(is);
		String model = this.configuration.getProperty("model");
		int order = Integer.parseInt(this.configuration.getProperty("ngram"));
		double mu = Double.parseDouble(this.configuration.getProperty("mu"));
		double sigma = Double.parseDouble(this.configuration.getProperty("sigma"));
		this.threshold = Double.parseDouble(this.configuration.getProperty("threshold"));
		if (model.equals("gt")) {
			double zero = Double.parseDouble(this.configuration.getProperty("gt.zero"));
			lm = new GoodTuringModel(order);
			((GoodTuringModel)lm).setZero(zero);
		}
		else if (model.equals("kb")) {
			double discount = Double.parseDouble(this.configuration.getProperty("kb.discount"));
			lm = new KatzBackoffModel(order, discount);
		}
		else
			throw new Exception("invalid language model: " + model);
		lm.setMu(mu);
		lm.setSigma(sigma);
		return lm;
	}
	
	/*
	 * Save the configuration properties including the mean and standard deviation
	 */
	private void saveConfiguration(File configurationFile) throws IOException {
		this.configuration.setProperty("mu", String.valueOf(this.lm.getMu()));
		this.configuration.setProperty("sigma", String.valueOf(this.lm.getSigma()));
		if (this.lm instanceof GoodTuringModel) {
			this.configuration.setProperty("gt.zero", String.valueOf(((GoodTuringModel)this.lm).getZero()));
		}
		this.configuration.store(new FileOutputStream(configurationFile), null);
	}
	
	/*
	 * Save the language model transition probability matrix.
	 * Save also specific model data
	 * The model is saved only after training which is done by the local application.
	 */
	private void saveModel(File dir) throws IOException {
		String modelType = getModelType();
		this.lm.getTransitionProbabilityMatrix().save(new File(dir.getPath() + "\\" + modelType + ".tpm.dat"));
		if (this.lm instanceof KatzBackoffModel) {
			File alphaFile = new File(dir.getPath() + "\\" + modelType + ".alpha.dat");
			((KatzBackoffModel)this.lm).getAlpha().save(alphaFile);
			File countsFile = new File(dir.getPath() + "\\" + modelType + ".counts.dat");
			((KatzBackoffModel)this.lm).getCounts().save(countsFile);
			File countsStarFile = new File(dir.getPath() + "\\" + modelType + ".cstar.dat");
			((KatzBackoffModel)this.lm).getCountsStar().save(countsStarFile);
		}
	}
	
	/*
	 * Currently support only these types:
	 */
	private String getModelType() {
		if (this.lm instanceof GoodTuringModel)
			return "gt";
		else if (this.lm instanceof KatzBackoffModel)
			return "kb";
		throw null;
	}
	
	/*
	 * If the module is loaded from a jar then need to load all the data from the jar resources.
	 * Otherwise load them from the local file system.
	 */
	private void load(String type) throws Exception {
		if (this.modelDir != null) {
			File configurationFile = new File(this.modelDir.getPath() + "\\" + type + ".properties");
			this.lm = initLanguageModel(configurationFile);
			loadModel(this.modelDir);
		}
		else {
			InputStream configurationInputStream = this.getClass().getResourceAsStream("/data/model/" + type + ".properties");
			this.lm = initLanguageModel(configurationInputStream);
			loadModel();
		}
	}
	
	/*
	 * Load the language model transition probability matrix.
	 * Also load specific model data
	 * This method is used when the program is run locally - load from file system
	 */
	private void loadModel(File dir) throws IOException {
		String modelType = getModelType();
		File tpmFile = new File(dir.getPath() + "\\" + modelType + ".tpm.dat");
		if (tpmFile.exists()) {
			this.lm.getTransitionProbabilityMatrix().load(tpmFile);
			if (this.lm instanceof KatzBackoffModel) {
				File alphaFile = new File(dir.getPath() + "\\" + modelType + ".alpha.dat");
				((KatzBackoffModel)this.lm).getAlpha().load(alphaFile);
				File countsFile = new File(dir.getPath() + "\\" + modelType + ".counts.dat");
				((KatzBackoffModel)this.lm).getCounts().load(countsFile);
				File countsStarFile = new File(dir.getPath() + "\\" + modelType + ".cstar.dat");
				((KatzBackoffModel)this.lm).getCountsStar().load(countsStarFile);
			}
		}
	}
	
	/*
	 * Load the language model transition probability matrix.
	 * Also load specific model data
	 * This method is used by the web application where the resource is read from a jar
	 */
	private void loadModel() throws IOException {
		String modelType = getModelType();
		InputStream tpmInputStream = this.getClass().getResourceAsStream("/data/model/" + modelType + ".tpm.dat");
		this.lm.getTransitionProbabilityMatrix().load(tpmInputStream);
		if (this.lm instanceof KatzBackoffModel) {
			InputStream alphaInputStream = this.getClass().getResourceAsStream("/data/model/" + modelType + ".alpha.dat");
			((KatzBackoffModel)this.lm).getAlpha().load(alphaInputStream);
			InputStream countsInputStream = this.getClass().getResourceAsStream("/data/model/" + modelType + ".tpm.dat");
			((KatzBackoffModel)this.lm).getCounts().load(countsInputStream);
			InputStream cstarInputStream = this.getClass().getResourceAsStream("/data/model/" + modelType + ".tpm.dat");
			((KatzBackoffModel)this.lm).getCountsStar().load(cstarInputStream);
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
			String modelType = args[1];
			File modelDir = new File(args[2]);
			PasswordChecker pc = new PasswordChecker(modelType, modelDir);
			if (action.equals("train")) {
				File trainingSetFile = new File(args[3]);
				pc.getLanguageModel().train(trainingSetFile);
				if (args.length > 4) {
					File testSetFile = new File(args[4]);
					Perplexity perplexity = new Perplexity(pc.getLanguageModel());
					double eval = perplexity.test(testSetFile);
					System.out.println(eval);
				}
				pc.save();
			}
			else if (action.equals("test")) {
				boolean flag = false;
				if (args.length > 4) {
					String flagStr = args[3];
					if (!flagStr.equals("-s")) {
						System.out.println("invalid flag: " + flagStr);
						System.out.println("usage: " + usage());
						System.exit(-1);
					}
					flag = true;
				}
				File testSetFile = new File(args[args.length-1]);
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(testSetFile), "UTF-8"));
				String line = null;
				while ((line = br.readLine()) != null) {
					double p = pc.getLanguageModel().test(line);
					if (flag)
						System.out.println(line + "\t" + (p < pc.getThreshold() ? "strong" : "weak") + "\t" + p);
					else
						System.out.println(p);
					
				}
				br.close();
			}
			else if (action.equals("pwd")) {
				boolean flag = false;
				if (args.length > 4) {
					String flagStr = args[3];
					if (!flagStr.equals("-s")) {
						System.out.println("invalid flag: " + flagStr);
						System.out.println("usage: " + usage());
						System.exit(-1);
					}
					flag = true;
				}
				String password = args[args.length-1];
				double p = pc.getLanguageModel().test(password);
				if (flag)
					System.out.println(password + "\t" + (p < pc.getThreshold() ? "strong" : "weak") + "\t" + p);
				else
					System.out.println(p);
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
		String str1 = "java PasswordChecker train <model_type> <model_dir> <training_set_file_path> [<test_set_file_path>]	;	train bad password language model and save it [use test set file to evaluate the model]";
		String str2 = "java PasswordChecker test <model_type> <model_dir> [-s] <test_set_file_path>	;	load language model and test all the passwords in test set file [-s for including classification]";
		String str3 = "java PasswordChecker pwd <model_type> <model_dir> [-s] <password>	;	load language model and check password [-s for including classification]";
		return str1 + "\n" + str2 + "\n" + str3 + "\n";
	}
}