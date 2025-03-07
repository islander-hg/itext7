/*

    This file is part of the iText (R) project.
    Copyright (c) 1998-2020 iText Group NV
    Authors: Bruno Lowagie, Paulo Soares, et al.

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation with the addition of the
    following permission added to Section 15 as permitted in Section 7(a):
    FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
    ITEXT GROUP. ITEXT GROUP DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
    OF THIRD PARTY RIGHTS

    This program is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
    or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License
    along with this program; if not, see http://www.gnu.org/licenses or write to
    the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
    Boston, MA, 02110-1301 USA, or download the license from the following URL:
    http://itextpdf.com/terms-of-use/

    The interactive user interfaces in modified source and object code versions
    of this program must display Appropriate Legal Notices, as required under
    Section 5 of the GNU Affero General Public License.

    In accordance with Section 7(b) of the GNU Affero General Public License,
    a covered work must retain the producer line in every PDF that is created
    or manipulated using iText.

    You can be released from the requirements of the license by purchasing
    a commercial license. Buying such a license is mandatory as soon as you
    develop commercial activities involving the iText software without
    disclosing the source code of your own applications.
    These activities include: offering paid services to customers as an ASP,
    serving PDFs on the fly in a web application, shipping iText with a closed
    source product.

    For more information, please contact iText Software Corp. at this
    address: sales@itextpdf.com
 */
package com.itextpdf.kernel.pdf;

import java.io.Serializable;
import java.security.SecureRandom;
import java.security.cert.Certificate;

/**
 * Allows configuration of output PDF encryption.
 */
public class EncryptionProperties implements Serializable {

    private static final long serialVersionUID = 3926570647944137843L;

    protected int encryptionAlgorithm;

    // StandardEncryption properties
    protected byte[] userPassword;
    protected byte[] ownerPassword;
    protected int standardEncryptPermissions;

    // PublicKeyEncryption properties
    protected Certificate[] publicCertificates;
    protected int[] publicKeyEncryptPermissions;

    /**
     * Sets the encryption options for the document.
     *
     * @param userPassword        the user password. Can be null or of zero length, which is equal to
     *                            omitting the user password
     * @param ownerPassword       the owner password. If it's null or empty, iText will generate
     *                            a random string to be used as the owner password
     * @param permissions         the user permissions. The open permissions for the document can be
     *                            {@link EncryptionConstants#ALLOW_PRINTING},
     *                            {@link EncryptionConstants#ALLOW_MODIFY_CONTENTS},
     *                            {@link EncryptionConstants#ALLOW_COPY},
     *                            {@link EncryptionConstants#ALLOW_MODIFY_ANNOTATIONS},
     *                            {@link EncryptionConstants#ALLOW_FILL_IN},
     *                            {@link EncryptionConstants#ALLOW_SCREENREADERS},
     *                            {@link EncryptionConstants#ALLOW_ASSEMBLY} and
     *                            {@link EncryptionConstants#ALLOW_DEGRADED_PRINTING}.
     *                            The permissions can be combined by ORing them
     * @param encryptionAlgorithm the type of encryption. It can be one of
     *                            {@link EncryptionConstants#STANDARD_ENCRYPTION_40},
     *                            {@link EncryptionConstants#STANDARD_ENCRYPTION_128},
     *                            {@link EncryptionConstants#ENCRYPTION_AES_128} or
     *                            {@link EncryptionConstants#ENCRYPTION_AES_256}.
     *                            Optionally {@link EncryptionConstants#DO_NOT_ENCRYPT_METADATA} can be OEed
     *                            to output the metadata in cleartext.
     *                            {@link EncryptionConstants#EMBEDDED_FILES_ONLY} can be ORed as well.
     *                            Please be aware that the passed encryption types may override permissions:
     *                            {@link EncryptionConstants#STANDARD_ENCRYPTION_40} implicitly sets
     *                            {@link EncryptionConstants#DO_NOT_ENCRYPT_METADATA} and
     *                            {@link EncryptionConstants#EMBEDDED_FILES_ONLY} as false;
     *                            {@link EncryptionConstants#STANDARD_ENCRYPTION_128} implicitly sets
     *                            {@link EncryptionConstants#EMBEDDED_FILES_ONLY} as false;
     *
     * @return this {@link EncryptionProperties}
     */
    public EncryptionProperties setStandardEncryption(byte[] userPassword, byte[] ownerPassword, int permissions,
            int encryptionAlgorithm) {
        clearEncryption();
        this.userPassword = userPassword;
        if (ownerPassword != null) {
            this.ownerPassword = ownerPassword;
        } else {
            this.ownerPassword = new byte[16];
            randomBytes(this.ownerPassword);
        }
        this.standardEncryptPermissions = permissions;
        this.encryptionAlgorithm = encryptionAlgorithm;

        return this;
    }

