import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.util.PDFMergerUtility;
import spark.*;
import spark.template.velocity.VelocityTemplateEngine;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class App {

    private static String fileName1;
    private static String fileName2;
    private static Path tempFile = null;

    public static void main(String[] args) {
        File uploadDir = new File("upload");
        uploadDir.mkdir(); // create the upload directory if it doesn't exist

        externalStaticFileLocation("upload");
        ProcessBuilder process = new ProcessBuilder();
        Integer port;
        if (process.environment().get("PORT") != null) {
            port = Integer.parseInt(process.environment().get("PORT"));
        } else {
            port = 4567;
        }

        setPort(port);
        staticFileLocation("resources");
        String layout = "templates/layout.vtl";

        get("/", (req, res) -> {
            cleanPreviousUsage(uploadDir);
            Map<String, Object> model = new HashMap();
            model.put("template", "templates/pdf-form.vtl");
            return new ModelAndView(model, layout);
        }, new VelocityTemplateEngine());

        post("/", (req, res) -> {
            MultipartConfigElement multipartConfigElement = new MultipartConfigElement("/tmp");
            req.raw().setAttribute("org.eclipse.multipartConfig", multipartConfigElement);
            Map<String, Object> model = new HashMap<>();
            model.put("template", "templates/pdf-form-success.vtl");

            //file 1
            try {
                tempFile = Files.createTempFile(uploadDir.toPath(), "", "");
            } catch (IOException e) {
                e.printStackTrace();
            }
            try (InputStream input = req.raw().getPart("file1").getInputStream()) { // getPart needs to use same "name" as input field in form
                Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ServletException e) {
                e.printStackTrace();
            }
            fileName1 = tempFile.getFileName().toString();
            File file1 = tempFile.toFile();
            model.put("fileName1", fileName1);

            //file2
            try {
                tempFile = Files.createTempFile(uploadDir.toPath(), "", "");
            } catch (IOException e) {
                e.printStackTrace();
            }
            try (InputStream input = req.raw().getPart("file2").getInputStream()) { // getPart needs to use same "name" as input field in form
                Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ServletException e) {
                e.printStackTrace();
            }
            fileName2 = tempFile.getFileName().toString();
            model.put("fileName2", fileName2);
            File file2 = tempFile.toFile();

            //merge docs and return url
            File[] files = new File[] {file1, file2};
            String resultFileName = combinePdfs(files, uploadDir);
            model.put("resultUrl", resultFileName);

            return new ModelAndView(model, layout);

        }, new VelocityTemplateEngine());

    }

    //method for combining pdf
    public static String combinePdfs(File[] files, File uploadDir) {
        //merge
        PDFMergerUtility ut = new PDFMergerUtility();
        for(int i = 0; i < files.length; i++) {
            ut.addSource(files[i]);
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ut.setDestinationStream(outputStream);
        try {
            ut.mergeDocuments();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (COSVisitorException e) {
            e.printStackTrace();
        }
        InputStream is = new ByteArrayInputStream(outputStream.toByteArray());
        try {
            tempFile = Files.createTempFile(uploadDir.toPath(), "", "");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tempFile.getFileName().toString();
    }

    //clean
    public static void cleanPreviousUsage(File uploadDir) {
        if (uploadDir != null) {
            for(File f : uploadDir.listFiles()) {
                f.delete();
            }
        }
    }


}


