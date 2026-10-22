package dk.gov.nemlogin.signing.model;

import dk.gov.nemlogin.signing.format.SignatureFormat;

public class IntegrityValues {

    private SignatureFormat signatureFormat;
    private byte[] originalDigest;
    private int byteRange1End;
    private int byteRange2Start;
    private int byteRange2Length;
    private String digestAlgorithm;

    public IntegrityValues() {}

    public IntegrityValues(SignatureFormat signatureFormat, String digestAlgorithm, byte[] originalDigest, int byteRange1End, int byteRange2Start, int byteRange2Length) {
        this.setSignatureFormat(signatureFormat);
        this.originalDigest = originalDigest;
        this.byteRange1End = byteRange1End;
        this.byteRange2Start = byteRange2Start;
        this.byteRange2Length = byteRange2Length;
        this.digestAlgorithm = digestAlgorithm;
    }

    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

	public byte[] getOriginalDigest() {
		return originalDigest;
	}

	public int getByteRange1End() {
		return byteRange1End;
	}

	public int getByteRange2Start() {
		return byteRange2Start;
	}

	public int getByteRange2Length() {
		return byteRange2Length;
	}

    public SignatureFormat getSignatureFormat() {
        return signatureFormat;
    }

    public void setSignatureFormat(SignatureFormat signatureFormat) {
        this.signatureFormat = signatureFormat;
    }

}
