package fr.abes.kbart2kafka.utils;

import fr.abes.kbart2kafka.exception.IllegalFileFormatException;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
public class CheckFiles {
    /**
     * Controle si le fichier à bien une extension tsv
     * @param file fichier en entrée
     * @throws IllegalFileFormatException format de fichier non conforme
     */
    public static void isFileWithTSVExtension(File file) throws IllegalFileFormatException {
        //Filename extension control
        String fileName = file.getName(); // get file name
        if (fileName.isEmpty()) {
            log.error("Message envoyé : {}", "Le nom du fichier est vide");
            throw new IllegalFileFormatException("Le nom du fichier est vide"); // check if file name is valid
            }
        String[] parts = fileName.split("\\."); // split by dot
        String extension = parts[parts.length - 1]; // get last part as extension
        // compare with tsv ignoring case
        if (!extension.equalsIgnoreCase("tsv")) {
            log.error("Message envoyé : {}", "le fichier n'est pas au format tsv");
            throw new IllegalFileFormatException("le fichier n'est pas au format tsv");
        }
    }

    /**
     * Détecte si le fichier présente des tabulations
     * @param file fichier en entrée
     * @throws IOException erreur avec le fichier en entrée
     */
    public static void detectTabulations(File file) throws IOException, IllegalFileFormatException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.contains("\t")) {
                    log.error("Message envoyé : {}", "Le fichier ne contient pas de tabulation");
                    throw new IllegalFileFormatException("Le fichier ne contient pas de tabulation");
                }
            }
        }
    }

    /**
     * Détecte la présence d'une entête dans le fichier
     * @param header terme à recherche dans l'entête
     * @param file   fichier en entrée
     * @throws IOException impossible de lire le fichier
     */
    public static void detectHeaderPresence(String header, File file) throws IOException, IllegalFileFormatException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (!line.contains(header)) {
                log.error("Message envoyé : {}", "Le champ " + header + " est absent de l'en tête du fichier");
                throw new IllegalFileFormatException("Le champ " + header + " est absent de l'en tête du fichier");
            }
        }
    }

    public static void detectFileName(File file) throws IllegalFileFormatException {
        String filename = file.getName();
        if(!filename.matches("(\\w+_\\w+_)+(\\d{4}-\\d{2}-\\d{2})+(.tsv)$")){
            throw new IllegalFileFormatException("Le nom du fichier "+ filename +" n'est pas correcte");
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
    public static void verifyFile(File file, String header) throws IllegalFileFormatException, IOException {
        CheckFiles.isFileWithTSVExtension(file);
        CheckFiles.detectTabulations(file);
        CheckFiles.detectHeaderPresence(header, file);
    }
}