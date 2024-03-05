package se.herrljunga.astta.utils;

import com.google.gson.*;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class Utils {
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
            Files.createDirectory(directoryPath);
        } catch (IOException ex) {
            System.err.println("An error occurred when trying to create directory.");
            throw new RuntimeException("Exception thrown in Utils, createTempDirectory " + ex.getMessage());
        }
    }

    /**
     * Deletes file or a directory and its contents.
     *
     * @param path The file or directory to be deleted.
     */

    public static void deleteFolderIfExists(File path) {
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
                System.out.println("Temp directory deleted.");
            } catch (IOException e) {
                System.err.println("An error occurred when trying to delete directory.");
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
            File audioFile = new File(audioFilePath);
            AudioFileFormat format = AudioSystem.getAudioFileFormat(audioFile);

            long audioFileLength = audioFile.length();
            int frameSize = format.getFormat().getFrameSize();
            float frameRate = format.getFormat().getFrameRate();
            return audioFileLength / (frameSize * frameRate);
        } catch (UnsupportedAudioFileException | IOException e) {
            System.err.println("An error occurred when trying to get audio file duration.");
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
        try {
            JsonObject jsonObject = new Gson().fromJson(content, JsonObject.class);
            jsonObject.addProperty("Language", language);
            jsonObject.addProperty("FileLength", duration);
            jsonObject.addProperty("TokensUsed", tokensUsed);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            return gson.toJson(jsonObject);
        } catch (JsonParseException e) {
            System.err.println("An error occurred when trying to parse Json.");
            throw new RuntimeException("Exception thrown in Utils, createJson " + e.getMessage());
        }
    }

    /**
     * @param writePath the path to write to
     * @param text      the content of the file
     **/
    public static void writeToFile(String writePath, String text) {
        try {
            FileWriter fileWriter = new FileWriter(writePath);
            fileWriter.write(text);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            System.err.println("An error occurred when trying to write to file.");
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
}
