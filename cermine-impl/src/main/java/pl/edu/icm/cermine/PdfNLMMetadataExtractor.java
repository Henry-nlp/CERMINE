package pl.edu.icm.cermine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import pl.edu.icm.cermine.evaluation.tools.PdfNlmIterator;
import pl.edu.icm.cermine.evaluation.tools.PdfNlmPair;
import pl.edu.icm.cermine.exception.AnalysisException;
import pl.edu.icm.cermine.metadata.EnhancerMetadataExtractor;
import pl.edu.icm.cermine.metadata.MetadataExtractor;
import pl.edu.icm.cermine.structure.HierarchicalReadingOrderResolver;
import pl.edu.icm.cermine.structure.ReadingOrderResolver;
import pl.edu.icm.cermine.structure.SVMInitialZoneClassifier;
import pl.edu.icm.cermine.structure.SVMMetadataZoneClassifier;
import pl.edu.icm.cermine.structure.ZoneClassifier;
import pl.edu.icm.cermine.structure.model.BxDocument;
import pl.edu.icm.cermine.structure.tools.BxModelUtils;
import pl.edu.icm.cermine.tools.classification.general.DirExtractor;
import pl.edu.icm.cermine.tools.classification.svm.SVMZoneClassifier;


/**
 * NLM-based metadata extractor from PDF files.
 *
 * @author Dominika Tkaczyk
 */
public class PdfNLMMetadataExtractor implements DocumentMetadataExtractor<Element> {

    /** geometric structure extractor */
    private DocumentStructureExtractor strExtractor;
   
    /** metadata zone classifier */
    private ZoneClassifier metadataClassifier;

    /** metadata extractor from labelled zones */
    private MetadataExtractor<Element> extractor;

    public PdfNLMMetadataExtractor() throws AnalysisException {
        strExtractor = new PdfBxStructureExtractor();
        metadataClassifier = new SVMMetadataZoneClassifier();
        extractor = new EnhancerMetadataExtractor();
    }
    
    public PdfNLMMetadataExtractor(InputStream metadataModel, InputStream metadataRange) throws AnalysisException, IOException {
        strExtractor = new PdfBxStructureExtractor();

        BufferedReader metaModelFileReader = new BufferedReader(new InputStreamReader(metadataModel));
        BufferedReader metaRangeFileReader = new BufferedReader(new InputStreamReader(metadataRange));
        metadataClassifier = new SVMMetadataZoneClassifier(metaModelFileReader, metaRangeFileReader);
        metaModelFileReader.close();
        metaRangeFileReader.close();

        extractor = new EnhancerMetadataExtractor();
    }

    public PdfNLMMetadataExtractor(DocumentStructureExtractor strExtractor, 
    		ZoneClassifier metadataClassifier, MetadataExtractor<Element> extractor) {
        this.strExtractor = strExtractor;
        this.metadataClassifier = metadataClassifier;
        this.extractor = extractor;
    }
         
    /**
     * Extracts metadata from PDF file and stores it in NLM format.
     * 
     * @param stream
     * @return extracted metadata in NLM format
     * @throws AnalysisException 
     */
    @Override
    public Element extractMetadata(InputStream stream) throws AnalysisException {
        BxDocument doc = strExtractor.extractStructure(stream);
        return extractMetadata(doc);
    }
    
    /**
     * Extracts metadata from PDF file and stores it in NLM format.
     * 
     * @param document
     * @return extracted metadata in NLM format
     * @throws AnalysisException 
     */
    @Override
    public Element extractMetadata(BxDocument document) throws AnalysisException {
        BxDocument doc = metadataClassifier.classifyZones(document);
        return extractor.extractMetadata(doc);
    }

    public void setExtractor(MetadataExtractor<Element> extractor) {
        this.extractor = extractor;
    }

    public void setMetadataClassifier(ZoneClassifier metadataClassifier) {
        this.metadataClassifier = metadataClassifier;
    }

    public void setStrExtractor(DocumentStructureExtractor strExtractor) {
        this.strExtractor = strExtractor;
    }
    
    private static String getXPathValue(Element nlm, String path) throws XPathExpressionException {
    	XPath xPath = XPathFactory.newInstance().newXPath();
        String res = (String) xPath.evaluate(path, nlm,  XPathConstants.STRING);
        if (res != null) {
            res = res.trim();
        }
        return res;
    }
    
    public static void main(String[] args) throws AnalysisException, FileNotFoundException, XPathExpressionException {
        PdfNLMMetadataExtractor metadataExtractor = new PdfNLMMetadataExtractor();
        
        for (PdfNlmPair pair : new PdfNlmIterator(args[0])) {
			Element doc = metadataExtractor.extractMetadata(new FileInputStream(pair.getPdf()));
	    	XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
	    	System.out.println(pair.getPdf().getName());
	    	System.out.println(outputter.outputString(doc));
            String journalTitle = getXPathValue(doc, "article/front//journal-title");
            String publisherName = getXPathValue(doc, "/article/front//publisher-name");
            String articleAbstract = getXPathValue(doc, "/article/front//abstract");
            System.out.println("Got title: " + journalTitle);
            System.out.println("Got publisherName: " + publisherName);
            System.out.println("Got articleAbstract: " + articleAbstract);
			System.out.println(doc.toString());
        }
    }
}