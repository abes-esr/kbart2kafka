package fr.abes.kbart2kafka.utils;

import fr.abes.kbart2kafka.exception.IllegalFileFormatException;
import fr.abes.kbart2kafka.exception.IllegalProviderException;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class CheckFilesTest {

    File file;
    File file0;
    File file1;
    File file2;
    File file3;

    @AfterEach
    public void cleanUp() {
        if(file != null && file.delete()){file.deleteOnExit();}     // ne pas supprimer. Indispensable pour que les TU fonctionnent.
        if(file0 != null && file0.delete()){file0.deleteOnExit();}   // ne pas supprimer. Indispensable pour que les TU fonctionnent.
        if(file1 != null && file1.delete()){file1.deleteOnExit();}  // ne pas supprimer. Indispensable pour que les TU fonctionnent.
        if(file2 != null && file2.delete()){file2.deleteOnExit();}  // ne pas supprimer. Indispensable pour que les TU fonctionnent.
        if(file3 != null && file3.delete()){file3.deleteOnExit();}  // ne pas supprimer. Indispensable pour que les TU fonctionnent.
    }

    @Test
    void isFileWithTSVExtension() throws IllegalFileFormatException {
        this.file = new File("test.tsv");
        CheckFiles.isFileWithTSVExtension(file);

        this.file2 = new File("");
        IllegalFileFormatException erreur2 = Assertions.assertThrows(IllegalFileFormatException.class, () -> CheckFiles.isFileWithTSVExtension(file2));
        Assertions.assertEquals("Le nom du fichier est vide", erreur2.getMessage());

        this.file3 = new File("test2.csv");
        IllegalFileFormatException erreur3 = Assertions.assertThrows(IllegalFileFormatException.class, () -> CheckFiles.isFileWithTSVExtension(file3));
        Assertions.assertEquals("le fichier n'est pas au format tsv", erreur3.getMessage());
    }

    @Test
    void detectFileName() throws IllegalFileFormatException {
        this.file = new File("test_test_test_1234-12-12.tsv");
        CheckFiles.detectFileNameAndReturnIsBypass(file);

        this.file2 = new File("test_test_test_1234-12-12_FORCE.tsv");
        CheckFiles.detectFileNameAndReturnIsBypass(file2);

        for(String name : Lists.newArrayList("123", "test_1234-12-12.tsv", "test_test_134-12-12.tsv", "test_test_1344-12-12.tsvf", "test_test_1344-12-123.tsv", "test_test_test_test1_1234-12-12_force.tsv")) {
            this.file3 = new File(name);
            IllegalFileFormatException erreur2 = Assertions.assertThrows(IllegalFileFormatException.class, () -> CheckFiles.detectFileNameAndReturnIsBypass(file3));
            Assertions.assertEquals("Le nom du fichier " + name + " n'est pas correct", erreur2.getMessage());
        }
    }

    @Test
    void detectProvider() throws IllegalFileFormatException {
        this.file = new File("test_test_test_1234-12-12.tsv");
        CheckFiles.detectFileNameAndReturnIsBypass(file);

        this.file2 = new File("test_test_test_1234-12-12_FORCE.tsv");
        CheckFiles.detectFileNameAndReturnIsBypass(file2);

        for(String name : Lists.newArrayList("123.tsv", "_test.tsv")) {
            this.file3 = new File(name);
            IllegalProviderException erreur2 = Assertions.assertThrows(IllegalProviderException.class, () -> CheckFiles.detectProvider(file3));
            Assertions.assertEquals("Le nom du fichier " + name + " ne contient pas de provider", erreur2.getMessage());
        }
    }

    @Test
    void detectTabulations() throws IOException, IllegalFileFormatException {
        this.file = new File("test.tsv");
        FileUtils.writeStringToFile(file, "test\ttest\ttest", StandardCharsets.UTF_8, true);
        CheckFiles.detectTabulations(file);

        this.file2 = new File("test2.tsv");
        FileUtils.writeStringToFile(file2, "test;test;test", StandardCharsets.UTF_8, true);
        IllegalFileFormatException erreur = Assertions.assertThrows(IllegalFileFormatException.class, () -> CheckFiles.detectTabulations(file2));
        Assertions.assertEquals("Le fichier ne contient pas de tabulation", erreur.getMessage());
    }

    @Test
    void detectOfHeaderPresence() throws IOException, IllegalFileFormatException {
        String header = "testA\ttestB\ttestC\ttestD\ttestE\ttestF\ttestG\ttestH\ttestI\ttestJ\ttestK\ttestL\ttestM\ttestN\ttestO\ttestP\ttestQ\ttestR\ttestS\ttestT\ttestU\ttestV\ttestW\ttestX\ttestY";

        // header à 25 colonnes et pas de _BYPASS
        this.file = new File("test.tsv");
        FileUtils.writeStringToFile(file, "testA\ttestB\ttestC\ttestD\ttestE\ttestF\ttestG\ttestH\ttestI\ttestJ\ttestK\ttestL\ttestM\ttestN\ttestO\ttestP\ttestQ\ttestR\ttestS\ttestT\ttestU\ttestV\ttestW\ttestX\ttestY", StandardCharsets.UTF_8, true);
        CheckFiles.detectHeaderPresence(header, file, false);

        // header à 25 colonnes et _BYPASS
        this.file0 = new File("test0.tsv");
        FileUtils.writeStringToFile(this.file0, "testA\ttestB\ttestC\ttestD\ttestE\ttestF\ttestG\ttestH\ttestI\ttestJ\ttestK\ttestL\ttestM\ttestN\ttestO\ttestP\ttestQ\ttestR\ttestS\ttestT\ttestU\ttestV\ttestW\ttestX\ttestY", StandardCharsets.UTF_8, true);
        CheckFiles.detectHeaderPresence(header, this.file0, true);
        this.file0.deleteOnExit();

        // header à 26 colonnes dont best_ppn et pas de _BYPASS
        this.file1 = new File("testByPass.tsv");
        FileUtils.writeStringToFile(this.file1, "testA\ttestB\ttestC\ttestD\ttestE\ttestF\ttestG\ttestH\ttestI\ttestJ\ttestK\ttestL\ttestM\ttestN\ttestO\ttestP\ttestQ\ttestR\ttestS\ttestT\ttestU\ttestV\ttestW\ttestX\ttestY\tbest_ppn", StandardCharsets.UTF_8, true);
        CheckFiles.detectHeaderPresence(header, this.file1, false);
        this.file1.deleteOnExit();

        // header à 26 colonnes dont best_ppn et option _BYPASS
        this.file2 = new File("test2.tsv");
        FileUtils.writeStringToFile(file2, "testA\ttestB\ttestC\ttestD\ttestE\ttestF\ttestG\ttestH\ttestI\ttestJ\ttestK\ttestL\ttestM\ttestN\ttestO\ttestP\ttestQ\ttestR\ttestS\ttestT\ttestU\ttestV\ttestW\ttestX\ttestY\tbest_ppn", StandardCharsets.UTF_8, true);
        IllegalFileFormatException erreur2 = Assertions.assertThrows(IllegalFileFormatException.class, () -> CheckFiles.detectHeaderPresence(header, file2, true));
        Assertions.assertEquals("Format du fichier incorrect. L'option _BYPASS n'est pas compatible avec la présence d'une colonne best_pnn", erreur2.getMessage());

        // header à 25 colonnes avec erreur de nom de colonne
        this.file3 = new File("test3.tsv");
        FileUtils.writeStringToFile(file3, "testAvecUneErreur\ttestB\ttestC\ttestD\ttestE\ttestF\ttestG\ttestH\ttestI\ttestJ\ttestK\ttestL\ttestM\ttestN\ttestO\ttestP\ttestQ\ttestR\ttestS\ttestT\ttestU\ttestV\ttestW\ttestX\ttestY", StandardCharsets.UTF_8, true);
        IllegalFileFormatException erreur3 = Assertions.assertThrows(IllegalFileFormatException.class, () -> CheckFiles.detectHeaderPresence(header, file3, false));
        Assertions.assertEquals("Format du fichier incorrect. L’en tête devrait être comme ceci : testA\ttestB\ttestC\ttestD\ttestE\ttestF\ttestG\ttestH\ttestI\ttestJ\ttestK\ttestL\ttestM\ttestN\ttestO\ttestP\ttestQ\ttestR\ttestS\ttestT\ttestU\ttestV\ttestW\ttestX\ttestY et best_ppn", erreur3.getMessage());

        // header à 26 colonnes avec erreur de nom de colonne
        File file4 = new File("test4.tsv");
        FileUtils.writeStringToFile(file4, "testA\ttestB\ttestC\ttestD\ttestE\ttestF\ttestG\ttestH\ttestI\ttestJ\ttestK\ttestL\ttestM\ttestN\ttestO\ttestP\ttestQ\ttestR\ttestS\ttestT\ttestU\ttestV\ttestW\ttestX\ttestY\ttestZ", StandardCharsets.UTF_8, true);
        IllegalFileFormatException erreur4 = Assertions.assertThrows(IllegalFileFormatException.class, () -> CheckFiles.detectHeaderPresence(header, file4, false));
        Assertions.assertEquals("Format du fichier incorrect. L’en tête devrait être comme ceci : testA\ttestB\ttestC\ttestD\ttestE\ttestF\ttestG\ttestH\ttestI\ttestJ\ttestK\ttestL\ttestM\ttestN\ttestO\ttestP\ttestQ\ttestR\ttestS\ttestT\ttestU\ttestV\ttestW\ttestX\ttestY et best_ppn", erreur4.getMessage());
        file4.deleteOnExit();

        // Test avec header à 24 colonnes
        File file5 = new File("test5.tsv");
        FileUtils.writeStringToFile(file5, "testA\ttestB\ttestC\ttestD\ttestE\ttestF\ttestG\ttestH\ttestI\ttestJ\ttestK\ttestL\ttestM\ttestN\ttestO\ttestP\ttestQ\ttestR\ttestS\ttestT\ttestU\ttestV\ttestW\ttestX", StandardCharsets.UTF_8, true);
        IllegalFileFormatException erreur5 = Assertions.assertThrows(IllegalFileFormatException.class, () -> CheckFiles.detectHeaderPresence(header, file5, false));
        Assertions.assertEquals("Format du fichier incorrect. L’en tête devrait être comme ceci : testA\ttestB\ttestC\ttestD\ttestE\ttestF\ttestG\ttestH\ttestI\ttestJ\ttestK\ttestL\ttestM\ttestN\ttestO\ttestP\ttestQ\ttestR\ttestS\ttestT\ttestU\ttestV\ttestW\ttestX\ttestY et best_ppn", erreur5.getMessage());
        file5.deleteOnExit();

        // Test avec header à 27 colonnes
        File file6 = new File("test6.tsv");
        FileUtils.writeStringToFile(file6, "testA\testB\ttestC\ttestD\ttestE\ttestF\ttestG\ttestH\ttestI\ttestJ\ttestK\ttestL\ttestM\ttestN\ttestO\ttestP\ttestQ\ttestR\ttestS\ttestT\ttestU\ttestV\ttestW\ttestX\ttestY\ttestZ\ttest27colonne", StandardCharsets.UTF_8, true);
        IllegalFileFormatException erreur6 = Assertions.assertThrows(IllegalFileFormatException.class, () -> CheckFiles.detectHeaderPresence(header, file6, false));
        Assertions.assertEquals("Format du fichier incorrect. L’en tête devrait être comme ceci : testA\ttestB\ttestC\ttestD\ttestE\ttestF\ttestG\ttestH\ttestI\ttestJ\ttestK\ttestL\ttestM\ttestN\ttestO\ttestP\ttestQ\ttestR\ttestS\ttestT\ttestU\ttestV\ttestW\ttestX\ttestY et best_ppn", erreur6.getMessage());
        file6.deleteOnExit();

        // Test d'une erreur avec header à 26 colonnes dont une colonne best_ppn
        File file7 = new File("test7.tsv");
        FileUtils.writeStringToFile(file7, "testAvecErreur\ttestB\ttestC\ttestD\ttestE\ttestF\ttestG\ttestH\ttestI\ttestJ\ttestK\ttestL\ttestM\ttestN\ttestO\ttestP\ttestQ\ttestR\ttestS\ttestT\ttestU\ttestV\ttestW\ttestX\ttestY\tbest_ppn", StandardCharsets.UTF_8, true);
        IllegalFileFormatException erreur7 = Assertions.assertThrows(IllegalFileFormatException.class, () -> CheckFiles.detectHeaderPresence(header, file7, false));
        Assertions.assertEquals("Format du fichier incorrect. L’en tête devrait être comme ceci : testA\ttestB\ttestC\ttestD\ttestE\ttestF\ttestG\ttestH\ttestI\ttestJ\ttestK\ttestL\ttestM\ttestN\ttestO\ttestP\ttestQ\ttestR\ttestS\ttestT\ttestU\ttestV\ttestW\ttestX\ttestY et best_ppn", erreur7.getMessage());
        file7.deleteOnExit();
    }
    @Test
    void detectOfHeaderPresence2() throws IOException {
        String header = "testA\ttestB\ttestC\ttestD\ttestE\ttestF\ttestG\ttestH\ttestI\ttestJ\ttestK\ttestL\ttestM\ttestN\ttestO\ttestP\ttestQ\ttestR\ttestS\ttestT\ttestU\ttestV\ttestW\ttestX\ttestY";

        // Test d'une erreur avec header à 27 colonnes et best ppn
        File file7 = new File("test7.tsv");
        FileUtils.writeStringToFile(file7, "testA\ttestB\ttestC\ttestD\ttestE\ttestF\ttestG\ttestH\ttestI\ttestJ\ttestK\ttestL\ttestM\ttestN\ttestO\ttestP\ttestQ\ttestR\ttestS\ttestT\ttestU\ttestV\ttestW\ttestX\tbest_ppn", StandardCharsets.UTF_8, true);
        IllegalFileFormatException erreur7 = Assertions.assertThrows(IllegalFileFormatException.class, () -> CheckFiles.detectHeaderPresence(header, file7, false));
        Assertions.assertEquals(erreur7.getMessage(),"Format du fichier incorrect. L’en tête devrait être comme ceci : testA\ttestB\ttestC\ttestD\ttestE\ttestF\ttestG\ttestH\ttestI\ttestJ\ttestK\ttestL\ttestM\ttestN\ttestO\ttestP\ttestQ\ttestR\ttestS\ttestT\ttestU\ttestV\ttestW\ttestX\ttestY et best_ppn");
        file7.deleteOnExit();
    }

    @Test
    void testEmptyFile() throws IOException, IllegalFileFormatException {
        File file = new File("test.tsv");
        FileUtils.writeStringToFile(file, "testA\ttestB\ttestC\ttestD\ttestE\ttestF\ttestG\ttestH\ttestI\ttestJ\ttestK\ttestL\ttestM\ttestN\ttestO\ttestP\ttestQ\ttestR\ttestS\ttestT\ttestU\ttestV\ttestW\ttestX\ttestY", StandardCharsets.UTF_8, true);
        Assertions.assertThrows(IllegalFileFormatException.class, () -> CheckFiles.checkEmptyFile(file));
        FileUtils.writeStringToFile(file, "", StandardCharsets.UTF_8, true);
        Assertions.assertThrows(IllegalFileFormatException.class, () -> CheckFiles.checkEmptyFile(file));

        FileUtils.writeStringToFile(file, "testA\ttestB\ttestC\ttestD\ttestE\ttestF\ttestG\ttestH\ttestI\ttestJ\ttestK\ttestL\ttestM\ttestN\ttestO\ttestP\ttestQ\ttestR\ttestS\ttestT\ttestU\ttestV\ttestW\ttestX\ttestY\n", StandardCharsets.UTF_8, false);
        FileUtils.writeStringToFile(file, "tejlksdjmflksjfmldf", StandardCharsets.UTF_8, true);
        CheckFiles.checkEmptyFile(file);
    }

    @Test
    void detectOptionError() throws IOException {
        this.file = new File("test3_BYPASS.tsv");
        FileUtils.writeStringToFile(file, "test\ttest\ttest\tbest_ppn", StandardCharsets.UTF_8, true);
        IllegalFileFormatException erreur1 = Assertions.assertThrows(IllegalFileFormatException.class, () -> CheckFiles.detectHeaderPresence("test\ttest\ttest\tbest_ppn", file, true));
        Assertions.assertEquals("Format du fichier incorrect. L'option _BYPASS n'est pas compatible avec la présence d'une colonne best_pnn", erreur1.getMessage());

        this.file2 = new File("test3_BYPASS_FORCE.tsv");
        FileUtils.writeStringToFile(file2, "test\ttest\ttest", StandardCharsets.UTF_8, true);
        IllegalFileFormatException erreur2 = Assertions.assertThrows(IllegalFileFormatException.class, () -> CheckFiles.detectFileNameAndReturnIsBypass(file2));
        Assertions.assertEquals("Le nom du fichier test3_BYPASS_FORCE.tsv n'est pas correct", erreur2.getMessage());
    }
}
