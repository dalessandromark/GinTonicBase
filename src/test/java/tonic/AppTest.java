package tonic;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.neo4j.driver.v1.Value;
import sun.awt.SunHints;

/**
 * Unit test for simple App.
 */
public class AppTest
{
    private static App database;
    private static long start;

    /**
     * Rigorous Test :-)
     */
    @Test
    public static void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    private static void startTime(){
        start = System.currentTimeMillis();
    }

    private static void stopTime(String functionName) {
        long finish = System.currentTimeMillis();
        System.out.println("Running function "+functionName+" took "+(finish-start)+" ms");
    }
    public static boolean timeSearchByName(final String NAME, final String TYPE) {
        startTime();
        QueryResult res = database.searchByName(NAME, TYPE);
        stopTime("searchByName");
        return true;
    }

    public static boolean timeSearchCombinationByIngredients(final String GIN, final String TONIC, final String GARNISH) {
        startTime();
        QueryResult res = database.searchCombinationByIngredients(GIN, TONIC, GARNISH);
        stopTime("searchCombinationByIngredients");
        return true;
    }

    public static boolean timeGetAverageRating(final String GIN, final String TONIC, final String GARNISH) {
        startTime();
        Float val = database.getAverageRating(GIN, TONIC, GARNISH);
        stopTime("getAverageRating");
        return true;
    }

    public static boolean timeGetCommentAmount(final String COMBONAME) {
        startTime();
        int res = database.getCommentAmount(COMBONAME);
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
        QueryResult res = database.searchComboRatingsByUser(USERNAME);
        stopTime("searchComboRatingsByUser");
        return true;
    }

    public static boolean timeGetNumOfUsersByCombo(final String COMBONAME){
        startTime();
        Value val = database.getNumOfUsersByCombo(COMBONAME);
        stopTime("getNumOfUsersByCombo");
        return true;
    }

    public static void main(String[] args) {
        database = new App( "bolt://localhost:7687", "neo4j", "patrick123");
        timeSearchByName("Gin 1030","Gin");
        timeSearchCombinationByIngredients("Gin 1000","Tonic 1000","Garnish 1000");
        timeGetAverageRating("Gin 1000","Tonic 1000","Garnish 1000");
        timeGetCommentAmount("Combo 1000");
        timeSortByHelpful("Combo 1000");
        timeSearchComboRatingsByUser("User 1000");
        timeGetNumOfUsersByCombo("Combo 1000");
    }
}
