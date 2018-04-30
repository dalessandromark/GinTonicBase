package tonic;


import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.types.Node;
//import org.neo4j.driver.v1.types.Type;

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
                    Iterable<String> keyIterable;
                    Iterable<Value> valueIterable;
                    Iterator<String> keyIterator;
                    Iterator<Value> valueIterator;
                    String resultString = "";
                    String keys = "";
                    String values = "";
                    StatementResult result = tx.run(QUERY);
                    keyIterable = result.keys();
                    keyIterator = keyIterable.iterator();
                    while (keyIterator.hasNext()) {
                        keys = keys.concat(keyIterator.next()+" ");
                    }
                    while (result.hasNext()) {
                        valueIterable = result.next().values();
                        //valueIterable = resNode.values()
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
    public static void main( String... args ) throws Exception
    {
        try ( App database = new App( "bolt://localhost:7687", "neo4j", "marcel123" ) )
        {
            //String[] result = database.dataQuery("MATCH (n)-[r]->(m) RETURN n,r,m;"); //Find all combinations and their components
            String[] result = database.dataQuery("MATCH (gin:Gin {name: 'bobbys-gin'}) RETURN gin"); //Search
            database.nicePrint(result);
            //greeter.testQuery();
        }
    }
}