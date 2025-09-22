package fr.abes.kbart2kafka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.kbart2kafka.dto.LigneKbartDto;
import fr.abes.kbart2kafka.exception.IllegalDateException;
import fr.abes.kbart2kafka.exception.IllegalFileFormatException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.File;

@SpringBootTest(classes = {FileService.class, ObjectMapper.class})
class FileServiceTest {
    @Value("${topic.name.target.kbart}")
    private String topicKbart;


    @Value("${abes.kafka.concurrency.nbThread}")
    private int nbThread;
    @Autowired
    FileService fileService;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    KafkaTemplate<String,String> kafkaTemplate;




    @Test
    void testConstructionDTOisOK() throws IllegalFileFormatException, IllegalDateException {
        String[] lineInput = {
                "Le titre de publication",
                "0001-4842",
                "1520-4898",
                "1996-01",
                "",
                "2",
                "1996-01-11",
                "3",
                "4",
                "https://pubs.acs.org/loi/achre4",
                "SamQ",
                "achre4",
                "c quoi",
                "fulltext",
                "c une notes",
                "American Chemical Society",
                "serial",
                "1996",
                "1996-01-13",
                "5",
                "c monograph_edition",
                "c first_editor",
                "jchsc2",
                "jchsc3",
                "P"
        };

        LigneKbartDto lineOut = fileService.constructDto(lineInput,1,1,false);

        Assertions.assertEquals(1, lineOut.getNbLinesTotal());
        Assertions.assertEquals(0, lineOut.getNbCurrentLines());
        Assertions.assertEquals("Le titre de publication", lineOut.getPublication_title());
        Assertions.assertEquals("0001-4842", lineOut.getPrint_identifier());
        Assertions.assertEquals("1520-4898", lineOut.getOnline_identifier());
        Assertions.assertEquals("1996-01-01", lineOut.getDate_first_issue_online());
        Assertions.assertEquals("",lineOut.getNum_first_vol_online());
        Assertions.assertEquals("2", lineOut.getNum_first_issue_online());
        Assertions.assertEquals("1996-01-11", lineOut.getDate_last_issue_online());
        Assertions.assertEquals("3", lineOut.getNum_last_vol_online());
        Assertions.assertEquals("4", lineOut.getNum_last_issue_online());
        Assertions.assertEquals("https://pubs.acs.org/loi/achre4", lineOut.getTitle_url());
        Assertions.assertEquals("SamQ", lineOut.getFirst_author());
        Assertions.assertEquals("achre4", lineOut.getTitle_id());
        Assertions.assertEquals("c quoi", lineOut.getEmbargo_info());
        Assertions.assertEquals("fulltext", lineOut.getCoverage_depth());
        Assertions.assertEquals("c une notes", lineOut.getNotes());
        Assertions.assertEquals("American Chemical Society", lineOut.getPublisher_name());
        Assertions.assertEquals("serial", lineOut.getPublication_type());
        Assertions.assertEquals("1996-01-01", lineOut.getDate_monograph_published_print());
        Assertions.assertEquals("1996-01-13", lineOut.getDate_monograph_published_online());
        Assertions.assertEquals("5", lineOut.getMonograph_volume());
        Assertions.assertEquals("c monograph_edition", lineOut.getMonograph_edition());
        Assertions.assertEquals("c first_editor", lineOut.getFirst_editor());
        Assertions.assertEquals("jchsc2", lineOut.getParent_publication_title_id());
        Assertions.assertEquals("jchsc3", lineOut.getPreceding_publication_title_id());
        Assertions.assertEquals("P", lineOut.getAccess_type());
        Assertions.assertNull(lineOut.getBestPpn());
    }

    @Test
    void testConstructionDTODateisKO() throws IllegalFileFormatException, IllegalDateException {
        String[] lineInput = {
                "Le titre de publication",
                "0001-4842",
                "1520-4898",
                "1996-01-10",
                "",
                "2",
                "1997",
                "4",
                "4",
                "https://pubs.acs.org/loi/achre4",
                "SamQ",
                "achre4",
                "c quoi",
                "fulltext",
                "c une notes",
                "American Chemical Society",
                "serial",
                "1996-01",
                "196-01-13",
                "5",
                "c monograph_edition",
                "c first_editor",
                "jchsc2",
                "jchsc3",
                "P"
        };

        Assertions.assertThrows(IllegalDateException.class, () -> fileService.constructDto(lineInput,1,1, false));
    }

