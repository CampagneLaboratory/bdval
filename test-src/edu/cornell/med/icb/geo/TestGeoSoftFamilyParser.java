/*
 * Copyright (C) 2007-2009 Institute for Computational Biomedicine,
 *                         Weill Medical College of Cornell University
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package edu.cornell.med.icb.geo;

import it.unimi.dsi.lang.MutableString;
import junit.framework.TestCase;

import java.io.StringReader;

/**
 * @author Fabien Campagne
 *         Date: Aug 15, 2007
 *         Time: 1:43:07 PM
 */
public class TestGeoSoftFamilyParser extends TestCase {
    final String input = "^DATABASE = GeoMiame\n" +
            "!Database_name = Gene Expression Omnibus (GEO)\n" +
            "!Database_institute = NCBI NLM NIH\n" +
            "!Database_web_link = http://www.ncbi.nlm.nih.gov/projects/geo\n" +
            "!Database_email = geo@ncbi.nlm.nih.gov\n" +
            "!Database_email = dummy_repeat@ncbi.nlm.nih.gov\n" +
            "!Database_ref = Nucleic Acids Res. 2005 Jan 1;33 Database Issue:D562-6\n" +
            "^PLATFORM = GPL6\n" +
            "!Platform_title = SAGE:10:Sau3A:Homo sapiens\n" +
            "!Platform_geo_accession = GPL6\n" +
            "!Platform_status = Public on Jan 04 2001\n" +
            "!Platform_submission_date = Jan 04 2001\n" +
            "!Platform_last_update_date = May 26 2005\n" +
            "!Platform_technology = SAGE Sau3A\n" +
            "!Platform_distribution = virtual\n" +
            "!Platform_organism = Homo sapiens\n" +
            "!Platform_contact_name = ,,GEO\n" +
            "!Platform_contact_address =\n" +
            "!Platform_sample_id = GSM10419\n" +
            "!Platform_sample_id = GSM10423\n" +
            "!Platform_sample_id = GSM10424\n" +
            "!Platform_sample_id = GSM10425\n" +
            "!Platform_sample_id = GSM10426\n" +
            "!Platform_sample_id = GSM10427\n" +
            "!Platform_sample_id = GSM10428\n" +
            "!Platform_sample_id = GSM10429\n" +
            "!Platform_sample_id = GSM32698\n" +
            "!Platform_sample_id = GSM32699\n" +
            "!Platform_sample_id = GSM32700\n" +
            "!Platform_sample_id = GSM36126\n" +
            "!Platform_sample_id = GSM36127\n" +
            "!Platform_sample_id = GSM151619\n" +
            "!Platform_sample_id = GSM151622\n" +
            "!Platform_series_id = GSE694\n" +
            "!Platform_series_id = GSE1865\n" +
            "!Platform_series_id = GSE1866\n" +
            "!Platform_series_id = GSE2017\n" +
            "!Platform_series_id = GSE6587\n" +
            "!Platform_data_row_count = 0\n" +
            "#ID = SAGE tag\n" +
            "#GI = best gi for the tag\n" +
            "!platform_table_begin\n" +
            "probeId1\texternalID1\t\n" +
            "probeId2\texternalID2\t\n" +
            "!platform_table_end\n" +
            "^SAMPLE = GSM10419\n" +
            "!Sample_title = Kidney glomerulus\n" +
            "!Sample_geo_accession = GSM10419\n" +
            "!Sample_status = Public on Nov 08 2003\n" +
            "!Sample_submission_date = Sep 23 2003\n" +
            "!Sample_last_update_date = May 28 2005\n" +
            "!Sample_type = SAGE\n" +
            "!Sample_anchor = Sau3A\n" +
            "!Sample_tag_length = 10\n" +
            "!Sample_tag_count = 44334\n" +
            "!Sample_channel_count = 1\n" +
            "!Sample_source_name_ch1 = microdissected glomeruli\n" +
            "!Sample_organism_ch1 = Homo sapiens\n" +
            "!Sample_molecule_ch1 = total RNA\n" +
            "!Sample_description = Library generated from microdissected kidney glomeruli obtained from the healthy pole of cancer\n" +
            "ous kidneys. Donors were from either sex. The library was generated using Sau3A I as anchoring enzyme. Data are provi\n" +
            "ded after removal of linkers derived sequences\n" +
            "!Sample_description = Keywords = microdissection\n" +
            "!Sample_description = Keywords = nephron\n" +
            "!Sample_description = Keywords = SADE\n" +
            "!Sample_platform_id = GPL6\n" +
            "!Sample_contact_name = Jean-Marc,,Elalouf\n" +
            "!Sample_contact_email = elalouf@dsvidf.cea.fr\n" +
            "!Sample_contact_phone = (33) 1 69088022\n" +
            "!Sample_contact_fax = (33) 1 69084712\n" +
            "!Sample_contact_laboratory = Laboratoire de PhysioGénomique\n" +
            "!Sample_contact_department = Département de Biologie Joliot-Curie\n" +
            "!Sample_contact_institute = CEA Saclay\n" +
            "!Sample_contact_address =\n" +
            "!Sample_contact_city = 91191 Gif/ Yvette Cedex\n" +
            "!Sample_contact_zip/postal_code = 91191\n" +
            "!Sample_contact_country = France\n" +
            "!Sample_supplementary_file = NONE\n" +
            "!Sample_series_id = GSE694\n" +
            "!Sample_data_row_count = 20525\n" +
            "#TAG = Tag sequence LINK_PRE:\"http://www.ncbi.nlm.nih.gov/SAGE/index.cgi?cmd=tagsearch&anchor=SAU3A&org=Hs&tag=\"\n" +
            "#COUNT = Tag abundance\n" +
            "!sample_table_begin\n" +
            "TAG     COUNT\n" +
            "probeId1      522\n" +
            "probeId2      277\n" +
            "!sample_table_end\n" +
            "^SAMPLE = GSM10423\n" +
            "!Sample_title = Kidney proximal convoluted tubule\n" +
            "!Sample_geo_accession = GSM10423\n" +
            "!Sample_status = Public on Nov 08 2003\n" +
            "!Sample_submission_date = Sep 24 2003\n" +
            "!Sample_last_update_date = May 28 2005\n" +
            "!Sample_type = SAGE\n" +
            "!Sample_anchor = Sau3A\n" +
            "!Sample_tag_length = 10\n" +
            "!Sample_tag_count = 45094\n" +
            "!Sample_channel_count = 1\n" +
            "!Sample_source_name_ch1 = microdissected proximal conv";

