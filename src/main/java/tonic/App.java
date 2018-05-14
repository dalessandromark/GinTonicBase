package tonic;


import org.neo4j.driver.v1.*;

import org.neo4j.driver.v1.types.Node;
//import org.neo4j.driver.v1.types.Type;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;

import static org.neo4j.driver.v1.Values.parameters;

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

    public QueryResult dataQuery(final String QUERY) throws org.neo4j.driver.v1.exceptions.NoSuchRecordException{
        final QueryResult queryResult;

        try( Session session = driver.session() ) {

            queryResult = session.writeTransaction(new TransactionWork<QueryResult>() {
                @Override
                public QueryResult execute(Transaction tx) {
                    QueryResult queryRes = new QueryResult();
                    StatementResult result = tx.run(QUERY);
                    Iterable<String> keyIterable = result.keys();
                    for (String key : keyIterable) {
                        queryRes.keys.add(key);
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
                                Iterable<Value> nodeValues = valueIterator.next().asNode().values();
                                for (Value v : nodeValues) {
                                    queryRes.values.add(v.asString());
                                }
                            } catch (org.neo4j.driver.v1.exceptions.value.Uncoercible e) {
                                //TODO: Maybe Insert code here for relationship values
                            }
                        }
                    }
                    return queryRes; }});
        }
        if (queryResult == null) throw new org.neo4j.driver.v1.exceptions.NoSuchRecordException("No matching record(s) found");
        return queryResult;
    }

    public QueryResult searchByName(final String NAME, final String TYPE) throws org.neo4j.driver.v1.exceptions.NoSuchRecordException{
        final String QUERY = "MATCH (n:"+TYPE+") WHERE n.name=~'"+NAME+".*' RETURN n";
        return dataQuery(QUERY);
    }

    public QueryResult searchCombinationByIngredients(final String GIN, final String TONIC, final String GARNISH) throws org.neo4j.driver.v1.exceptions.NoSuchRecordException {
        final String QUERY1, QUERY2;
        QueryResult result;

        if (GARNISH.equals("")) {
            QUERY1 = "Match (g:Gin)-->(c:Combo)<--(t:Tonic), (rat:Rating) --> (c) WHERE g.name='"+GIN+"' AND t.name='"+TONIC+"' RETURN rat.rating, rat.comment";
            QUERY2 = "Match (g:Gin)-->(c:Combo)<--(t:Tonic), (rat:Rating) --> (c)  WHERE g.name='"+GIN+"' AND t.name='"+TONIC+"'RETURN avg(rat.rating)";
        } else {
            QUERY1 = "Match (g:Gin)-->(c:Combo)<--(t:Tonic), (ga:Garnish)-->(c), (rat:Rating) --> (c) WHERE g.name='"+GIN+"' AND t.name='"+TONIC+"' AND ga.name='"+GARNISH+"'  RETURN rat.rating, rat.comment";
            QUERY2 = "Match (g:Gin)-->(c:Combo)<--(t:Tonic), (ga:Garnish)-->(c), (rat:Rating) --> (c) WHERE g.name='"+GIN+"' AND t.name='"+TONIC+"' AND ga.name='"+GARNISH+"' RETURN avg(rat.rating)";
        }

        try( Session session = driver.session() ) {

            result = session.writeTransaction(new TransactionWork<QueryResult>() {
                @Override
                public QueryResult execute(Transaction tx) {
                    QueryResult queryResult = new QueryResult();
                    StatementResult result = tx.run(QUERY1);
                    StatementResult result2 = tx.run(QUERY2);
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
                                queryResult.values.add(valueIterator.next().asInt()+"\t");
                                queryResult.values.add(valueIterator.next().asString());
                            } catch (org.neo4j.driver.v1.exceptions.value.Uncoercible e) {
                            }
                        }
                    }

                    if (!result2.hasNext()) {
                        return null;
                    }
                    queryResult.extraValues.add(String.valueOf(result2.single().get(0).asFloat()));

                    return queryResult; }});
        }
        if (result == null) throw new org.neo4j.driver.v1.exceptions.NoSuchRecordException("No matching record(s) found");
        return result;
    }

    public void dataAdder(final String type, final String newName) {
        //match (n:Gin) return count(*)       returns amount of Gin
        if(type.equals("Gin")||type.equals("Tonic")||type.equals("Garnish")){
            String askIfThisNeedsToBeHere;

            try( Session session = driver.session() ) {

                askIfThisNeedsToBeHere = session.writeTransaction(new TransactionWork<String>() {
                    @Override
                    public String execute(Transaction tx) {
                        StatementResult amountFinder = tx.run("match (n:" + type + ") return count(*)");
                        int amount = Integer.parseInt(amountFinder.next().values().get(0).toString());
                        //System.out.println(amount);
                        String q = "CREATE(" + type.substring(0,3).toLowerCase() + amount+":"+type+"{name: '"+newName+"' })";
                        //System.out.println(q);
                        StatementResult add = tx.run(q);
                        System.out.println("Successfully added a " + type + " with the name " + newName + " to the database");
                        return q; }});

            }
        }
        else{System.out.println("Invalid input on type, can only use Gin, Tonic or Garnish");}
    }
    public void deleteDatabase() {
        //match (n:Gin) return count(*)       returns amount of Gin
        String askIfThisNeedsToBeHere;

        try( Session session = driver.session() ) {

            askIfThisNeedsToBeHere = session.writeTransaction(new TransactionWork<String>() {
                @Override
                public String execute(Transaction tx) {

                    String q = "MATCH (n) DETACH DELETE n";
                    StatementResult add = tx.run(q);
                    System.out.println("Successfully deleted the database");
                    return q; }});

        }
    }
    public void createDatabaseFromFile() {
        try( Session session = driver.session() ) {

             session.writeTransaction(new TransactionWork<String>() {
                @Override

                public String execute(Transaction tx) {
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
                    s=s.concat("/base base base.txt/");

                    try(BufferedReader br = new BufferedReader(new FileReader(s))) {
                        StringBuilder sb = new StringBuilder();
                        String line = br.readLine();

                        while (line != null) {
                            sb.append(line);
                            sb.append(System.lineSeparator());
                            line = br.readLine();
                        }
                        q = sb.toString();
                        StatementResult add = tx.run(q);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Successfully created the database");
                    String t = "";
                    return t; }});

        }
    }
    public void resetDatabase(App database){
        database.deleteDatabase();
        database.createDatabaseFromFile();
    }

    public int getCommentAmount(final String comboName){
        int amount;
        try(Session session = driver.session()){
            amount = session.writeTransaction(new TransactionWork<Integer>() {
                @Override
                public Integer execute(Transaction tx) {

                    String q = "MATCH (n:Rating)-[r]->(b:Combo)" +
                            " WHERE b.name='" + comboName + "'"
                            + " RETURN COUNT(r)";
                    StatementResult add = tx.run(q);
                    Value a = add.next().get(0);
                    return a.asInt(); }});
        }
        return amount;
    }

    public void createNewRating(final int rating, final String comment, final String comboName){
        try(Session session = driver.session()){
            session.writeTransaction(new TransactionWork<String>() {
                @Override
                public String execute(Transaction tx) {
                    int amount = getCommentAmount(comboName);
                    String noSpace = comboName.replace(" ","");
                    String comName = "comment " + amount + " for " + comboName;
                    String query = "CREATE ("+ noSpace + "rating" + amount + ":Rating { name: '" + comName + "'," + " rating: " + rating + ", comment: '" + comment + "', helpfuls: 0})" ;
                    //System.out.println(query);
                    StatementResult run = tx.run(query);
                    //System.out.println("Comment node has been submitted to the database.");
                    String relationQuery = "MATCH (a:Rating),(b:Combo) " + "WHERE a.name = '" + comName + "' AND b.name = '" + comboName +
                            "' CREATE (a)-[r:Rating_For]->(b)";
                    run = tx.run(relationQuery);
                    //System.out.println("The Relation between rating and Combo has been created.");
                    return relationQuery;
                }
            });
        }
    }

    public void createNewRating(final int rating, final String comment, final String comboName, final String userName){
        try(Session session = driver.session()){
            session.writeTransaction(new TransactionWork<String>() {
                @Override
                public String execute(Transaction tx) {
                    int amount = getCommentAmount(comboName);
                    String noSpace = comboName.replace(" ","");
                    String comName = "comment " + amount + " for " + comboName;
                    String query = "CREATE ("+ noSpace + "rating" + amount + ":Rating { name: '" + comName + "'," + " rating: " + rating + ", comment: '" + comment + "', helpfuls: 0})" ;
                    //System.out.println(query);
                    StatementResult run = tx.run(query);

                    //System.out.println("Comment node has been submitted to the database.");
                    String relationQuery = "MATCH (a:Rating),(b:Combo) " + "WHERE a.name = '" + comName + "' AND b.name = '" + comboName +
                            "' CREATE (a)-[r:Rating_For]->(b)";
                    run = tx.run(relationQuery);
                    //System.out.println("The Relation between rating and Combo has been created.");
                    String userRatingQuery = "MATCH (a:User),(b:Rating) " +
                            "WHERE a.name = '" + userName + "' AND b.name = 'comment " + amount + " for " + comboName +
                            "' CREATE (a)-[r:Owner_Of]->(b)";
                    run = tx.run(userRatingQuery);
                    return userRatingQuery;
                }
            });
        }

    }

    public void incrHelpful(final String comName){

        try( Session session = driver.session() ) {

            session.writeTransaction(new TransactionWork<String>() {
                @Override
                public String execute(Transaction tx) {

                    String q = "MATCH (n:Rating) " +
                            "WHERE n.name='" + comName + "' " +
                            "SET n.helpfuls=n.helpfuls+1";
                    StatementResult add = tx.run(q);
                    //System.out.println("WE INCREMENTED IT BOIS!!!");
                    return q; }});

        }
    }

    public void createNewUser(final String userName){
        try( Session session = driver.session() ) {

            session.writeTransaction(new TransactionWork<String>() {
                @Override
                public String execute(Transaction tx) {

                    String q = "CREATE (" + userName + ":User { name: '" + userName + "'})";
                    StatementResult add = tx.run(q);
                    //System.out.println("New User Added to Database.");
                    return q; }});

        }
    }

    //match (n:Tonic) return *      return all Tonics
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
                QueryResult res = database.searchCombinationByIngredients("juniper-gin", "Top Note Indian Tonic", "Olive oil");
                //QueryResult res = database.dataQuery("MATCH (n)-[r]->(m) RETURN n,r,m;"); //Find all combinations and their components
                res.nicePrint();
            } catch (org.neo4j.driver.v1.exceptions.NoSuchRecordException e) {
                System.out.println("No fucking records cunt");
            }

        }
    }
}