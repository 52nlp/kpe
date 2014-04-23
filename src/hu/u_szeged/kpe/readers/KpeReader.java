package hu.u_szeged.kpe.readers;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.pipeline.SzTECoreNLP;

public abstract class KpeReader {

  public static SzTECoreNLP sentenceAnalyzer;
  protected Charset m_encoding;
  protected String fileType;
  protected boolean goldAnnotation;

  public abstract List<DocumentData> getContent(String dir, String file);

  public abstract String getText(String file, int numberWithinFile);

  protected abstract boolean mightBeSectionHeader(String line);

  protected abstract void setDetails();

  public KpeReader() {
    m_encoding = Charset.defaultCharset();
    setDetails();
  }

  public KpeReader(String extension) {
    this(Charset.defaultCharset(), extension);
  }

  public KpeReader(Charset encoding, String extension) {
    m_encoding = encoding;
    fileType = extension;
  }

  public void initGrammar(boolean isMweFeatureOn, boolean isNeFeatureOn, boolean isSyntacticFeatureOn) {
    if (sentenceAnalyzer == null) {
      String annotators = "tokenize, ssplit, pos, lemma, stopword, normalize";
      annotators += isMweFeatureOn ? ", mwe" : "";
      annotators += isNeFeatureOn ? ", ner" : "";
      annotators += isSyntacticFeatureOn ? ", parse" : "";

      Properties props = new Properties();
      props.put("annotators", annotators);
      // TODO this is clearly not necessary in all the cases
      // this should be used only when it is desired for new lines to act as sentence boundaries
      props.put("ssplit.boundariesToDiscard", "*NL*");
      props.put("tokenize.options", "invertible,ptb3Escaping=true,tokenizeNLs");
      props.put("pos.maxlen", "100");

      if (isMweFeatureOn) {
        props.put("mwe.file", System.getProperty("user.dir") + "/resources/wikiMWEfreqs.txt");
      }
      if (isNeFeatureOn) {
        props.put("ner.useSUTime", "false");
      }
      if (isSyntacticFeatureOn) {
        props.put("parser.maxlen", "100");
      }
      sentenceAnalyzer = new SzTECoreNLP(props);
    }
  }

  protected void setFileExtension(String ext) {
    fileType = ext;
  }

  public void setUseGoldAnnotation(boolean ga) {
    goldAnnotation = ga;
  }

  public Charset getEncoding() {
    return m_encoding;
  }

  /**
   * @param dir
   *          - directory to search for the documents
   * @param train
   *          - whether the documents will serve as train instances
   */
  public void addDirectoryOfFiles(String dir, boolean train, DocumentSet docSet) {
    for (File f : new File(dir).listFiles()) {
      if (f.getName().endsWith(fileType)) {
        for (DocumentData dd : getContent(dir, f.getAbsolutePath())) {
          if (!train || (dd.getKeyphrases() != null && dd.getKeyphrases().size() > 0)) {
            docSet.add(dd);
          }
        }
      }
    }
  }
}