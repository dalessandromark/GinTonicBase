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

    public void printGreeting( final String message )
    {
        try ( Session session = driver.session() )
        {
            String greeting = session.writeTransaction( new TransactionWork<String>()
            {
                @Override
                public String execute( Transaction tx )
                {
                    StatementResult result = tx.run( "CREATE (a:Greeting) " +
                                    "SET a.message = $message " +
                                    "RETURN a.message + ', from node ' + id(a)",
                            parameters( "message", message ) );
                    return result.single().get( 0 ).asString();
                }
            } );
            System.out.println( greeting );
        }
    }
    public String[] dataQuery(final String QUERY) {
        String ress;
        String[] resultArray;

        try( Session session = driver.session() ) {

            ress = session.writeTransaction(new TransactionWork<String>() {
                @Override
                public String execute(Transaction tx) {
                    String resultString = "";
                    String keys = "";
                    String values = "";
                    StatementResult result = tx.run(QUERY);
                    Iterable<String> keyIterable = result.keys();
                    for (String key : keyIterable) {
                        keys = keys.concat(key+" ");
                    }
                    Iterable<Value> valueIterable;
                    Iterator<Value> valueIterator;
                    while (result.hasNext()) {
                        valueIterable = result.next().values();
                        valueIterator = valueIterable.iterator();
                        while (valueIterator.hasNext()) {
                            try {
                                Iterable<Value> nodeValues = valueIterator.next().asNode().values();
                                for (Value v : nodeValues) {
                                    values = values.concat(v.asString()+" ");
                                }
                            } catch (org.neo4j.driver.v1.exceptions.value.Uncoercible e) {
                                //TODO: Maybe Insert code here for relationship values
                            }

                        }
                        values = values.concat("%");
                        }
                    resultString = resultString.concat(keys);
                    resultString = resultString.concat("#");
                    resultString = resultString.concat(values);
                    return resultString; }});
        }
        resultArray = ress.split("#");
        return resultArray;
    }

    public void testQuery(){

        try( Session session = driver.session() ) {

            String ress = session.writeTransaction(new TransactionWork<String>() {
                @Override
                public String execute(Transaction tx) {
                    StatementResult result = tx.run("MATCH (tom {name: \"Tom Hanks\"}) RETURN tom");
                    Iterable<Value> sit = result.single().get(0).asNode().values();
                    Iterator<Value> iterator = sit.iterator();
                    while (iterator.hasNext()) {
                        Value val = iterator.next();
                        /*
                        if (val.asObject().getClass().toString().equals("Integer") ) {
                            patrick = patrick.concat(String.valueOf(val.asInt()));
                        }
                        else{
                            patrick = patrick.concat(val.asString());
                        }
                    }
                    return patrick;
                    */
                        System.out.println(val.asObject().toString());
                    }
                    return "Ayy";
                }
            });
            System.out.println(ress);
        }
    }
    public void nicePrint(String[] resultList) {
        System.out.println(resultList[0]);
        String[] values = resultList[1].split("%");
        for (String val : values) {
            System.out.println(val);
        }
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
        //match (n:Gin) return count(*)       returns amount of Gin
        String askIfThisNeedsToBeHere;

        try( Session session = driver.session() ) {

            askIfThisNeedsToBeHere = session.writeTransaction(new TransactionWork<String>() {
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
                    //StatementResult amountFinder = tx.run("match (n:" + type + ") return count(*)");
                    //int amount = Integer.parseInt(amountFinder.next().values().get(0).toString());
                    //System.out.println(amount);
                    //String q = "CREATE(" + type.substring(0,3).toLowerCase() + amount+":"+type+"{name: '"+newName+"' })";
                    //String q = "MATCH (n) DETACH DELETE n";
                    //System.out.println(q);
                    //StatementResult add = tx.run(q);
                    System.out.println("Successfully created the database");
                    String t = "";
                    return t; }});

        }
    }
    public void resetDatabase(App database){
        database.deleteDatabase();
        database.createDatabaseFromFile();
    }

    public void createNewRating(final int rating, final String comment, final String comboName){


        String amount;
        try(Session session = driver.session()){
            amount = session.writeTransaction(new TransactionWork<String>() {
                @Override
                public String execute(Transaction tx) {

                    String q = "MATCH (n:Rating)-[r]->(b:Combo)" +
                            " WHERE b.name='" + comboName + "'"
                            + " RETURN COUNT(r)";
                    StatementResult add = tx.run(q);
                    Value a = add.next().get(0);
                    System.out.println("Amount of comments: " + a);
                    return a.toString(); }});
        }

        String noSpace = comboName.replace(" ","");
        String comName = "'comment " + amount + " for " + comboName +
        "'";
        String query = "CREATE ("+ noSpace + "rating" + amount + ":Rating { name: " + comName + "," + " rating: " + rating + ", comment: '" + comment + "', helpfuls: 0})" ;
        dataQuery(query);
        System.out.println("Comment node has been submitted to the database.");
        //nicePrint(dataReturn);
        String relationQuery = "MATCH (a:Rating),(b:Combo) " + "WHERE a.name = " + comName + " AND b.name = '" + comboName +
                "' CREATE (a)-[r:ComboComment]->(b) RETURN type(r)";
        dataQuery(relationQuery);
        System.out.println("The Relation between rating and Combo has been created.");
        //nicePrint(relDataReturn);
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
                    System.out.println("WE DID IT BOIS!!!");
                    return q; }});

        }
    }
    //match (n:Tonic) return *      return all Tonics
    public static void main( String... args ) throws Exception
    {
        //try ( App database = new App( "bolt://localhost:7687", "neo4j", "marcel123" ) )
        try ( App database = new App( "bolt://localhost:7687", "neo4j", "patrick123" ) )
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
            //String[] result = database.dataAdder("MATCH (gin:Gin {name: 'bobbys-gin'}) RETURN gin", "Garnish", "New Garnish");
            //database.nicePrint(result);
            //greeter.testQuery();
        }
    }
}