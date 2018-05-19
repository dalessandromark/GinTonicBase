package tonic;


import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.exceptions.NoSuchRecordException;
import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.DuplicateFormatFlagsException;
import java.util.Iterator;

public class App implements AutoCloseable
{
    private final Driver driver;

    public App( String uri, String user, String password )
    {
        driver = GraphDatabase.driver( uri, AuthTokens.basic( user, password ) );
    }

    @Override
    public void close() throws Exception
    {
        driver.close();
    }
    //Good method, works generally, not just on specific types.
    public QueryResult searchByName(final String NAME, final String TYPE, final boolean REGEXMATCHING) throws NoSuchRecordException{
        final String QUERY;
        if(REGEXMATCHING){
            QUERY = "MATCH (n:"+TYPE+") WHERE n.name=~'(?i)^"+NAME+".*' RETURN n";
        } else {
            QUERY = "MATCH (n:"+TYPE+") WHERE n.name=~'(?i)^"+NAME+"' RETURN n";
        }
        return multiValueQuery(QUERY);
    }

    //This name makes no sense since were searching for ratings and comments.
    public QueryResult getComboRating(final String GIN, final String TONIC, final String GARNISH) throws NoSuchRecordException {
        final String QUERY;
        QueryResult result;

        if (GARNISH.equals("")) {
            QUERY = "Match (g:Gin)-->(c:Combo)<--(t:Tonic), (rat:Rating) --> (c) WHERE g.name=~'(?i)^"+GIN+"' AND t.name=~'(?i)^"+TONIC+"' RETURN rat.rating, rat.comment";
        } else {
            QUERY = "Match (g:Gin)-->(c:Combo)<--(t:Tonic), (ga:Garnish)-->(c), (rat:Rating) --> (c) WHERE g.name=~'(?i)^"+GIN+"' AND t.name=~'(?i)^"+TONIC+"' AND ga.name=~'(?i)^"+GARNISH+"'  RETURN rat.rating, rat.comment";
        }
        result = multiValueQuery(QUERY);
        return result;
    }

    //Type specific, Code convention not upheld, Slow because of the variable "val", does a scan of the "type".
    public void dataAdder(final String type, final String newName) {
        if(type.equals("Gin")||type.equals("Tonic")||type.equals("Garnish")){

            Value val = singleValueQuery("match (n:" + type + ") return count(*)");
            String q = "CREATE(" + type.substring(0,3).toLowerCase() + val.asInt()+":"+type+"{name: '"+newName+"' })";
            voidQuery(q);
            //System.out.println("Successfully added a " + type + " with the name " + newName + " to the database");
        }
        else{System.out.println("Invalid input on type, can only use Gin, Tonic or Garnish");}
    }

    //Good convenience method, non-requirement.
    public void deleteDatabase() {
        voidQuery("MATCH (n) DETACH DELETE n");
        System.out.println("Successfully deleted the database");
    }

