package com.ge.predix.acs.migration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.privilege.management.dao.GraphSubjectRepository;
import com.ge.predix.acs.privilege.management.dao.SubjectRepository;

@Profile("titan")
@Component
public class SubjectMigrationManager {

    @Autowired(required = false)
    private GraphSubjectRepository graphRepository;

    @Autowired
    @Qualifier("subjectRepository")
    private SubjectRepository nonGraphRepository;

    public void migrate() {

        if (this.graphRepository.getVersion() == 0) {
            migrateToGraphV0();
            this.graphRepository.setVersion(1);
        }
        return;
    }

    public void migrateToGraphV0() {
        throw new RuntimeException("Not implemented yet.");
    }

}
