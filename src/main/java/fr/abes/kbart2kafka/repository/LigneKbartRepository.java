package fr.abes.kbart2kafka.repository;

import fr.abes.kbart2kafka.dto.LigneKbartDto;
import fr.abes.kbart2kafka.entity.LigneKbart;
import fr.abes.kbart2kafka.entity.ProviderPackage;
import fr.abes.kbart2kafka.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class LigneKbartRepository {
    private final DataSource baconDataSource;

    public LigneKbartRepository(DataSource baconDataSource) {
        this.baconDataSource = baconDataSource;
    }

    public List<LigneKbartDto> findAllByProviderPackage(Integer idProviderPackage) {
        List<LigneKbartDto> result = new ArrayList<>();

        String query = "select" +
                "IDT_LIGNE_KBART," +
                "ACCESS_TYPE," +
                "BEST_PPN," +
                "COVERAGE_DEPTH," +
                "DATE_FIRST_ISSUE_ONLINE," +
                "DATE_LAST_ISSUE_ONLINE," +
                "DATE_MONOGRAPH_PUBLISHED_ONLIN," +
                "DATE_MONOGRAPH_PUBLISHED_PRINT," +
                "EMBARGO_INFO," +
                "FIRST_AUTHOR," +
                "FIRST_EDITOR," +
                "MONOGRAPH_EDITION," +
                "MONOGRAPH_VOLUME," +
                "NOTES," +
                "NUM_FIRST_ISSUE_ONLINE," +
                "NUM_FIRST_VOL_ONLINE," +
                "NUM_LAST_VOL_ONLINE," +
                "NUM_LAST_ISSUE_ONLINE," +
                "ONLINE_IDENTIFIER," +
                "PARENT_PUBLICATION_TITLE_ID," +
                "PRECEDING_PUBLICATION_TITLE_ID," +
                "PRINT_IDENTIFIER," +
                "ID_PROVIDER_PACKAGE," +
                "PUBLICATION_TITLE," +
                "PUBLICATION_TYPE," +
                "PUBLISHER_NAME," +
                "TITLE_ID," +
                "TITLE_URL " +
                "from" +
                "LIGNE_KBART" +
                "where " +
                "lk1_0.ID_PROVIDER_PACKAGE=" + idProviderPackage;
        try (
                Connection connection = baconDataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(query);
        ) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LigneKbartDto ligne = new LigneKbartDto();
                ligne.setAccess_type(rs.getString("ACCESS_TYPE"));
                ligne.setBestPpn(rs.getString("BEST_PPN"));
                ligne.setCoverage_depth(rs.getString("COVERAGE_DEPTH"));
                ligne.setPublication_title(rs.getString("PUBLICATION_TITLE"));
                ligne.setPrint_identifier(rs.getString("PRINT_IDENTIFIER"));
                ligne.setOnline_identifier(rs.getString("ONLINE_IDENTIFIER"));
                ligne.setDate_first_issue_online(rs.getString("DATE_FIRST_ISSUE_ONLINE"));
                ligne.setNum_first_vol_online(rs.getString("NUM_FIRST_VOL_ONLINE"));
                ligne.setNum_first_issue_online(rs.getString("NUM_FIRST_ISSUE_ONLINE"));
                ligne.setDate_last_issue_online(rs.getString("DATE_LAST_ISSUE_ONLINE"));
                ligne.setNum_last_vol_online(rs.getString("NUM_LAST_VOL_ONLINE"));
                ligne.setNum_last_issue_online(rs.getString("NUM_LAST_ISSUE_ONLINE"));
                ligne.setTitle_url(rs.getString("TITLE_URL"));
                ligne.setFirst_author(rs.getString("FIRST_AUTHOR"));
                ligne.setTitle_id(rs.getString("TITLE_ID"));
                ligne.setEmbargo_info(rs.getString("EMBARGO_INFO"));
                ligne.setCoverage_depth(rs.getString("COVERAGE_DEPTH"));
                ligne.setNotes(rs.getString("NOTES"));
                ligne.setPublisher_name(rs.getString("PUBLISHER_NAME"));
                ligne.setPublication_type(rs.getString("PUBLICATION_TYPE"));
                ligne.setDate_monograph_published_print(rs.getString("DATE_MONOGRAPH_PUBLISHED_PRINT"));
                ligne.setDate_monograph_published_online(rs.getString("DATE_MONOGRAPH_PUBLISHED_ONLIN"));
                ligne.setMonograph_volume(rs.getString("MONOGRAPH_VOLUME"));
                ligne.setMonograph_edition(rs.getString("MONOGRAPH_EDITION"));
                ligne.setFirst_editor(rs.getString("FIRST_EDITOR"));
                ligne.setParent_publication_title_id(rs.getString("PARENT_PUBLICATION_TITLE_ID"));
                ligne.setPreceding_publication_title_id(rs.getString("PRECEDING_PUBLICATION_TITLE_ID"));
                result.add(ligne);
            }
        }  catch (SQLException e) {
            log.error(e.getMessage());
        }
        return result;
    }
}
