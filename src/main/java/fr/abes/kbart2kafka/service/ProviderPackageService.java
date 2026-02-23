package fr.abes.kbart2kafka.service;

import fr.abes.kbart2kafka.dto.LigneKbartDto;
import fr.abes.kbart2kafka.entity.LigneKbart;
import fr.abes.kbart2kafka.entity.Provider;
import fr.abes.kbart2kafka.entity.ProviderPackage;
import fr.abes.kbart2kafka.repository.LigneKbartRepository;
import fr.abes.kbart2kafka.repository.ProviderPackageRepository;
import fr.abes.kbart2kafka.repository.ProviderRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class ProviderPackageService {
    private final ProviderPackageRepository repository;

    private final ProviderRepository providerRepository;
    private final LigneKbartRepository ligneKbartRepository;

    public ProviderPackageService(ProviderPackageRepository repository, ProviderRepository providerRepository, LigneKbartRepository ligneKbartRepository) {
        this.repository = repository;
        this.providerRepository = providerRepository;
        this.ligneKbartRepository = ligneKbartRepository;
    }

    public boolean hasMoreRecentPackageInBdd(String provider, String packageName, Date datePackage) {
        Optional<Provider> providerBdd = providerRepository.findByProvider(provider);
        if (providerBdd.isPresent()) {
            List<ProviderPackage> packageList = repository.findByPackageNameAndProviderIdtProvider(packageName, providerBdd.get().getIdtProvider());
            Collections.sort(packageList);
            if (!packageList.isEmpty())
                return packageList.getFirst().getDateP().after(datePackage);
        }
        return false;
    }

    public ProviderPackage getLastProviderPackage(String provider, String packageName) {
        Optional<Provider> providerBdd = providerRepository.findByProvider(provider);
        if(providerBdd.isPresent()) {
            List<ProviderPackage> packageList = repository.findByPackageNameAndProviderIdtProvider(packageName, providerBdd.get().getIdtProvider());
            Collections.sort(packageList);
            if (!packageList.isEmpty())
                return packageList.getFirst();
        }
        return null;
    }

    public List<LigneKbartDto> getLigneKbartByProviderPackage(ProviderPackage providerPackage){
        return ligneKbartRepository.findAllByProviderPackage(providerPackage.getIdProviderPackage());
    }
}