    public void testSections() {
        final GeoSoftFamilyParser parser = new GeoSoftFamilyParser(new StringReader(input));

        assertTrue(parser.skipToDatabaseSection());
        assertEquals(new MutableString("GeoMiame"), parser.getSectionAttribute());
        assertTrue(parser.skipToPlatformSection());
        assertEquals(new MutableString("GPL6"), parser.getSectionAttribute());
        assertTrue(parser.skipToSampleSection());
        assertEquals(new MutableString("GSM10419"), parser.getSectionAttribute());
        assertTrue(parser.skipToSampleSection());
        assertEquals(new MutableString("GSM10423"), parser.getSectionAttribute());
        assertFalse(parser.skipToSampleSection());
        assertTrue(parser.isEOF());
    }

    public void testSectionProperties() {
        final GeoSoftFamilyParser parser = new GeoSoftFamilyParser(new StringReader(input));
        assertTrue(parser.skipToDatabaseSection());
        assertEquals(new MutableString("GeoMiame"), parser.getSectionAttribute());
        final SectionProperties props = parser.parseSectionProperties();
        assertNotNull(props);
        assertEquals(new MutableString("Gene Expression Omnibus (GEO)"), props.getUniqueValue("name"));
        assertEquals(new MutableString("NCBI NLM NIH"), props.getUniqueValue("institute"));
        assertEquals(new MutableString("http://www.ncbi.nlm.nih.gov/projects/geo"), props.getUniqueValue("web_link"));
        assertEquals(null, props.getUniqueValue("email"));
        assertEquals(2, props.size("email"));
    }

