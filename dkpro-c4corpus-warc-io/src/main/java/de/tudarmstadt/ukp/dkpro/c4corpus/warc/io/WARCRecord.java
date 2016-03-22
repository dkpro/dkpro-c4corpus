/*
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Copyright (c) 2014 Martin Kleppmann
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
package de.tudarmstadt.ukp.dkpro.c4corpus.warc.io;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable implementation of a record in a WARC file. You create a {@link WARCRecord}
 * by parsing it out of a {@link DataInput} stream.
 * <br>
 * The file format is documented in the
 * [ISO Standard](http://bibnum.bnf.fr/warc/WARC_ISO_28500_version1_latestdraft.pdf).
 * In a nutshell, it's a textual format consisting of lines delimited by `\r\n`.
 * Each record has the following structure:
 * <br>
 * 1. A line indicating the WARC version number, such as `WARC/1.0`.
 * 2. Several header lines (in key-value format, similar to HTTP or email headers),
 * giving information about the record. The header is terminated by an empty line.
 * 3. A body consisting of raw bytes (the number of bytes is indicated in one of the headers).
 * 4. A final separator of `\r\n\r\n` before the next record starts.
 * <br>
 * There are various different types of records, as documented on
 * {@link Header#getRecordType()}.
 * <br>
 * Based on https://github.com/ept/warc-hadoop
 * <br>
 * Note: originally published under MIT license, which is compatible with ASL license
 * https://www.gnu.org/philosophy/license-list.html
 *
 * @author Martin Kleppmann
 * @author Ivan Habernal
 */
public class WARCRecord
{

    public static final String WARC_VERSION = "WARC/1.0";
    private static final int MAX_LINE_LENGTH = 10000;
    private static final Pattern VERSION_PATTERN = Pattern.compile("WARC/[0-9\\.]+");
    private static final Pattern CONTINUATION_PATTERN = Pattern.compile("^[\\t ]+.*");
    private static final String CR_LF = "\r\n";
    private static final byte[] CR_LF_BYTES = { 13, 10 };

    private final Header header;
    private byte[] content;

    /**
     * Regex pattern for content type
     */
    private static final Pattern CONTENT_TYPE_PATTERN = Pattern
            .compile("Content-Type:\\s*([^\\s;]+)");

    /**
     * Regex pattern for charset
     */
    private static final Pattern CHARSET_PATTERN = Pattern
            .compile("Content-Type:.*charset=\\s*([^\\s;]+)");

    /**
     * Creates a new WARCRecord by parsing it out of a {@link DataInput} stream.
     *
     * @param in The input source from which one record will be read.
     * @throws IOException I/O exception
     */
    public WARCRecord(DataInput in)
            throws IOException
    {
        header = readHeader(in);
        content = new byte[header.getContentLength()];
        in.readFully(content);
        readSeparator(in);
    }

    /**
     * Creates a new {@code WARCRecord} instance by copying the content of another instance.
     *
     * @param other other instance
     */
    public WARCRecord(WARCRecord other)
    {
        this.header = other.header;
        this.content = other.content;
    }

    private static Header readHeader(DataInput in)
            throws IOException
    {
        String versionLine = readLine(in);
        if (!VERSION_PATTERN.matcher(versionLine).matches()) {
            throw new IOException("Expected WARC version, but got: " + versionLine);
        }

        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        String line, fieldName = null;

        do {
            line = readLine(in);
            if (fieldName != null && CONTINUATION_PATTERN.matcher(line).matches()) {
                headers.put(fieldName, headers.get(fieldName) + line);
            }
            else if (!line.isEmpty()) {
                String[] field = line.split(":", 2);

                if (field.length < 2) {
                    throw new IOException("Malformed header line: " + line);
                }

                fieldName = field[0].trim();
                headers.put(fieldName, field[1].trim());
            }
        }
        while (!line.isEmpty());

        return new Header(headers);
    }

