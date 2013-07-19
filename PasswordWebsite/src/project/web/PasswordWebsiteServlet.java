package project.web;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.http.*;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import project.ppc.PasswordChecker;

@SuppressWarnings("serial")
public class PasswordWebsiteServlet extends HttpServlet {
	
	private String htmlString;
	private PasswordChecker pc;
	
	public PasswordWebsiteServlet() throws Exception {
		this.htmlString = getFileContents(new File("pwdcheck.html"));
		this.pc = initPasswordChecker("gt");
	}
	
	/*
	 * Respond to a get request
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/html");
		String responsePage = this.htmlString.replace("%outputTable", "");
		responsePage = responsePage.replace("%pwdValue", "");
		resp.getWriter().println(responsePage);
	}
	
	/*
	 * Respond to a post form submission by testing the input with the password checker
	 * and send back a table with the results. 
	 */
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (ServletFileUpload.isMultipartContent(req)) {
			ServletFileUpload upload = new ServletFileUpload();
		    FileItemIterator iter;
			try {
				iter = upload.getItemIterator(req);
				while (iter.hasNext()) {
					FileItemStream fileItem = iter.next();
				    InputStream is = fileItem.openStream();
				    //ByteArrayOutputStream baos = getPasswordsStrength(is);
				    //resp.setContentType("application/octet-stream");
				    //resp.setHeader("Content-Type", "save as filename=" + fileItem.getName() + ".tsv");
				    //resp.setHeader("Content-Disposition", "attachment; filename=\""+fileItem.getName() + ".tsv");
				    //resp.getOutputStream().write(baos.toByteArray());
				    String outputTable = getTable(is);
				    String responsePage = this.htmlString.replace("%outputTable", outputTable);
				    responsePage = responsePage.replace("%pwdValue", "");
					resp.getWriter().println(responsePage);
				}
			    
			} catch (FileUploadException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			String pwdValue = req.getParameter("pwd");
			try {
				String outputTable = getTable(pwdValue);
			    String responsePage = this.htmlString.replace("%outputTable", outputTable);
			    responsePage = responsePage.replace("%pwdValue", pwdValue);
				resp.getWriter().println(responsePage);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private String getFileContents(File f) throws IOException {
		String str = new String();
		FileInputStream fis = new FileInputStream(f);
		int size;
		byte[] bytes = new byte[1024];
		while ((size = fis.read(bytes)) > 0) {
			str += new String(bytes, 0, size);
		}
		fis.close();
		return str;
	}
	
	/*
	 * Create and initialize the password checker.
	 * Pass a null value to indicate that the language model should be loaded from the jar resource. 
	 */
	private PasswordChecker initPasswordChecker(String type) throws Exception {
		PasswordChecker pc = new PasswordChecker(type, null);
		return pc;
	}
	
	private String getPasswordStrength(String pwd) throws Exception {
		double p = this.pc.getLanguageModel().test(pwd);
		if (p < this.pc.getThreshold())
			return "strong";
		return "weak";
	}
	
	private ByteArrayOutputStream getPasswordsStrength(InputStream is) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = br.readLine()) != null) {
			String strength = getPasswordStrength(line);
			baos.write((line + "\t" + strength + "\n").getBytes());
		}
		br.close();
		baos.close();
		return baos;
	}
	
	/*
	 * Process one password at a time and add a row to the output table
	 */
	private String getTable(InputStream is) throws Exception {
		String str = getTableHeader();
		BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		String line;
		int counter = 0;
		while ((line = br.readLine()) != null && counter++ < 1000) {
			double score = this.pc.getLanguageModel().test(line);
			String strength = score < this.pc.getThreshold() ? "strong" : "weak";
			str += getTableRow(line, strength, score);
		}
		br.close();
		str += "</table>";
		return str;
	}
	
	/*
	 * Test the password and generate a single row table
	 */
	private String getTable(String pwdValue) throws Exception {
		String str = getTableHeader();
		double score = this.pc.getLanguageModel().test(pwdValue);
		String strength = score < this.pc.getThreshold() ? "strong" : "weak";
		str += getTableRow(pwdValue, strength, score);
		str += "</table>";
		return str;
	}
	
	private String getTableHeader() {
		String str = 
			"<table>\n" +
			"	<tr>\n" +
			"		<th align=\"left\" width=\"100\">Password</th>\n" +
			"		<th align=\"left\" width=\"100\">Strength</th>\n" +
			"		<th align=\"left\" width=\"100\">Score</th>\n" +
			"	</tr>\n";
		return str;
	}
	
	private String getTableRow(String pwd, String strength, double score) {
		String color = strength.equals("weak") ? "red" : "green";
		String str = 
				"	<tr>\n" +
				"		<td align=\"left\">" + pwd + "</td>\n" +
				"		<td style=\"color: " + color + "\" align=\"left\">" + strength + "</td>\n" +
				"		<td align=\"left\">" + score + "</td\n" +
				"	</tr>\n";
		return str;
	}
}