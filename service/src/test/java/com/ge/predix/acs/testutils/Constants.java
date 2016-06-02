package com.ge.predix.acs.testutils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.ge.predix.acs.model.Attribute;

public class Constants {
    public Constants() {
        // Prevents instantiation.
    }

    public static final String ISSUER_URI = "acs.example.org";
    public static final Attribute ATTR_SITE_0 = new Attribute(ISSUER_URI, "site", "basement");
    public static final Attribute ATTR_SITE_1 = new Attribute(ISSUER_URI, "site", "pentagon");
    public static final Attribute ATTR_SITE_2 = new Attribute(ISSUER_URI, "site", "quantico");
    public static final Attribute ATTR_CLASSIFICATION_0 = new Attribute(ISSUER_URI, "classification", "top secret");
    public static final Attribute ATTR_CLASSIFICATION_1 = new Attribute(ISSUER_URI, "classification", "secret");
    public static final Attribute ATTR_GROUP_0 = new Attribute(ISSUER_URI, "group", "special-agents");
    public static final Attribute ATTR_TYPE_0 = new Attribute(ISSUER_URI, "type", "mytharc");
    public static final Attribute ATTR_TYPE_1 = new Attribute("acs.predix.ge.com", "type", "monster-of-the-week");

    public static final String SUBJECT_GROUP_0_ID = "fbi";
    public static final Set<Attribute> SUBJECT_GROUP_0_ATTRS_0 = new HashSet<>(
            Arrays.asList(new Attribute[] { ATTR_SITE_2 }));

    public static final String SUBJECT_GROUP_1_ID = "special-agents";
    public static final Set<Attribute> SUBJECT_GROUP_1_ATTRS_0 = new HashSet<>(
            Arrays.asList(new Attribute[] { ATTR_GROUP_0 }));

    public static final String SUBJECT_GROUP_2_ID = "top-secret-clearance";
    public static final Set<Attribute> SUBJECT_GROUP_2_ATTRS_0 = new HashSet<>(
            Arrays.asList(new Attribute[] { ATTR_CLASSIFICATION_0 }));

    public static final String SUBJECT_GROUP_3_ID = "secret-clearance";
    public static final Set<Attribute> SUBJECT_GROUP_3_ATTRS_0 = new HashSet<>(
            Arrays.asList(new Attribute[] { ATTR_CLASSIFICATION_1}));

    public static final String SUBJECT_USER_1_ID = "mulder";
    public static final Set<Attribute> SUBJECT_USER_1_ATTRS_0 = new HashSet<>(
            Arrays.asList(new Attribute[] { ATTR_SITE_0 }));

    public static final String SUBJECT_USER_2_ID = "scully";
    public static final Set<Attribute> SUBJECT_USER_2_ATTRS_0 = new HashSet<>(
            Arrays.asList(new Attribute[] { ATTR_SITE_0 }));

    public static final String RESOURCE_SITE_0_ID = "/site/basement";
    public static final Set<Attribute> RESOURCE_SITE_0_ATTRS_0 = new HashSet<>(
            Arrays.asList(new Attribute[] { ATTR_SITE_0 }));

    public static final String RESOURCE_SITE_1_ID = "/site/pentagon";
    public static final Set<Attribute> RESOURCE_SITE_1_ATTRS_0 = new HashSet<>(
            Arrays.asList(new Attribute[] { ATTR_SITE_1 }));

    public static final String RESOURCE_XFILE_0_ID = "/x-files/ascension";
    public static final Set<Attribute> RESOURCE_XFILE_0_ATTRS_0 = new HashSet<>(
            Arrays.asList(new Attribute[] { ATTR_GROUP_0, ATTR_TYPE_0 }));

    public static final String RESOURCE_XFILE_1_ID = "/x-files/drive";
    public static final Set<Attribute> RESOURCE_XFILE_1_ATTRS_0 = new HashSet<>(
            Arrays.asList(new Attribute[] { ATTR_TYPE_1 }));

    public static final String RESOURCE_XFILE_2_ID = "/x-files/josechung";
    public static final Set<Attribute> RESOURCE_XFILE_2_ATTRS_0 = new HashSet<>(
            Arrays.asList(new Attribute[] { ATTR_TYPE_1 }));

    public static final String RESOURCE_EVIDENCE_0_ID = "/evidence/scullys-testimony";
    public static final Set<Attribute> RESOURCE_EVIDENCE_0_ATTRS_0 = new HashSet<Attribute>(
            Arrays.asList(new Attribute[] { ATTR_CLASSIFICATION_0 }));

    public static final String RESOURCE_EVIDENCE_1_ID = "/evidence/implant";
    public static final Set<Attribute> RESOURCE_EVIDENCE_1_ATTRS_0 = new HashSet<>(
            Arrays.asList(new Attribute[] { ATTR_CLASSIFICATION_0 }));
}
