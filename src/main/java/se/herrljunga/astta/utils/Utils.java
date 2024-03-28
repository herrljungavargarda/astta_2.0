package se.herrljunga.astta.utils;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.herrljunga.astta.analyze.GetAgentName;
import se.herrljunga.astta.filehandler.StorageHandler;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    private static Logger logger = LoggerFactory.getLogger(Utils.class);

    /**
     * Creates a temporary directory to store temporary files.
     * If the directory already exists, its contents and the directory are deleted before creating a new e directory.
     */
    public static void createTempDirectory() {
        File path = new File(Config.pathToTemp);
        Path directoryPath = Paths.get(path.getPath());

        // Delete the folder if it exists, we don't want old temp files
        deleteFolderIfExists(path);

        try {
            logger.info("Creating temp directory");
            Files.createDirectory(directoryPath);
            logger.info("Done creating temp directory");
        } catch (IOException ex) {
            logger.info("An error occurred when trying to create directory.");
            throw new RuntimeException("Exception thrown in Utils, createTempDirectory " + ex.getMessage());
        }
    }

    /**
     * Deletes file or a directory and its contents.
     *
     * @param path The file or directory to be deleted.
     */

    public static void deleteFolderIfExists(File path) {
        logger.info("Deleting temp directory if it exists.");
        if (path.exists()) {
            try {
                Files.walkFileTree(path.toPath(), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir); // Delete directory after its contents are deleted
                        return FileVisitResult.CONTINUE;
                    }
                });
                logger.info("Temp directory deleted.");
            } catch (IOException e) {
                logger.error("An error occurred when trying to delete directory." + e);
                throw new RuntimeException("Exception thrown in Utils, deleteFolderIfExists " + e.getMessage());
            }
        }
    }


    /**
     * Remove only the path from an entire path
     *
     * @param path the path to remove from the entire file path
     * @return the file name with file extension
     **/
    public static String removePathFromFilename(String path) { //Remove "src/main/temp/" from file name
        String[] result = path.split("/");
        return result[result.length - 1];
    }


    /**
     * Remove path AND filetype, src/main/temp/hej.json" becomes "hej"
     *
     * @param path the path of the file
     * @return the file name without file extension
     **/
    public static String getFileName(String path) {
        String[] splitPath = path.split("/");
        int lastIndex = splitPath[splitPath.length - 1].lastIndexOf(".");
        // Extract substring before the last "."
        return splitPath[splitPath.length - 1].substring(0, lastIndex);
    }

    /**
     * @param audioFilePath the path to the audio file
     * @return the duration of the audio file in seconds
     * @throws UnsupportedAudioFileException if audio file is not supported
     * @throws IOException                   if an I/O error occurs
     */
    public static double getAudioDuration(String audioFilePath) {
        try {
            logger.info("Calculating audio file duration.");
            File audioFile = new File(audioFilePath);
            AudioFileFormat format = AudioSystem.getAudioFileFormat(audioFile);

            long audioFileLength = audioFile.length();
            int frameSize = format.getFormat().getFrameSize();
            float frameRate = format.getFormat().getFrameRate();
            logger.info("Done calculating audio file duration: " + audioFileLength / (frameSize * frameRate));
            return audioFileLength / (frameSize * frameRate);
        } catch (UnsupportedAudioFileException | IOException e) {
            logger.error("An error occurred when trying to get audio file duration." + e);
            throw new RuntimeException("Exception thrown in Utils, getAudioDuration " + e.getMessage());
        }
    }


    /**
     * Creates a json object and adds language, duration and tokensUsed to the json object
     *
     * @param content    The "base" content of the json file
     * @param duration   The duration of the call
     * @param tokensUsed Tokens used in the analyzing process
     * @return a json string of a complete json object
     **/


    public static String createJson(String content, String duration, int tokensUsed, String path) {
        logger.info("Creating and parsing json");
        try {
            JsonObject jsonObject = new Gson().fromJson(content, JsonObject.class);
            jsonObject.addProperty("FileLength", duration);
            jsonObject.addProperty("TokensUsed", tokensUsed);
            jsonObject.addProperty("AgentName", GetAgentName.getAgentName(path));
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            logger.info("Done creating and parsing json");
            return gson.toJson(jsonObject);
        } catch (JsonParseException e) {
            logger.error("An error occurred when trying to parse Json." + e);
            logger.error("Json: " + content);
            throw new RuntimeException("Exception thrown in Utils, createJson " + e.getMessage());
        }
    }

    public static boolean validateJson(String jsonToValidate) {

        try {
            new Gson().fromJson(jsonToValidate, JsonObject.class);
            return true;
        } catch (Exception e) {
            logger.warn("Bad json string:\n" + jsonToValidate);
            return false;
        }
    }

    /**
     * @param analyzedCalls the calls to write to the file
     **/
    public static void writeToFile(List<AnalyzedCall> analyzedCalls) {
        for (var analyzedCall : analyzedCalls) {
            writeToFile(analyzedCall);
        }
    }

    public static void writeToFile(AnalyzedCall analyzedCall) {
        logger.info("Writing to file: " + analyzedCall.savePath());
        try {
            FileWriter fileWriter = new FileWriter(analyzedCall.savePath());
            fileWriter.write(analyzedCall.analyzedCallJson());
            fileWriter.flush();
            fileWriter.close();
            logger.info("Done writing to file: " + analyzedCall.savePath());
        } catch (IOException e) {
            logger.error("An error occurred when trying to write to file." + e);
            throw new RuntimeException("Exception thrown in Utils, writeToFile " + e.getMessage());
        }
    }

    public static String getElementFromJson(String response, String elementToGet) {
        JsonObject jsonObject = new Gson().fromJson(response, JsonObject.class);
        return jsonObject.get(elementToGet).getAsString();
    }

    public static List<String> extractReport(List<String> paths, StorageHandler reportFilePath) {
        List<String> filteredPaths = new ArrayList<>();
        for(var path: paths) {
            if (path.contains("_report")) {
                reportFilePath.saveSingleFileToStorage(path);
            }
            else if (!paths.contains("_report")) {
                filteredPaths.add(path);
            }
        }
        return filteredPaths;
    }
}
