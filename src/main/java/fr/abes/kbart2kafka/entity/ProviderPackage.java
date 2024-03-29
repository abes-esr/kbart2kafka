package fr.abes.kbart2kafka.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "PROVIDER_PACKAGE")
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProviderPackage implements Serializable, Comparable<ProviderPackage> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_PROVIDER_PACKAGE")
    private Integer idProviderPackage;
    @Column(name = "PACKAGE")
    private String packageName;
    @Column(name = "DATE_P")
    private Date dateP;
    @Column(name = "PROVIDER_IDT_PROVIDER")
    private Integer providerIdtProvider;
    @Column(name = "LABEL_ABES")
    private char labelAbes;

    @ManyToOne
    @JoinColumn(referencedColumnName = "IDT_PROVIDER", insertable = false, updatable = false)
    private Provider provider;

    public ProviderPackage(String packageName, Date dateP, Integer providerIdtProvider, char labelAbes) {
        this.packageName = packageName;
        this.dateP = dateP;
        this.providerIdtProvider = providerIdtProvider;
        this.labelAbes = labelAbes;
    }

    public ProviderPackage(Integer idProviderPackage, String packageName, Date dateP, Integer providerIdtProvider, char labelAbes) {
        this.idProviderPackage = idProviderPackage;
        this.packageName = packageName;
        this.dateP = dateP;
        this.providerIdtProvider = providerIdtProvider;
        this.labelAbes = labelAbes;
    }

    @Override
    public String toString() {
        return "{ id:"+idProviderPackage + ", packageName:"+packageName+", providerIdt:"+providerIdtProvider+" dateP:"+dateP+" }";
    }

    @Override
    public int compareTo(ProviderPackage o) {
        return o.getDateP().compareTo(this.getDateP());
    }
}
