import javafx.util.Pair;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    //  Database credentials
    static final String DB_URL = "jdbc:postgresql://localhost:5432/moodsdb";
    static final String USER = "roofflex";
    static final String PASS = "TelegramDB";

    static final int COLLECTION_FAIL = 0;
    static final int MOOD_FAIL = 0;


    private static volatile DatabaseManager instance;
    private static volatile Connection connection;

    /**
     * Private constructor (due to Singleton)
     */
    private DatabaseManager() {
        try {
            connection = DriverManager
                    .getConnection(DB_URL, USER, PASS);

        } catch (SQLException e) {
            System.out.println("Connection Failed");
            e.printStackTrace();
            return;
        }
    }


    public static DatabaseManager getInstance() {
        final DatabaseManager currentInstance;
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
                currentInstance = instance;
            }
        } else {
            currentInstance = instance;
        }
        return currentInstance;
    }

    public static boolean createCollection(Integer userId, String collectionName){
        try {
            final PreparedStatement preparedStatement = connection.prepareStatement
                    ("INSERT INTO \"Collection User\" (collection, userid) VALUES(?, ?)");
            preparedStatement.setString(1, collectionName);
            preparedStatement.setInt(2, userId);
            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Collection creating failed");
        }
        return false;
    }

    public int createMood(String collectionName, String moodName){
        try {
            int collectionId = getCollectionId(collectionName);
            final PreparedStatement preparedStatement = connection.prepareStatement
                    ("INSERT INTO \"Collection Moods\" (collectionid, mood) VALUES(?, ?)");
            preparedStatement.setInt(1, collectionId);
            preparedStatement.setString(2, moodName);
            preparedStatement.executeUpdate();
            return getMoodId(collectionId, moodName);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Collection creating failed");
        }
        return MOOD_FAIL;
    }

    public int setMoodPhoto(int moodId,String photoId){
        try {
            final PreparedStatement preparedStatement = connection.prepareStatement
                    ("UPDATE \"Collection Moods\" SET photoid=? WHERE moodid=?");
            preparedStatement.setString(1, photoId);
            preparedStatement.setInt(2, moodId);
            preparedStatement.executeUpdate();
            return moodId;
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Mood creating failed");
        }
        return MOOD_FAIL;
    }

    public int setMoodTrack(int moodId,String trackId){
        try {
            final PreparedStatement preparedStatement = connection.prepareStatement
                    ("UPDATE \"Collection Moods\" SET trackid=? WHERE moodid=?");
            preparedStatement.setString(1, trackId);
            preparedStatement.setInt(2, moodId);
            preparedStatement.executeUpdate();
            return moodId;
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Mood creating failed");
        }
        return MOOD_FAIL;
    }

    public  int getCollectionId(String collectionName){
        try{
            final PreparedStatement preparedStatement = connection.prepareStatement
                    ("SELECT collectionid FROM \"Collection User\" WHERE collection=?");
            preparedStatement.setString(1, collectionName);
            final ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                return Integer.parseInt(resultSet.getString(1));
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return 0;
    }

    private int getMoodId(int collectionId, String moodName){
        try{
            final PreparedStatement preparedStatement = connection.prepareStatement
                    ("SELECT moodid FROM \"Collection Moods\" WHERE collectionid=? AND mood=?");
            preparedStatement.setInt(1, collectionId);
            preparedStatement.setString(2, moodName);
            final ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                return Integer.parseInt(resultSet.getString(1));
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return 0;
    }

    public List<String> getMoodsByCollectionId(int collectionId){
        List<String> moodsList = new ArrayList<>();
        try{
            final PreparedStatement preparedStatement = connection.prepareStatement
                    ("SELECT mood FROM \"Collection Moods\" WHERE collectionid=?");
            preparedStatement.setInt(1, collectionId);
            final ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()){
                moodsList.add(resultSet.getString(1));
            }
            return moodsList;
        } catch (SQLException e){
            e.printStackTrace();
        }
        return moodsList;
    }

    public Pair<String, String> getMoodByName(String moodName, int collectionId){
        try{
            final PreparedStatement preparedStatement = connection.prepareStatement
                    ("SELECT photoid, trackid FROM \"Collection Moods\" WHERE collectionid=? AND mood=?");
            preparedStatement.setInt(1, collectionId);
            preparedStatement.setString(2, moodName);
            final ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            Pair<String,String> moodData = new Pair<>(resultSet.getString(1), resultSet.getString(2));
            return moodData;
        } catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getUsersCollections(int userId){
        List<String> collectionsList = new ArrayList<>();
        try{
            final PreparedStatement preparedStatement = connection.prepareStatement
                    ("SELECT collection FROM \"Collection User\" WHERE userid=?");
            preparedStatement.setInt(1, userId);
            final ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()){
                collectionsList.add(resultSet.getString(1));
            }
            return collectionsList;
        } catch (SQLException e){
            e.printStackTrace();
        }
        return collectionsList;
    }

    public int getCollectionIdByPartOfName(String partOfName, int userId){
        try{
            final PreparedStatement preparedStatement = connection.prepareStatement
                    ("SELECT collection, collectionid FROM \"Collection User\" WHERE userid=?");
            preparedStatement.setInt(1, userId);
            final ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()){
                if(resultSet.getString(1).contains(partOfName)){
                    return Integer.parseInt(resultSet.getString(2));
                }
            }
            return COLLECTION_FAIL;
        } catch (SQLException e){
            e.printStackTrace();
        }
        return COLLECTION_FAIL;
    }

    public boolean checkIfCollection(String name){
        try{
            final PreparedStatement preparedStatement = connection.prepareStatement
                    ("SELECT * FROM \"Collection User\" WHERE collection=?");
            preparedStatement.setString(1, name);
            final ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                return true;
            } else {
                return false;
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    public boolean checkIfMood(String name){
        try{
            final PreparedStatement preparedStatement = connection.prepareStatement
                    ("SELECT * FROM \"Collection Moods\" WHERE mood=?");
            preparedStatement.setString(1, name);
            final ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                return true;
            } else {
                return false;
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return false;
    }
}

