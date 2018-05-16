package tonic;

import java.util.*;

public class QueryResult {

    public List<String> keys;
    public List<String> values;
    public List<String> extraValues;


    public QueryResult() {
        this.keys = new ArrayList<>();
        this.values = new ArrayList<>();
        this.extraValues = new ArrayList<>();
    }

    public void nicePrint() {
        for (String key : this.keys) {
            System.out.print(key+"\t");
        }
        System.out.println();
        int keyAmount = 0;
        for (String val : this.values) {
            if(keyAmount == this.keys.size()){
                System.out.println();
                keyAmount = 0;
            }
            System.out.println(val);
            keyAmount++;
        }
        for (String val : this.extraValues) {
            System.out.println(val);
        }
    }
}