    @Test
    void testConstructionDTOlengthisKO() throws IllegalFileFormatException, IllegalDateException {
        String[] lineInput = {
                "Le titre de publication",
                "0001-4842",
                "1520-4898",
                "1996-01-10",
                "",
                "2",
                "1997-01",
                "3",
                "4",
                "https://pubs.acs.org/loi/achre4",
                "SamQ",
                "achre4",
                "c quoi",
                "fulltext",
                "c une notes",
                "American Chemical Society",
                "serial",
                "1996",
                "1996-01-13",
                "5",
                "c monograph_edition",
                "c first_editor",
                "jchsc2",
                "jchsc3",
        };

        Assertions.assertThrows(IllegalFileFormatException.class, () -> fileService.constructDto(lineInput,1,1,false));
    }

//    @Test
//    void testConstructionDTOpublicationTypeisKO() throws IllegalFileFormatException, IllegalDateException {
//        String[] lineInput = {
//                "Le titre de publication",
//                "0001-4842",
//                "1520-4898",
//                "1996-01-10",
//                "",
//                "2",
//                "1997-01",
//                "3",
//                "4",
//                "https://pubs.acs.org/loi/achre4",
//                "SamQ",
//                "achre4",
//                "c quoi",
//                "fulltext",
//                "c une notes",
//                "American Chemical Society",
//                "seriafvdl",
//                "1996",
//                "1996-01-13",
//                "5",
//                "c monograph_edition",
//                "c first_editor",
//                "jchsc2",
//                "jchsc3",
//                "P"
//        };
//        Assertions.assertThrows(IllegalFileFormatException.class, () -> fileService.constructDto(lineInput,1,1,false));
//    }

//    @Test
//    void testConstructionDTOCoverageDephtisKO() throws IllegalFileFormatException, IllegalDateException {
//        String[] lineInput = {
//                "Le titre de publication",
//                "0001-4842",
//                "1520-4898",
//                "1996-01-10",
//                "",
//                "2",
//                "1997-01",
//                "3",
//                "4",
//                "https://pubs.acs.org/loi/achre4",
//                "SamQ",
//                "achre4",
//                "c quoi",
//                "",
//                "c une notes",
//                "American Chemical Society",
//                "serial",
//                "1996",
//                "1996-01-13",
//                "5",
//                "c monograph_edition",
//                "c first_editor",
//                "jchsc2",
//                "jchsc3",
//                "P"
//        };
//        Assertions.assertThrows(IllegalFileFormatException.class, () -> fileService.constructDto(lineInput,1,1,false));
//    }


//    @Test
//    void testConstructionDTOtitleurlisKO() throws IllegalFileFormatException, IllegalDateException {
//        String[] lineInput = {
//                "Le titre de publication",
//                "0001-4842",
//                "1520-4898",
//                "1996-01-10",
//                "",
//                "2",
//                "1997-01",
//                "3",
//                "4",
//                "",
//                "SamQ",
//                "achre4",
//                "c quoi",
//                "fulltext",
//                "c une notes",
//                "American Chemical Society",
//                "serial",
//                "1996",
//                "1996-01-13",
//                "5",
//                "c monograph_edition",
//                "c first_editor",
//                "jchsc2",
//                "jchsc3",
//                "P"
//        };
//        Assertions.assertThrows(IllegalFileFormatException.class, () -> fileService.constructDto(lineInput,1,1,false));
//    }