    /**
     * Sets the certificate encryption options for the document.
     * <p>
     * An array of one or more public certificates must be provided together with an array of the same size
     * for the permissions for each certificate.
     *
     * @param certs               the public certificates to be used for the encryption
     * @param permissions         the user permissions for each of the certificates
     *                            The open permissions for the document can be
     *                            {@link EncryptionConstants#ALLOW_PRINTING},
     *                            {@link EncryptionConstants#ALLOW_MODIFY_CONTENTS},
     *                            {@link EncryptionConstants#ALLOW_COPY},
     *                            {@link EncryptionConstants#ALLOW_MODIFY_ANNOTATIONS},
     *                            {@link EncryptionConstants#ALLOW_FILL_IN},
     *                            {@link EncryptionConstants#ALLOW_SCREENREADERS},
     *                            {@link EncryptionConstants#ALLOW_ASSEMBLY} and
     *                            {@link EncryptionConstants#ALLOW_DEGRADED_PRINTING}.
     *                            The permissions can be combined by ORing them
     * @param encryptionAlgorithm the type of encryption. It can be one of
     *                            {@link EncryptionConstants#STANDARD_ENCRYPTION_40},
     *                            {@link EncryptionConstants#STANDARD_ENCRYPTION_128},
     *                            {@link EncryptionConstants#ENCRYPTION_AES_128} or
     *                            {@link EncryptionConstants#ENCRYPTION_AES_256}.
     *                            Optionally {@link EncryptionConstants#DO_NOT_ENCRYPT_METADATA}
     *                            can be ORed to output the metadata in cleartext.
     *                            {@link EncryptionConstants#EMBEDDED_FILES_ONLY} can be ORed as well.
     *                            Please be aware that the passed encryption types may override permissions:
     *                            {@link EncryptionConstants#STANDARD_ENCRYPTION_40} implicitly sets
     *                            {@link EncryptionConstants#DO_NOT_ENCRYPT_METADATA} and
     *                            {@link EncryptionConstants#EMBEDDED_FILES_ONLY} as false;
     *                            {@link EncryptionConstants#STANDARD_ENCRYPTION_128} implicitly sets
     *                            {@link EncryptionConstants#EMBEDDED_FILES_ONLY} as false;
     * @return this {@link EncryptionProperties}
     */
    public EncryptionProperties setPublicKeyEncryption(Certificate[] certs, int[] permissions,
            int encryptionAlgorithm) {
        clearEncryption();
        this.publicCertificates = certs;
        this.publicKeyEncryptPermissions = permissions;
        this.encryptionAlgorithm = encryptionAlgorithm;

        return this;
    }

    boolean isStandardEncryptionUsed() {
        return ownerPassword != null;
    }

    boolean isPublicKeyEncryptionUsed() {
        return publicCertificates != null;
    }

    private void clearEncryption() {
        this.publicCertificates = null;
        this.publicKeyEncryptPermissions = null;
        this.userPassword = null;
        this.ownerPassword = null;
    }

    private static void randomBytes(byte[] bytes) {
        new SecureRandom().nextBytes(bytes);
    }
}
