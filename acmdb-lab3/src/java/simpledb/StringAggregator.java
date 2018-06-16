package simpledb;
import java.util.ArrayList;
/**
 * Knows how to compute some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;
	private int gbfield, afield;
    private Type gbfieldtype;
    private Op what;

    private ArrayList<Field> gbFieldList;
    private ArrayList<Integer> cList;

    /**
     * Aggregate constructor
     * @param gbfield the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield the 0-based index of the aggregate field in the tuple
     * @param what aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */

    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // some code goes here
        this.gbfield = gbfield;
        this.afield = afield;
        this.gbfieldtype = gbfieldtype;
        this.what = what;
        gbFieldList = new ArrayList<Field>();
        cList = new ArrayList<Integer>();
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // some code goes here
        int index;
        if (gbfield == NO_GROUPING) index = 0;
        else index = gbFieldList.indexOf(tup.getField(gbfield));
        if (index == -1) index = gbFieldList.size();
        if (index == cList.size()) {
            if (gbfield != NO_GROUPING) gbFieldList.add(tup.getField(gbfield));
            cList.add(1);
            return;
        }
        int c_prev = cList.get(index);
        switch (what) {
            case COUNT:
                cList.set(index, c_prev + 1);
                break;
            default:
                assert false : "Unsupported Aggregator.Op in lab2.";
        }
    }

    /**
     * Create a DbIterator over group aggregate results.
     *
     * @return a DbIterator whose tuples are the pair (groupVal,
     *   aggregateVal) if using group, or a single (aggregateVal) if no
     *   grouping. The aggregateVal is determined by the type of
     *   aggregate specified in the constructor.
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
                case COUNT:
                    v = cList.get(0);
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
            for (int i = 0; i < cList.size(); i ++) {
                Tuple t = new Tuple(td);
                int v = 0;
                switch (what) {
                    case COUNT:
                        v = cList.get(i);
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