    @Test
    void testConstructionDTOtitleurlisOK() throws IllegalFileFormatException, IllegalDateException {
        String[] lineInput = {
                "Le titre de publication",
                "0001-4842",
                "1520-4898",
                "1996-01-10",
                "",
                "2",
                "1997-01",
                "3",
                "4",
                "",
                "SamQ",
                "achre4",
                "c quoi",
                "fulltext",
                "c une notes",
                "American Chemical Society",
                "serial",
                "1996",
                "1996-01-13",
                "5",
                "c monograph_edition",
                "c first_editor",
                "jchsc2",
                "jchsc3",
                "P"
        };
        LigneKbartDto lineOut = fileService.constructDto(lineInput,1,1,true);
        Assertions.assertEquals(1, lineOut.getNbLinesTotal());
        Assertions.assertEquals(0, lineOut.getNbCurrentLines());
        Assertions.assertEquals("Le titre de publication", lineOut.getPublication_title());
        Assertions.assertEquals("0001-4842", lineOut.getPrint_identifier());
        Assertions.assertEquals("1520-4898", lineOut.getOnline_identifier());
        Assertions.assertEquals("1996-01-10", lineOut.getDate_first_issue_online());
        Assertions.assertEquals("",lineOut.getNum_first_vol_online());
        Assertions.assertEquals("2", lineOut.getNum_first_issue_online());
        Assertions.assertEquals("1997-01-01", lineOut.getDate_last_issue_online());
        Assertions.assertEquals("3", lineOut.getNum_last_vol_online());
        Assertions.assertEquals("4", lineOut.getNum_last_issue_online());
        Assertions.assertEquals("", lineOut.getTitle_url());
        Assertions.assertEquals("SamQ", lineOut.getFirst_author());
        Assertions.assertEquals("achre4", lineOut.getTitle_id());
        Assertions.assertEquals("c quoi", lineOut.getEmbargo_info());
        Assertions.assertEquals("fulltext", lineOut.getCoverage_depth());
        Assertions.assertEquals("c une notes", lineOut.getNotes());
        Assertions.assertEquals("American Chemical Society", lineOut.getPublisher_name());
        Assertions.assertEquals("serial", lineOut.getPublication_type());
        Assertions.assertEquals("1996-01-01", lineOut.getDate_monograph_published_print());
        Assertions.assertEquals("1996-01-13", lineOut.getDate_monograph_published_online());
        Assertions.assertEquals("5", lineOut.getMonograph_volume());
        Assertions.assertEquals("c monograph_edition", lineOut.getMonograph_edition());
        Assertions.assertEquals("c first_editor", lineOut.getFirst_editor());
        Assertions.assertEquals("jchsc2", lineOut.getParent_publication_title_id());
        Assertions.assertEquals("jchsc3", lineOut.getPreceding_publication_title_id());
        Assertions.assertEquals("P", lineOut.getAccess_type());
        Assertions.assertNull(lineOut.getBestPpn());
    }

//    @Test
//    void testConstructionDTOtitleIdisKO() throws IllegalFileFormatException, IllegalDateException {
//        String[] lineInput = {
//                "Le titre de publication",
//                "0001-4842",
//                "1520-4898",
//                "1996-01-10",
//                "",
//                "2",
//                "1997-01",
//                "3",
//                "4",
//                "https://pubs.acs.org/loi/achre4",
//                "SamQ",
//                "",
//                "c quoi",
//                "fulltext",
//                "c une notes",
//                "American Chemical Society",
//                "serial",
//                "1996",
//                "1996-01-13",
//                "5",
//                "c monograph_edition",
//                "c first_editor",
//                "jchsc2",
//                "jchsc3",
//                "P"
//        };
//        Assertions.assertThrows(IllegalFileFormatException.class, () -> fileService.constructDto(lineInput,1,1,false));
//    }