    public void testPlatform() {
        final GeoSoftFamilyParser parser = new GeoSoftFamilyParser(new StringReader(input));
        assertTrue(parser.skipToPlatformSection());
        final GEOPlatformIndexed platform = parser.parsePlatform();
        assertEquals(2, platform.getNumProbeIds());
        assertEquals(0, platform.getExternalIndexForProbeId(new MutableString("probeId1")));
        assertEquals(1, platform.getExternalIndexForProbeId(new MutableString("probeId2")));
        assertEquals(0, platform.getProbeIds().getInt(new MutableString("probeId1")));
        assertEquals(1, platform.getProbeIds().getInt(new MutableString("probeId2")));
        assertTrue(parser.skipToSampleSection());
        final ParseSAGECountsSampleDataCallback callback = new ParseSAGECountsSampleDataCallback(platform);
        parser.parseSampleData(platform, callback);
        assertEquals(2, callback.getParsedData().count.length);

        assertEquals(522, callback.getParsedData().count[0]);
        assertEquals(277, callback.getParsedData().count[1]);
    }

    public void testPlatform2() {
        final GeoSoftFamilyParser parser = new GeoSoftFamilyParser(new StringReader(input));

        assertTrue(parser.skipToDatabaseSection());
        assertTrue(parser.skipToPlatformSection());
        parser.parseSectionProperties();
        final GEOPlatformIndexed platform = parser.parsePlatform();
        assertEquals(2, platform.getNumProbeIds());
        assertEquals(0, platform.getExternalIndexForProbeId(new MutableString("probeId1")));
        assertEquals(1, platform.getExternalIndexForProbeId(new MutableString("probeId2")));
        assertEquals(0, platform.getProbeIds().getInt(new MutableString("probeId1")));
        assertEquals(1, platform.getProbeIds().getInt(new MutableString("probeId2")));
        assertTrue(parser.skipToSampleSection());
        final ParseSAGECountsSampleDataCallback callback = new ParseSAGECountsSampleDataCallback(platform);
        parser.parseSampleData(platform, callback);
        assertEquals(2, callback.getParsedData().count.length);

        assertEquals(522, callback.getParsedData().count[0]);
        assertEquals(277, callback.getParsedData().count[1]);
    }

