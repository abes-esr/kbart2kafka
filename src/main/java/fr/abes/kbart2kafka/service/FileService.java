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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
        executor = Executors.newFixedThreadPool(nbThread);
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
                    log.error("Erreur dans le fichier en entrée à la ligne " + cpt.get() + " : " + e.getMessage());
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

//        checkKbart(kbartLineInDtoObject, isForcedOrBypassed);
        return kbartLineInDtoObject;

    }

    private void checkKbart(LigneKbartDto ligneKbartDto, Boolean isForcedOrBypassed) throws IllegalFileFormatException {
        if(ligneKbartDto.getPrint_identifier().equals(ligneKbartDto.getOnline_identifier()) && !isForcedOrBypassed){
            throw new IllegalFileFormatException("Les champs PRINT_IDENTIFIER et ONLINE_IDENTIFIER sont identiques");
        }
        if((!ligneKbartDto.getNum_first_vol_online().isEmpty() && !ligneKbartDto.getNum_first_vol_online().matches("\\d+")) && !isForcedOrBypassed){
            throw new IllegalFileFormatException("La valeur de NUM_FIRST_VOL_ONLINE n'est pas un nombre");
        }
        if((!ligneKbartDto.getNum_first_issue_online().isEmpty() && !ligneKbartDto.getNum_first_issue_online().matches("\\d+")) && !isForcedOrBypassed){
            throw new IllegalFileFormatException("La valeur de NUM_FIRST_ISSUE_ONLINE n'est pas un nombre");
        }
        if((!ligneKbartDto.getNum_last_vol_online().isEmpty() && !ligneKbartDto.getNum_last_vol_online().matches("\\d+")) && !isForcedOrBypassed){
            throw new IllegalFileFormatException("La valeur de NUM_LAST_VOL_ONLINE n'est pas un nombre");
        }
        if((!ligneKbartDto.getNum_last_issue_online().isEmpty() && !ligneKbartDto.getNum_last_issue_online().matches("\\d+")) && !isForcedOrBypassed){
            throw new IllegalFileFormatException("La valeur de NUM_LAST_ISSUE_ONLINE n'est pas un nombre");
        }
        if(ligneKbartDto.getTitle_url().isEmpty() && !isForcedOrBypassed){
            throw new IllegalFileFormatException("La valeur de TITLE_URL est vide");
        }
        if(ligneKbartDto.getTitle_id().isEmpty() && !isForcedOrBypassed){
            throw new IllegalFileFormatException("La valeur de TITLE_ID est vide");
        }
        if(!ligneKbartDto.getCoverage_depth().equals("fulltext")){
            throw new IllegalFileFormatException("La valeur de COVERAGE_DEPTH est invalide");
        }
        try {
            PUBLICATION_TYPE.valueOf(ligneKbartDto.getPublication_type());
        } catch (IllegalArgumentException ex) {
            throw new IllegalFileFormatException("La valeur de PUBLICATION_TYPE est invalide. (valeurs acceptées : monograph, serial)");
        }
        if(!ligneKbartDto.getAccess_type().equals("P") && !ligneKbartDto.getAccess_type().equals("F")){
            throw new IllegalFileFormatException("La valeur de ACCESS_TYPE est invalide. (valeurs acceptées : P, F)");
        }
        if (ligneKbartDto.getPublication_type().equals("serial") && ligneKbartDto.getDate_first_issue_online().isEmpty() && !isForcedOrBypassed){
            throw new IllegalFileFormatException("DATE_FIRST_ISSUE_ONLINE est obligatoire si PUBLICATION_TYPE est serial");
        }

        if( !ligneKbartDto.getDate_first_issue_online().isEmpty() &&
                !ligneKbartDto.getDate_last_issue_online().isEmpty() &&
                Utils.isDateBeforeOtherDate(ligneKbartDto.getDate_last_issue_online(),ligneKbartDto.getDate_first_issue_online())){
            throw new IllegalFileFormatException("DATE_LAST_ISSUE_ONLINE ne peut pas être antérieure à DATE_FIRST_ISSUE_ONLINE");
        }
    }
}
