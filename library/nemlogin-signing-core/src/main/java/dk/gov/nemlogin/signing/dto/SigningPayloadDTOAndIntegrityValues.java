package dk.gov.nemlogin.signing.dto;

import dk.gov.nemlogin.signing.model.IntegrityValues;

public class SigningPayloadDTOAndIntegrityValues {

    private final SigningPayloadDTO signingPayloadDTO;
    private final IntegrityValues integrityValues;

    public SigningPayloadDTOAndIntegrityValues(SigningPayloadDTO signingPayloadDto, IntegrityValues integrityValues) {
        this.signingPayloadDTO = signingPayloadDto;
        this.integrityValues = integrityValues;
    }

    public SigningPayloadDTO getSigningPayloadDTO() {
        return signingPayloadDTO;
    }

    public IntegrityValues getIntegrityValues() {
        return integrityValues;
    }


}
