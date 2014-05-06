import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterIterator;
import org.apache.lucene.analysis.pattern.PatternReplaceCharFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
//import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ReferenceManager;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.lucene.search.vectorhighlight.FragListBuilder;
import org.apache.lucene.search.vectorhighlight.FragmentsBuilder;
import org.apache.lucene.search.vectorhighlight.SimpleFragListBuilder;
import org.apache.lucene.search.vectorhighlight.SimpleFragmentsBuilder;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;
import org.json.JSONObject;

@SuppressWarnings("deprecation")
public class Lucene {
	public CustomAnalyzer  analyzer;
	public Version version;
	public IndexReader reader;
	public FragListBuilder fragListBuilder;
	public FragmentsBuilder fragmentsBuilder;
	public FastVectorHighlighter highlighter;
	//public IndexSearcher searcher;
	public MultiFieldQueryParser multiparser;
	String[] fields = {"notebook_id","description","created_at","updated_at","content","starcount","avatar_url","user_url","commited_at","user"};
	public static String[] pretag = {"<b style='background:yellow'>"}; 
	public static String[] posttag = {"</b>"}; 
	boolean unmapHack = true;
	public static final String sp = "--";
	public SearcherManager sm;
	public IndexWriter writer;

	public static void main (String[] args) throws Exception { 
		Lucene lucene = new Lucene("F:/vagrant_workspace/open");
		//for (int i=0; i< 50; i++){
		//String[] data = {"26", "notebook", "1995-12-31T23:59:59.999Z", "1995-12-31T23:59:59.999Z", "\nfoo \rnaresh", "1", "google.com", "google.com", "1995-12-31T23:59:59.999Z", "naresh"};	
		//lucene.indexing("F:/vagrant_workspace/open", data);
		//}*/
		//System.out.println(lucene.getResults("naresh"));


	}

	public Lucene(String path) throws Exception{ 
		version = Version.LUCENE_47;
		analyzer = new CustomAnalyzer(version, "standard");
		File Dir = new File(path);
		if(!Dir.exists()) {
			Dir.mkdir();
		}
		createIndexes(path);
		sm = new SearcherManager(MMapDirectory.open(new File(path)), new SearcherFactory());
		multiparser = new MultiFieldQueryParser(version, fields, analyzer);
		fragListBuilder = new SimpleFragListBuilder();
		fragmentsBuilder = new SimpleFragmentsBuilder(pretag, posttag);	
		highlighter = new FastVectorHighlighter(true, true, fragListBuilder, fragmentsBuilder);
	}

	public String getResults(String query) throws IOException, ParseException  {
		try{
			JSONObject jsoncontent = new JSONObject();
			sm.maybeRefresh();
			IndexSearcher s = sm.acquire();
			IndexReader r = s.getIndexReader();
			Query q = multiparser.parse(query);
			TopDocs hits = s.search(q,1000);
			System.out.println(hits.totalHits);
			FieldQuery fq = highlighter.getFieldQuery(q);
			for (ScoreDoc sd : hits.scoreDocs) {
				Document doc = s.doc(sd.doc);
				String frag = highlighter.getBestFragment(fq, r, sd.doc, "content", 2000000000);
				if (frag == null) {
					frag = "[{\"filename\":\"part1.R\",\"content\":[]}]";
				}
				jsoncontent.put(doc.get("notebook_id") +sp+ doc.get("description") +sp+ doc.get("updated_at") +sp+ doc.get("user") +sp+ doc.get("starcount"), frag);
			}
			return jsoncontent.toString();
		} catch (Exception e) {
			System.out.println("Error while Searching" + " " + e);
			return null;
		}
	}


	public void indexing(String path, String[] data) throws IOException {
		try {
			Document doc = createDoc(data);
			Query q = multiparser.parse("notebook_id:"+data[0]);
			sm.maybeRefresh();
			IndexSearcher searcher = sm.acquire();
			TopDocs hits = searcher.search(q, 100);
			IndexWriter w = getIndexWriter(path);
			if (hits.totalHits != 0) {
				w.updateDocument(new Term("notebook_id", data[0]), doc);
				closeIndexWriter(w);
			} else {
				w.addDocument(doc);
				closeIndexWriter(w);				
			}

		} catch (Exception e) {
			System.out.println("Error while creating Indexes" + e);
		}
	}


