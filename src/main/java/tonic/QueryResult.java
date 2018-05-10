package tonic;

public class QueryResult {

    public String[] keys;
    public String[] values;
    public String[] extraValues;


    public QueryResult() {
        this.keys = new String[0];
        this.values = new String[0];
        this.extraValues = new String[0];
    }

    public void formatResultString(String resS) {
        String[] keysValues = resS.split("#");
        this.keys = keysValues[0].split("§");
        this.values = keysValues[1].split("%");
    }

    public void nicePrint() {
        for (String key : this.keys) {
            System.out.println(key);
        }
        for (String val : this.values) {
            System.out.println(val);
        }
        for (String val : this.extraValues) {
            System.out.println(val);
        }
    }
}
