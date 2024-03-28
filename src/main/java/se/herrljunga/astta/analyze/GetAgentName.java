package se.herrljunga.astta.analyze;

import java.io.File;

public class GetAgentName {
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