    @Test
    void testConstructionDTOtitleIdisOK() throws IllegalFileFormatException, IllegalDateException {
        String[] lineInput = {
                "Le titre de publication",
                "0001-4842",
                "1520-4898",
                "1996-01-10",
                "",
                "2",
                "1997-01",
                "3",
                "4",
                "https://pubs.acs.org/loi/achre4",
                "SamQ",
                "",
                "c quoi",
                "fulltext",
                "c une notes",
                "American Chemical Society",
                "serial",
                "1996",
                "1996-01-13",
                "5",
                "c monograph_edition",
                "c first_editor",
                "jchsc2",
                "jchsc3",
                "P"
        };
        LigneKbartDto lineOut = fileService.constructDto(lineInput,1,1,true);
        Assertions.assertEquals(1, lineOut.getNbLinesTotal());
        Assertions.assertEquals(0, lineOut.getNbCurrentLines());
        Assertions.assertEquals("Le titre de publication", lineOut.getPublication_title());
        Assertions.assertEquals("0001-4842", lineOut.getPrint_identifier());
        Assertions.assertEquals("1520-4898", lineOut.getOnline_identifier());
        Assertions.assertEquals("1996-01-10", lineOut.getDate_first_issue_online());
        Assertions.assertEquals("",lineOut.getNum_first_vol_online());
        Assertions.assertEquals("2", lineOut.getNum_first_issue_online());
        Assertions.assertEquals("1997-01-01", lineOut.getDate_last_issue_online());
        Assertions.assertEquals("3", lineOut.getNum_last_vol_online());
        Assertions.assertEquals("4", lineOut.getNum_last_issue_online());
        Assertions.assertEquals("https://pubs.acs.org/loi/achre4", lineOut.getTitle_url());
        Assertions.assertEquals("SamQ", lineOut.getFirst_author());
        Assertions.assertEquals("", lineOut.getTitle_id());
        Assertions.assertEquals("c quoi", lineOut.getEmbargo_info());
        Assertions.assertEquals("fulltext", lineOut.getCoverage_depth());
        Assertions.assertEquals("c une notes", lineOut.getNotes());
        Assertions.assertEquals("American Chemical Society", lineOut.getPublisher_name());
        Assertions.assertEquals("serial", lineOut.getPublication_type());
        Assertions.assertEquals("1996-01-01", lineOut.getDate_monograph_published_print());
        Assertions.assertEquals("1996-01-13", lineOut.getDate_monograph_published_online());
        Assertions.assertEquals("5", lineOut.getMonograph_volume());
        Assertions.assertEquals("c monograph_edition", lineOut.getMonograph_edition());
        Assertions.assertEquals("c first_editor", lineOut.getFirst_editor());
        Assertions.assertEquals("jchsc2", lineOut.getParent_publication_title_id());
        Assertions.assertEquals("jchsc3", lineOut.getPreceding_publication_title_id());
        Assertions.assertEquals("P", lineOut.getAccess_type());
        Assertions.assertNull(lineOut.getBestPpn());
    }

//    @Test
//    void testConstructionDTONumisKO() throws IllegalFileFormatException, IllegalDateException {
//        String[] lineInput = {
//                "Le titre de publication",
//                "0001-4842",
//                "1520-4898",
//                "1996-01-10",
//                "",
//                "2",
//                "1997-01",
//                "3",
//                "4F",
//                "https://pubs.acs.org/loi/achre4",
//                "SamQ",
//                "achre4",
//                "c quoi",
//                "fulltext",
//                "c une notes",
//                "American Chemical Society",
//                "serial",
//                "1996",
//                "1996-01-13",
//                "5",
//                "c monograph_edition",
//                "c first_editor",
//                "jchsc2",
//                "jchsc3",
//                "P"
//        };
//        Assertions.assertThrows(IllegalFileFormatException.class, () -> fileService.constructDto(lineInput,1,1,false));
//    }

