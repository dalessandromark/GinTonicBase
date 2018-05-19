package tonic;


import org.neo4j.driver.v1.Value;


public class AppTest
{
    private static App database;
    private static long start;


    private static void startTime(){
        start = System.currentTimeMillis();
    }

    private static void stopTime(String functionName) {
        long finish = System.currentTimeMillis();
        System.out.println("Running function "+functionName+" took "+(finish-start)+" ms");
    }
    public static boolean timeSearchByName(final String NAME, final String TYPE) {
        startTime();
        QueryResult res = database.searchByName(NAME, TYPE,true);
        stopTime("searchByName");
        return true;
    }

    public static boolean timegetComboRating(final String GIN, final String TONIC, final String GARNISH) {
        startTime();
        QueryResult res = database.getComboRating(GIN, TONIC, GARNISH);
        stopTime("getComboRating");
        return true;
    }

    public static boolean timeGetAverageRating(final String GIN, final String TONIC, final String GARNISH) {
        startTime();
        Value val = database.getAverageRating(GIN, TONIC, GARNISH);
        Float rating = val.asFloat();
        stopTime("getAverageRating");
        return true;
    }

    public static boolean timeGetCommentAmount(final String COMBONAME) {
        startTime();
        int res = database.getCommentAmount(COMBONAME).asInt();
        stopTime("getCommentAmount");
        return true;
    }

    public static boolean timeSortByHelpful(final String comboName){
        startTime();
        QueryResult res = database.sortByHelpful(comboName);
        stopTime("sortByHelpful");
        return true;
    }

    public static boolean timeSearchComboRatingsByUser(final String USERNAME){
        startTime();
        QueryResult res = database.getUserRatings(USERNAME);
        stopTime("getUserRatings");
        return true;
    }

    public static boolean timeGetNumOfUsersByCombo(final String COMBONAME){
        startTime();
        Value val = database.getUserAmount(COMBONAME);
        stopTime("getUserAmount");
        return true;
    }

    //Creates 3 new ingredient nodes, and 1 new rating node to the database.
    public static boolean nonReturnAcceptanceTest(){
        database.dataAdder("Gin", "New Gin");
        database.dataAdder("Tonic", "New Tonic");
        database.dataAdder("Garnish", "New Garnish");
        database.createRating(4,"Insert comment for a new rating here.","Combo 123","User 125");
        return true;
    }

    public static boolean returnAcceptanceTest(){
        QueryResult res = new QueryResult();

        System.out.println("Search for newly created ingredients and rating.");
        res = database.searchByName("New Gin", "Gin", true);
        res.nicePrint();
        res = new QueryResult();
        System.out.println();

        res = database.searchByName("New Tonic", "Tonic", true);
        res.nicePrint();
        res = new QueryResult();
        System.out.println();

        res = database.searchByName("New Garnish", "Garnish", true);
        res.nicePrint();
        res = new QueryResult();
        System.out.println();

        res = database.searchByName("comment 1","Rating", true);
        res.nicePrint();
        res = new QueryResult();
        System.out.println();

        System.out.println("Search for multiple Gins where 'Gin 1000' is the start of the name.");
        res = database.searchByName("Gin 1000", "Gin", true);
        res.nicePrint();
        res = new QueryResult();
        System.out.println();

        System.out.println("Search for all ratings given a set of ingredients.");
        res = database.getComboRating("Gin 123", "Tonic 123", "");
        res.nicePrint();
        res = new QueryResult();
        System.out.println();

        System.out.println("Average Rating on Combo123:\n" + database.getAverageRating("Gin 123", "Tonic 123", "Garnish 123"));
        System.out.println();

        Value val = database.getUserAmount("Combo 123");
        System.out.println("Number of users having rated Combo123:");
        database.printValue(val);
        System.out.println();

        System.out.println("Search all combinations rated by 'User 125' and the corresponding ratings.");
        res = database.getUserRatings("User 125");
        res.nicePrint();
        res = new QueryResult();
        System.out.println();

        System.out.println("Sort ratings by helpfuls on Combo 123.");
        res = database.sortByHelpful("Combo 123");
        res.nicePrint();
        res = new QueryResult();
        System.out.println();

        System.out.println("Increment the helpfuls on one of the ratings and sort again.");
        database.incrHelpful("comment 1 for Combo 123");
        res = database.sortByHelpful("Combo 123");
        res.nicePrint();

        return true;
    }

    public static boolean speedTest(){
        timeDataAdder("Gin", "Gin 12345");
        timeSearchByName("Gin 1030","Gin");
        timegetComboRating("Gin 1000","Tonic 1000","Garnish 1000");
        timeGetAverageRating("Gin 1000","Tonic 1000","Garnish 1000");
        timeGetCommentAmount("Combo 1000");
        timeSortByHelpful("Combo 1000");
        timeSearchComboRatingsByUser("User 1000");
        timeGetNumOfUsersByCombo("Combo 1000");
        return true;
    }

    public static boolean timeDataAdder(final String TYPE, final String NEWNAME) {
        startTime();
        database.dataAdder(TYPE,NEWNAME);
        stopTime("dataAdder");
        return true;
    }

    public static void main(String[] args) {
        database = new App( "bolt://localhost:7687", "neo4j", "patrick123");
        long start = System.currentTimeMillis();
        database.resetDatabase(database,"thiccdata");
        System.out.println("Time on database reset: " + (System.currentTimeMillis()-start) + "ms");
        nonReturnAcceptanceTest();
        returnAcceptanceTest();
        speedTest();

    }
}
