package fr.abes.kbart2kafka.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class LigneKbartDtoTest {

    @Test
    void toStringTest() {
        LigneKbartDto ligne = new LigneKbartDto();
        ligne.setOnline_identifier("");
        ligne.setPrint_identifier("");
        ligne.setPublication_title("test");
        ligne.setPublication_type("monograph");

        Assertions.assertEquals("publication title : test / publication_type : monograph", ligne.toString());

        ligne.setOnline_identifier("11111111");
        Assertions.assertEquals("publication title : test / publication_type : monograph / online_identifier : 11111111", ligne.toString());

        ligne.setPrint_identifier("987123456789");
        Assertions.assertEquals("publication title : test / publication_type : monograph / online_identifier : 11111111 / print_identifier : 987123456789", ligne.toString());

        ligne.setOnline_identifier("");
        Assertions.assertEquals("publication title : test / publication_type : monograph / print_identifier : 987123456789", ligne.toString());
    }

    @Test
    @DisplayName("Test de format de date")
    void toHashMapTestDateFormat(){
        LigneKbartDto ligne1 = new LigneKbartDto();
        ligne1.setDate_monograph_published_print("2024-03-03");

        LigneKbartDto ligne2 = new LigneKbartDto();
        ligne2.setDate_monograph_published_print("2024-03-03 00:00:00");

        LigneKbartDto ligne3 = new LigneKbartDto();
        ligne3.setDate_monograph_published_print("2024");

        LigneKbartDto ligne4 = new LigneKbartDto();
        ligne4.setDate_monograph_published_print("2024-01-01");

        LigneKbartDto ligne5 = new LigneKbartDto();
        ligne5.setDate_monograph_published_print("2025");

        Assertions.assertEquals(ligne1.toHash(), ligne2.toHash());

        Assertions.assertNotEquals(ligne1.toHash(), ligne4.toHash());

        Assertions.assertNotEquals(ligne3.toHash(), ligne4.toHash());

        Assertions.assertNotEquals(ligne3.toHash(), ligne5.toHash());

    }

}
