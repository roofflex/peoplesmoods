import javafx.util.Pair;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.api.methods.send.SendAudio;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.objects.PhotoSize;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.api.objects.inlinequery.result.InlineQueryResultAudio;
import org.telegram.telegrambots.api.objects.inlinequery.result.InlineQueryResultPhoto;
import org.telegram.telegrambots.api.objects.inlinequery.result.chached.InlineQueryResultCachedAudio;
import org.telegram.telegrambots.api.objects.inlinequery.result.chached.InlineQueryResultCachedPhoto;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PeoplesMoods extends TelegramLongPollingBot{
    public static void main(String[] args) {
        ApiContextInitializer.init();
        TelegramBotsApi botsApi=new TelegramBotsApi();
        try{
            botsApi.registerBot(new PeoplesMoods());
        } catch (TelegramApiException e){
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "PeoplesMoodsBot";
    }

    @Override
    public String getBotToken() {
        return "744583869:AAFO_IyUDu_LlAvkJoOQ0A-Vg7oS8Q-Sun8";
    }

    HashMap<Integer, Boolean>  creatingCollection = new HashMap<>();

    HashMap<Integer, String>  currentCollection = new HashMap<>();

    HashMap<Integer, Integer> currentMoodId = new HashMap<>();

    HashMap<Integer, Integer>  creatingMood = new HashMap<>();

    HashMap<Integer, Long> chatIdBuInlineMessageId = new HashMap<>();

    // HashMap<Integer, String>  = new HashMap<>();

    DatabaseManager databaseManager = DatabaseManager.getInstance();

    Integer userId;

    Long chatId;

    static final int MOOD_FAIL = 0;
    static final int MOOD_NAME = 1;
    static final int MOOD_PHOTO = 2;
    static final int MOOD_TRACK = 3;
    static final int MOOD_COMPLETE = 7;



    @SuppressWarnings("deprecation")
    public void onUpdateReceived(Update update) {

        if(update.hasInlineQuery()){

            if(update.getInlineQuery().getQuery().length()>2) {
                String queryId = update.getInlineQuery().getId();
                userId = update.getInlineQuery().getFrom().getId();
                String queryText = update.getInlineQuery().getQuery();
                AnswerInlineQuery answerInlineQuery = new AnswerInlineQuery()
                        .setInlineQueryId(queryId)
                        .setResults(createInlineQueryResultList(queryText, userId))
                        .setPersonal(true);
                try {
                    answerInlineQuery(answerInlineQuery);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }


        }


        if(update.hasMessage()) {
            userId = update.getMessage().getFrom().getId();
            chatId = update.getMessage().getChat().getId();
        }

        if(update.hasMessage() && update.getMessage().isCommand()){
            String command = update.getMessage().getText();
            StringBuilder messageBuilder = new StringBuilder();

            SendMessage answer = new SendMessage();

            answer.setChatId(update.getMessage().getChat().getId());

            switch (command){
                case "/start":
                    String firstName = update.getMessage().getFrom().getFirstName();

                    messageBuilder.append("Привет, ").append(firstName).append("!").append("\n");
                    messageBuilder.append("Ты можешь создать коллекцию с помощью /createcollection");
                    break;

                case "/createcollection":
                    messageBuilder.append("Пожалуйста, напиши название коллекции.").append("\n");
                    messageBuilder.append("Рекомендую назвать коллекцию в честь человека, чьи фото ты будешь туда загружать.");
                    creatingCollection.put(userId, true);
                    break;

                case "/createmood":
                    if(!(currentCollection.get(userId).equals(""))){
                        messageBuilder.append("Напишите название настроения.");
                        creatingMood.put(userId, MOOD_NAME);
                    }
                    break;

            }

            answer.setText(messageBuilder.toString());

            try {
                execute(answer);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        if(update.hasMessage() && update.getMessage().hasText() && !(update.getMessage().getText().contains("/")) && !(update.getMessage().getFrom().getBot())) {
            if (creatingCollection.get(userId)) {             // User creates collection
                createCollection(update.getMessage().getText(), userId, chatId);
            }

            if (creatingMood.get(userId).equals(MOOD_NAME)) {
                setMoodName(update.getMessage().getText(), userId, chatId);
            }
        }

        if(update.hasMessage()) {
            if (creatingMood.get(userId).equals(MOOD_PHOTO)) {
                if (update.getMessage().hasPhoto()) {
                    String photoId = update.getMessage().getPhoto().get(0).getFileId();
                    setMoodPhoto(photoId, userId, chatId);
                }
            }

            if (creatingMood.get(userId).equals(MOOD_TRACK)) {
                String trackId = update.getMessage().getAudio().getFileId();
                setMoodTrack(trackId, userId, chatId);
            }
        }

        if(update.hasCallbackQuery() && update.getCallbackQuery().getData().contains("trackId")){
            SendAudio sendAudio = new SendAudio()
                    .setChatId(update.getCallbackQuery().getMessage().getChatId())
                    .setAudio(update.getCallbackQuery().getData().replace("trackId", ""));
            try{
                sendAudio(sendAudio);
            } catch (TelegramApiException e){
                e.printStackTrace();
            }
        }

        if(update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
            userId = chatId.intValue();
            switch (update.getCallbackQuery().getData()) {
                case "Menu":
                    SendMessage menuView = new SendMessage();
                    menuView.setChatId(chatId);
                    List<String> collectionsList = databaseManager.getUsersCollections(userId);
                    if(collectionsList.size()>0) {
                        menuView.setText("Выбирай коллекцию");
                        menuView.setReplyMarkup(createMenuView(userId, collectionsList));
                    } else {
                        menuView.setText("У вас нет коллекций :(");
                    }
                    try{
                        execute(menuView);
                    } catch (TelegramApiException e){
                        e.printStackTrace();
                    }
                    break;

                case "Collection":          //REMAKE!
                    sendCollectionView(chatId);
                    break;

                default:
                    if(databaseManager.checkIfCollection(update.getCallbackQuery().getData())){
                        sendCollectionView(chatId);
                        currentCollection.put(userId, update.getCallbackQuery().getData());
                    } else {
                        Pair<String, String> data = getMoodDataByName(update.getCallbackQuery().getData(), userId);
                        String photoId = data.getKey();
                        String trackId = data.getValue();
                        SendPhoto sendPhoto = new SendPhoto()
                                .setChatId(chatId)
                                .setPhoto(photoId);
                        SendAudio sendAudio = new SendAudio()
                                .setChatId(chatId)
                                .setAudio(trackId);
                        try {
                            sendPhoto(sendPhoto);
                            sendAudio(sendAudio);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    }
            }
        }



    }

    public void createCollection(String collectionName, Integer targetUserId, Long targetChatId){
        SendMessage answer = new SendMessage();
        answer.setChatId(targetChatId);
        if(databaseManager.createCollection(targetUserId, collectionName)){
            answer.setText("Коллекция успешно создана! Можете добавить настроение с помощью /createmood");
            currentCollection.put(targetUserId, collectionName);
        } else {
            answer.setText("Ошибка при создании коллекции :(");
        }
        try{
            execute(answer);
        } catch (TelegramApiException e){
            e.printStackTrace();
        }
        creatingCollection.put(targetUserId, false);
    }

    public void setMoodName(String moodName, Integer targetUserId, Long targetChatId){
        SendMessage answer = new SendMessage();
        answer.setChatId(targetChatId);
        int moodId = databaseManager.createMood(currentCollection.get(targetUserId), moodName);
        if(!(moodId==MOOD_FAIL)){
            answer.setText("Теперь пришли мне фотографию.");
            creatingMood.put(targetUserId, MOOD_PHOTO);
            currentMoodId.put(targetUserId, moodId);
        } else {
            answer.setText("Ошибка при создании настроения :(");
        }
        try{
            execute(answer);
        } catch (TelegramApiException e){
            e.printStackTrace();
        }
    }

    public void setMoodPhoto(String photoId, Integer targetUserId, Long targetChatId){
        SendMessage answer = new SendMessage();
        answer.setChatId(targetChatId);
        int moodId = currentMoodId.get(targetUserId);
        moodId = databaseManager.setMoodPhoto(moodId, photoId);
        if(!(moodId==MOOD_FAIL)){
            answer.setText("Осталось скинуть мне трек.");
            creatingMood.put(targetUserId, MOOD_TRACK);
        } else {
            answer.setText("Ошибка при создании настроения :(");
        }
        try{
            execute(answer);
        } catch (TelegramApiException e){
            e.printStackTrace();
        }
    }

    public void setMoodTrack(String trackId, Integer targetUserId, Long targetChatId){
        SendMessage answer = new SendMessage();
        answer.setChatId(targetChatId);
        int moodId = currentMoodId.get(targetUserId);
        moodId = databaseManager.setMoodTrack(moodId, trackId);
        if(!(moodId==MOOD_FAIL)){

            InlineKeyboardMarkup moodCreatedMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> moodCreatedKeyboard=new ArrayList<>();
            List<InlineKeyboardButton> menu=new ArrayList<>();
            menu.add(new InlineKeyboardButton().setText("Главное Меню").setCallbackData("Menu"));
            List<InlineKeyboardButton> collection=new ArrayList<>();
            collection.add(new InlineKeyboardButton().setText("Текущая коллекция").setCallbackData("Collection"));
            moodCreatedKeyboard.add(menu);
            moodCreatedKeyboard.add(collection);
            moodCreatedMarkup.setKeyboard(moodCreatedKeyboard);

            answer.setText("Вы успешно создали настроение.");
            answer.setReplyMarkup(moodCreatedMarkup);
            creatingMood.put(targetUserId, MOOD_COMPLETE);
        } else {
            answer.setText("Ошибка при создании настроения :(");

        }
        try{
            execute(answer);
        } catch (TelegramApiException e){
            e.printStackTrace();
        }
    }

    public InlineKeyboardMarkup createCurrentCollectionView(Integer targetUserId){
        InlineKeyboardMarkup collectionViewKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> collectionView = new ArrayList<>();
        int collectionId = databaseManager.getCollectionId(currentCollection.get(targetUserId));
        List<String> moodsList = databaseManager.getMoodsByCollectionId(collectionId);
        if(moodsList.size()>0){
            for(int i=0; i<moodsList.size(); i++){
                List<InlineKeyboardButton> temporaryList = new ArrayList<>();
                temporaryList.add(new InlineKeyboardButton().setText(moodsList.get(i)).setCallbackData(moodsList.get(i)));
                collectionView.add(temporaryList);
            }
        }
        collectionViewKeyboardMarkup.setKeyboard(collectionView);
        return collectionViewKeyboardMarkup;
    }

    public List<InlineQueryResult> createInlineQueryResultList(String queryText, Integer targetUserId){
        List<InlineQueryResult> inlineQueryResultList = new ArrayList<>();
        int collectionId = databaseManager.getCollectionIdByPartOfName(queryText, targetUserId);
        if (collectionId>0){
            List<String> moodsList = databaseManager.getMoodsByCollectionId(collectionId);
            if(moodsList.size()>0){
                for(int i=0; i<moodsList.size(); i++){
                    Pair<String, String> temporaryPair = databaseManager.getMoodByName(moodsList.get(i), collectionId);

//                    InlineKeyboardMarkup downloadAudio = new InlineKeyboardMarkup();
//                    List<List<InlineKeyboardButton>> audioList = new ArrayList<>();
//                    List<InlineKeyboardButton> audio = new ArrayList<>();
//                    audio.add(new InlineKeyboardButton().setText("загрузить трек").setCallbackData("trackId"+temporaryPair.getValue()));
//                    audioList.add(audio);
//                    downloadAudio.setKeyboard(audioList);

                        InlineQueryResultCachedPhoto cachedPhoto = new InlineQueryResultCachedPhoto()
                                .setId((temporaryPair.getKey() + i))
                                .setPhotoFileId(temporaryPair.getKey())
                                .setDescription(moodsList.get(i));
//                            .setReplyMarkup(downloadAudio);
                        inlineQueryResultList.add(cachedPhoto);
                        InlineQueryResultCachedAudio cachedAudio = new InlineQueryResultCachedAudio()
                                .setId((temporaryPair.getValue() + i))
                                .setAudioFileId(temporaryPair.getValue());
//                            .setReplyMarkup(downloadAudio);
                        inlineQueryResultList.add(cachedAudio);
                }
            }

        }
        return inlineQueryResultList;
    }

    public Pair<String, String> getMoodDataByName(String moodName, Integer targetUserId){
        String collectionName = currentCollection.get(targetUserId);
        int collectionId = databaseManager.getCollectionId(collectionName);
        Pair<String,String> moodData = databaseManager.getMoodByName(moodName, collectionId);
        return moodData;
    }

    public InlineKeyboardMarkup createMenuView(Integer targetUserId, List<String> collectionsList){
        InlineKeyboardMarkup menuViewKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> menuView = new ArrayList<>();
        for(int i=0; i<collectionsList.size(); i++){
            List<InlineKeyboardButton> temporaryList = new ArrayList<>();
            temporaryList.add(new InlineKeyboardButton().setText(collectionsList.get(i)).setCallbackData(collectionsList.get(i)));
            menuView.add(temporaryList);
        }
        menuViewKeyboardMarkup.setKeyboard(menuView);
        return menuViewKeyboardMarkup;
    }

    public void sendCollectionView(Long targetChatId){
        SendMessage collectionView = new SendMessage();
        collectionView.setChatId(targetChatId);
        collectionView.setText("Вот, что тут есть:");
        collectionView.setReplyMarkup(createCurrentCollectionView(targetChatId.intValue()));
        try{
            execute(collectionView);
        } catch (TelegramApiException e){
            e.printStackTrace();
        }
    }


    }