	private IndexWriter getIndexWriter(String path) {
		try{
			Directory indexDir = FSDirectory.open(new File(path));
			IndexWriterConfig config = new IndexWriterConfig(version, analyzer);
			IndexWriter writer = new IndexWriter(indexDir, config);
			return writer;
		} catch(IOException e){
			System.out.println("Error while creating IndexWriter" + e);
			return null;
		}
	} 

	private void createIndexes(String path) throws Exception{
		try {
			MMapDirectory index = new MMapDirectory(new File(path));
			try{
				index.setUseUnmap(unmapHack);
			} catch (Exception e){
				System.out.println("Unmap not supported on this JVM, continuing on without setting unmap\n" + e);
			}
			reader = DirectoryReader.open(index);
		} catch (IndexNotFoundException e) {
			IndexWriterConfig config = new IndexWriterConfig(version, analyzer);
			config.setOpenMode(OpenMode.CREATE_OR_APPEND);
			Directory indexDir = FSDirectory.open(new File(path));
			try{
				IndexWriter w = new IndexWriter(indexDir, config);
				closeIndexWriter(w);
			} catch (LockObtainFailedException ex){
				IndexWriter.unlock(indexDir);
				IndexWriter w = new IndexWriter(indexDir, config);
				closeIndexWriter(w);
			}
		} 
	}

	private void closeIndexWriter(IndexWriter writer) {
		try {
			writer.commit();
			writer.close();
		} catch (IOException e) {
			System.out.println("Indexer Cannot be closed");
		}
	}

	private Document createDoc(String[] data) {
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
		return doc;
	}

	public void deleteindexes(String path,String...strings)  {
		try {
			IndexWriter w = getIndexWriter(path);
			if (strings.length != 0) {
				w.deleteDocuments(new Term(strings[0], strings[1]));
			} else {
				w.deleteAll();
			}
			closeIndexWriter(w);
		} catch(IOException e){
			System.out.println("Error while deleting Indexes");
		}
	}

	private final class CustomAnalyzer extends Analyzer {
		private Version matchVersion;
		private String name;
		private Pattern pattern1 = Pattern.compile("\n");
		private Pattern pattern2 = Pattern.compile("\r");
		private String replacement = " ";
		final List<String> DelimWords = Arrays.asList(
				"!", "@", "#", "$", "%", "^", "&", "*", "(",
				")", "_", "+", "[", "]", "{",
				"}", ":", "'", ">", "<", "?",
				"/");
		
		public CustomAnalyzer(Version matchVersion, String name) {
			this.matchVersion = matchVersion; 
			this.name = name;
		}

		protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
			if (name == "standard"){
				final Tokenizer source = new StandardTokenizer(matchVersion, reader);
				TokenStream sink = new StandardFilter(matchVersion, source);
				sink = new LowerCaseFilter(matchVersion, sink);
				sink = new StopFilter(matchVersion, sink, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
				return new TokenStreamComponents(source, sink);
			} else {
				final Tokenizer source = new WhitespaceTokenizer(matchVersion, reader);
				TokenStream sink = new StandardFilter(matchVersion, source);
				final CharArraySet DelimSet = new CharArraySet(matchVersion, DelimWords, false);
				sink = new WordDelimiterFilter(sink, 1, DelimSet);
				sink = new LowerCaseFilter(matchVersion, sink);
				sink = new StopFilter(matchVersion, sink, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
				return new TokenStreamComponents(source, sink);
			}
		}

		protected Reader initReader(String fieldName, Reader reader) {
	        Reader p1 = new PatternReplaceCharFilter(pattern1, replacement, reader);
	        return new PatternReplaceCharFilter(pattern2, replacement, p1);
	    }
		
	}

}
