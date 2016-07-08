package com.ge.predix.acs.testutils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.acs.rest.Parent;

public final class XFiles {
    private XFiles() {
        // Prevents instantiation.
    }

    public static final String ISSUER_URI = "acs.example.org";
    public static final Attribute SITE_BASEMENT = new Attribute(ISSUER_URI, "site", "basement");
    public static final Attribute SITE_PENTAGON = new Attribute(ISSUER_URI, "site", "pentagon");
    public static final Attribute SITE_QUANTICO = new Attribute(ISSUER_URI, "site", "quantico");
    public static final Attribute TOP_SECRET_CLASSIFICATION = new Attribute(ISSUER_URI, "classification", "top secret");
    public static final Attribute SECRET_CLASSIFICATION = new Attribute(ISSUER_URI, "classification", "secret");
    public static final Attribute SPECIAL_AGENTS_GROUP_ATTRIBUTE = new Attribute(ISSUER_URI, "group", "special-agents");
    public static final Attribute TYPE_MYTHARC = new Attribute(ISSUER_URI, "type", "mytharc");
    public static final Attribute TYPE_MONSTER_OF_THE_WEEK = new Attribute("acs.predix.ge.com", "type",
            "monster-of-the-week");

    public static final String FBI = "fbi";
    public static final Set<Attribute> FBI_ATTRIBUTES = new HashSet<>(Arrays.asList(new Attribute[] { SITE_QUANTICO }));

    public static final String SPECIAL_AGENTS_GROUP = "special-agents";
    public static final Set<Attribute> SPECIAL_AGENTS_GROUP_ATTRIBUTES = new HashSet<>(
            Arrays.asList(new Attribute[] { SPECIAL_AGENTS_GROUP_ATTRIBUTE }));

    public static final String TOP_SECRET_GROUP = "top-secret-clearance";
    public static final Set<Attribute> TOP_SECRET_GROUP_ATTRIBUTES = new HashSet<>(
            Arrays.asList(new Attribute[] { TOP_SECRET_CLASSIFICATION }));

    public static final String SECRET_GROUP = "secret-clearance";
    public static final Set<Attribute> SECRET_GROUP_ATTRIBUTES = new HashSet<>(
            Arrays.asList(new Attribute[] { SECRET_CLASSIFICATION }));

    public static final String AGENT_MULDER = "mulder";
    public static final Set<Attribute> MULDERS_ATTRIBUTES = new HashSet<>(
            Arrays.asList(new Attribute[] { SITE_BASEMENT }));

    public static final String AGENT_SCULLY = "scully";
    public static final Set<Attribute> SCULLYS_ATTRIBUTES = new HashSet<>(
            Arrays.asList(new Attribute[] { SITE_BASEMENT }));

    public static final String BASEMENT_SITE_ID = "/site/basement";
    public static final Set<Attribute> BASEMENT_ATTRIBUTES = new HashSet<>(
            Arrays.asList(new Attribute[] { SITE_BASEMENT }));

    public static final String PENTAGON_SITE_ID = "/site/pentagon";
    public static final Set<Attribute> PENTAGON_ATTRIBUTES = new HashSet<>(
            Arrays.asList(new Attribute[] { SITE_PENTAGON }));

    public static final String ASCENSION_ID = "/x-files/ascension";
    public static final Set<Attribute> ASCENSION_ATTRIBUTES = new HashSet<>(
            Arrays.asList(new Attribute[] { SPECIAL_AGENTS_GROUP_ATTRIBUTE, TYPE_MYTHARC }));

    public static final String DRIVE_ID = "/x-files/drive";
    public static final Set<Attribute> DRIVE_ATTRIBUTES = new HashSet<>(
            Arrays.asList(new Attribute[] { TYPE_MONSTER_OF_THE_WEEK }));

    public static final String JOSECHUNG_ID = "/x-files/josechung";
    public static final Set<Attribute> JOSECHUNG_ATTRIBUTES = new HashSet<>(
            Arrays.asList(new Attribute[] { TYPE_MONSTER_OF_THE_WEEK }));

    public static final String EVIDENCE_SCULLYS_TESTIMONY_ID = "/evidence/scullys-testimony";
    public static final Set<Attribute> SCULLYS_TESTIMONY_ATTRIBUTES = new HashSet<>(
            Arrays.asList(new Attribute[] { TOP_SECRET_CLASSIFICATION }));

