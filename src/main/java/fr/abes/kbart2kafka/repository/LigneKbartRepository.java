package fr.abes.kbart2kafka.repository;

import fr.abes.kbart2kafka.entity.LigneKbart;
import fr.abes.kbart2kafka.entity.ProviderPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LigneKbartRepository extends JpaRepository<LigneKbart, Integer> {
    List<LigneKbart> findAllByProviderPackage(ProviderPackage providerPackage);
}
