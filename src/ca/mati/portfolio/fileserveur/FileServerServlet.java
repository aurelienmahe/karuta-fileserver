/* =======================================================
	Copyright 2014 - ePortfolium - Licensed under the
	Educational Community License, Version 2.0 (the "License"); you may
	not use this file except in compliance with the License. You may
	obtain a copy of the License at

	http://www.osedu.org/licenses/ECL-2.0

	Unless required by applicable law or agreed to in writing,
	software distributed under the License is distributed on an "AS IS"
	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
	or implied. See the License for the specific language governing
	permissions and limitations under the License.
   ======================================================= */

package ca.mati.portfolio.fileserveur;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A servlet to store and retrieve files from different persistent backends.
 * Implemented: SQL backend (MySQL) and File System backend
 */

// @WebServlet("/user/*")
// Moved to <servlet> in the web.xml
// @MultipartConfig(maxFileSize = 104857600)    // upload file's size up to 100 MB
// Moved to <multipart-config> in the web.xml
public class FileServerServlet extends HttpServlet
{
	final Logger logger = LoggerFactory.getLogger(FileServerServlet.class);

	private String serverType;

	public static final boolean TRACE = true;

	private static final long serialVersionUID = 1L;

	// size of byte buffer to send file
	private static final int BUFFER_SIZE = 4096;

	@Override
	public void init()
	{
		logger.info("FileServerServlet starting");
		serverType = getServletConfig().getServletContext().getInitParameter("serverType");
	}

	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String uuid = request.getPathInfo();
		uuid = uuid.substring(1);

		try
		{
			String path = getServletContext().getRealPath("/");
			PersistenceFactory factory = new PersistenceFactory(serverType);
			String app = request.getHeader("app");
			ApiFilePersistence filePersistence = factory.createFilePersistence(path, app);

			String message;
			if ( filePersistence.isFileDeleted(uuid) ) {
				message = "400 - ERROR - resource: " + uuid + " is already deleted";
			} else {
				message = "200 - " + filePersistence.deleteFile(uuid);
			}

			PrintWriter out = response.getWriter();
			out.println(message);

		} catch (Exception e) {
			response.getWriter().print("500 - ERROR: " + e.getMessage());
		}
	}

	@Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
		String uuid = request.getPathInfo();
		uuid = uuid.substring(1);

		try
		{
			String path = getServletContext().getRealPath("/");
			PersistenceFactory factory = new PersistenceFactory(serverType);
			String app = request.getHeader("app");
			ApiFilePersistence filePersistence = factory.createFilePersistence(path, app);

			InputStream inputStream = filePersistence.getFileInputStream(uuid);
			int fileLength = inputStream.available();
			if (TRACE) System.out.println("fileLength = " + fileLength);

			response.setContentLength(fileLength);

			// write the file to the client
			OutputStream outStream = response.getOutputStream();

			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead = -1;
			int total = 0;

			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outStream.write(buffer, 0, bytesRead);
				total += bytesRead;
			}
			System.out.println("Total = " + total);
			outStream.flush();
			outStream.close();
			inputStream.close();
		} catch (IOException exIO) {
			response.getWriter().print("500 - ERROR - IO Error: " + exIO.getMessage());
		} catch (SQLException exSql ) {
			response.getWriter().print("500 - ERROR - SQL Error: " + exSql.getMessage());
		} catch (Exception e) {
			response.getWriter().print("500 - ERROR: " + e.getMessage());
		}
	}

	@Override
  protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
		String uuid = request.getPathInfo();
		uuid = uuid.substring(1);

		try
		{
			String path = getServletContext().getRealPath("/");
			PersistenceFactory factory = new PersistenceFactory(serverType);
			String app = request.getHeader("app");
			ApiFilePersistence filePersistence = factory.createFilePersistence(path ,app);

			// get the upload file part from multipart request
			ServletInputStream inputStream = request.getInputStream();

			uuid = filePersistence.updateFile(uuid, inputStream);

			PrintWriter out = response.getWriter();
			out.println("");
		} catch (SQLException exSql) {
			response.getWriter().print("500 - ERROR - IO Error: " + exSql.getMessage());
		} catch (Exception e) {
			response.getWriter().print("500 - ERROR: " + e.getMessage());
		}

		PrintWriter writer = response.getWriter();
		writer.write(uuid);
		writer.close();
	}

	@Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
		String uuid = request.getPathInfo();
		uuid = uuid.substring(1);

		if( "".equals(uuid) )
			uuid = UUID.randomUUID().toString();

		try
		{
			String path = getServletContext().getRealPath("/");
			PersistenceFactory factory = new PersistenceFactory(serverType);
			String app = request.getHeader("app");
			ApiFilePersistence filePersistence = factory.createFilePersistence(path, app);

			// get input stream of the upload file
			ServletInputStream inputStream = request.getInputStream();

			uuid = filePersistence.saveFile(uuid, inputStream);

		} catch (SQLException exSql) {
			response.getWriter().print("500 - ERROR - IO Error: " + exSql.getMessage());
		} catch (Exception e) {
			response.getWriter().print("500 - ERROR: " + e.getMessage());
		}

		PrintWriter writer = response.getWriter();
		writer.write(uuid);
		writer.close();
	}

}
