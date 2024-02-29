package se.herrljunga.astta.utils;

/**
 * A record to save analyzed calls
 * @param savePath The path to save the json file to
 * @param analyzedCallJson The analyzed call in the form of a json string
 * **/
public record AnalyzedCall(String savePath, String analyzedCallJson) {
}
