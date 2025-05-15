package fr.abes.kbart2kafka.utils;

import fr.abes.kbart2kafka.exception.IllegalDateException;
import fr.abes.kbart2kafka.exception.IllegalPackageException;
import fr.abes.kbart2kafka.exception.IllegalProviderException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

class UtilsTest {
    @Test
    void extractDomainFromUrlTest1() throws URISyntaxException {
        String url = "https://www.doi.org/test";
        Assertions.assertEquals("www.doi.org", Utils.extractDomainFromUrl(url));
    }

    @Test
    void extractDomainFromUrlTest2() throws URISyntaxException {
        String url = "http://www.doi.org/test";
        Assertions.assertEquals("www.doi.org", Utils.extractDomainFromUrl(url));
    }

    @Test
    void extractDomainFromUrlTest3() {
        String url = "teskljgfklj/test";
        Assertions.assertThrows(URISyntaxException.class, () -> Utils.extractDomainFromUrl(url));
    }

    @Test
    void extractDate() throws IllegalDateException, ParseException {
        String string = "2023-08-21";
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(string);

        Assertions.assertEquals(date, Utils.extractDateFilename("SPRINGER_GLOBAL_ALLEBOOKS_2023-08-21.tsv"));
    }

    @Test
    @DisplayName("test récupération package dans nom de fichier")
    void testextractPackageName() throws IllegalPackageException {
        String filename = "SPRINGER_GLOBAL_ALLEBOOKS_2023-05-01_FORCE.tsv";
        Assertions.assertEquals("GLOBAL_ALLEBOOKS", Utils.extractPackageName(filename));

        filename = "SPRINGER_GLOBAL_ALLEBOOKS_2023-05-01.tsv";
        Assertions.assertEquals("GLOBAL_ALLEBOOKS", Utils.extractPackageName(filename));

    }

    @Test
    @DisplayName("test récupération provider dans nom de fichier")
    void testExtractProvider() throws IllegalProviderException {
        String filename = "SPRINGER_GLOBAL_ALLEBOOKS_2023-05-01_FORCE.tsv";
        Assertions.assertEquals("SPRINGER", Utils.extractProvider(filename));

        String filename2 = "test";
        Assertions.assertThrows(IllegalProviderException.class, () -> Utils.extractProvider(filename2));
    }

    @Test
    @DisplayName("test extraction nom de fichier")
    void extractFilename() {
        String sep = FileSystems.getDefault().getSeparator();
        String path = "SPRINGER_GLOBAL_ALLEBOOKS_2023-05-01_FORCE.tsv";
        Assertions.assertEquals("SPRINGER_GLOBAL_ALLEBOOKS_2023-05-01_FORCE.tsv", Utils.extractFilename(path));

        path = sep + "app" + sep + "local" + sep + "SPRINGER_GLOBAL_ALLEBOOKS_2023-05-01_FORCE.tsv";
        Assertions.assertEquals("SPRINGER_GLOBAL_ALLEBOOKS_2023-05-01_FORCE.tsv", Utils.extractFilename(path));
    }

    @Test
    @DisplayName("Test reformatage date")
    void reformatDate() throws IllegalDateException {
        String date  = "";
        Assertions.assertEquals("", Utils.reformatDateKbart(date));
        date = "2024-03-03";
        Assertions.assertEquals("2024-03-03", Utils.reformatDateKbart(date));
        date = "2024-03";
        Assertions.assertEquals("2024-03-01", Utils.reformatDateKbart(date));
        date = "2024";
        Assertions.assertEquals("2024-01-01", Utils.reformatDateKbart(date));

        date = "jklsdfhlksjf";
        String finalDate = date;
        Assertions.assertThrows(IllegalDateException.class, () -> Utils.reformatDateKbart(finalDate));

//        date = "2024-02-30"; // 30 fev existe pas
//        String finalDate1 = date;
//        Assertions.assertThrows(IllegalDateException.class, () -> Utils.reformatDateKbart(finalDate1));

//        date = "202";
//        String finalDate2 = date;
//        Assertions.assertThrows(IllegalDateException.class, () -> Utils.reformatDateKbart(finalDate2));
    }

    @Test
    @DisplayName("Test before date")
    void beforeDate() {
        String date  = "2024-03-03";
        String otherDate = "2025-03-03";
        Assertions.assertTrue(Utils.isDateBeforeOtherDate(date, otherDate));
        Assertions.assertFalse(Utils.isDateBeforeOtherDate(otherDate, date));
        Assertions.assertFalse(Utils.isDateBeforeOtherDate(date, date));
    }


}
