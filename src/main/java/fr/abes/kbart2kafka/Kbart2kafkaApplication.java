package fr.abes.kbart2kafka;

import fr.abes.kbart2kafka.exception.IllegalDateException;
import fr.abes.kbart2kafka.exception.IllegalFileFormatException;
import fr.abes.kbart2kafka.exception.IllegalPackageException;
import fr.abes.kbart2kafka.exception.IllegalProviderException;
import fr.abes.kbart2kafka.repository.ProviderRepository;
import fr.abes.kbart2kafka.service.EmailService;
import fr.abes.kbart2kafka.service.FileService;
import fr.abes.kbart2kafka.service.ProviderPackageService;
import fr.abes.kbart2kafka.utils.CheckFiles;
import fr.abes.kbart2kafka.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;

@Slf4j
@SpringBootApplication
public class Kbart2kafkaApplication implements CommandLineRunner {
    @Value("${kbart.header}")
    private String kbartHeader;
    private final FileService service;

    private final ProviderPackageService providerPackageService;

    private final ProviderRepository providerRepository;

    private final EmailService emailService;

    public Kbart2kafkaApplication(FileService service, ProviderPackageService providerPackageService, ProviderRepository providerRepository, EmailService emailService) {
        this.service = service;
        this.providerPackageService = providerPackageService;
        this.providerRepository = providerRepository;
        this.emailService = emailService;
    }

    public static void main(String[] args) {
        SpringApplication.run(Kbart2kafkaApplication.class, args);
    }

    /**
     * Méthode run qui lit un fichier tsv et en extrait les données ligne par ligne pour les envoyer dans un topic kafka
     *
     * @param args fichier tsv placé dans un répertoire défini
     * @throws IOException Exception levée lorsque aucun fichier tsv n'a été trouvé.
     */
    @Override
    public void run(String... args) throws IOException {
        long startTime = System.currentTimeMillis();
        //	Contrôle de la présence d'un paramètre au lancement de Kbart2kafkaApplication
        if (args.length == 0 || args[0] == null || args[0].trim().isEmpty()) {
            log.error("Message envoyé : {}", "Le chemin d'accès au fichier tsv n'a pas été trouvé dans les paramètres de l'application");
        } else {
            log.info("Debut envois kafka de : " + Utils.extractFilename(args[0]));
            ThreadContext.put("package", Utils.extractFilename(args[0]));
            //	Récupération du chemin d'accès au fichier
            File tsvFile = new File(args[0]);
            try {
                CheckFiles.verifyFile(tsvFile, kbartHeader);
                checkExistingPackage(tsvFile.getName());
            } catch (IllegalFileFormatException | IllegalProviderException | IllegalPackageException | IllegalDateException e) {
                log.error(e.getMessage());
                emailService.sendKbartFileReadError(tsvFile.getName(), "Erreur de lecture du fichier " + tsvFile.getName() + " : " + e.getMessage());
                throw new RuntimeException(e);
            }
            service.loadFile(tsvFile, kbartHeader);
        }
        long endTime = System.currentTimeMillis();
        double executionTime = (double) (endTime - startTime) / 1000;
        log.debug("Temps d'exécution : " + executionTime + " secondes");
    }

    private void checkExistingPackage(String filename) throws IllegalProviderException, IllegalPackageException, IllegalDateException {
        if (providerPackageService.hasMoreRecentPackageInBdd(Utils.extractProvider(filename), Utils.extractPackageName(filename), Utils.extractDate(filename)))
            throw new IllegalPackageException("Un package plus récent est déjà présent dans la base");
    }
}
