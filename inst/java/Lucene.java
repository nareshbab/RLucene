import java.io.File;
import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.lucene.search.vectorhighlight.FragListBuilder;
import org.apache.lucene.search.vectorhighlight.FragmentsBuilder;
import org.apache.lucene.search.vectorhighlight.SimpleFragListBuilder;
import org.apache.lucene.search.vectorhighlight.SimpleFragmentsBuilder;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.json.JSONObject;

@SuppressWarnings("deprecation")
public class Lucene {
	public Analyzer SA;
	public Version version;
	public Directory dirIndex;
	public Directory writeIndex;
	public RAMDirectory index;
	public IndexReader reader;
	public IndexWriter writer;
	public FragListBuilder fragListBuilder;
	public FragmentsBuilder fragmentsBuilder;
	public FastVectorHighlighter highlighter;
	public IndexWriterConfig config;
	public IndexSearcher searcher;
	String[] fields = {"notebook_id","description","created_at","updated_at","content","starcount","avatar_url","user_url","commited_at","user"};
	public static String[] pretag = {"<b style='background:yellow'>"}; 
	public static String[] posttag = {"</b>"}; 
	public MultiFieldQueryParser multiparser;
	boolean unmapHack = true;
	public static void main (String[] args) throws Exception {
	}

	public Lucene(String[] path) throws IOException{ 
		version = Version.LUCENE_47;
		SA = new StandardAnalyzer(version);
		MMapDirectory index = new MMapDirectory(new File(path[0]));
		try {
      			index.setUseUnmap(unmapHack);
		 } catch (Exception e) {
      			System.out.println("Unmap not supported on this JVM, continuing on without setting unmap\n" + e);
    		}
		writeIndex = FSDirectory.open(new File(path[1]));
		reader = DirectoryReader.open(index);
		searcher = new IndexSearcher(reader);
		config = new IndexWriterConfig(version, SA);
		writer = new IndexWriter(writeIndex, config);
		multiparser = new MultiFieldQueryParser(version, fields, SA);
		fragListBuilder = new SimpleFragListBuilder();
		fragmentsBuilder = new SimpleFragmentsBuilder(pretag, posttag);	
		highlighter = new FastVectorHighlighter(true, true, fragListBuilder, fragmentsBuilder);

	}

	public String getResults(String query) throws Exception{
		Query q = multiparser.parse(query);
		TopDocs hits = searcher.search(q,1000);
		FieldQuery fq = highlighter.getFieldQuery(q);
		JSONObject json = new JSONObject();
		for (ScoreDoc sd : hits.scoreDocs) {
        	Document doc = searcher.doc(sd.doc);
			String frag = highlighter.getBestFragment(fq, reader, sd.doc, "content", 2000000000);
			json.put(doc.get("notebook_id"), frag);
        }
		return json.toString();
	}

	public void indexing(String[] data) throws IOException {
		Document doc = new Document();
		doc.add(new Field("notebook_id", data[0], Store.YES, Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
		doc.add(new Field("description", data[1], Store.YES, Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
		doc.add(new Field("created_at", data[2], Store.YES, Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
		doc.add(new Field("updated_at", data[3], Store.YES, Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
		doc.add(new Field("content", data[4], Store.YES, Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
		doc.add(new Field("starcount", data[5], Store.YES, Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
		doc.add(new Field("avatar_url", data[6], Store.YES, Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
		doc.add(new Field("user_url", data[7], Store.YES, Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
		doc.add(new Field("commited_at", data[8], Store.YES, Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
		doc.add(new Field("user", data[9], Store.YES, Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
		writer.addDocument(doc);
		writer.close();
	}
}