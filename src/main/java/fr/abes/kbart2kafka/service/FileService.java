package fr.abes.kbart2kafka.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.kbart2kafka.dto.LigneKbartDto;
import fr.abes.kbart2kafka.exception.IllegalDateException;
import fr.abes.kbart2kafka.exception.IllegalFileFormatException;
import fr.abes.kbart2kafka.utils.Utils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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

    private final ObjectMapper mapper;
    ExecutorService executor;

    public FileService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper mapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.mapper = mapper;
    }

    @PostConstruct
    void initExecutor() {
        executor = Executors.newFixedThreadPool(nbThread);
    }


    public void loadFile(File fichier, String kbartHeader) throws IllegalFileFormatException, IOException {
        executeMultiThread(fichier, kbartHeader);
    }

    private void executeMultiThread(File fichier, String kbartHeader) throws IOException, IllegalFileFormatException {
        try (BufferedReader buff = new BufferedReader(new FileReader(fichier))) {
            List<String> fileContent = buff.lines().toList();
            List<String> kbartsToSend = new ArrayList<>();
            Integer nbLignesFichier = fileContent.size() - 1;
            log.debug("Début d'envoi de " + nbLignesFichier + " lignes du fichier");
            AtomicInteger cpt = new AtomicInteger(0);
            List<String> errorsList = new ArrayList<>();
            fileContent.stream().skip(1).forEach(ligneKbart -> {
                String[] tsvElementsOnOneLine = ligneKbart.split("\t");
                try {
                    kbartsToSend.add(mapper.writeValueAsString(constructDto(tsvElementsOnOneLine, cpt.incrementAndGet())));
                } catch (IllegalDateException | IllegalFileFormatException | JsonProcessingException e) {
                    errorsList.add("Erreur dans le fichier en entrée à la ligne " + cpt.get());
                }
            });
            if (errorsList.isEmpty()) {
                cpt.set(1);
                kbartsToSend.forEach(kbart -> {
                    executor.execute(() -> {
                        cpt.incrementAndGet();
                        ThreadContext.put("package", fichier.getName());
                        List<org.apache.kafka.common.header.Header> headers = new ArrayList<>();
                        headers.add(new RecordHeader("nbCurrentLines", String.valueOf(cpt.get()).getBytes()));
                        headers.add(new RecordHeader("nbLinesTotal", String.valueOf(nbLignesFichier).getBytes()));
                        ProducerRecord<String, String> record = new ProducerRecord<>(topicKbart, new Random().nextInt(nbThread), fichier.getName(), kbart, headers);
                        CompletableFuture<SendResult<String, String>> result = kafkaTemplate.send(record);
                        result.whenComplete((sr, ex) -> {
                            if (ex != null) {
                                log.error(ex.getMessage()); // vérification du résultat et log
                                sendErrorToKafka("erreur d'insertion dans le topic pour la ligne " + cpt.get(), fichier.getName());
                            }
                        });
                    });
                });
            } else {
                errorsList.forEach(error -> sendErrorToKafka(error, fichier.getName()));
            }
        } catch (IOException ex) {
            log.error("Erreur d'envoi dans kafka " + ex.getMessage());
            sendErrorToKafka("erreur d'envoi des données : ", fichier.getName());
        } finally {
            executor.shutdown();
        }

    }

    private void sendErrorToKafka(String errorMessage, String filename) {
        log.error(errorMessage + " - " + filename);
        kafkaTemplate.send(new ProducerRecord<>(topicErrors, filename, errorMessage));
    }

    /**
     * Construction de la dto
     *
     * @param line ligne en entrée
     * @return Un objet DTO initialisé avec les informations de la ligne
     */
    private LigneKbartDto constructDto(String[] line, Integer ligneDuFichier) throws IllegalFileFormatException, IllegalDateException {
        if ((line.length > 26) || (line.length < 25)) {
            throw new IllegalFileFormatException("La ligne n°" + ligneDuFichier + " ne comporte pas le bon nombre de colonnes");
        }
        LigneKbartDto kbartLineInDtoObject = new LigneKbartDto();
        kbartLineInDtoObject.setPublication_title(line[0]);
        kbartLineInDtoObject.setPrint_identifier(line[1]);
        kbartLineInDtoObject.setOnline_identifier(line[2]);
        kbartLineInDtoObject.setDate_first_issue_online(Utils.reformatDateKbart(line[3]));
        kbartLineInDtoObject.setNum_first_vol_online(Integer.getInteger(line[4]));
        kbartLineInDtoObject.setNum_first_issue_online(Integer.getInteger(line[5]));
        kbartLineInDtoObject.setDate_last_issue_online(Utils.reformatDateKbart(line[6]));
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
        kbartLineInDtoObject.setDate_monograph_published_print(Utils.reformatDateKbart(line[17]));
        kbartLineInDtoObject.setDate_monograph_published_online(Utils.reformatDateKbart(line[18]));
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
