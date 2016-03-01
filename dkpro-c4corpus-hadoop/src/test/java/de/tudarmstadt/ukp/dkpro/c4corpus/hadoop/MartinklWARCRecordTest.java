/*
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop;

import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCRecord;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author zayed
 */
public class MartinklWARCRecordTest
{
    // FIXME ??
    @Ignore
    @Test
    public void testParseWARCInfo()
            throws IOException
    {
        DataInputStream stream = new DataInputStream(
                new ByteArrayInputStream(("WARC/1.0\n" + "WARC-Type: warcinfo\r\n"
                        + "WARC-Date: 2014-03-18T17:47:38Z\r\n"
                        + "WARC-Record-ID: <urn:uuid:d9bbb325-c09f-473c-8600-1c9dbd4ec443>\r\n"
                        + "Content-Length: 371\r\n" + "Content-Type: application/warc-fields\r\n"
                        + "WARC-Filename: CC-MAIN-20140313024455-00000-ip-10-183-142-35.ec2.internal.warc.gz\r\n"
                        + "\r\n" + "robots: classic\r\n"
                        + "hostname: ip-10-183-142-35.ec2.internal\r\n"
                        + "software: Nutch 1.6 (CC)/CC WarcExport 1.0\r\n"
                        + "isPartOf: CC-MAIN-2014-10\r\n" + "operator: CommonCrawl Admin\r\n"
                        + "description: Wide crawl of the web with URLs provided by Blekko for March 2014\r\n"
                        + "publisher: CommonCrawl\r\n" + "format: WARC File Format 1.0\r\n"
                        + "conformsTo: http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1_latestdraft.pdf\r\n"
                        + "\r\n" + "\r\n" + "\r\n").getBytes("UTF-8")));
        WARCRecord record = new WARCRecord(stream);
        assertEquals(371, record.getHeader().getContentLength());
        assertEquals("warcinfo", record.getHeader().getRecordType());
        assertEquals("2014-03-18T17:47:38Z", record.getHeader().getDateString());
        assertEquals("<urn:uuid:d9bbb325-c09f-473c-8600-1c9dbd4ec443>",
                record.getHeader().getRecordID());
        assertEquals("application/warc-fields", record.getHeader().getContentType());
        assertNull(record.getHeader().getTargetURI());
    }

    @Test
    public void testExtractHTTPHeaderContentType()
            throws Exception
    {
        String httpHeaders = "HTTP/1.1 200 OK\n"
                + "Date: Sun, 05 Jul 2015 17:30:02 GMT\n"
                + "Server: Apache/2.2.29 (Unix) FrontPage/5.0.2.2635\n"
                + "X-Powered-By: PHP/5.3.29-pl0-gentoo\n"
                + "X-Pingback: http://0351de3.netsolhost.com/WordPress/xmlrpc.php\n"
                + "Link: <http://0351de3.netsolhost.com/WordPress/?p=8261>; rel=shortlink\n"
                + "Connection: close\n"
                + "Content-Type: text/html; charset=UTF-8\n";

        assertEquals("text/html", WARCRecord.extractHTTPHeaderContentType(httpHeaders));

        httpHeaders = "HTTP/1.1 200 OK\n"
                + "Date: Sun, 05 Jul 2015 17:30:02 GMT\n"
                + "Content-Type: application/xhtml+xml\n"
                + "Server: Apache/2.2.29 (Unix) FrontPage/5.0.2.2635\n";

        assertEquals("application/xhtml+xml", WARCRecord.extractHTTPHeaderContentType(httpHeaders));


        httpHeaders = "HTTP/1.1 200 OK\n"
                + "Date: Sun, 05 Jul 2015 17:30:02 GMT\n"
                + "Content-Type: application/wordperfect5.1;\n"
                + "Server: Apache/2.2.29 (Unix) FrontPage/5.0.2.2635\n";

        assertEquals("application/wordperfect5.1", WARCRecord.extractHTTPHeaderContentType(httpHeaders));


    }

    @Test
    public void testExtractHTTPHeaderCharset()
            throws Exception
    {
        String httpHeaders = "HTTP/1.1 200 OK\n"
                + "Date: Sun, 05 Jul 2015 17:30:02 GMT\n"
                + "Server: Apache/2.2.29 (Unix) FrontPage/5.0.2.2635\n"
                + "X-Powered-By: PHP/5.3.29-pl0-gentoo\n"
                + "X-Pingback: http://0351de3.netsolhost.com/WordPress/xmlrpc.php\n"
                + "Link: <http://0351de3.netsolhost.com/WordPress/?p=8261>; rel=shortlink\n"
                + "Connection: close\n"
                + "Content-Type: text/html; charset= UTF-8\n";

        assertEquals("utf-8", WARCRecord.extractHTTPHeaderCharset(httpHeaders));

        httpHeaders = "HTTP/1.1 200 OK\n"
                + "Date: Sun, 05 Jul 2015 17:30:02 GMT\n"
                + "Server: Apache/2.2.29 (Unix) FrontPage/5.0.2.2635\n"
                + "X-Powered-By: PHP/5.3.29-pl0-gentoo\n"
                + "X-Pingback: http://0351de3.netsolhost.com/WordPress/xmlrpc.php\n"
                + "Link: <http://0351de3.netsolhost.com/WordPress/?p=8261>; rel=shortlink\n"
                + "Connection: close\n"
                + "Content-Type: text/html; charset=charset=ISO-8859-4";

        assertEquals("iso-8859-4", WARCRecord.extractHTTPHeaderCharset(httpHeaders));

    }
}
