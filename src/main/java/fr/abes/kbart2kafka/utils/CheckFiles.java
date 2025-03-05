package fr.abes.kbart2kafka.utils;

import fr.abes.kbart2kafka.exception.IllegalFileFormatException;
import fr.abes.kbart2kafka.exception.IllegalProviderException;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class CheckFiles {

    public static Boolean detectFileNameAndReturnIsBypass(File file) throws IllegalFileFormatException {
        String filename = file.getName();
        if (!filename.matches("([a-zA-Z0-9\\-]+_){3}(\\d{4}-\\d{2}-\\d{2})+(_FORCE|_BYPASS)?+(.tsv)$")) {
            throw new IllegalFileFormatException("Le nom du fichier "+ filename +" n'est pas correct");
        } else return filename.matches("([a-zA-Z0-9\\-]+_){3}(\\d{4}-\\d{2}-\\d{2})+(_BYPASS)+(.tsv)$");
    }

    public static void detectProvider(File file) throws IllegalProviderException {
        String filename = file.getName();
        filename = filename.replace("\\", "/");
        if(!filename.contains("_") || filename.substring(0, filename.indexOf('_')).isEmpty()) {
            throw new IllegalProviderException("Le nom du fichier "+ filename +" ne contient pas de provider");
        }
    }

    /**
     * Controle si le fichier à bien une extension tsv
     * @param file fichier en entrée
     * @throws IllegalFileFormatException format de fichier non conforme
     */
    public static void isFileWithTSVExtension(File file) throws IllegalFileFormatException {
        //Filename extension control
        String fileName = file.getName(); // get file name
        if (fileName.isEmpty())
            throw new IllegalFileFormatException("Le nom du fichier est vide"); // check if file name is valid
        String[] parts = fileName.split("\\."); // split by dot
        String extension = parts[parts.length - 1]; // get last part as extension
        // compare with tsv ignoring case
        if (!extension.equalsIgnoreCase("tsv"))
            throw new IllegalFileFormatException("le fichier n'est pas au format tsv");
    }

    public static void checkPublicationTitle(File file) throws IllegalFileFormatException, IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<String> lines = reader.lines().toList();

            if(lines.stream().anyMatch(line ->
                    line.split("\t")[0] == null || line.split("\t")[0].isEmpty()
            )){
                throw new IllegalFileFormatException("Le fichier a une ligne qui ne contient pas de publication_title");
            }
        }
    }

    /**
     * Détecte si le fichier présente des tabulations
     * @param file fichier en entrée
     * @throws IOException erreur avec le fichier en entrée
     */
    public static void detectTabulations(File file) throws IOException, IllegalFileFormatException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<String> lines = reader.lines().toList();

            if(lines.stream().anyMatch(line ->
                    (!line.contains("\t") && !(line.isBlank() && lines.lastIndexOf(line) == lines.size() -1)) // && isNotLast and blank
            )){
                throw new IllegalFileFormatException("Le fichier ne contient pas de tabulation");
            }
        }
    }

    /**
     * Détecte la présence d'un entête dans le fichier
     * @param header liste de header
     * @param file   fichier en entrée
     * @throws IOException impossible de lire le fichier
     */
    public static void detectHeaderPresence(String header, File file, Boolean isBypassOptionPresent) throws IOException, IllegalFileFormatException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            line = line.replace("bestppn","best_ppn");
            String[] headerKbart = line.split("\t");

            if(isBypassOptionPresent && line.contains("best_ppn")) {
                throw new IllegalFileFormatException("L'en tete du fichier est incorrecte. L'option _BYPASS n'est pas compatible avec la présence d'une colonne best_pnn");
            }else if ((!(headerKbart.length == 25 && line.contains(header)) && !(headerKbart.length == 26 && line.contains(header) && line.contains("best_ppn")))) {
                throw new IllegalFileFormatException( "L'en tete du fichier est incorrecte. L’en tête devrait être comme ceci : " + header + " et best_ppn" );
            }
        }
    }

    /**
     * Contrôle que le fichier à une extension tsv, qu'il contient des tabulations et
     * qu'il contient un entête avec la présence d'un terme en paramètre
     * @param file le fichier en entrée
     * @param header la chaine de caractère à rechercher
     * @throws IllegalFileFormatException Format de fichier non conforme
     * @throws IOException Impossible de lire le fichier
     */
    public static void verifyFile(File file, String header) throws IllegalFileFormatException, IOException, IllegalProviderException {
        if(file.exists()) {
            Boolean isBypassOptionPresent = detectFileNameAndReturnIsBypass(file);
            detectProvider(file);
            isFileWithTSVExtension(file);
            detectTabulations(file);
            detectHeaderPresence(header, file, isBypassOptionPresent);
            checkEmptyFile(file);
            checkPublicationTitle(file);
        } else {
            throw new IllegalFileFormatException("Le fichier "+file.getName()+" n'est pas trouvable");
        }
    }

    public static void checkEmptyFile(File file) throws IOException, IllegalFileFormatException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            reader.readLine(); // lecture à vide de la ligne d'en-tête
            String line = reader.readLine();
            if (line == null || line.isEmpty())
                throw new IllegalFileFormatException("Le fichier est vide !");
        }
    }

    public static void isValidUtf8(String input) throws IllegalFileFormatException {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        String messageErreur = "le fichier contient des caracters qui ne sont pas en UTF8";
        try {
            decoder.decode(java.nio.ByteBuffer.wrap(input.getBytes(StandardCharsets.UTF_8)));
        } catch (CharacterCodingException e) {
            throw new IllegalFileFormatException(messageErreur);
        }
        if(input.contains("�")){
            throw new IllegalFileFormatException(messageErreur);
        }
    }
}
