package fr.abes.kbart2kafka.controller;

import fr.abes.kbart2kafka.service.FileService;
import fr.abes.kbart2kafka.service.ProviderPackageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class KbartControllerTest {

    @TempDir
    Path tempDirectory;

    @Test
    void telechargeUnRapportDepuisLeSousDossierBad() throws IOException {
        String filename = "TEST_GLOBAL_ALLTITLES_2026-07-21_400.bad";
        byte[] expectedContent = "LINE\tMESSAGE\n2\tErreur de validation\n"
                .getBytes(StandardCharsets.UTF_8);
        Path badDirectory = Files.createDirectories(tempDirectory.resolve("bad"));
        Files.write(badDirectory.resolve(filename), expectedContent);

        KbartController controller = new KbartController(
                mock(FileService.class),
                mock(ProviderPackageService.class));
        ReflectionTestUtils.setField(
                controller,
                "pathToKbart",
                tempDirectory.toString() + System.getProperty("file.separator"));

        ResponseEntity<?> response = controller.getFile("bad", filename);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        InputStreamResource resource = assertInstanceOf(
                InputStreamResource.class,
                response.getBody());
        try (var inputStream = resource.getInputStream()) {
            assertArrayEquals(expectedContent, inputStream.readAllBytes());
        }
    }
}