    public static final String EVIDENCE_IMPLANT_ID = "/evidence/implant";
    public static final Set<Attribute> EVIDENCE_IMPLANT_ATTRIBUTES = new HashSet<>(
            Arrays.asList(new Attribute[] { TOP_SECRET_CLASSIFICATION }));

    public static List<BaseSubject> createSubjectHierarchy() {
        BaseSubject fbi = new BaseSubject(FBI);
        fbi.setAttributes(FBI_ATTRIBUTES);

        BaseSubject specialAgentsGroup = new BaseSubject(SPECIAL_AGENTS_GROUP);
        specialAgentsGroup.setAttributes(SPECIAL_AGENTS_GROUP_ATTRIBUTES);
        specialAgentsGroup
                .setParents(new HashSet<>(Arrays.asList(new Parent[] { new Parent(fbi.getSubjectIdentifier()) })));

        BaseSubject topSecretGroup = new BaseSubject(TOP_SECRET_GROUP);
        topSecretGroup.setAttributes(TOP_SECRET_GROUP_ATTRIBUTES);

        BaseSubject agentMulder = new BaseSubject(AGENT_MULDER);
        agentMulder.setAttributes(MULDERS_ATTRIBUTES);
        agentMulder.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(specialAgentsGroup.getSubjectIdentifier()),
                        new Parent(topSecretGroup.getSubjectIdentifier()) })));

        return Arrays.asList(new BaseSubject[] { fbi, specialAgentsGroup, topSecretGroup, agentMulder });
    }

    public static List<BaseResource> createTwoParentResourceHierarchy() {
        BaseResource pentagon = new BaseResource(PENTAGON_SITE_ID);
        pentagon.setAttributes(PENTAGON_ATTRIBUTES);

        BaseResource ascension = new BaseResource(ASCENSION_ID);
        ascension.setAttributes(ASCENSION_ATTRIBUTES);

        BaseResource evidenceImplant = new BaseResource(EVIDENCE_IMPLANT_ID);
        evidenceImplant.setAttributes(EVIDENCE_IMPLANT_ATTRIBUTES);
        evidenceImplant.setParents(new HashSet<>(Arrays.asList(new Parent[] {
                new Parent(pentagon.getResourceIdentifier()), new Parent(ascension.getResourceIdentifier()) })));

        return Arrays.asList(new BaseResource[] { pentagon, ascension, evidenceImplant });
    }

    public static List<BaseResource> createThreeLevelResourceHierarchy() {
        BaseResource basement = new BaseResource(BASEMENT_SITE_ID);
        basement.setAttributes(BASEMENT_ATTRIBUTES);

        BaseResource ascension = new BaseResource(ASCENSION_ID);
        ascension.setAttributes(ASCENSION_ATTRIBUTES);
        ascension.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(basement.getResourceIdentifier()) })));

        BaseResource scullysTestimony = new BaseResource(EVIDENCE_SCULLYS_TESTIMONY_ID);
        scullysTestimony.setAttributes(SCULLYS_TESTIMONY_ATTRIBUTES);
        scullysTestimony.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(ascension.getResourceIdentifier()) })));

        return Arrays.asList(new BaseResource[] { basement, ascension, scullysTestimony });
    }

    public static List<BaseSubject> createScopedSubjectHierarchy() {
        BaseSubject fbi = new BaseSubject(FBI);
        fbi.setAttributes(FBI_ATTRIBUTES);

        BaseSubject specialAgentsGroup = new BaseSubject(SPECIAL_AGENTS_GROUP);
        specialAgentsGroup.setAttributes(SPECIAL_AGENTS_GROUP_ATTRIBUTES);
        specialAgentsGroup
                .setParents(new HashSet<>(Arrays.asList(new Parent[] { new Parent(fbi.getSubjectIdentifier()) })));

        BaseSubject topSecretGroup = new BaseSubject(TOP_SECRET_GROUP);
        topSecretGroup.setAttributes(TOP_SECRET_GROUP_ATTRIBUTES);

        BaseSubject agentMulder = new BaseSubject(AGENT_MULDER);
        agentMulder.setAttributes(MULDERS_ATTRIBUTES);
        agentMulder.setParents(new HashSet<>(Arrays.asList(new Parent[] {
                new Parent(specialAgentsGroup.getSubjectIdentifier()), new Parent(topSecretGroup.getSubjectIdentifier(),
                        new HashSet<>(Arrays.asList(new Attribute[] { SITE_BASEMENT }))) })));
        return Arrays.asList(new BaseSubject[] { fbi, specialAgentsGroup, topSecretGroup, agentMulder });
    }
}
