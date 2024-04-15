package se.herrljunga.astta.analyze;

/**
 * The GetAgentName class.
 *
 * This class provides a method to get the agent name from a given file name.
 * It checks if the file name contains certain names (henrik, linus, alexander, axel) and returns the corresponding agent name.
 * If the file name does not contain any of the specified names, it returns "Okänt".
 */
public class GetAgentName {

    /**
     * Returns the agent name based on the given file name.
     *
     * This method checks if the file name contains certain names (henrik, linus, alexander, axel) and returns the corresponding agent name.
     * If the file name does not contain any of the specified names, it returns "Okänt".
     *
     * @param fileName The name of the file.
     * @return The agent name.
     */
    public static String getAgentName(String fileName) {
        if (fileName.contains("henrik")) {
            return "Henrik";
        }
        else if (fileName.contains("linus")) {
            return "Linus";
        }
        else if (fileName.contains("alexander")) {
            return "Alexander";
        }
        else if (fileName.contains("axel")) {
            return "Axel";
        }
        return "Okänt";
    }
}