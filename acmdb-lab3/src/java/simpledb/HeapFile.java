package simpledb;

import java.io.*;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 * 
 * @see simpledb.HeapPage#HeapPage
 * @author Sam Madden
 */
public class HeapFile implements DbFile {
    private File _file;
    private TupleDesc _td;
    private int tableId;
    /**
     * Constructs a heap file backed by the specified file.
     * 
     * @param f
     *            the file that stores the on-disk backing store for this heap
     *            file.
     */
    public HeapFile(File f, TupleDesc td) {
        _file = f;
	this.tableId = f.getAbsoluteFile().hashCode();
        _td = td;

        // some code goes here
    }

    /**
     * Returns the File backing this HeapFile on disk.
     * 
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        // some code goes here
        return _file;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     * 
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        return tableId;
        // some code goes here
        //throw new UnsupportedOperationException("implement this");
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     * 
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        return _td;
        //throw new UnsupportedOperationException("implement this");
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) {
        if (!(pid instanceof HeapPageId))
            throw new IllegalArgumentException("only accept HeapPageId");

        byte[] data = new byte[BufferPool.pageSize];
        RandomAccessFile raf = null;

        try {
            raf = new RandomAccessFile(_file, "r");
            raf.seek(BufferPool.pageSize * pid.pageNumber());
            raf.readFully(data);
            return new HeapPage((HeapPageId) pid, data);

        } catch (IOException e) {   // better throw IOException
            e.printStackTrace();
            System.exit(1);

        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        // some code goes here
        return null;
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        // some code goes here
        // not necessary for lab1
	
        RandomAccessFile raf = new RandomAccessFile(_file, "rw");
        raf.seek(BufferPool.getPageSize() * page.getId().pageNumber());
        raf.write(page.getPageData());
        raf.close();
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        // some code goes here
        //return 0;
    	return (int)Math.ceil(_file.length()/BufferPool.pageSize);

    }

    // see DbFile.java for javadocs
    public ArrayList<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        if (!_td.equals(t.getTupleDesc())) throw new DbException("TupleDesc does not match.");
        int i = 0;
        HeapPage hp = null;
        for (i = 0; i < numPages(); i ++) {
            if (((HeapPage)(Database.getBufferPool().getPage(
                                tid, new HeapPageId(tableId, i), Permissions.READ_ONLY))).getNumEmptySlots() > 0)
                break;
        }
		System.out.println("Test Shit");
        if (i == numPages()) {
            
            synchronized(this) {
                i = numPages();
                // All files are full
                hp = new HeapPage(new HeapPageId(tableId, i), HeapPage.createEmptyPageData());
                try {
                    int pageSize = BufferPool.getPageSize();
                    byte[] byteStream = hp.getPageData();
                    RandomAccessFile raf = new RandomAccessFile(_file, "rw");
                    raf.seek(pageSize * i);
                    raf.write(byteStream);
                    raf.close();
                }
                catch (IOException e) {
                    throw e;
                }
            }
        }
        hp = (HeapPage)(Database.getBufferPool().getPage(tid, new HeapPageId(tableId, i), Permissions.READ_WRITE));
        hp.insertTuple(t);
        //System.out.println(tid.toString() + "    " + ((IntField)(t.getField(0))).getValue());
        ArrayList<Page> pList = new ArrayList<Page>();
        pList.add(hp);
        return pList;
        // not necessary for lab1
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        // some code goes here
        //return null;
        // not necessary for lab1
		if (tableId != t.getRecordId().getPageId().getTableId()) throw new DbException("Table Id does not match.");
        int pageno = t.getRecordId().getPageId().pageNumber();
        if (pageno < 0 || pageno >= numPages()) throw new DbException("Page number is illegal.");
        HeapPage hp = (HeapPage)(Database.getBufferPool().getPage(tid, t.getRecordId().getPageId(), Permissions.READ_WRITE));
        hp.deleteTuple(t);
        //System.out.println(tid.toString() + "   " + ((IntField)(t.getField(0))).getValue());
        ArrayList<Page> pList = new ArrayList<Page>();
        pList.add(hp);
        return pList;
    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        // some code goes here
        return new HeapFileIterator(tid, this);
    }
    public class HeapFileIterator implements DbFileIterator {
    private TransactionId _tid;
    private int _tableId;
    private int _numPages;
    private BufferPool _bufferPool;

    private int _pageNo;
    private HeapPage _page;
    private Iterator<Tuple> _tuples; // tuples iterator of _page

    private boolean _isOpen;

    public HeapFileIterator(TransactionId tid, HeapFile heapFile) {
        _tid = tid;
        _tableId = heapFile.getId();
        _numPages = heapFile.numPages();
        _bufferPool = Database.getBufferPool();
        _isOpen = false;
    }

    @Override
    public void open() throws DbException, TransactionAbortedException {
        _pageNo = 0;
        _page = (HeapPage) _bufferPool.getPage(_tid,
                                               new HeapPageId(_tableId, _pageNo),
                                               Permissions.READ_ONLY);
        _tuples = _page.iterator();
        _isOpen = true;
    }

    @Override
    public boolean hasNext() throws DbException, TransactionAbortedException {
        if (!_isOpen)
            return false;

        if (_tuples.hasNext())
            return true;

        while (_pageNo < _numPages - 1) {   // has more pages
            // read next page
            _pageNo++;
            _page = (HeapPage) _bufferPool.getPage(_tid,
                                                   new HeapPageId(_tableId, _pageNo),
                                                   Permissions.READ_ONLY);
            _tuples = _page.iterator();
            if (_tuples.hasNext()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
        if (!hasNext())
            throw new NoSuchElementException("no more tuples");
        return _tuples.next();
    }

    @Override
    public void rewind() throws DbException, TransactionAbortedException {
        this.close();
        this.open();
    }

    @Override
    public void close() {
        _isOpen = false;
    }
}

}