    final String input2 = "^DATABASE = GeoMiame\n" +
            "!Database_name = Gene Expression Omnibus (GEO)\n" +
            "!Database_institute = NCBI NLM NIH\n" +
            "!Database_web_link = http://www.ncbi.nlm.nih.gov/projects/geo\n" +
            "!Database_email = geo@ncbi.nlm.nih.gov\n" +
            "!Database_ref = Nucleic Acids Res. 2005 Jan 1;33 Database Issue:D562-6\n" +
            "^PLATFORM = GPL1443\n" +
            "!Platform_title = Massively Parallel Signature Sequencing - MPSS (classic method)\n" +
            "!Platform_geo_accession = GPL1443\n" +
            "!Platform_status = Public on Nov 29 2005\n" +
            "!Platform_submission_date = Sep 10 2004\n" +
            "!Platform_last_update_date = May 31 2005\n" +
            "!Platform_technology = MPSS\n" +
            "!Platform_distribution = virtual\n" +
            "!Platform_organism = Homo sapiens\n" +
            "!Platform_description = Sequencing from the 3' most DpnII site from mRNA using Lynx's MPSS technology.\n" +
            "!Platform_description = MPSS allows a comprehensive survey of all expressed transcripts in a cell or tissue, without apriori knowledge.\n" +
            "!Platform_description = This dataset was annotated using a combination of LocusLink, Unigene (build 171) and the latest human build as provided from UCSC (hg1\n" +
            "6).\n" +
            "!Platform_description = Annotation Classe Table:\n" +
            "!Platform_description = Virtual Signature Class    mRNA Orientation                        Poly-Adenylation Features     Position\n" +
            "!Platform_description = 0                          Either - Repeat Warning                 Not applicable                Not applicable\n" +
            "!Platform_description = 1                          Forward Strand                          Poly-A Signal, Poly-A Tail    3' most\n" +
            "!Platform_description = 2                          Forward Strand                          Poly-A Signal                 3' most\n" +
            "!Platform_description = 3                          Forward Strand                          Poly-A Tail                   3' most\n" +
            "!Platform_description = 4                          Forward Strand                          None                          3' most\n" +
            "!Platform_description = 5                          Forward Strand                          None                          Not 3' most\n" +
            "!Platform_description = 6                          Forward Strand                          Internal Poly-A               Not 3' most\n" +
            "!Platform_description = 11                         Reverse Strand                          Poly-A Signal, Poly-A Tail    5' most\n" +
            "!Platform_description = 12                         Reverse Strand                          Poly-A Signal                 5' most\n" +
            "!Platform_description = 13                         Reverse Strand                          Poly-A Tail                   5' most\n" +
            "!Platform_description = 14                         Reverse Strand                          None                          5' most\n" +
            "!Platform_description = 15                         Reverse Strand                          None                          Not 5' most\n" +
            "!Platform_description = 16                         Reverse Strand                          Internal Poly-A               Not 5' most\n" +
            "!Platform_description = 22                         Unknown                                 Poly-A Signal                 Last before signal\n" +
            "!Platform_description = 23                         Unknown                                 Poly-A Tail                   Last before tail\n" +
            "!Platform_description = 24                         Unknown                                 None                          Last in sequence\n" +
            "!Platform_description = 25                         Unknown                                 None                          Not last\n" +
            "!Platform_description = 26                         Unknown                                 Internal Poly-A               Not 3' most\n" +
            "!Platform_description = 1000                       Unknown- Derived from Genomic Sequence  Not applicable                Not applicable\n" +
            "!Platform_description = Keywords = MPSS, mRNA sequencing, tags\n" +
            "!Platform_contributor = Christian,D,Haudenschild\n" +
            "!Platform_contributor = Daixing,,Zhou\n" +
            "!Platform_contributor = Irina,,Khrebtukova\n" +
            "!Platform_contributor = Tom,,Vasicek\n" +
            "!Platform_contact_name = Lynx,,Lynx\n" +
            "!Platform_contact_email = tvasicek@lynxgen.com\n" +
            "!Platform_contact_phone = 510-670-9300\n" +
            "!Platform_contact_department =  \n" +
            "!Platform_contact_institute = Lynx Therapeutics, Inc.\n" +
            "!Platform_contact_address = 25861 Industrial Blvd.\n" +
            "!Platform_contact_city = Hayward\n" +
            "!Platform_contact_state = CA\n" +
            "!Platform_contact_zip/postal_code = 94545\n" +
            "!Platform_contact_country = USA\n" +
            "!Platform_contact_web_link = http://www.lynxgen.com/wt/tert.php3?page_name=mpss\n" +
            "!Platform_sample_id = GSM30478\n" +
            "!Platform_sample_id = GSM30479\n" +
            "!Platform_sample_id = GSM30480";

    public void testPlatform3() {
        final GeoSoftFamilyParser parser = new GeoSoftFamilyParser(new StringReader(input2));

        assertTrue("Must find the database section.",parser.skipToDatabaseSection());
        assertFalse("No series to find in input2", parser.skipToSeriesSection());
        assertFalse("Cannot find the platform section because we already passed it when looking for the non-existent Series section..",parser.skipToPlatformSection());

    }

}
