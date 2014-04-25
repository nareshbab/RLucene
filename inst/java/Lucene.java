import java.io.File;
import java.io.IOException;
import org.apache.lucene.document.TextField;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.json.JSONObject;

public class Lucene {
	public Analyzer SA;
	public Version version;
	public Directory dirIndex;
	public Directory writeIndex;
	public RAMDirectory index;
	public IndexReader reader;
	public IndexWriter writer;
	public IndexWriterConfig config;
	public IndexSearcher searcher;
	public Formatter shtml;
	String[] fields = {"notebook_id","description","created_at","updated_at","content","starcount","avatar_url","user_url","commited_at","user"};
	public MultiFieldQueryParser multiparser;
	public static void main (String[] args) throws Exception {
		
	}
	
	public Lucene(String[] path) throws IOException{ 
		version = Version.LUCENE_47;
		SA = new StandardAnalyzer(version);
		dirIndex = FSDirectory.open(new File(path[0]));
		writeIndex = FSDirectory.open(new File(path[1]));
		config = new IndexWriterConfig(version, SA);
		writer = new IndexWriter(writeIndex, config);
		index = new RAMDirectory(dirIndex, new IOContext());
		reader = DirectoryReader.open(index);
		searcher = new IndexSearcher(reader);
		multiparser = new MultiFieldQueryParser(version, fields, SA);
		shtml = new SimpleHTMLFormatter("<b style='background:yellow'>", "</b>");
	}
	public String getResults(String query) throws Exception{
		Query q = multiparser.parse(query);
		TopDocs hits = searcher.search(q,1000);
		QueryScorer scorer = new QueryScorer(q, "content");
        Highlighter highlighter = new Highlighter(shtml, scorer);
        highlighter.setMaxDocCharsToAnalyze(900000000);
        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer);
        highlighter.setTextFragmenter(fragmenter);
        JSONObject json = new JSONObject();
        for (ScoreDoc sd : hits.scoreDocs) {
        	Document doc = searcher.doc(sd.doc);
        	String content = doc.get("content");
            TokenStream stream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), sd.doc, "content", doc, SA);
            TextFragment[] frag = highlighter.getBestTextFragments(stream, content, true, 10000000);
            String x=null;
            if ((frag[0] != null) && (frag[0].getScore() > 0)) {
                     x = frag[0].toString();
            }
            json.put(doc.get("notebook_id"),x);
        }
		return json.toString();
	}
	
	public void indexing(String[] data) throws Exception {
		addDoc(writer, data);
		writer.close();
	}
	
	public static void addDoc(IndexWriter writer, String[] data) throws IOException {
		  Document doc = new Document();
		  doc.add(new StringField("notebook_id", data[0], Store.YES));
		  doc.add(new TextField("description", data[1], Store.YES));
		  doc.add(new TextField("created_at", data[2], Store.YES));
		  doc.add(new TextField("updated_at", data[3], Store.YES));
		  doc.add(new TextField("content", data[4], Store.YES));
		  doc.add(new StringField("starcount", data[5], Store.YES));
		  doc.add(new TextField("avatar_url", data[6], Store.YES));
		  doc.add(new TextField("user_url", data[7], Store.YES));
		  doc.add(new TextField("commited_at", data[8], Store.YES));
		  doc.add(new TextField("user", data[9], Store.YES));
		  writer.addDocument(doc);
	}
}
