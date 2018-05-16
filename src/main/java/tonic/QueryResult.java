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
            System.out.print(key+"\t\t");
        }
        System.out.println();
        int longestVal = 0;
        for(int i = 0; i < this.values.size(); i++){
            if(this.values.get(i).length() > longestVal && i % this.keys.size() == 0){
                longestVal = this.values.get(i).length();
            }
        }
        int keyAmount = 0;
        for (String val : this.values) {
            if(keyAmount == this.keys.size()){
                System.out.println();
                keyAmount = 0;
            }
            System.out.print(val);
            int valLength = val.length();
            while(valLength < longestVal){
                System.out.print(" ");
                valLength++;
            }
            System.out.print("\t");
            keyAmount++;
        }
        for (String val : this.extraValues) {
            System.out.println(val);
        }
    }
}