    private static String readLine(DataInput in)
            throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean seenCR = false, seenCRLF = false;
        while (!seenCRLF) {
            if (out.size() > MAX_LINE_LENGTH) {
                throw new IOException("Exceeded maximum line length");
            }
            byte b = in.readByte();
            if (!seenCR && b == 13) {
                seenCR = true;
            }
            else if (seenCR && b == 10) {
                seenCRLF = true;
            }
            else {
                seenCR = false;
                out.write(b);
            }
        }
        return out.toString("UTF-8");
    }

    private static void readSeparator(DataInput in)
            throws IOException
    {
        byte[] sep = new byte[4];
        in.readFully(sep);
        if (sep[0] != 13 || sep[1] != 10 || sep[2] != 13 || sep[3] != 10) {
            throw new IOException(String.format(
                    "Expected final separator CR LF CR LF, but got: %d %d %d %d",
                    sep[0], sep[1], sep[2], sep[3]));
        }
    }

    /**
     * Returns true, if the Content-Type is {@code application/http; msgtype=response},
     * false otherwise
     *
     * @return boolean value
     */
    public boolean isContentApplicationHttpResponse()
    {
        return "application/http; msgtype=response".equals(header.getContentType());
    }

    /**
     * Returns HTTP response header, i.e.
     * <pre>
     * HTTP/1.1 200 OK
     * Date: Fri, 31 Dec 1999 23:59:59 GMT
     * Content-Type: text/html
     * </pre>
     * <br>
     * Only if the content starts with the HTTP response header which is delimited from the rest
     * by new line. Otherwise returns null.
     *
     * @return HTTP headers as a single string, or null if there is not HTTP header.
     */
    public String getHTTPHeaders()
    {
        String rawContent = new String(this.content);

        if (rawContent.startsWith("HTTP")) {
            int newLineIndex = rawContent.indexOf("\r\n\r\n");
            if (newLineIndex > 0) {
                return rawContent.substring(0, newLineIndex);
            }
        }

        return null;
    }

    /**
     * Returns the parsed header structure of the WARC record.
     *
     * @return header
     */
    public Header getHeader()
    {
        return header;
    }

    /**
     * Returns the body of the record, as an unparsed raw array of bytes. The content
     * of the body depends on the type of record (see {@link Header#getRecordType()}).
     * For example, in the case of a `response` type header, the body consists of the
     * full HTTP response returned by the server (HTTP headers followed by the body).
     *
     * @return content
     */
    public byte[] getContent()
    {
        return content;
    }

    /**
     * Writes this record to a {@link DataOutput} stream. The output may, in some edge
     * cases, be not byte-for-byte identical to what was parsed from a {@link DataInput}.
     * However it has the same meaning and should not lose any information.
     *
     * @param out The output stream to which this record should be appended.
     * @throws IOException I/O exception
     */
    public void write(DataOutput out)
            throws IOException
    {
        header.write(out);
        out.write(CR_LF_BYTES);
        out.write(content);
        out.write(CR_LF_BYTES);
        out.write(CR_LF_BYTES);
    }

    /**
     * Returns a human-readable string representation of the record.
     */
    @Override
    public String toString()
    {
        return header.toString();
    }

    public void setContent(String newContent)
            throws UnsupportedEncodingException
    {
        content = newContent.getBytes("utf-8");
    }

    /**
     * Extracts content type from the HTTP header, as specified in
     * {@code http://tools.ietf.org/html/rfc7231#section-3.1.1.5}, i.e.,
     * <pre>
     * Content-Type: TEXT/HTML; charset=ISO-8859-4
     * </pre>
     * returns "text/html" (always lower-cased)
     *
     * @param httpHeaderText http header
     * @return content type or null, if not specified in the http header
     * @throws IOException if httpHeaderText is null
     */
    public static String extractHTTPHeaderContentType(String httpHeaderText)
            throws IOException
    {
        if (httpHeaderText == null) {
            throw new IOException("httpHeaderText parameter is null");
        }

        Matcher matcher = CONTENT_TYPE_PATTERN.matcher(httpHeaderText);

        if (matcher.find()) {
            return matcher.group(1).toLowerCase();
        }

        return null;
    }

    /**
     * Extracts encoding from the HTTP header, as specified in
     * {@code http://tools.ietf.org/html/rfc7231#section-3.1.1.5}, i.e.,
     * <pre>
     * Content-Type: text/html; charset=ISO-8859-4
     * </pre>
     * returns "iso-8859-4" (always lower-cased)
     *
     * @param httpHeaderText http header
     * @return charset or null, if not specified in the http header
     * @throws IOException if httpHeaderText is null
     */
    public static String extractHTTPHeaderCharset(String httpHeaderText)
            throws IOException
    {
        if (httpHeaderText == null) {
            throw new IOException("httpHeaderText parameter is null");
        }

        Matcher matcher = CHARSET_PATTERN.matcher(httpHeaderText);

        if (matcher.find()) {
            return matcher.group(1).toLowerCase();
        }

        return null;
    }

    /**
     * Contains the parsed headers of a {@link WARCRecord}. Each record contains a number
     * of headers in key-value format, where some header keys are standardised, but
     * nonstandard ones can be added.
     * <br>
     * The documentation of the methods in this class is excerpted from the
     * [WARC 1.0 specification](http://bibnum.bnf.fr/warc/WARC_ISO_28500_version1_latestdraft.pdf).
     * Please see the specification for more detail.
     */
    public static class Header
    {
        private final Map<String, String> fields;

        private Header(Map<String, String> fields)
        {
            this.fields = fields;
        }

        /**
         * Returns the type of WARC record (the value of the `WARC-Type` header field).
         * WARC 1.0 defines the following record types: (for full definitions, see the
         * [spec](http://bibnum.bnf.fr/warc/WARC_ISO_28500_version1_latestdraft.pdf))
         * <br>
         * *  `warcinfo`: Describes the records that follow it, up through end of file,
         * end of input, or until next `warcinfo` record. Typically, this appears once and
         * at the beginning of a WARC file. For a web archive, it often contains information
         * about the web crawl which generated the following records.
         * <br>
         * The format of this descriptive record block may vary, though the use of the
         * `"application/warc-fields"` content-type is recommended. (...)
         * <br>
         * *  `response`: The record should contain a complete scheme-specific response, including
         * network protocol information where possible. For a target-URI of the `http` or
         * `https` schemes, a `response` record block should contain the full HTTP
         * response received over the network, including headers. That is, it contains the
         * 'Response' message defined by section 6 of HTTP/1.1 (RFC2616).
         * <br>
         * The WARC record's Content-Type field should contain the value defined by HTTP/1.1,
         * `"application/http;msgtype=response"`. The payload of the record is defined as its
         * 'entity-body' (per RFC2616), with any transfer-encoding removed.
         * <br>
         * *  `resource`: The record contains a resource, without full protocol response
         * information. For example: a file directly retrieved from a locally accessible
         * repository or the result of a networked retrieval where the protocol information
         * has been discarded. For a target-URI of the `http` or `https` schemes, a `resource`
         * record block shall contain the returned 'entity-body' (per RFC2616, with any
         * transfer-encodings removed), possibly truncated.
         * <br>
         * *  `request`: The record holds the details of a complete scheme-specific request,
         * including network protocol information where possible. For a target-URI of the
         * `http` or `https` schemes, a `request` record block should contain the full HTTP
         * request sent over the network, including headers. That is, it contains the
         * 'Request' message defined by section 5 of HTTP/1.1 (RFC2616).
         * <br>
         * The WARC record's Content-Type field should contain the value defined by HTTP/1.1,
         * `"application/http;msgtype=request"`. The payload of a `request` record with a
         * target-URI of scheme `http` or `https` is defined as its 'entity-body' (per
         * RFC2616), with any transfer-encoding removed.
         * <br>
         * *  `metadata`: The record contains content created in order to further describe,
         * explain, or accompany a harvested resource, in ways not covered by other record
         * types. A `metadata` record will almost always refer to another record of another
         * type, with that other record holding original harvested or transformed content.
         * <br>
         * The format of the metadata record block may vary. The `"application/warc-fields"`
         * format may be used.
         * <br>
         * *  `revisit`: The record describes the revisitation of content already archived,
         * and might include only an abbreviated content body which has to be interpreted
         * relative to a previous record. Most typically, a `revisit` record is used
         * instead of a `response` or `resource` record to indicate that the content
         * visited was either a complete or substantial duplicate of material previously
         * archived.
         * <br>
         * A `revisit` record shall contain a WARC-Profile field which determines the
         * interpretation of the record's fields and record block. Please see the
         * specification for details.
         * <br>
         * *  `conversion`: The record shall contain an alternative version of another
         * record's content that was created as the result of an archival process.
         * Typically, this is used to hold content transformations that maintain viability
         * of content after widely available rendering tools for the originally stored
         * format disappear. As needed, the original content may be migrated (transformed)
         * to a more viable format in order to keep the information usable with current
         * tools while minimizing loss of information.
         * <br>
         * *  `continuation`: Record blocks from `continuation` records must be appended to
         * corresponding prior record blocks (eg. from other WARC files) to create the
         * logically complete full-sized original record. That is, `continuation`
         * records are used when a record that would otherwise cause a WARC file size to
         * exceed a desired limit is broken into segments. A continuation record shall
         * contain the named fields `WARC-Segment-Origin-ID` and `WARC-Segment-Number`,
         * and the last `continuation` record of a series shall contain a
         * `WARC-Segment-Total-Length` field. Please see the specification for details.
         * <br>
         * *  Other record types may be added in future, so this list is not exclusive.
         *
         * @return The record's `WARC-Type` header field, as a string.
         */
        public String getRecordType()
        {
            return fields.get("WARC-Type");
        }

        /**
         * A 14-digit UTC timestamp formatted according to YYYY-MM-DDThh:mm:ssZ, described
         * in the W3C profile of ISO8601. The timestamp shall represent the instant that
         * data capture for record creation began. Multiple records written as part of a
         * single capture event shall use the same WARC-Date, even though the times of
         * their writing will not be exactly synchronized.
         *
         * @return The record's `WARC-Date` header field, as a string.
         */
        public String getDateString()
        {
            return fields.get("WARC-Date");
        }

        /**
         * An identifier assigned to the current record that is globally unique for its
         * period of intended use. No identifier scheme is mandated by this specification,
         * but each record-id shall be a legal URI and clearly indicate a documented and
         * registered scheme to which it conforms (e.g., via a URI scheme prefix such as
         * `http:` or `urn:`).
         *
         * @return The record's `WARC-Record-ID` header field, as a string.
         */
        public String getRecordID()
        {
            return fields.get("WARC-Record-ID");
        }

        /**
         * The MIME type (RFC2045) of the information contained in the record's block. For
         * example, in HTTP request and response records, this would be `application/http`
         * as per section 19.1 of RFC2616 (or `application/http; msgtype=request` and
         * `application/http; msgtype=response` respectively).
         * <br>
         * In particular, the content-type is *not* the value of the HTTP Content-Type
         * header in an HTTP response, but a MIME type to describe the full archived HTTP
         * message (hence `application/http` if the block contains request or response
         * headers).
         *
         * @return The record's `Content-Type` header field, as a string.
         */
        public String getContentType()
        {
            return fields.get("Content-Type");
        }

        /**
         * The original URI whose capture gave rise to the information content in this record.
         * In the context of web harvesting, this is the URI that was the target of a
         * crawler's retrieval request. For a `revisit` record, it is the URI that was the
         * target of a retrieval request. Indirectly, such as for a `metadata`, or `conversion`
         * record, it is a copy of the `WARC-Target-URI` appearing in the original record to
         * which the newer record pertains. The URI in this value shall be properly escaped
         * according to RFC3986, and written with no internal whitespace.
         *
         * @return The record's `WARC-Target-URI` header field, as a string.
         */
        public String getTargetURI()
        {
            return fields.get("WARC-Target-URI");
        }

        /**
         * The number of bytes in the body of the record, similar to RFC2616.
         *
         * @return The record's `Content-Length` header field, parsed into an int.
         * @throws IOException if header is malformed
         */
        public int getContentLength()
                throws IOException
        {
            String lengthStr = fields.get("Content-Length");
            if (lengthStr == null)
                throw new IOException("Missing Content-Length header");
            try {
                return Integer.parseInt(lengthStr);
            }
            catch (NumberFormatException e) {
                throw new IOException("Malformed Content-Length header: " + lengthStr);
            }
        }

        /**
         * Returns the value of a selected header field, or null if there is no header with
         * that field name.
         *
         * @param field The name of the header to return (case-sensitive).
         * @return The value associated with that field name, or null if not present.
         */
        public String getField(String field)
        {
            return fields.get(field);
        }

        /**
         * Sets the value of the field; only single-line values are allowed
         *
         * @param field field name
         * @param value value (can be null)
         * @throws IOException if the field value is not null and contains new lines
         */
        public void setField(String field, String value)
                throws IOException
        {
            if (value != null && (value.contains("\n") || value.contains("\r"))) {
                throw new IOException("Field value contains new lines");
            }

            fields.put(field, value);
        }

        /**
         * Appends this header to a {@link DataOutput} stream, in WARC/1.0 format.
         *
         * @param out The data output to which the header should be written.
         * @throws IOException I/O exception
         */
        public void write(DataOutput out)
                throws IOException
        {
            out.write(toString().getBytes("UTF-8"));
        }

        /**
         * Formats this header in WARC/1.0 format, consisting of a version line followed
         * by colon-delimited key-value pairs, and `\r\n` line endings.
         */
        @Override
        public String toString()
        {
            StringBuilder buf = new StringBuilder();
            buf.append(WARC_VERSION);
            buf.append(CR_LF);
            for (Map.Entry<String, String> field : fields.entrySet()) {
                buf.append(field.getKey());
                buf.append(": ");
                buf.append(field.getValue());
                buf.append(CR_LF);
            }
            return buf.toString();
        }
    }

    /**
     * Constants for additional project-specific fields in {@linkplain WARCRecord.Header#getField(String)}
     */
    public static class WARCRecordFieldConstants
    {
        /**
         * Whether boilerplate has been removed
         */
        public static final String NO_BOILERPLATE = "c4_noBoilerplate";

        /**
         * Detected language
         */
        public static final String LANGUAGE = "c4_language";

        /**
         * Detected license
         */
        public static final String LICENSE = "c4_license";

        /**
         * Detected register
         */
        public static final String REGISTER = "c4_register";

        /**
         * Sim hash
         */
        public static final String SIMHASH = "c4_simhash";

        /**
         * Original location
         */
        public static final String ORIGINAL_LOCATION = "c4_originalLocation";

        /**
         * Boolean tag for minimal html
         */
        public static final String MINIMAL_HTML = "c4_minimalHtml";
    }
}
