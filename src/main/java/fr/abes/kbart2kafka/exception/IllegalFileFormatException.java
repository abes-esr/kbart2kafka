package fr.abes.kbart2kafka.exception;

public class IllegalFileFormatException extends Exception {
    public IllegalFileFormatException() {
        super("Format du fichier incorrect.");
    }

    public IllegalFileFormatException(String message) {
        super("Format du fichier incorrect. " + message);
    }
}