    //Good convenience method, non-requirement.
    public void createDatabaseFromFile(String fileNmae) {
        String s = null;
        String q = "";
        try {
            s = App.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath().toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        for(int c = 0; c<3;c++){
            int i = s.lastIndexOf("/");
            s = s.substring(0,i);
        }
        //s = s.concat("/dataBaseWithComments.txt/");
        s = s.concat("/"+ fileNmae +".txt/");

        try(BufferedReader br = new BufferedReader(new FileReader(s))) {
            String line = br.readLine();
            StringBuilder sb = new StringBuilder();
            int counter = 0;
            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                //q = q.concat(sb.toString());
                //System.out.println(sb.toString());
                if(counter % 100 == 0){
                    voidQuery(sb.toString());
                    //q = "";
                    sb = new StringBuilder();
                }

                counter++;
                line = br.readLine();
            }
            voidQuery(sb.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Successfully created the database");
    }

    public void resetDatabase(App database, String fileName){
        database.deleteDatabase();
        database.createDatabaseFromFile(fileName);
    }

    public Value getAverageRating(final String GIN, final String TONIC, final String GARNISH) throws NoSuchRecordException {
        final String QUERY;
        Value result;

        if (GARNISH.equals("")) {
            QUERY = "Match (g:Gin)-->(c:Combo)<--(t:Tonic), (rat:Rating) --> (c)  WHERE g.name=~'(?i)^"+GIN+"' AND t.name=~'(?i)^"+TONIC+"' RETURN avg(rat.rating)";
        } else {
            QUERY = "Match (g:Gin)-->(c:Combo)<--(t:Tonic), (ga:Garnish)-->(c), (rat:Rating) --> (c) WHERE g.name=~'(?i)^"+GIN+"' AND t.name=~'(?i)^"+TONIC+"' AND ga.name=~'(?i)^"+GARNISH+"' RETURN avg(rat.rating)";
        }

        result = singleValueQuery(QUERY);
        if (result == null) throw new NoSuchRecordException("No matching record(s) found");
        return result;
    }

    private Value singleValueQuery(final String QUERY) throws NoSuchRecordException {
        Value result;
        try(Session session = driver.session()){

            result = session.writeTransaction(new TransactionWork<Value>() {
                @Override
                public Value execute(Transaction tx) {
                    String q = QUERY;
                    StatementResult res = tx.run(q);
                    Value val = res.next().get(0);
                    return val; }});
        }
        if (result == null) throw new NoSuchRecordException("No matching record(s) found");
        return result;
    }

    private QueryResult multiValueQuery(final String QUERY) {
        QueryResult result;
        try( Session session = driver.session() ) {

            result = session.writeTransaction(new TransactionWork<QueryResult>() {
                @Override
                public QueryResult execute(Transaction tx) {
                    QueryResult queryResult = new QueryResult();
                    StatementResult result = tx.run(QUERY);
                    Iterable<String> keyIterable = result.keys();
                    for (String key : keyIterable) {
                        queryResult.keys.add(key);
                    }
                    Iterable<Value> valueIterable;
                    Iterator<Value> valueIterator;
                    if (!result.hasNext()) {
                        return null;
                    }
                    while (result.hasNext()) {
                        valueIterable = result.next().values();
                        valueIterator = valueIterable.iterator();
                        while (valueIterator.hasNext()) {
                            try {
                                Value val = valueIterator.next();
                                addValue(queryResult, val);
                            } catch (org.neo4j.driver.v1.exceptions.value.Uncoercible e) {
                            }
                        }
                    }
                    return queryResult; }});
        }
        if (result == null) throw new NoSuchRecordException("No matching record(s) found");
        return result;
    }

    private void addValue(QueryResult queryResult, Value val){
        switch (val.type().name()) {
            case "STRING":
                queryResult.values.add(val.asString());
                break;
            case "INTEGER":
                queryResult.values.add(String.valueOf(val.asInt()));
                break;
            case "FLOAT":
                queryResult.values.add(String.valueOf(val.asFloat()));
                break;
            case "NODE":
                try {
                    Iterable<Value> nodeValues = val.asNode().values();
                    for (Value v : nodeValues) {
                        addValue(queryResult, v);
                    }
                } catch (org.neo4j.driver.v1.exceptions.value.Uncoercible e) {
                    //TODO: Maybe Insert code here for relationship values
                }
            case "DOUBLE":
                queryResult.values.add(String.valueOf(val.asDouble()));
                break;
                    //TODO: Maybe add default other values of other types
        }
    }
    private void voidQuery(final String QUERY) {
        try( Session session = driver.session() ) {

            session.writeTransaction(new TransactionWork<String>() {
                @Override
                public String execute(Transaction tx) {
                    StatementResult add = tx.run(QUERY);
                    return null; }});
        }
    }

    public Value getCommentAmount(final String COMBONAME) throws NoSuchRecordException{
        Value result;
        String query = "MATCH (n:Rating)-[r]->(b:Combo)" +
                " WHERE b.name=~'(?i)^" + COMBONAME + "'"
                + " RETURN COUNT(r)";
        result = singleValueQuery(query);
        return result;
    }

    public void createRating(final int RATING, final String COMMENT, final String COMBONAME) throws IllegalArgumentException, NoSuchRecordException{
        try{
            searchByName(COMBONAME,"Combo",false).values.contains(COMBONAME);
        } catch(NoSuchRecordException ex){
            System.err.println("No such Combination.");
        }
        if(!COMMENT.equals("") || !(RATING > 5) && !(RATING < 1)) {
            int amount = getCommentAmount(COMBONAME).asInt();
            String noSpace = COMBONAME.replace(" ", "");
            String comName = "comment " + amount + " for " + COMBONAME;
            String query = "CREATE (" + noSpace + "rating" + amount + ":Rating { name: '" + comName + "'," + " rating: " +
                    RATING + ", comment: '" + COMMENT + "', helpfuls: 0})";
            String relationQuery = "MATCH (a:Rating),(b:Combo) " + "WHERE a.name = '" + comName + "' AND b.name = '" + COMBONAME +
                    "' CREATE (a)-[r:Rating_For]->(b)";
            voidQuery(query);
            voidQuery(relationQuery);
        } else if(COMMENT.equals("")) {
            throw new IllegalArgumentException("The comment provided is empty.");
        } else {
            throw new IllegalArgumentException("The Rating provided is invalid.");
        }
    }

    public void createRating(final int RATING, final String COMMENT, final String COMBONAME, final String USERNAME) throws IllegalArgumentException, NoSuchRecordException{
        createRating(RATING,COMMENT,COMBONAME);
        int amount = getCommentAmount(COMBONAME).asInt();
        String userRatingQuery = "MATCH (a:User),(b:Rating) " +
                "WHERE a.name = '" + USERNAME + "' AND b.name = 'comment " + amount + " for " + COMBONAME +
                "' CREATE (a)-[r:Owner_Of]->(b)";
        voidQuery(userRatingQuery);
    }

    public void incrHelpful(final String COMNAME) throws NoSuchRecordException{
        try{
            searchByName(COMNAME,"Rating",false);
        } catch (NoSuchRecordException ex){
            System.err.println("No such Rating.");
        }
        String q = "MATCH (n:Rating) " +
                "WHERE n.name=~'(?i)^" + COMNAME + "' " +
                "SET n.helpfuls=n.helpfuls+1";
        voidQuery(q);
    }

    public void createUser(final String USERNAME) throws IllegalArgumentException{
        if(!searchByName(USERNAME,"User",false).values.contains(USERNAME)){
            String q = "CREATE (" + USERNAME + ":User { name: '" + USERNAME + "'})";
            voidQuery(q);
        } else {
            throw new IllegalArgumentException("Username is taken.");
        }
    }

    public QueryResult sortByHelpful(final String COMBONAME) throws NoSuchRecordException{
        String q = "MATCH (r:Rating)-->(c:Combo) " +
                "WHERE c.name=~'(?i)^" + COMBONAME + "' " +
                "RETURN r.rating, r.helpfuls, r.comment " +
                "ORDER BY r.helpfuls DESC";
        QueryResult res = multiValueQuery(q);
        return res;
    }

    public QueryResult getUserRatings(final String USERNAME) throws NoSuchRecordException{
        String query = "MATCH (u:User)-->(r:Rating)-->(c:Combo) " +
                "WHERE u.name='" + USERNAME + "' " +
                "RETURN c.name, r.rating, r.comment";
        QueryResult res = multiValueQuery(query);
        return res;
    }

    public Value getUserAmount(final String COMBONAME) throws NoSuchRecordException{
        String query = "MATCH (c:Combo)<--(r:Rating)<--(u:User) " +
                "WHERE c.name=~'(?i)^" + COMBONAME + "' " +
                "RETURN COUNT(DISTINCT u)";
        Value res = singleValueQuery(query);
        return res;
    }

    public void printValue(Value VALUE){
        switch (VALUE.type().name()) {
            case "STRING":
                System.out.println("Value: " + VALUE.asString());
                break;
            case "INTEGER":
                System.out.println("Value: " + VALUE.asInt());
                break;
            case "FLOAT":
                System.out.println("Value: " + VALUE.asFloat());
                break;
            case "NODE":
                try {
                    Iterable<Value> nodeValues = VALUE.asNode().values();
                    for (Value v : nodeValues) {
                        printValue(v);
                    }
                } catch (org.neo4j.driver.v1.exceptions.value.Uncoercible e) {
                    //TODO: Maybe Insert code here for relationship values
                }
            case "DOUBLE":
                System.out.println("Value: " + VALUE.asDouble());
                break;
            //TODO: Maybe add default other values of other types
        }
    }

    public void createCombination(final String NEWNAME, final String GIN, final String TONIC) throws IllegalArgumentException, NoSuchRecordException{
        if(searchByName(NEWNAME,"Combo",false).values.contains(NEWNAME)){
            throw new IllegalArgumentException("Combination already exists.");
        }
        try{
            searchByName(GIN,"Gin",false);
            searchByName(TONIC,"Tonic",false);
            String q = "MATCH (b:Combo)"
                    + " RETURN COUNT(b)";
            Value amount = singleValueQuery(q);

            q = "CREATE (com" + amount.asInt() + ":Combo { name: '" + NEWNAME + "' })";
            voidQuery(q);

            q = "MATCH (a:Gin),(b:Tonic),(d:Combo) " +
                    "WHERE a.name = '" + GIN + "' AND b.name = '" + TONIC + "' AND d.name='" + NEWNAME + "' " +
                    "CREATE (a)-[:IS_IN]->(d), (b)-[:IS_IN]->(d)";
            voidQuery(q);
        } catch(NoSuchRecordException ex){
            System.err.println("The Gin or Tonic provided does not exists.");
        }
    }

    public void createCombination(final String NEWNAME, final String GIN, final String TONIC, final String GARNISH) throws IllegalArgumentException, NoSuchRecordException{
        createCombination(NEWNAME, GIN, TONIC);
        try{
            searchByName(GARNISH,"Garnish",false);
            String q = "MATCH (c:Garnish),(d:Combo) " +
                    "WHERE c.name='" + GARNISH + "' AND d.name='" + NEWNAME + "' " +
                    "CREATE (c)-[:IS_IN]->(d)";
            voidQuery(q);
        } catch (NoSuchRecordException ex){
            System.err.println("The Garnish provided does not exist.");
        }
    }
/*
    public static void main( String... args )
    {
        App database = new App( "bolt://localhost:7687", "neo4j", "patrick123");


        //http://localhost:7474/browser/
        {
            //String[] result = database.dataQuery("MATCH (n)-[r]->(m) RETURN n,r,m;"); //Find all combinations and their components
            //String[] result = database.dataQuery("MATCH (gin:Gin {name: 'bobbys-gin'}) RETURN gin"); //Search

            //String[] result = database.dataAdder("MATCH (gin:Gin {name: 'bobbys-gin'}) RETURN gin", "Gin", "New Gin");
            //database.deleteDatabase();
            //database.createDatabaseFromFile();

            String fileName = "thiccdata";
            //String fileName = "dataBaseWithComments";
            //long time = System.currentTimeMillis();
            //database.resetDatabase(database, fileName);
            //long finish = System.currentTimeMillis();
            //System.out.println("It took: " + ((finish -time) /1000));
            //database.dataAdder("Gin", "New Gin");
            //database.dataAdder("Tonic", "New Tonic");
            //database.dataAdder("Garnish", "New Garnish");
            //database.createRating(5, "Super good stuff!", "The rice");
            //database.createRating(1, "GARBAGE!!!", "The rice");
            //database.incrHelpful("comment 0 for The rice");
            //database.createUser("Peter");
            //database.createRating(3,"Its alright i guess.", "The rice", "Peter");
            //database.createRating(1, "Test Rating", "The cat", "Joseph");
            //String[] result = database.dataAdder("MATCH (gin:Gin {name: 'bobbys-gin'}) RETURN gin", "Garnish", "New Garnish");
            //database.nicePrint(result);
            try {
                boolean testCaseSensitive = false;
                boolean showTime = true;
                if(testCaseSensitive==true && fileName.equals("thiccdata")) {
                    System.out.println("\nTesting searchByName, find gins starting with 'aN': ");
                    long startTime = System.currentTimeMillis();
                    QueryResult res = database.searchByName("aN", "Gin");
                    long stopTime = System.currentTimeMillis();
                    long searchByNameTime = stopTime - startTime;
                    //System.out.println(searchByNameTime);
                    res.nicePrint();

                    System.out.println("\n\nTesting getComboRating with case sensitivity: ");
                    startTime = System.currentTimeMillis();
                    res = database.getComboRating("iNverroche-gin", "fEver-Tree Drink Gift Pack", "fRied Onion");
                    stopTime = System.currentTimeMillis();
                    long getComboRatingTime = stopTime - startTime;
                    //System.out.println(getComboRatingTime);
                    res.nicePrint();

                    //dataAdder

                    System.out.println("\n\nTesting getAverageRating for case sensitivity: ");
                    startTime = System.currentTimeMillis();
                    System.out.println("Average rating: " + database.getAverageRating("iNverroche-gin", "fEver-Tree Drink Gift Pack", "fRied Onion"));
                    stopTime = System.currentTimeMillis();
                    long getAverageRatingTime = stopTime - startTime;
                    //System.out.println(getAverageRatingTime);

                    //getCommentAmount

                    //createRating*2

                    //incHelpful

                    //createUser

                    System.out.println("\n\nTesting sortByHelpful for 'ThE Cat': ");
                    startTime = System.currentTimeMillis();
                    res = database.sortByHelpful("ThE Cat");
                    stopTime = System.currentTimeMillis();
                    long sortByHelpfulTime = stopTime - startTime;
                    //System.out.println(sortByHelpfulTime);
                    res.nicePrint();

                    System.out.println("\n\nTesting getUserRatings for 'miChAeL': ");
                    startTime = System.currentTimeMillis();
                    res = database.getUserRatings("miChAeL");
                    stopTime = System.currentTimeMillis();
                    long searchComboRatingsByUserTime = stopTime - startTime;
                    //System.out.println(searchComboRatingsByUserTime);
                    res.nicePrint();

                    System.out.println("\n\nTesting getUserAmount for 'ThE CaT': ");
                    startTime = System.currentTimeMillis();
                    Value resSingle = database.getUserAmount("ThE CaT");
                    stopTime = System.currentTimeMillis();
                    long getNumOfUsersByComboTime = stopTime - startTime;
                    //System.out.println(getNumOfUsersByComboTime);
                    //printValue(resSingle);

                    if(showTime==true){
                        System.out.println("\n\nPrinting times of executions");
                        System.out.println("searchByName: " + searchByNameTime);
                        System.out.println("getComboRating: " + getComboRatingTime);
                        System.out.println("getAverageRating: "+getAverageRatingTime);
                        System.out.println("sortByHelpful: "+sortByHelpfulTime);
                        System.out.println("getUserRatings: "+searchComboRatingsByUserTime);
                        System.out.println("getUserAmount: "+getNumOfUsersByComboTime);
                        System.out.println("\nAll: " + (searchByNameTime+getComboRatingTime+getAverageRatingTime+sortByHelpfulTime+searchComboRatingsByUserTime+getNumOfUsersByComboTime));
                    }

                }

                //QueryResult res = database.multiValueQuery("MATCH (n)-[r]->(m) RETURN n,r,m;"); //Find all combinations and their components

            } catch (org.neo4j.driver.v1.exceptions.NoSuchRecordException e) {
                System.out.println("No fucking records cunt");
            }
        }
    }*/
}