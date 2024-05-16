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

/**
 * The Utils class.
 *
 * This class provides utility methods for the application.
 * It includes methods for creating and deleting temporary directories, manipulating file paths, calculating audio file duration, creating and validating JSON objects, and writing to files.
 */
public class Utils {
    private static Logger logger = LoggerFactory.getLogger(Utils.class);
    private static Config config = ConfigLoader.loadConfig();
    /**
     * Creates a temporary directory to store temporary files.
     * If the directory already exists, its contents and the directory are deleted before creating a new directory.
     */
    public static void createTempDirectory() {
        File path = new File(config.utils.pathToTemp);
        Path directoryPath = Paths.get(path.getPath());
        // Delete the folder if it exists, we don't want old temp files
        deleteFolderIfExists(path);

        try {
            logger.info("Creating temp directory");
            Files.createDirectory(directoryPath);
            File analyzedJsonSaveDirectory = new File(config.utils.analyzedJsonSaveDirectory);
            Path analyzedJsonSaveDirectorydirectoryPath = Paths.get(analyzedJsonSaveDirectory.getPath());
            Files.createDirectory(analyzedJsonSaveDirectorydirectoryPath); // Create temp analyzed directory to store analyzed files
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
                logger.error("An error occurred when trying to delete directory.{}", e);
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
     * Calculates the duration of an audio file in seconds.
     *
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
            logger.info("Done calculating audio file duration: {}", audioFileLength / (frameSize * frameRate));
            return audioFileLength / (frameSize * frameRate);
        } catch (UnsupportedAudioFileException | IOException e) {
            logger.error("An error occurred when trying to get audio file duration.{}", e.getMessage());
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
        logger.info("Creating and parsing json: {}", path);
        try {
            JsonObject jsonObject = new Gson().fromJson(content, JsonObject.class);
            jsonObject.addProperty("FileLength", duration);
            jsonObject.addProperty("TokensUsed", tokensUsed);
            jsonObject.addProperty("AgentName", GetAgentName.getAgentName(path));
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            logger.info("Done creating and parsing json: {}", path);
            return gson.toJson(jsonObject);
        } catch (JsonParseException e) {
            logger.error("An error occurred when trying to parse Json.{}", e.getMessage());
            logger.error("Json: {}", content);
            throw new RuntimeException("Exception thrown in Utils, createJson " + e.getMessage());
        }
    }
    /**
     * Validates a JSON string.
     *
     * @param jsonToValidate The JSON string to validate.
     * @return True if the JSON string is valid, false otherwise.
     */
    public static boolean validateJson(String jsonToValidate) {

        try {
            new Gson().fromJson(jsonToValidate, JsonObject.class);
            return true;
        } catch (Exception e) {
            logger.warn("Bad json string:\n{}", jsonToValidate);
            return false;
        }
    }

    /**
     * Writes a list of AnalyzedCall objects to files.
     *
     * @param analyzedCalls the calls to write to the file
     */
    public static void writeToFile(List<AnalyzedCall> analyzedCalls) {
        for (var analyzedCall : analyzedCalls) {
            writeToFile(analyzedCall);
        }
    }
    /**
     * Writes an AnalyzedCall object to a file.
     *
     * @param analyzedCall The AnalyzedCall object to write to a file.
     */
    public static void writeToFile(AnalyzedCall analyzedCall) {
        logger.info("Writing to file: {}", analyzedCall.savePath());
        try {
            FileWriter fileWriter = new FileWriter(analyzedCall.savePath());
            fileWriter.write(analyzedCall.analyzedCallJson());
            fileWriter.flush();
            fileWriter.close();
            logger.info("Done writing to file: {}", analyzedCall.savePath());
        } catch (IOException e) {
            logger.error("An error occurred when trying to write to file.{}", e);
            throw new RuntimeException("Exception thrown in Utils, writeToFile " + e.getMessage());
        }
    }

    /**
     * Extracts a specific element from a JSON string.
     *
     * @param response The JSON string.
     * @param elementToGet The element to extract from the JSON string.
     * @return The value of the specified element as a string.
     */
    public static String getElementFromJson(String response, String elementToGet) {
        try{
        JsonObject jsonObject = new Gson().fromJson(response, JsonObject.class);
        return jsonObject.get(elementToGet).getAsString();
        }
        catch (JsonSyntaxException e){
            logger.error("An error occurred when extracting {} from Json file \n{} \n{}", elementToGet, response, e.getMessage());
            throw new RuntimeException("Exception thrown in Utils, writeToFile " + e.getMessage());
        }
    }

    /**
     * Extracts report files from a list of paths and saves them to a specified StorageHandler.
     * Non-report paths are returned in a new list.
     *
     * @param paths The list of paths.
     * @param reportBlobHandler The StorageHandler to save the report files to.
     * @return A list of paths that do not contain reports.
     */
    public static List<String> extractReport(List<String> paths, StorageHandler reportBlobHandler) {
        List<String> filteredPaths = new ArrayList<>();
        for(var path: paths) {
            if (path.contains("_report")) {
                reportBlobHandler.saveSingleFileToStorage(path);
                logger.info("Saved report");
            }
            else if (!paths.contains("_report")) {
                filteredPaths.add(path);
            }
        }
        return filteredPaths;
    }
}
