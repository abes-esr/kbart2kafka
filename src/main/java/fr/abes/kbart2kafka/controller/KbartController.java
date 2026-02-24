package fr.abes.kbart2kafka.controller;

import fr.abes.kbart2kafka.dto.LigneKbartDto;
import fr.abes.kbart2kafka.entity.ProviderPackage;
import fr.abes.kbart2kafka.exception.IllegalDateException;
import fr.abes.kbart2kafka.exception.IllegalPackageException;
import fr.abes.kbart2kafka.exception.IllegalProviderException;
import fr.abes.kbart2kafka.service.FileService;
import fr.abes.kbart2kafka.service.ProviderPackageService;
import fr.abes.kbart2kafka.utils.CheckFiles;
import fr.abes.kbart2kafka.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1")
@Slf4j
public class KbartController {
    @Value("${kbart.header}")
    private String kbartHeader;
    @Value("${abes.pathToKbart}")
    private String pathToKbart;
    private final FileService fileService;

    private final ProviderPackageService providerPackageService;

    public KbartController(FileService fileService, ProviderPackageService providerPackageService) {
        this.fileService = fileService;
        this.providerPackageService = providerPackageService;
    }

    @PostMapping(value ={"/uploadFile/{fileName}/{isBestPpnComputed}", "/uploadFile/{fileName}"})
    public void uploadFile( @PathVariable String fileName, @PathVariable Optional<Boolean> isBestPpnComputed) {
        long startTime = System.currentTimeMillis();
        //	Contrôle de la présence d'un paramètre au lancement de Kbart2kafkaApplication
        if (fileName == null || fileName.isEmpty() ) {
            log.error("Message envoyé : {}", "Le chemin d'accès au fichier tsv n'a pas été trouvé dans les paramètres de l'application");
        } else {
            ThreadContext.put("package", fileName);
            log.info("Debut envois kafka de : {}", fileName);
            //	Récupération du chemin d'accès au fichier
            File tsvFile = new File(pathToKbart + fileName);
            try {
                CheckFiles.verifyFile(tsvFile, kbartHeader);
                checkExistingPackage(tsvFile.getName());

                //todo reajuster pour la valeur par defaut de isBestPpnComputed
                if(isBestPpnComputed.isEmpty() || isBestPpnComputed.get()) {
                    List<LigneKbartDto> lastLignesKbart = getLigneKbartFromLastExistingPackage(tsvFile);
                    Map<String, String> lastLignesKbartHash = HashMap.newHashMap(lastLignesKbart.size());
                    lastLignesKbart.forEach(ligneKbart -> {
                        lastLignesKbartHash.put(ligneKbart.toHash(), ligneKbart.getBestPpn());
                    });
                    fileService.loadFile(tsvFile, lastLignesKbartHash);
                } else {
                    fileService.loadFile(tsvFile, new HashMap<>());
                }
            } catch (Exception | IllegalPackageException e) {
                log.error(e.getMessage());
                log.info("Traitement refusé du fichier {}", tsvFile.getName());
            }
        }
        long endTime = System.currentTimeMillis();
        double executionTime = (double) (endTime - startTime) / 1000;
        log.debug("Temps d'exécution : {} secondes", executionTime);
    }

    @GetMapping(value = {"/file/{fileName}", "/file/{path}/{fileName}"})
    public ResponseEntity<?> getFile(@PathVariable(required = false) String path, @PathVariable String fileName) {
        boolean isReport = ((path != null) && path.equals("report"));

        if (fileName == null || fileName.isEmpty()) {
            return ResponseEntity.badRequest().body("Le paramètre fileName est vide.");
        } else if ((path != null) && !path.equals("report")){
            return ResponseEntity.badRequest().body("Le chemin est incorrect");
        }
        try {
            File fichier = isReport ? new File(pathToKbart + path + File.separator + fileName) : new File(pathToKbart + fileName);

            if (!fichier.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Le fichier " + fileName + " est introuvable.");
            }

            FileInputStream fs = new FileInputStream(fichier);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fichier.getName());
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM); // Use setContentType for better clarity

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(fichier.length())
                    .body(new InputStreamResource(fs));

        } catch (FileNotFoundException e) {
            // This should ideally never happen given the file.exists() check, but it's good practice to keep it.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la lecture du fichier : " + e.getMessage());
        }
    }

    @GetMapping(value = {"/checkfile/{fileName}", "/checkfile/{fileName}"})
    public ResponseEntity<String> checkFile(@PathVariable String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return ResponseEntity.badRequest().body("Le paramètre fileName est vide.");
        }

        File fichier = new File(pathToKbart + File.separator + fileName);

        if (!fichier.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Le fichier " + fileName + " est introuvable.");
        }
        return ResponseEntity.ok(fileService.checkFile(fichier));
    }

    private void checkExistingPackage(String filename) throws IllegalProviderException, IllegalPackageException, IllegalDateException {
        if (providerPackageService.hasMoreRecentPackageInBdd(Utils.extractProvider(filename), Utils.extractPackageName(filename), Utils.extractDateFilename(filename)))
            throw new IllegalPackageException("Un package plus récent est déjà présent dans la base");
    }

    private List<LigneKbartDto> getLigneKbartFromLastExistingPackage(File tsvFile) throws IllegalProviderException, IllegalPackageException {
        ProviderPackage providerPackage = providerPackageService.getLastProviderPackage(Utils.extractProvider(tsvFile.getName()), Utils.extractPackageName(tsvFile.getName()));
        if (providerPackage != null) {
            return providerPackageService.getLigneKbartByProviderPackage(providerPackage);
        }
        return new ArrayList<>();
    }

}
