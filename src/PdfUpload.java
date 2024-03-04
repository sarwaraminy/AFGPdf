import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;


@WebServlet("/PdfUpload")
@MultipartConfig(fileSizeThreshold=1024*1024*2,	// 2MB 
				 maxFileSize=1024*1024*200,		// 200MB
				 maxRequestSize=1024*1024*400)	// 400MB
public class PdfUpload extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * handles file upload
	 */
	
	protected void doPost(HttpServletRequest request,
		HttpServletResponse response) throws ServletException, IOException {
		
		String         ibi_tmp_path              = null;
		String         host_name                 = null;
		String         drop_file_name            = null;
		String         upload_destination_path   = null;
		String         upload_destination_parent_path = null;
		String         log_destination_path      = null;
		String[]       kernels                   = null;
		String         approot                   = null;
		String         client                    = null;
		String         root                      = null;
		String         suite                     = null;
		Boolean        pathing_is_custom         = null;
		String         user                      = null;
		String         staticpdf                 = null;
        BufferedReader vitals_reader             = null;
        BufferedWriter upload_log_writer         = null;
        PrintWriter    html_response_writer      = null;
        File           drop_file                 = null;
        File           upload_log_file           = null;
        Calendar       cal                       = null;
        DateFormat     dateFormat                = null;		
		
		host_name            = System.getenv("COMPUTERNAME").split(";")[0];
		ibi_tmp_path         = File.separator + File.separator + host_name + File.separator + "IBI_TEMP";
		drop_file_name       = request.getParameter("pdfn");
		cal                  = Calendar.getInstance();
		dateFormat           = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		html_response_writer = response.getWriter();
		drop_file            = new File(ibi_tmp_path + File.separator + drop_file_name + ".txt");
		response.setContentType("text/html");
		Part   pdf = request.getPart("pdf");
		String fqp = extractFileName(pdf);
		int    last_path_separator = fqp.lastIndexOf(File.separator);
		String file_name = last_path_separator < 0 || last_path_separator == fqp.length() ? fqp : fqp.substring(last_path_separator + 1);

		if (drop_file.exists()) {
			try {
				vitals_reader           = new BufferedReader(new FileReader(drop_file));
				kernels                 = vitals_reader.readLine().split(":");
				approot                 = kernels[3];
				client                  = kernels[4];
				root                    = kernels[5];
				suite                   = kernels[7];
				pathing_is_custom       = Integer.parseInt(kernels[8]) == 1 ? true : false;
				user                    = kernels[9];
				staticpdf               = kernels[10];
				// create parent directory
				upload_destination_parent_path = pathing_is_custom ?
						                  approot + File.separator + "CUST"   +
                                                    File.separator + suite  +
                                                    File.separator + client +
                                                    File.separator + "STATICPDF" +
                                                    File.separator + client :
                                          approot + File.separator + root   +
                                                    File.separator + "CUST"   +
	                                                File.separator + "STATICPDF" +
                                                    File.separator + client;
				//uploaded path
				upload_destination_path = pathing_is_custom ?
										  approot + File.separator + "CUST"   +
						                            File.separator + suite  +
						                            File.separator + client +
						                            File.separator + staticpdf   :
						                  approot + File.separator + root   +
						                            File.separator + "CUST"   +
							                        File.separator + staticpdf;
				log_destination_path    = pathing_is_custom ?
										  approot + File.separator + "DATA" +
										  			File.separator + client :
										  approot + File.separator + root   +
										  			File.separator + "DATA" +
										  			File.separator + client;

				upload_log_file         = new File(log_destination_path + File.separator + "pdfUploadLog.log");
				upload_log_writer       = new BufferedWriter(new FileWriter(upload_log_file, true));
				// check if the parent directory is exist or not
				File PfileDfltDir = new File(upload_destination_parent_path);
				if(!PfileDfltDir.exists()){
					PfileDfltDir.mkdir();
				}
				// check if the directory is exist or not
				File fileDfltDir = new File(upload_destination_path);
				if(!fileDfltDir.exists()){
					fileDfltDir.mkdir();
				}
			}
			catch (IOException drop_file_exception) {
				drop_file_exception.printStackTrace();
			}
			finally {
				vitals_reader.close();
			}

			try {
				
				String mime_type = getServletContext().getMimeType(file_name);
                
				if(file_name != "") { //check the file name is not empty
					if(mime_type.startsWith("application/pdf")) {
						pdf.write(upload_destination_path + File.separator + file_name);

						try {
							upload_log_writer.write("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
						 	upload_log_writer.newLine();
						    upload_log_writer.write("upload Date: "+dateFormat.format(cal.getTime()));
						    upload_log_writer.newLine();
						    upload_log_writer.write("The PDF Filename is: "+ file_name);
						    upload_log_writer.newLine();
						    upload_log_writer.write("Successfully uploaded in the following path:");
						    upload_log_writer.newLine();
						   	upload_log_writer.write(upload_destination_path + File.separator);
						   	upload_log_writer.newLine();
						    upload_log_writer.write("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
						    upload_log_writer.newLine();
						    upload_log_writer.flush();
						}
						catch (IOException ioe) {
						     ioe.printStackTrace();
						}
						finally {
							if (upload_log_writer != null) {
								try {
									upload_log_writer.close();
								}
						        catch (IOException ioe2) {
						          // just ignore it
						        }
							}
							drop_file.delete();
						}
						html_response_writer.println("<pre id=\"servlet-status\">File Upload Successful</pre>");
						html_response_writer.println("<pre id=\"servlet-message\">Successfully uploaded</pre>");
					}
					else {
						try{
							upload_log_writer.write("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
							upload_log_writer.newLine();
							upload_log_writer.write("upload Date: "+ dateFormat.format(cal.getTime()));
							upload_log_writer.newLine();
							upload_log_writer.write("The upload file type is: " + mime_type);
							upload_log_writer.newLine();
							upload_log_writer.write("Only .pdf file type is allowed to upload");
							upload_log_writer.newLine();
							upload_log_writer.flush();
						}
						catch (IOException ioe) {
							ioe.printStackTrace();
						}
						finally { // always close the file
							if (upload_log_writer != null)
								try {
									upload_log_writer.close();
								}
								catch (IOException ioe2) {
						        // just ignore it
								}
					    } // end try/catch/finally
						html_response_writer.println("<pre id=\"servlet-status\">Servlet Upload Error</pre>");
						html_response_writer.println("<pre id=\"servlet-message\">Only .pdf file type is allowed to upload</pre>");
					}
				}
				else {
					try{
						upload_log_writer.write("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
						upload_log_writer.newLine();
						upload_log_writer.write("upload Date: "+ dateFormat.format(cal.getTime()));
						upload_log_writer.newLine();
						upload_log_writer.write("The upload file appears to be missing");
						upload_log_writer.newLine();
						upload_log_writer.flush();
					}
					catch (IOException ioe) {
						ioe.printStackTrace();
					}
					finally { // always close the file
						if (upload_log_writer != null)
							try {
								upload_log_writer.close();
							}
							catch (IOException ioe2) {
					        // just ignore it
							}
				    } // end try/catch/finally
					html_response_writer.println("<pre id=\"servlet-status\">Servlet Upload Error</pre>");
					html_response_writer.println("<pre id=\"servlet-message\">The upload file appears to be missing</pre>");
				}
			} 
			catch (Exception e) {
				upload_log_writer.write("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				upload_log_writer.newLine();
				upload_log_writer.write("Error Date: "+dateFormat.format(cal.getTime()));
				upload_log_writer.newLine();
				upload_log_writer.write(e.getMessage());
				upload_log_writer.flush();
				if (upload_log_writer != null) {
					upload_log_writer.close();
				}
				html_response_writer.println("<pre id=\"servlet-status\">Servlet Upload Error</pre>");
				html_response_writer.println("<pre id=\"servlet-message\">Error Date: " + dateFormat.format(cal.getTime()));
				html_response_writer.println(e.getMessage()+"</pre>"); 
			}			
		}
		else {
			html_response_writer.println("<pre id=\"servlet-status\">Servlet Upload Error</pre>");
			html_response_writer.println("<pre id=\"servlet-message\">Unable to determine authorization</pre>");
		}
		
	}

	/**
	 * Extracts file name from HTTP header content-disposition
	 */
	private String extractFileName(Part part) {
		String contentDisp = part.getHeader("content-disposition");
		String[] items = contentDisp.split(";");
		for (String s : items) {
			if (s.trim().startsWith("filename")) {
				return s.substring(s.indexOf("=") + 2, s.length()-1);
			}
		}
		return "";
	}
}