    @Test
    void testConstructionDTONumisOK() throws IllegalFileFormatException, IllegalDateException {
        String[] lineInput = {
                "Le titre de publication",
                "0001-4842",
                "1520-4898",
                "1996-01-10",
                "",
                "2",
                "1997-01",
                "3",
                "4F",
                "https://pubs.acs.org/loi/achre4",
                "SamQ",
                "achre4",
                "c quoi",
                "fulltext",
                "c une notes",
                "American Chemical Society",
                "serial",
                "1996",
                "1996-01-13",
                "5",
                "c monograph_edition",
                "c first_editor",
                "jchsc2",
                "jchsc3",
                "P"
        };
        LigneKbartDto lineOut = fileService.constructDto(lineInput,1,1,true);
        Assertions.assertEquals(1, lineOut.getNbLinesTotal());
        Assertions.assertEquals(0, lineOut.getNbCurrentLines());
        Assertions.assertEquals("Le titre de publication", lineOut.getPublication_title());
        Assertions.assertEquals("0001-4842", lineOut.getPrint_identifier());
        Assertions.assertEquals("1520-4898", lineOut.getOnline_identifier());
        Assertions.assertEquals("1996-01-10", lineOut.getDate_first_issue_online());
        Assertions.assertEquals("",lineOut.getNum_first_vol_online());
        Assertions.assertEquals("2", lineOut.getNum_first_issue_online());
        Assertions.assertEquals("1997-01-01", lineOut.getDate_last_issue_online());
        Assertions.assertEquals("3", lineOut.getNum_last_vol_online());
        Assertions.assertEquals("4F", lineOut.getNum_last_issue_online());
        Assertions.assertEquals("https://pubs.acs.org/loi/achre4", lineOut.getTitle_url());
        Assertions.assertEquals("SamQ", lineOut.getFirst_author());
        Assertions.assertEquals("achre4", lineOut.getTitle_id());
        Assertions.assertEquals("c quoi", lineOut.getEmbargo_info());
        Assertions.assertEquals("fulltext", lineOut.getCoverage_depth());
        Assertions.assertEquals("c une notes", lineOut.getNotes());
        Assertions.assertEquals("American Chemical Society", lineOut.getPublisher_name());
        Assertions.assertEquals("serial", lineOut.getPublication_type());
        Assertions.assertEquals("1996-01-01", lineOut.getDate_monograph_published_print());
        Assertions.assertEquals("1996-01-13", lineOut.getDate_monograph_published_online());
        Assertions.assertEquals("5", lineOut.getMonograph_volume());
        Assertions.assertEquals("c monograph_edition", lineOut.getMonograph_edition());
        Assertions.assertEquals("c first_editor", lineOut.getFirst_editor());
        Assertions.assertEquals("jchsc2", lineOut.getParent_publication_title_id());
        Assertions.assertEquals("jchsc3", lineOut.getPreceding_publication_title_id());
        Assertions.assertEquals("P", lineOut.getAccess_type());
        Assertions.assertNull(lineOut.getBestPpn());
    }

//    @Test
//    void testConstructionDTOAssertTypeisKO() throws IllegalFileFormatException, IllegalDateException {
//        String[] lineInput = {
//                "Le titre de publication",
//                "0001-4842",
//                "1520-4898",
//                "1996-01-10",
//                "",
//                "2",
//                "1997-01",
//                "3",
//                "4F",
//                "https://pubs.acs.org/loi/achre4",
//                "SamQ",
//                "achre4",
//                "c quoi",
//                "fulltext",
//                "c une notes",
//                "American Chemical Society",
//                "serial",
//                "1996",
//                "1996-01-13",
//                "5",
//                "c monograph_edition",
//                "c first_editor",
//                "jchsc2",
//                "jchsc3",
//                ""
//        };
//        Assertions.assertThrows(IllegalFileFormatException.class, () -> fileService.constructDto(lineInput,1,1,false));
//    }


//    @Test
//    void testConstructionDTOisPrintIdAndOnlineIdisKO() throws IllegalFileFormatException, IllegalDateException {
//        String[] lineInput = {
//                "Le titre de publication",
//                "0001-4842",
//                "0001-4842",
//                "1996-01-10",
//                "",
//                "2",
//                "1997-01",
//                "3",
//                "4",
//                "https://pubs.acs.org/loi/achre4",
//                "SamQ",
//                "achre4",
//                "c quoi",
//                "fulltext",
//                "c une notes",
//                "American Chemical Society",
//                "serial",
//                "1996",
//                "1996-01-13",
//                "5",
//                "c monograph_edition",
//                "c first_editor",
//                "jchsc2",
//                "jchsc3",
//                "P"
//        };
//        Assertions.assertThrows(IllegalFileFormatException.class, () -> fileService.constructDto(lineInput,1,1,false));
//    }

