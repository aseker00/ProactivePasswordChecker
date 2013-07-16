package project.web;

import java.io.File;
import java.io.IOException;
import javax.servlet.http.*;

@SuppressWarnings("serial")
public class PasswordWebsiteServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/html");
		resp.getWriter().println("Hello, world");
	}
	
	private String getFileContents(File f) {
		return null;
	}
}
