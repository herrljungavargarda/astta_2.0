package se.herrljunga.astta.analyze;
/**
 * The AnalyzeResult record class.
 *
 * This class is a record, a special kind of class in Java that is used to create immutable data objects.
 * It contains two fields: result and tokensUsed.
 * The result field is a String that represents the result of an analysis operation.
 * The tokensUsed field is an integer that represents the number of tokens used in the analysis operation.
 *
 * The class provides a constructor to initialize the fields, and methods to get the values of the fields.
 * Since it's a record, it also provides an implementation of equals(), hashCode(), and toString() methods.
 */
public record AnalyzeResult(String result, int tokensUsed) {

}
