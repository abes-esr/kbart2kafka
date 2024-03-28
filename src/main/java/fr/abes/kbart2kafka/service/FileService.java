package fr.abes.kbart2kafka.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.kbart2kafka.dto.LigneKbartDto;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class FileService {

    @Value("${topic.name.target.kbart}")
    private String topicKbart;

    @Value("${topic.name.target.errors}")
    private String topicErrors;

    @Value("${spring.kafka.producer.nbthread}")
    private int nbThread;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private final EmailService emailService;

    private final ObjectMapper mapper;
    ExecutorService executor;

    public FileService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper mapper, EmailService emailService) {
        this.kafkaTemplate = kafkaTemplate;
        this.mapper = mapper;
        this.emailService = emailService;
    }

    @PostConstruct
    void initExecutor() {
        executor = Executors.newFixedThreadPool(nbThread);
    }

    @Transactional
    public void loadFile(File fichier, String kbartHeader) {
        try {
            executeMultiThread(fichier, kbartHeader);
        } catch (IOException ex) {
            log.error("Erreur dans la lecture du fichier");
        }

    }

    private void executeMultiThread(File fichier, String kbartHeader) throws IOException {
        // Compteur du nombre de lignes dans le kbart
        int lineCounter = 0;
        List<String> errorList = new ArrayList<>();
        try (BufferedReader buff = new BufferedReader(new FileReader(fichier))) {
            List<String> fileContent = buff.lines().toList();
            Integer nbLignesFichier = fileContent.size() - 1;
            log.debug("Début d'envoi de "+ nbLignesFichier + " lignes du fichier");
            for (String ligneKbart : fileContent) {
                if (!ligneKbart.contains(kbartHeader)) {
                    lineCounter++;
                    // Crée un nouvel objet dto, set les différentes parties et envoi au service topicProducer
                    String[] tsvElementsOnOneLine = ligneKbart.split("\t");
                    LigneKbartDto ligneKbartDto = constructDto(tsvElementsOnOneLine);

                    final int finalLineCounter = lineCounter;
                    executor.execute(() -> {
                        try {
                            ThreadContext.put("package", fichier.getName());
                            List<org.apache.kafka.common.header.Header> headers = new ArrayList<>();
                            headers.add(new RecordHeader("nbCurrentLines", String.valueOf(finalLineCounter).getBytes()));
                            headers.add(new RecordHeader("nbLinesTotal", String.valueOf(nbLignesFichier).getBytes()));
                            ProducerRecord<String, String> record = new ProducerRecord<>(topicKbart, new Random().nextInt(nbThread), fichier.getName(), mapper.writeValueAsString(ligneKbartDto), headers);
                            CompletableFuture<SendResult<String, String>> result = kafkaTemplate.executeInTransaction(kt -> kt.send(record));
                            assert result != null;
                            result.whenComplete((sr, ex) -> {
                                try {
                                    log.debug(mapper.writeValueAsString(result.get().getProducerRecord().value())); // vérification du résultat de la transaction et log
                                } catch (JsonProcessingException | InterruptedException | ExecutionException e) {
                                    log.warn("Erreur dans le mapping du résultat de l'envoi dans le topic pour la ligne " + ligneKbartDto);
                                }
                            });
                        } catch (JsonProcessingException e) {
                            sendErrorToKafka("erreur de mapping des données au chargement de la ligne " + finalLineCounter, e, fichier.getName());
                            errorList.add("Erreur de mapping des données au chargement de la ligne " + finalLineCounter + " : " + e.getMessage());
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        } catch (IOException ex) {
            sendErrorToKafka("erreur de lecture du fichier", ex, fichier.getName());
            errorList.add("Erreur de lecture du fichier : " + ex.getMessage());
        } finally {
            if (!errorList.isEmpty()) emailService.sendKbartFileReadError(fichier.getName(), errorList.toString());
            executor.shutdown();
        }
    }

    private void sendErrorToKafka(String errorMessage, Exception exception, String filename) {
        log.error(errorMessage + exception + " - " + filename);
        kafkaTemplate.send(new ProducerRecord<>(topicErrors, new Random().nextInt(nbThread), filename, errorMessage + exception.getMessage()));
    }

    /**
     * Construction de la dto
     *
     * @param line ligne en entrée
     * @return Un objet DTO initialisé avec les informations de la ligne
     */
    private LigneKbartDto constructDto(String[] line) {
        LigneKbartDto kbartLineInDtoObject = new LigneKbartDto();
        kbartLineInDtoObject.setPublication_title(line[0]);
        kbartLineInDtoObject.setPrint_identifier(line[1]);
        kbartLineInDtoObject.setOnline_identifier(line[2]);
        kbartLineInDtoObject.setDate_first_issue_online(line[3]);
        kbartLineInDtoObject.setNum_first_vol_online(Integer.getInteger(line[4]));
        kbartLineInDtoObject.setNum_first_issue_online(Integer.getInteger(line[5]));
        kbartLineInDtoObject.setDate_last_issue_online(line[6]);
        kbartLineInDtoObject.setNum_last_vol_online(Integer.getInteger(line[7]));
        kbartLineInDtoObject.setNum_last_issue_online(Integer.getInteger(line[8]));
        kbartLineInDtoObject.setTitle_url(line[9]);
        kbartLineInDtoObject.setFirst_author(line[10]);
        kbartLineInDtoObject.setTitle_id(line[11]);
        kbartLineInDtoObject.setEmbargo_info(line[12]);
        kbartLineInDtoObject.setCoverage_depth(line[13]);
        kbartLineInDtoObject.setNotes(line[14]);
        kbartLineInDtoObject.setPublisher_name(line[15]);
        kbartLineInDtoObject.setPublication_type(line[16]);
        kbartLineInDtoObject.setDate_monograph_published_print(line[17]);
        kbartLineInDtoObject.setDate_monograph_published_online(line[18]);
        kbartLineInDtoObject.setMonograph_volume(Integer.getInteger(line[19]));
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
}
