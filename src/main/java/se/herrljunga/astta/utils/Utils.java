package se.herrljunga.astta.utils;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

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
        String[] splitFileType = splitPath[splitPath.length - 1].split("\\.");
        return splitFileType[0];
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
     * @param language   The language of the call
     * @param duration   The duration of the call
     * @param tokensUsed Tokens used in the analyzing process
     * @return a json string of a complete json object
     **/
    public static String createJson(String content, String language, double duration, int tokensUsed) {
        logger.info("Creating and parsing json");
        try {
            JsonObject jsonObject = new Gson().fromJson(content, JsonObject.class);
            jsonObject.addProperty("Language", language);
            jsonObject.addProperty("FileLength", duration);
            jsonObject.addProperty("TokensUsed", tokensUsed);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            logger.info("Done creating and parsing json");
            return gson.toJson(jsonObject);
        } catch (JsonParseException e) {
            logger.error("An error occurred when trying to parse Json." + e );
            logger.error("Json: " + content);
            throw new RuntimeException("Exception thrown in Utils, createJson " + e.getMessage());
        }
    }

    /**
     * @param writePath the path to write to
     * @param text      the content of the file
     **/
    public static void writeToFile(String writePath, String text) {
        logger.info("Writing to file: " + writePath);
        try {
            FileWriter fileWriter = new FileWriter(writePath);
            fileWriter.write(text);
            fileWriter.flush();
            fileWriter.close();
            logger.info("Done writing to file: " + writePath);
        } catch (IOException e) {
            logger.error("An error occurred when trying to write to file." + e);
            throw new RuntimeException("Exception thrown in Utils, writeToFile " + e.getMessage());
        }
    }

    /**
     * An overload of writeToFile() for writing analyzed calls to a file
     *
     * @param analyzedCall the call to write to the file
     **/
    public static void writeToFile(AnalyzedCall analyzedCall) {
        writeToFile(analyzedCall.savePath(), analyzedCall.analyzedCallJson());
    }

    public static String getElementFromJson(String response, String elementToGet) {
        JsonObject jsonObject = new Gson().fromJson(response, JsonObject.class);
        return jsonObject.get(elementToGet).getAsString();
    }
}
