package se.herrljunga.astta.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

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
     * Creates a temporary directory to store fetched WAV files.
     * If the directory already exists, its contents are deleted before creating a new directory.
     */
    public static void createTempFile() {
        File path = new File(Config.pathToTemp);
        Path directoryPath = Paths.get(path.getPath());
        deleteFolderIfExists(path);

        try {
            Files.createDirectory(directoryPath);
        } catch (IOException ex) {
            System.err.println("Couldn't create directory " + ex.getMessage());
        }
    }

    /**
     * Deletes a file or directory if it exists.
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
                System.out.println("Failed to delete directory: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static String removePathFromFilename(String path) { //Remove "src/main/temp/" from file name
        String[] result = path.split("/");
        return result[result.length - 1];
    }

    // Removes path and filetype
    public static String getFileName(String path){
        String[] splitPath = path.split("/");
        String[] splitFileType = splitPath[splitPath.length-1].split("\\.");
        return splitFileType[0];

    }

    public static String removeWavFromFilename(String name) {
        return name.replace(".wav", "");
    }

    /**
     * @param audioFilePath the path to the audio file
     * @return the duration of the audio file in secounds
     * @throws UnsupportedAudioFileException if audio file is not supported
     * @throws IOException                   if an I/O error occurs
     */

    public static double getAudioDuration(String audioFilePath) throws UnsupportedAudioFileException, IOException {
        File audioFIle = new File(audioFilePath);
        AudioFileFormat format = AudioSystem.getAudioFileFormat(audioFIle);
        long audioFileLength = audioFIle.length();
        int frameSize = format.getFormat().getFrameSize();
        float frameRate = format.getFormat().getFrameRate();
        double durationInSeconds = (audioFileLength / (frameSize * frameRate));
        //System.out.println("Audio file length: " + durationInSeconds + " sec");
        return durationInSeconds;
    }

    public static String createJson(String content, String language, double lengthOfFile, int tokensUsed) {
        JsonObject jsonObject = new Gson().fromJson(content, JsonObject.class);
        jsonObject.addProperty("Language", language);
        jsonObject.addProperty("FileLength", lengthOfFile);
        jsonObject.addProperty("TokensUsed", tokensUsed);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        return gson.toJson(jsonObject);
    }

    public static void writeToFile(String writePath, String text) throws IOException {
        // Write the JSON content to the file
        FileWriter fileWriter = new FileWriter(writePath);
        fileWriter.write(text); // The '2' argument is for indentation
        fileWriter.flush();
        fileWriter.close();
    }

    public static void writeToFile(AnalyzedCall analyzedCall) throws IOException {
        writeToFile(analyzedCall.getSavePath(), analyzedCall.getAnalyzedCallJson());
    }
}
