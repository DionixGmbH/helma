# helma.Search

Lucene-based full-text search.

```javascript
app.addRepository("modules/helma/Search.js");
```

## Setup

```javascript
app.addRepository("modules/helma/Search.js");

// Get an analyzer
var analyzer = Search.getAnalyzer("standard");    // also "simple", "whitespace", etc.

// Open a directory
var dir = Search.getDirectory("/var/index", true);       // file-based
var dir = Search.getRAMDirectory();                       // in-memory

// Create or open an index
var index = Search.createIndex(dir, analyzer);             // creates new
var index = new Search.Index(dir, analyzer);               // opens existing
```

## Index Operations

```javascript
// Add documents
var doc = new Search.Document();
doc.addField("id", post._id, { store: true });
doc.addField("title", post.title, { store: true, index: true });
doc.addField("body", post.body, { index: true });
index.addDocument(doc);

// Batch add
index.addDocuments([doc1, doc2, doc3], 10);    // mergeFactor=10

// Update
index.updateDocument(doc, "id");                // by id field

// Remove
index.removeDocument("id", post._id);

// Manage
index.optimize();
index.size();
index.count("title", "hello");
index.isLocked();
index.unlock();
index.close();
```

## Searching

```javascript
var searcher = new Search.Searcher(index);

// Term query
var q = new Search.TermQuery("title", "hello");

// Boolean query (AND/OR/NOT)
var q = new Search.BooleanQuery();
q.addTerm("title", "hello", "must");           // AND
q.addTerm("body", "world", "should");           // OR
q.addTerm("body", "spam", "must_not");          // NOT

// Phrase
var q = new Search.PhraseQuery("body");
q.addTerm("body", "hello");
q.addTerm("body", "world");

// Range
var q = new Search.RangeQuery("date", "2026-01-01", "2026-12-31", true);

// Fuzzy
var q = new Search.FuzzyQuery("title", "helo");    // matches "hello"

// Prefix
var q = new Search.PrefixQuery("title", "hel");

// Wildcard
var q = new Search.WildcardQuery("title", "h*o");

// Search
var hits = searcher.search(q);
hits.size();
for (var i = 0; i < hits.size(); i++) {
    var doc = hits.get(i);
    print(doc.getField("title").getValue());
}

// Sort
searcher.sortBy("created", "string", true);     // reverse-sort by created

// With filter
var filter = new Search.QueryFilter(filterQuery);
var hits = searcher.search(q, filter);
```

## Document API

```javascript
var doc = new Search.Document();

doc.addField(name, value, options);
// options: { store: bool, index: bool, tokenize: bool, termVector: bool, compressed: bool }

doc.getField(name);
doc.getFields(name);
doc.removeField(name);

doc.getBoost();
doc.setBoost(2.0);            // boost this doc in search ranking
```

## Example: Indexing on Save

```javascript
// Post/lifecycle.js
function onPersist() {
    app.addRepository("modules/helma/Search.js");
    var index = app.data.searchIndex;
    if (!index) {
        var analyzer = Search.getAnalyzer("standard");
        var dir = Search.getDirectory("/var/index", true);
        index = new Search.Index(dir, analyzer);
        app.data.searchIndex = index;
    }

    var doc = new Search.Document();
    doc.addField("id", this._id, { store: true });
    doc.addField("title", this.title, { store: true, index: true });
    doc.addField("body", this.body, { index: true });
    index.updateDocument(doc, "id");
}
```

## See Also

- [Apache Lucene 2.2 docs](https://lucene.apache.org/) (Helma bundles `lucene-core-2.2.0.jar`)
- [`jala.IndexManager`](../jala.md) — async incremental index manager built on top
- [`modules/helma/Search.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/helma/Search.js)
