package tonic;


import org.neo4j.driver.v1.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Iterator;
import jdk.nashorn.internal.runtime.regexp.joni.Syntax;
import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;

import static org.neo4j.driver.v1.Values.NULL;

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
    public QueryResult searchByName(final String NAME, final String TYPE) throws org.neo4j.driver.v1.exceptions.NoSuchRecordException{
        final String QUERY = "MATCH (n:"+TYPE+") WHERE n.name=~'"+NAME+".*' RETURN n";
        return multiValueQuery(QUERY);
    }

    public QueryResult searchCombinationByIngredients(final String GIN, final String TONIC, final String GARNISH) throws org.neo4j.driver.v1.exceptions.NoSuchRecordException {
        final String QUERY;
        QueryResult result;

        if (GARNISH.equals("")) {
            QUERY = "Match (g:Gin)-->(c:Combo)<--(t:Tonic), (rat:Rating) --> (c) WHERE g.name='"+GIN+"' AND t.name='"+TONIC+"' RETURN rat.rating, rat.comment";
        } else {
            QUERY = "Match (g:Gin)-->(c:Combo)<--(t:Tonic), (ga:Garnish)-->(c), (rat:Rating) --> (c) WHERE g.name='"+GIN+"' AND t.name='"+TONIC+"' AND ga.name='"+GARNISH+"'  RETURN rat.rating, rat.comment";
        }
        result = multiValueQuery(QUERY);
        return result;
    }

    public void dataAdder(final String type, final String newName) {
        if(type.equals("Gin")||type.equals("Tonic")||type.equals("Garnish")){

            Value val = singleValueQuery("match (n:" + type + ") return count(*)");
            String q = "CREATE(" + type.substring(0,3).toLowerCase() + val.asInt()+":"+type+"{name: '"+newName+"' })";
            voidQuery(q);
            System.out.println("Successfully added a " + type + " with the name " + newName + " to the database");
        }
        else{System.out.println("Invalid input on type, can only use Gin, Tonic or Garnish");}
    }


    public void deleteDatabase() {
        voidQuery("MATCH (n) DETACH DELETE n");
        System.out.println("Successfully deleted the database");
    }

    public void createDatabaseFromFile() {
        String s = null;
        String q;
        try {
            s = App.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath().toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        for(int c = 0; c<3;c++){
            int i = s.lastIndexOf("/");
            s = s.substring(0,i);
        }
        s = s.concat("/dataBaseWithComments.txt/");

        try(BufferedReader br = new BufferedReader(new FileReader(s))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            q = sb.toString();
            voidQuery(q);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Successfully created the database");
    }

    public void resetDatabase(App database){
        database.deleteDatabase();
        database.createDatabaseFromFile();
    }

    public float getAverageRating(final String GIN, final String TONIC, final String GARNISH) throws org.neo4j.driver.v1.exceptions.NoSuchRecordException {
        final String QUERY;
        Float result;

        if (GARNISH.equals("")) {
            QUERY = "Match (g:Gin)-->(c:Combo)<--(t:Tonic), (rat:Rating) --> (c)  WHERE g.name='"+GIN+"' AND t.name='"+TONIC+"'RETURN avg(rat.rating)";
        } else {
            QUERY = "Match (g:Gin)-->(c:Combo)<--(t:Tonic), (ga:Garnish)-->(c), (rat:Rating) --> (c) WHERE g.name='"+GIN+"' AND t.name='"+TONIC+"' AND ga.name='"+GARNISH+"' RETURN avg(rat.rating)";
        }
        try( Session session = driver.session() ) {

            result = session.writeTransaction(new TransactionWork<Float>() {
                @Override
                public Float execute(Transaction tx) {
                    StatementResult result = tx.run(QUERY);
                    if (result.peek().values().get(0) == NULL) {
                        return null;
                    }
                    return result.single().get(0).asFloat();}});
        }
        if (result == null) throw new org.neo4j.driver.v1.exceptions.NoSuchRecordException("No matching record(s) found");
        return result;
    }

    private Value singleValueQuery(final String QUERY) throws org.neo4j.driver.v1.exceptions.NoSuchRecordException {
        Value result;
        try(Session session = driver.session()){
            result = session.writeTransaction(new TransactionWork<Value>() {
                @Override
                public Value execute(Transaction tx) {
                    String q = QUERY;
                    StatementResult add = tx.run(q);
                    Value a = add.next().get(0);
                    return a; }});
        }
        if (result == null) throw new org.neo4j.driver.v1.exceptions.NoSuchRecordException("No matching record(s) found");
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
                                addValue(queryResult, val, valueIterator);
                            } catch (org.neo4j.driver.v1.exceptions.value.Uncoercible e) {
                            }
                        }
                    }
                    return queryResult; }});
        }
        if (result == null) throw new org.neo4j.driver.v1.exceptions.NoSuchRecordException("No matching record(s) found");
        return result;
    }

    private void addValue(QueryResult queryResult, Value val, Iterator<Value> valueIterator){
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
                        addValue(queryResult, v, valueIterator);
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

    public int getCommentAmount(final String COMBONAME){
        String query = "MATCH (n:Rating)-[r]->(b:Combo)" +
                            " WHERE b.name='" + COMBONAME + "'"
                            + " RETURN COUNT(r)";
        Value amount = singleValueQuery(query);
        return amount.asInt();
    }

    public void createNewRating(final int RATING, final String COMMENT, final String COMBONAME){
        int amount = getCommentAmount(COMBONAME);
        String noSpace = COMBONAME.replace(" ","");
        String comName = "comment " + amount + " for " + COMBONAME;
        String query = "CREATE ("+ noSpace + "rating" + amount + ":Rating { name: '" + comName + "'," + " rating: " +
                RATING + ", comment: '" + COMMENT + "', helpfuls: 0})" ;
        String relationQuery = "MATCH (a:Rating),(b:Combo) " + "WHERE a.name = '" + comName + "' AND b.name = '" + COMBONAME +
                "' CREATE (a)-[r:Rating_For]->(b)";
        voidQuery(query);
        voidQuery(relationQuery);
    }

    public void createNewRating(final int RATING, final String COMMENT, final String COMBONAME, final String USERNAME){
        int amount = getCommentAmount(COMBONAME);
        createNewRating(RATING,COMMENT,COMBONAME);
        String userRatingQuery = "MATCH (a:User),(b:Rating) " +
                "WHERE a.name = '" + USERNAME + "' AND b.name = 'comment " + amount + " for " + COMBONAME +
                "' CREATE (a)-[r:Owner_Of]->(b)";
        voidQuery(userRatingQuery);
    }

    public void incrHelpful(final String COMNAME){
        String q = "MATCH (n:Rating) " +
                "WHERE n.name='" + COMNAME + "' " +
                "SET n.helpfuls=n.helpfuls+1";
        voidQuery(q);
    }

    public void createNewUser(final String userName){
        String q = "CREATE (" + userName + ":User { name: '" + userName + "'})";
        voidQuery(q);
    }
    public QueryResult sortByHelpful(final String comboName){
        String q = "MATCH (r:Rating)-->(c:Combo) " +
                "WHERE c.name='" + comboName + "' " +
                "RETURN r.comment, r.rating,r.helpfuls " +
                "ORDER BY r.helpfuls DESC";
        QueryResult res = multiValueQuery(q);
        return res;
    }


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
            database.resetDatabase(database);
            //database.dataAdder("Gin", "New Gin");
            //database.dataAdder("Tonic", "New Tonic");
            //database.dataAdder("Garnish", "New Garnish");
            database.createNewRating(5, "Super good stuff!", "The rice");
            database.createNewRating(1, "GARBAGE!!!", "The rice");
            database.incrHelpful("comment 0 for The rice");
            database.createNewUser("Peter");
            database.createNewRating(3,"Its alright i guess.", "The rice", "Peter");
            //String[] result = database.dataAdder("MATCH (gin:Gin {name: 'bobbys-gin'}) RETURN gin", "Garnish", "New Garnish");
            //database.nicePrint(result);
            try {
                //QueryResult res = database.searchByName("juniper-gin", "Gin");
                //QueryResult res = database.searchCombinationByIngredients("juniper-gin", "Top Note Indian Tonic", "Olive oil");
                //QueryResult res = database.multiValueQuery("MATCH (n)-[r]->(m) RETURN n,r,m;"); //Find all combinations and their components
                QueryResult res = database.sortByHelpful("The cat");
                res.nicePrint();
                //System.out.println("Average rating: " + database.getAverageRating("juniper-gin", "Top Note Indian Tonic", "Olive oil"));
            } catch (org.neo4j.driver.v1.exceptions.NoSuchRecordException e) {
                System.out.println("No fucking records cunt");
            }

        }
    }
}