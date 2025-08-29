package fr.abes.kbart2kafka.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.kbart2kafka.dto.LigneKbartDto;
import fr.abes.kbart2kafka.exception.IllegalDateException;
import fr.abes.kbart2kafka.exception.IllegalFileFormatException;
import fr.abes.kbart2kafka.utils.CheckFiles;
import fr.abes.kbart2kafka.utils.PUBLICATION_TYPE;
import fr.abes.kbart2kafka.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class FileService {

    @Value("${topic.name.target.kbart}")
    private String topicKbart;


    @Value("${abes.kafka.concurrency.nbThread}")
    private int nbThread;
    private final AtomicInteger lastThreadUsed;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper mapper;
    ExecutorService executor;

    public FileService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper mapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.mapper = mapper;
        this.lastThreadUsed = new AtomicInteger(0);
    }

//    @PostConstruct
    void initExecutor() {
        int corePoolSize    = nbThread;
        int maximumPoolSize = nbThread;
        long keepAliveTime  = 0L;
        TimeUnit unit       = TimeUnit.MILLISECONDS;
        // Choisissez une capacité de file adaptée à votre volume
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(1000);

        executor = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                queue,
                // ici, si la file est pleine, la tâche s'exécutera dans le thread appelant
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }


    public void loadFile(File fichier) throws IllegalFileFormatException, IOException {
        executeMultiThread(fichier);
    }

    private void executeMultiThread(File fichier) throws IllegalFileFormatException {
        initExecutor();
        try (BufferedReader buff = new BufferedReader(new FileReader(fichier))) {
            List<String> fileContent = buff.lines().toList();
            List<String> kbartsToSend = new ArrayList<>();
            Integer nbLignesFichier = fileContent.size() - 1;
            log.debug("Début d'envoi de " + nbLignesFichier + " lignes du fichier");
            AtomicInteger cpt = new AtomicInteger(0);
            AtomicBoolean isOnError = new AtomicBoolean(false);
            Boolean isForcedOrBypassed = fichier.getName().contains("FORCE") || fichier.getName().contains("BYPASS");
            fileContent.stream().skip(1).forEach(ligneKbart -> {
                cpt.incrementAndGet();
                ThreadContext.put("package", fichier.getName() + ";" + cpt.get());
                String[] tsvElementsOnOneLine = ligneKbart.split("\t");
                try {
                    CheckFiles.isValidUtf8(ligneKbart);
                    kbartsToSend.add(mapper.writeValueAsString(constructDto(tsvElementsOnOneLine, cpt.get(), nbLignesFichier, isForcedOrBypassed)));
                } catch (IllegalDateException | IllegalFileFormatException | JsonProcessingException e) {
                    log.error("Erreur dans le fichier en entrée à la ligne " + (cpt.get() + 1) + " : " + e.getMessage());
                    isOnError.set(true);
                }
            });
            if (!isOnError.get()) {
                cpt.set(1);
                kbartsToSend.forEach(kbart -> executor.execute(() -> {
                    cpt.incrementAndGet();
                    String key = fichier.getName() + "_" + cpt.get();
                    ThreadContext.put("package", fichier.getName() + ";" + cpt.get());
                    ProducerRecord<String, String> record = new ProducerRecord<>(topicKbart, calculatePartition(nbThread), key, kbart);
                    kafkaTemplate.send(record);
                }));
            } else {
                ThreadContext.put("package", fichier.getName());
                throw new IllegalFileFormatException("Format du fichier incorrect");
            }
        } catch (IOException ex) {
            ThreadContext.put("package", fichier.getName());
            log.error("Erreur d'envoi dans kafka " + ex.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    public Integer calculatePartition(Integer nbPartitions) throws ArithmeticException {
        if (nbPartitions == 0) {
            throw new ArithmeticException("Nombre de threads = 0");
        }
        return lastThreadUsed.getAndIncrement() % nbPartitions;
    }

    /**
     * Construction de la dto
     *
     * @param line ligne en entrée
     * @return Un objet DTO initialisé avec les informations de la ligne
     */
    public LigneKbartDto constructDto(String[] line, Integer ligneCourante, Integer nbLignesFichier, Boolean isForcedOrBypassed) throws IllegalFileFormatException, IllegalDateException {
        if ((line.length > 26) || (line.length < 25)) {
            throw new IllegalFileFormatException("Nombre de colonnes incorrect");
        }
        LigneKbartDto kbartLineInDtoObject = new LigneKbartDto();
        kbartLineInDtoObject.setNbCurrentLines(ligneCourante - 1);
        kbartLineInDtoObject.setNbLinesTotal(nbLignesFichier);
        kbartLineInDtoObject.setPublication_title(line[0]);
        kbartLineInDtoObject.setPrint_identifier(line[1]);
        kbartLineInDtoObject.setOnline_identifier(line[2]);
        kbartLineInDtoObject.setDate_first_issue_online(Utils.reformatDateKbart(line[3]));
        kbartLineInDtoObject.setNum_first_vol_online(line[4]);
        kbartLineInDtoObject.setNum_first_issue_online(line[5]);
        kbartLineInDtoObject.setDate_last_issue_online(Utils.reformatDateKbart(line[6]));
        kbartLineInDtoObject.setNum_last_vol_online(line[7]);
        kbartLineInDtoObject.setNum_last_issue_online(line[8]);
        kbartLineInDtoObject.setTitle_url(line[9].trim());
        kbartLineInDtoObject.setFirst_author(line[10]);
        kbartLineInDtoObject.setTitle_id(line[11]);
        kbartLineInDtoObject.setEmbargo_info(line[12]);
        kbartLineInDtoObject.setCoverage_depth(line[13]);
        kbartLineInDtoObject.setNotes(line[14]);
        kbartLineInDtoObject.setPublisher_name(line[15]);
        kbartLineInDtoObject.setPublication_type(line[16]);
        kbartLineInDtoObject.setDate_monograph_published_print(Utils.reformatDateKbart(line[17]));
        kbartLineInDtoObject.setDate_monograph_published_online(Utils.reformatDateKbart(line[18]));
        kbartLineInDtoObject.setMonograph_volume(line[19]);
        kbartLineInDtoObject.setMonograph_edition(line[20]);
        kbartLineInDtoObject.setFirst_editor(line[21]);
        kbartLineInDtoObject.setParent_publication_title_id(line[22]);
        kbartLineInDtoObject.setPreceding_publication_title_id(line[23]);
        kbartLineInDtoObject.setAccess_type(line[24]);
        // Vérification de la présence d'un best ppn déjà renseigné dans le kbart
        if (line.length == 26) {
            kbartLineInDtoObject.setBestPpn(line[25]);
        }

        if(kbartLineInDtoObject.getPublication_title().length() > 4000){
            throw new IllegalFileFormatException("La valeur de PUBLICATION_TITLE est trop grande (grandeur acceptée : 4000, grandeur actuelle : " + kbartLineInDtoObject.getPublication_title().length() + ")");
        }

        try {
            PUBLICATION_TYPE.valueOf(kbartLineInDtoObject.getPublication_type());
        } catch (IllegalArgumentException ex) {
            throw new IllegalFileFormatException("La valeur de PUBLICATION_TYPE est invalide. (valeurs acceptées : monograph, serial)");
        }
//        checkKbart(kbartLineInDtoObject, isForcedOrBypassed);
        return kbartLineInDtoObject;

    }

    public String checkFile(File file){
        AtomicReference<String> result = new AtomicReference<>("Nb_line ; Message d'erreur<br/>");
        try (BufferedReader buff = new BufferedReader(new FileReader(file))) {
            List<String> fileContent = buff.lines().toList();
            AtomicInteger currentNbLine = new AtomicInteger(1);
            fileContent.stream().skip(1).forEach(ligneKbart -> {
                try {
                    ArrayList<String> listeError = checkKbart(constructDtoLazyMode(ligneKbart.split("\t"), currentNbLine.get(),fileContent.size()));
                    listeError.forEach(error ->
//                            log.error("Erreur dans la ligne {} : {}", currentNbLine.get(), error)
                            result.set(result.get() + currentNbLine.get() + " ; " + error + "<br/>")
                    );
                } catch (IllegalFileFormatException | IllegalDateException e) {
                    log.info("Erreur dans la ligne {} : {}", currentNbLine.get(), e.getMessage());
                } finally {
                    currentNbLine.incrementAndGet();
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result.get();
    }

    private LigneKbartDto constructDtoLazyMode(String[] line, Integer ligneCourante, Integer nbLignesFichier) throws IllegalFileFormatException, IllegalDateException {
        if ((line.length > 26) || (line.length < 25)) {
            throw new IllegalFileFormatException("Nombre de colonnes incorrect");
        }
        LigneKbartDto kbartLineInDtoObject = new LigneKbartDto();
        kbartLineInDtoObject.setNbCurrentLines(ligneCourante - 1);
        kbartLineInDtoObject.setNbLinesTotal(nbLignesFichier);
        kbartLineInDtoObject.setPublication_title(line[0]);
        kbartLineInDtoObject.setPrint_identifier(line[1]);
        kbartLineInDtoObject.setOnline_identifier(line[2]);
        kbartLineInDtoObject.setDate_first_issue_online(Utils.reformatDateKbart(line[3]));
        kbartLineInDtoObject.setNum_first_vol_online(line[4]);
        kbartLineInDtoObject.setNum_first_issue_online(line[5]);
        kbartLineInDtoObject.setDate_last_issue_online(Utils.reformatDateKbart(line[6]));
        kbartLineInDtoObject.setNum_last_vol_online(line[7]);
        kbartLineInDtoObject.setNum_last_issue_online(line[8]);
        kbartLineInDtoObject.setTitle_url(line[9].trim());
        kbartLineInDtoObject.setFirst_author(line[10]);
        kbartLineInDtoObject.setTitle_id(line[11]);
        kbartLineInDtoObject.setEmbargo_info(line[12]);
        kbartLineInDtoObject.setCoverage_depth(line[13]);
        kbartLineInDtoObject.setNotes(line[14]);
        kbartLineInDtoObject.setPublisher_name(line[15]);
        kbartLineInDtoObject.setPublication_type(line[16]);
        kbartLineInDtoObject.setDate_monograph_published_print(Utils.reformatDateKbart(line[17]));
        kbartLineInDtoObject.setDate_monograph_published_online(Utils.reformatDateKbart(line[18]));
        kbartLineInDtoObject.setMonograph_volume(line[19]);
        kbartLineInDtoObject.setMonograph_edition(line[20]);
        kbartLineInDtoObject.setFirst_editor(line[21]);
        kbartLineInDtoObject.setParent_publication_title_id(line[22]);
        kbartLineInDtoObject.setPreceding_publication_title_id(line[23]);
        kbartLineInDtoObject.setAccess_type(line[24]);
        // Vérification de la présence d'un best ppn déjà renseigné dans le kbart
        if (line.length == 26) {
            kbartLineInDtoObject.setBestPpn(line[25]);
        }
        return kbartLineInDtoObject;
    }

    private ArrayList<String> checkKbart(LigneKbartDto ligneKbartDto) throws IllegalFileFormatException {
        ArrayList<String> errors = new ArrayList<>();
        if(ligneKbartDto.getPrint_identifier().equals(ligneKbartDto.getOnline_identifier())){
            errors.add("Les champs PRINT_IDENTIFIER et ONLINE_IDENTIFIER sont identiques");
        }
        if(!ligneKbartDto.getNum_first_vol_online().isEmpty() && !ligneKbartDto.getNum_first_vol_online().matches("\\d+")){
            errors.add("La valeur de NUM_FIRST_VOL_ONLINE n'est pas un nombre");
        }
        if(!ligneKbartDto.getNum_first_issue_online().isEmpty() && !ligneKbartDto.getNum_first_issue_online().matches("\\d+")){
            errors.add("La valeur de NUM_FIRST_ISSUE_ONLINE n'est pas un nombre");
        }
        if(!ligneKbartDto.getNum_last_vol_online().isEmpty() && !ligneKbartDto.getNum_last_vol_online().matches("\\d+")){
            errors.add("La valeur de NUM_LAST_VOL_ONLINE n'est pas un nombre");
        }
        if(!ligneKbartDto.getNum_last_issue_online().isEmpty() && !ligneKbartDto.getNum_last_issue_online().matches("\\d+")){
            errors.add("La valeur de NUM_LAST_ISSUE_ONLINE n'est pas un nombre");
        }
        if(ligneKbartDto.getTitle_url().isEmpty()){
            errors.add("La valeur de TITLE_URL est vide");
        }
        if(ligneKbartDto.getTitle_id().isEmpty()){
            errors.add("La valeur de TITLE_ID est vide");
        }
        if(!ligneKbartDto.getCoverage_depth().equals("fulltext")){
            errors.add("La valeur de COVERAGE_DEPTH est invalide");
        }
        if(!ligneKbartDto.getAccess_type().equals("P") && !ligneKbartDto.getAccess_type().equals("F")){
            errors.add("La valeur de ACCESS_TYPE est invalide. (valeurs acceptées : P, F)");
        }
        if (ligneKbartDto.getPublication_type().equals("serial") && ligneKbartDto.getDate_first_issue_online().isEmpty()){
            errors.add("DATE_FIRST_ISSUE_ONLINE est obligatoire si PUBLICATION_TYPE est serial");
        }

        if( !ligneKbartDto.getDate_first_issue_online().isEmpty() &&
                !ligneKbartDto.getDate_last_issue_online().isEmpty() ) {

            if (Utils.isDateBeforeOtherDate(ligneKbartDto.getDate_last_issue_online(),ligneKbartDto.getDate_first_issue_online())) {
                errors.add("DATE_LAST_ISSUE_ONLINE ne peut pas être antérieure à DATE_FIRST_ISSUE_ONLINE");
            }
        }

        if( !ligneKbartDto.getDate_first_issue_online().isEmpty() && Utils.isDateAfterToday(ligneKbartDto.getDate_first_issue_online())){
            errors.add("La Date DATE_FIRST_ISSUE_ONLINE est mal formatées (ex. années invalides : « " + ligneKbartDto.getDate_first_issue_online() + " »");
        }

        if( !ligneKbartDto.getDate_last_issue_online().isEmpty() && Utils.isDateAfterToday(ligneKbartDto.getDate_last_issue_online())){
            errors.add("La Date DATE_LAST_ISSUE_ONLINE est mal formatées (ex. années invalides : « " + ligneKbartDto.getDate_last_issue_online() + " »");
        }

        if( !ligneKbartDto.getDate_monograph_published_online().isEmpty() && Utils.isDateAfterToday(ligneKbartDto.getDate_monograph_published_online())){
            errors.add("La Date DATE_MONOGRAPH_PUBLISHED_ONLINE est mal formatées (ex. années invalides : « " + ligneKbartDto.getDate_monograph_published_online() + " »");
        }

        if( !ligneKbartDto.getDate_monograph_published_print().isEmpty() && Utils.isDateAfterToday(ligneKbartDto.getDate_monograph_published_print())){
            errors.add("La Date DATE_MONOGRAPH_PUBLISHED_PRINT est mal formatées (ex. années invalides : « " + ligneKbartDto.getDate_monograph_published_print() + " »");
        }

        if(ligneKbartDto.getPublication_title().length() > 4000){
            errors.add("La valeur de PUBLICATION_TITLE est trop grande (grandeur acceptée : 4000, grandeur actuelle : " + ligneKbartDto.getPublication_title().length() + ")");
        }

        try {
            PUBLICATION_TYPE.valueOf(ligneKbartDto.getPublication_type());
        } catch (IllegalArgumentException ex) {
            errors.add("La valeur de PUBLICATION_TYPE est invalide. (valeurs acceptées : monograph, serial)");
        }

        if( ligneKbartDto.getPublication_type().equals("serial") && ligneKbartDto.getDate_first_issue_online().isEmpty()){
            errors.add("Date de début de publication manquante pour les revues");
        }

        if ( ligneKbartDto.getTitle_url().isEmpty() ) {
            errors.add("TITLE_URL est vide");
        } else {
            if( !ligneKbartDto.getTitle_url().startsWith("http://") && !ligneKbartDto.getTitle_url().startsWith("https://") && !ligneKbartDto.getTitle_url().startsWith("www.")) {
                errors.add("Le TITLE_URL ne contient pas 'http://' ni 'https://' ni 'www.'");
            }

            if (ligneKbartDto.getTitle_id().contains(" ")) {
                errors.add("Le TITLE_URL contient des espaces");
            }
        }

        if( ligneKbartDto.getTitle_id().isEmpty() ) {
            errors.add("TITLE_ID est vide");

        }

        return errors;
    }
}