    @Test
    void testConstructionDTOisPrintIdAndOnlineIdisOK() throws IllegalFileFormatException, IllegalDateException {
        String[] lineInput = {
                "Le titre de publication",
                "0001-4842",
                "0001-4842",
                "1996-01-10",
                "",
                "2",
                "1997-01",
                "3",
                "4",
                "https://pubs.acs.org/loi/achre4",
                "SamQ",
                "achre4",
                "c quoi",
                "fulltext",
                "c une notes",
                "American Chemical Society",
                "serial",
                "1996",
                "1996-01-13",
                "5",
                "c monograph_edition",
                "c first_editor",
                "jchsc2",
                "jchsc3",
                "P"
        };
        LigneKbartDto lineOut = fileService.constructDto(lineInput,1,1,true);
        Assertions.assertEquals(1, lineOut.getNbLinesTotal());
        Assertions.assertEquals(0, lineOut.getNbCurrentLines());
        Assertions.assertEquals("Le titre de publication", lineOut.getPublication_title());
        Assertions.assertEquals("0001-4842", lineOut.getPrint_identifier());
        Assertions.assertEquals("0001-4842", lineOut.getOnline_identifier());
        Assertions.assertEquals("1996-01-10", lineOut.getDate_first_issue_online());
        Assertions.assertEquals("",lineOut.getNum_first_vol_online());
        Assertions.assertEquals("2", lineOut.getNum_first_issue_online());
        Assertions.assertEquals("1997-01-01", lineOut.getDate_last_issue_online());
        Assertions.assertEquals("3", lineOut.getNum_last_vol_online());
        Assertions.assertEquals("4", lineOut.getNum_last_issue_online());
        Assertions.assertEquals("https://pubs.acs.org/loi/achre4", lineOut.getTitle_url());
        Assertions.assertEquals("SamQ", lineOut.getFirst_author());
        Assertions.assertEquals("achre4", lineOut.getTitle_id());
        Assertions.assertEquals("c quoi", lineOut.getEmbargo_info());
        Assertions.assertEquals("fulltext", lineOut.getCoverage_depth());
        Assertions.assertEquals("c une notes", lineOut.getNotes());
        Assertions.assertEquals("American Chemical Society", lineOut.getPublisher_name());
        Assertions.assertEquals("serial", lineOut.getPublication_type());
        Assertions.assertEquals("1996-01-01", lineOut.getDate_monograph_published_print());
        Assertions.assertEquals("1996-01-13", lineOut.getDate_monograph_published_online());
        Assertions.assertEquals("5", lineOut.getMonograph_volume());
        Assertions.assertEquals("c monograph_edition", lineOut.getMonograph_edition());
        Assertions.assertEquals("c first_editor", lineOut.getFirst_editor());
        Assertions.assertEquals("jchsc2", lineOut.getParent_publication_title_id());
        Assertions.assertEquals("jchsc3", lineOut.getPreceding_publication_title_id());
        Assertions.assertEquals("P", lineOut.getAccess_type());
        Assertions.assertNull(lineOut.getBestPpn());
    }

    @Test
    void testCheckFile(){
        File file = new File("src/test/resources/fichierTest.tsv");
        String result = fileService.checkFile(file);
        Assertions.assertEquals(
                "Nb_line ; Message d'erreur<br/>" +
                "1 ; La valeur de COVERAGE_DEPTH est invalide<br/>" +
                "1 ; La Date DATE_MONOGRAPH_PUBLISHED_PRINT est mal formatées (ex. années invalides : « 21546445-01-01 »<br/>" +
                "2 ; La valeur de COVERAGE_DEPTH est invalide<br/>" +
                "2 ; La Date DATE_FIRST_ISSUE_ONLINE est mal formatées (ex. années invalides : « 5500-01-01 »<br/>" +
                "2 ; La Date DATE_MONOGRAPH_PUBLISHED_PRINT est mal formatées (ex. années invalides : « 21546445-01-01 »<br/>" +
                "5 ; Les champs PRINT_IDENTIFIER et ONLINE_IDENTIFIER sont identiques<br/>" +
                "7 ; Les champs PRINT_IDENTIFIER et ONLINE_IDENTIFIER sont identiques<br/>" +
                "7 ; La valeur de NUM_LAST_ISSUE_ONLINE n'est pas un nombre<br/>" +
                "7 ; La valeur de COVERAGE_DEPTH est invalide<br/>" +
                "7 ; La valeur de ACCESS_TYPE est invalide. (valeurs acceptées : P, F)<br/>" +
                "7 ; La valeur de PUBLICATION_TYPE est invalide. (valeurs acceptées : monograph, serial)<br/>",
                result);
    }
}
