package lambda.chat.chatviz;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.Map;

public class ChatCall implements RequestHandler<Map<String, String>, String> {

    @Override
    public String handleRequest(Map<String, String> input, Context context) {
        String userInput = input.get("userInput");
        String response;
        try {
            MistralConnection mistral = new MistralConnection(userInput);
            response = mistral.taskEvaluation();
        } catch (Exception e) {
            response = "Erro: " + e.getMessage();
        }
        return response;
    }
    

    
}
