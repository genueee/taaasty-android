package ru.taaasty.ui.messages;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ru.taaasty.rest.model.conversations.Message;


public class UnsentMessageRepository {

    static private Map<Long, Map<String,Message>> messagesRepository = new HashMap<>();

    synchronized public static void addMessage(Message message) {
        long conversationId = message.conversationId;
        Map<String, Message> messagesFromConversationMap = messagesRepository.get(conversationId);
        if (messagesFromConversationMap == null) {
            messagesFromConversationMap = new HashMap<>();
            messagesRepository.put(message.conversationId, messagesFromConversationMap);
        }
        messagesFromConversationMap.put(message.uuid,message);
    }
    synchronized public static Map<String,Message> getUnsentMessagesMap(long conversationId){
        Map<String, Message> messagesFromConversationMap = messagesRepository.get(conversationId);
        if (messagesFromConversationMap == null) {
            messagesFromConversationMap = new HashMap<>();
        }
        return Collections.unmodifiableMap(messagesFromConversationMap);
    }
    synchronized public static Message[] getUnsentMessagesArray(long conversationId){
        Map<String, Message> messagesFromConversationMap = messagesRepository.get(conversationId);
        if (messagesFromConversationMap == null) {
            return new Message[0];
        }else{
            Message[] messageArray = new Message[messagesFromConversationMap.size()];
            messagesFromConversationMap.values().toArray(messageArray);
            return messageArray;
        }

    }
    synchronized public static void removeMessage(long conversationId,String messageUuid){
        Map<String, Message> messagesFromConversationMap = messagesRepository.get(conversationId);
        if (messagesFromConversationMap==null) return;
        messagesFromConversationMap.remove(messageUuid);
        if (messagesFromConversationMap.isEmpty()){
            messagesRepository.remove(conversationId);
        }
    }
}
