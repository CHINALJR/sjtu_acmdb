package simpledb;
import java.util.ArrayList;
/**
 * Knows how to compute some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;
	private int gbfield, afield;
    private Type gbfieldtype;
    private Op what;
    private ArrayList<Field> gbFieldList;
    private ArrayList<Integer> vList, cList;
    /**
     * Aggregate constructor
     * 
     * @param gbfield
     *            the 0-based index of the group-by field in the tuple, or
     *            NO_GROUPING if there is no grouping
     * @param gbfieldtype
     *            the type of the group by field (e.g., Type.INT_TYPE), or null
     *            if there is no grouping
     * @param afield
     *            the 0-based index of the aggregate field in the tuple
     * @param what
     *            the aggregation operator
     */

    public IntegerAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        this.gbfield = gbfield;
        this.afield = afield;
        this.gbfieldtype = gbfieldtype;
        this.what = what;
        gbFieldList = new ArrayList<Field>();
        vList = new ArrayList<Integer>();
        cList = new ArrayList<Integer>();
        // some code goes here
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the
     * constructor
     * 
     * @param tup
     *            the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        int index;
        if (gbfield == NO_GROUPING) index = 0;
        else index = gbFieldList.indexOf(tup.getField(gbfield));
        if (index == -1) index = gbFieldList.size();
        int v = ((IntField)tup.getField(afield)).getValue();
        if (index == vList.size()) {
            if (gbfield != NO_GROUPING) gbFieldList.add(tup.getField(gbfield));
            vList.add(v);
            cList.add(1);
            return;
        }
        int v_prev = vList.get(index);
        int c_prev = cList.get(index);
        switch (what) {
            case MIN:
                if (v < v_prev) vList.set(index, v);
                break;
            case MAX:
                if (v > v_prev) vList.set(index, v);
                break;
            case SUM:
            case AVG:
            case COUNT:
                vList.set(index, v + v_prev);
                cList.set(index, c_prev + 1);
                break;
            default:
                break;
        }
        // some code goes here
    }

    /**
     * Create a DbIterator over group aggregate results.
     * 
     * @return a DbIterator whose tuples are the pair (groupVal, aggregateVal)
     *         if using group, or a single (aggregateVal) if no grouping. The
     *         aggregateVal is determined by the type of aggregate specified in
     *         the constructor.
     */

    public DbIterator iterator() {
        // some code goes here
        ArrayList<Tuple> tList = new ArrayList<Tuple>();
        TupleDesc td = null;
        if (gbfield == NO_GROUPING) {
            Type[] tps = new Type[1];
            tps[0] = Type.INT_TYPE;
            td = new TupleDesc(tps);
            Tuple t = new Tuple(td);
            int v = 0;
            switch (what) {
                case MIN:
                case MAX:
                case SUM:
                    v = vList.get(0);
                    break;
                case COUNT:
                    v = cList.get(0);
                    break;
                case AVG:
                    v = vList.get(0)/cList.get(0);
                    break;
                default:
                    break;
            }
            t.setField(0, new IntField(v));
            tList.add(t);
        } else {
            Type[] tps = new Type[2];
            tps[0] = gbfieldtype;
            tps[1] = Type.INT_TYPE;
            td = new TupleDesc(tps);
            for (int i = 0; i < vList.size(); i ++) {
                Tuple t = new Tuple(td);
                int v = 0;
                switch (what) {
                    case MIN:
                    case MAX:
                    case SUM:
                        v = vList.get(i);
                        break;
                    case COUNT:
                        v = cList.get(i);
                        break;
                    case AVG:
                        v = vList.get(i)/cList.get(i);
                        break;
                    default:
                        break;
                }
                t.setField(0, gbFieldList.get(i));
                t.setField(1, new IntField(v));
                tList.add(t);
            }
        }
        return new TupleIterator(td, tList);
 
    }

}
