package lambda.chat.chatviz;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
public final class MistralConnection {

    private String text;
    private ArrayList<Object> embedding;

    public MistralConnection(String text) {
        this.text = text;
    }
    public MistralConnection(ArrayList<Object> embedding) {
        this.embedding = embedding;
    }
    
    private String prompt() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
               "<Rules>\n" +
               "    <Goals>\n" +
               "        The principal achive is search in database the best correspondency of indicators (Sales, Conversions and many others) to business teams that dont have a know how in data area. So we need create a funnel with user inputs to search the best result.\n" +
               "    </Goals>\n" +
               "    <Essentials>\n" +
               "        you have to follow this rules strictly. Never change return format. Do you need act like a human, so be natural. Try talk with user to catch good informartions, dont be a bot. \n" +
               "    </Essentials>\n" +
               "    <LanguageSettings>\n" +
               "        If the text is in english, translate to portugues. If you will return client response, you can return the same language that user input.\n" +
               "    </LanguageSettings>\n" +
               "    <TextComprehension>\n" +
               "        It tries to extract as much information as possible from the user, so we can have a more targeted search. When you have as much information as possible, create a perfect text for search. This text will go through a series of treatments and embeddings. We will have cases where the user needs to be more specific about the subject, so ask as muitas perguntas quanto necessário.\n" +
               "    </TextComprehension>\n" +
               "    <ReturnRules>\n" +
               "        We will have two types of returns, the customer return in which we will obtain more information from the user and the return in which you will generate a prompt that will be used to perform a semantic search in the database. You will only generate the second return when you have important information to proceed. Try to have a simple and information-rich prompt.\n" +
               "    </ReturnRules>\n" +
               "    <ReturnFormat>\n" +
               "        So we will have two returns, consider just return only what is inside the keys\n" +
               "        {'client':'AskText'}\n" +
               "        {'system_search':'PromptText'}\n" +
               "    </ReturnFormat>\n" +
               "</Rules>";
    }

    public String addPrompt(String promptText) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        
        Document doc;
        try {
            doc = dBuilder.parse(new ByteArrayInputStream(prompt().getBytes()));
            doc.getDocumentElement().normalize();
        } catch (Exception e) {
            throw new Exception("Erro ao carregar o documento XML: " + e.getMessage());
        }

        Element newPrompt = doc.createElement("prompt");
        newPrompt.setTextContent(promptText); 
        doc.getDocumentElement().appendChild(newPrompt);
        
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);

        String safePromptText = writer.toString().trim().replaceAll("[\\r\\n]+", "").replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>", "").replaceAll("[ ]+", " ");
        
        return safePromptText;
    }
    
    public String taskEvaluation() throws Exception {
        String url = "https://api.mistral.ai/v1/chat/completions";
        
        HttpClient client = HttpClient.newHttpClient();
        
        String requestBody = "{\n" +
                "    \"model\": \"mistral-large-latest\",\n" +
                "    \"messages\": [{\"role\": \"user\", \"content\": \"" + addPrompt(this.text) + "\"}]\n" +
                "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer j06isnvg1AsoVlZKQw5tolrsMtULddZA")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonElement jsonResponse = JsonParser.parseString(response.body());
            JsonObject jsonObject = jsonResponse.getAsJsonObject();
            String message = jsonObject.getAsJsonArray("choices")
                                       .get(0)
                                       .getAsJsonObject()
                                       .getAsJsonObject("message")
                                       .get("content")
                                       .getAsString();
            return message;
        } else {
            return "Nao foi possivel conversar com o Tersk " + response.statusCode();
        }
    }
    
}
