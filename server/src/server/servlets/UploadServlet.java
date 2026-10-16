package server.servlets;

import engine.dto.LoadResult;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * POST /api/upload, multipart/form-data. The XML is parsed straight from the part's input stream;
 * fileSizeThreshold keeps the part in memory, so nothing is ever written to the server's disk.
 */
@WebServlet(name = "UploadServlet", urlPatterns = "/api/upload")
@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 10, maxFileSize = 1024 * 1024 * 10,
        maxRequestSize = 1024 * 1024 * 11)
public class UploadServlet extends ApiServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, username) -> {
            Collection<Part> parts = request.getParts();
            for (Part part : parts) {
                if (part.getSubmittedFileName() != null) {
                    try (InputStream content = part.getInputStream()) {
                        return engine.uploadEventsFile(username, part.getSubmittedFileName(), content);
                    }
                }
            }
            return LoadResult.failure("No file was received. Please choose an XML file to upload.");
        });
    }
}
