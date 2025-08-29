package dk.gov.nemlogin.signing.broker.config;

/**
 * SAML properties
 **/
public class SamlProperties {
    private String spEntityId;
    private String assertionConsumerUrl;
    private String attrSsnPersistenceLevel;
    private String attrCertPolicy;
    private String attrLoa;
    private String attrFirstName;
    private String attrLastName;
    private String attrEmail;
    private String attrAge;
    private String attrAnonymized;

    public String getSpEntityId() {
        return spEntityId;
    }

    public void setSpEntityId(String spEntityId) {
        this.spEntityId = spEntityId;
    }

    public String getAssertionConsumerUrl() {
        return assertionConsumerUrl;
    }

    public void setAssertionConsumerUrl(String assertionConsumerUrl) {
        this.assertionConsumerUrl = assertionConsumerUrl;
    }

    public String getAttrSsnPersistenceLevel() {
        return attrSsnPersistenceLevel;
    }

    public void setAttrSsnPersistenceLevel(String attrSsnPersistenceLevel) {
        this.attrSsnPersistenceLevel = attrSsnPersistenceLevel;
    }

    public String getAttrCertPolicy() {
        return attrCertPolicy;
    }

    public void setAttrCertPolicy(String attrCertPolicy) {
        this.attrCertPolicy = attrCertPolicy;
    }

    public String getAttrLoa() {
        return attrLoa;
    }

    public void setAttrLoa(String attrLoa) {
        this.attrLoa = attrLoa;
    }

    public String getAttrFirstName() {
        return attrFirstName;
    }

    public void setAttrFirstName(String attrFirstName) {
        this.attrFirstName = attrFirstName;
    }

    public String getAttrLastName() {
        return attrLastName;
    }

    public void setAttrLastName(String attrLastName) {
        this.attrLastName = attrLastName;
    }

    public String getAttrEmail() {
        return attrEmail;
    }

    public void setAttrEmail(String attrEmail) {
        this.attrEmail = attrEmail;
    }

    public String getAttrAge() {
        return attrAge;
    }

    public void setAttrAge(String attrAge) {
        this.attrAge = attrAge;
    }

    public String getAttrAnonymized() {
        return attrAnonymized;
    }

    public void setAttrAnonymized(String attrAnonymized) {
        this.attrAnonymized = attrAnonymized;
    }
}
