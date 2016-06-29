package com.ge.predix.acs.testutils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.ge.predix.acs.model.Attribute;

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
}
