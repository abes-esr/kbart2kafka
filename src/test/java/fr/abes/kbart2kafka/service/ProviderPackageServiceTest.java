package fr.abes.kbart2kafka.service;

import fr.abes.kbart2kafka.entity.Provider;
import fr.abes.kbart2kafka.entity.ProviderPackage;
import fr.abes.kbart2kafka.repository.LigneKbartRepository;
import fr.abes.kbart2kafka.repository.ProviderPackageRepository;
import fr.abes.kbart2kafka.repository.ProviderRepository;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@SpringBootTest(classes = {ProviderPackageService.class})
class ProviderPackageServiceTest {
    @Autowired
    ProviderPackageService service;

    @MockitoBean
    ProviderPackageRepository providerPackageRepository;

    @MockitoBean
    ProviderRepository providerRepository;

    @MockitoBean
    LigneKbartRepository ligneKbartRepository;

    @Test
    @DisplayName("test isMoreRecentPackage : cas général")
    void isMoreRecentPackageInBdd() {
        Provider provider = new Provider(1, "provider", "nomContact", "prenomContact", "mail@contact.com", "displayName");
        Mockito.when(providerRepository.findByProvider("provider")).thenReturn(Optional.of(provider));
        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, Calendar.NOVEMBER, 1);
        Date date1 = calendar.getTime();
        ProviderPackage package1 = new ProviderPackage(1, "package", date1, 1, 'Y');
        calendar.set(2023, Calendar.SEPTEMBER, 10);
        Date date2 = calendar.getTime();
        ProviderPackage package2 = new ProviderPackage(2, "package", date2, 1, 'Y');
        calendar.set(2023, Calendar.AUGUST, 5);
        Date date3 = calendar.getTime();
        ProviderPackage package3 = new ProviderPackage(3, "package", date3, 1, 'Y');

        List<ProviderPackage> packageList = Lists.newArrayList(package1, package2, package3);
        Mockito.when(providerPackageRepository.findByPackageNameAndProviderIdtProvider("package", 1)).thenReturn(packageList);

        calendar.set(2023, Calendar.MAY, 20);
        Date inputDate = calendar.getTime();
        Assertions.assertTrue(service.hasMoreRecentPackageInBdd("provider", "package", inputDate));


        calendar.set(2023, Calendar.DECEMBER, 20);
        inputDate = calendar.getTime();
        Assertions.assertFalse(service.hasMoreRecentPackageInBdd("provider", "package", inputDate));

        //cas date égale
        calendar.set(2023, Calendar.SEPTEMBER, 10);
        inputDate = calendar.getTime();
        Assertions.assertFalse(service.hasMoreRecentPackageInBdd("provider", "package", inputDate));
    }

    @Test
    @DisplayName("test isMoreRecentPacakge : cas pas de provider trouvé")
    void isMoreRecentPacakageInBddNoProvider() {
        Mockito.when(providerRepository.findByProvider("provider")).thenReturn(Optional.empty());
        Assertions.assertFalse(service.hasMoreRecentPackageInBdd("provider", "package", Calendar.getInstance().getTime()));
    }
}