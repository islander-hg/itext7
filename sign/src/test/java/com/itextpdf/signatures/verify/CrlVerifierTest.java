/*
    This file is part of the iText (R) project.
    Copyright (c) 1998-2020 iText Group NV
    Authors: iText Software.

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
package com.itextpdf.signatures.verify;

import com.itextpdf.io.util.DateTimeUtil;
import com.itextpdf.signatures.CRLVerifier;
import com.itextpdf.signatures.VerificationException;
import com.itextpdf.signatures.testutils.SignTestPortUtil;
import com.itextpdf.signatures.testutils.builder.TestCrlBuilder;
import com.itextpdf.signatures.testutils.client.TestCrlClient;
import com.itextpdf.test.ExtendedITextTest;
import com.itextpdf.test.annotations.type.UnitTest;
import com.itextpdf.test.signutils.Pkcs12FileHelper;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collection;

@Category(UnitTest.class)
public class CrlVerifierTest extends ExtendedITextTest {
    private static final String certsSrc = "./src/test/resources/com/itextpdf/signatures/certs/";
    private static final char[] password = "testpass".toCharArray();

    @Rule
    public ExpectedException junitExpectedException = ExpectedException.none();

    @BeforeClass
    public static void before() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void validCrl01() throws GeneralSecurityException, IOException {
        X509Certificate caCert = (X509Certificate) Pkcs12FileHelper.readFirstChain(certsSrc + "rootRsa.p12", password)[0];
        TestCrlBuilder crlBuilder = new TestCrlBuilder(caCert, DateTimeUtil.addDaysToDate(DateTimeUtil.getCurrentTimeDate(), -1));
        Assert.assertTrue(verifyTest(crlBuilder));
    }

    @Test
    public void invalidRevokedCrl01() throws GeneralSecurityException, IOException {
        junitExpectedException.expect(VerificationException.class);

        X509Certificate caCert = (X509Certificate) Pkcs12FileHelper.readFirstChain(certsSrc + "rootRsa.p12", password)[0];
        TestCrlBuilder crlBuilder = new TestCrlBuilder(caCert, DateTimeUtil.addDaysToDate(DateTimeUtil.getCurrentTimeDate(), -1));

        String checkCertFileName = certsSrc + "signCertRsa01.p12";
        X509Certificate checkCert = (X509Certificate) Pkcs12FileHelper.readFirstChain(checkCertFileName, password)[0];
        crlBuilder.addCrlEntry(checkCert, DateTimeUtil.addDaysToDate(DateTimeUtil.getCurrentTimeDate(), -40), CRLReason.keyCompromise);

        verifyTest(crlBuilder);
    }

    @Test
    public void invalidOutdatedCrl01() throws GeneralSecurityException, IOException {
        X509Certificate caCert = (X509Certificate) Pkcs12FileHelper.readFirstChain(certsSrc + "rootRsa.p12", password)[0];
        TestCrlBuilder crlBuilder = new TestCrlBuilder(caCert, DateTimeUtil.addDaysToDate(DateTimeUtil.getCurrentTimeDate(), -2));
        crlBuilder.setNextUpdate(DateTimeUtil.addDaysToDate(DateTimeUtil.getCurrentTimeDate(), -1));

        Assert.assertFalse(verifyTest(crlBuilder));
    }

    private boolean verifyTest(TestCrlBuilder crlBuilder) throws GeneralSecurityException, IOException {
        String caCertFileName = certsSrc + "rootRsa.p12";
        X509Certificate caCert = (X509Certificate) Pkcs12FileHelper.readFirstChain(caCertFileName, password)[0];
        PrivateKey caPrivateKey = Pkcs12FileHelper.readFirstKey(caCertFileName, password, password);
        String checkCertFileName = certsSrc + "signCertRsa01.p12";
        X509Certificate checkCert = (X509Certificate) Pkcs12FileHelper.readFirstChain(checkCertFileName, password)[0];


        TestCrlClient crlClient = new TestCrlClient(crlBuilder, caPrivateKey);
        Collection<byte[]> crlBytesCollection = crlClient.getEncoded(checkCert, null);

        boolean verify = false;
        for (byte[] crlBytes : crlBytesCollection) {
            X509CRL crl = (X509CRL) SignTestPortUtil.parseCrlFromStream(new ByteArrayInputStream(crlBytes));
            CRLVerifier verifier = new CRLVerifier(null, null);
            verify = verifier.verify(crl, checkCert, caCert, DateTimeUtil.getCurrentTimeDate());
            break;
        }
        return verify;
    }
}
