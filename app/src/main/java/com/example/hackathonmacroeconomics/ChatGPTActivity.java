package com.example.hackathonmacroeconomics;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatGPTActivity extends AppCompatActivity {

    private EditText userInput;
    private Button sendButton;


    private static final String token ="";

    private static final String instruction = "You are a specialist information retrieval bot designed to work with attached PDF documents. To the questions I ask you, get the answers only from the files I provided and also provide information on from which PDF filename the information is extracted. The filename from which the information is extracted is very important. The response should have information regarding the query and at the end have a Citation section and give the filename in that place. Please, always strive to learn from the feedback given, adapting your approach to ensure that each summary better meets the expectations outlined. Your goal is not only to provide information but to evolve in your capacity to discern and communicate the essence of the PDF documents in the most effective manner possible. Make sure you take less than 9 seconds to get a response so prioritize the time limit too.";

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<Message> messageList = new ArrayList<>();

    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatgpt);

        userInput = findViewById(R.id.userInput);
        sendButton = findViewById(R.id.sendButton);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);

        chatAdapter = new ChatAdapter(messageList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String messageContent = userInput.getText().toString().trim();
                if (!messageContent.isEmpty()) {
                    Log.d("ChatGPTActivity", messageContent);

                    // Add user's message to the list
                    messageList.add(new Message(messageContent, true)); // true indicates user message
                    chatAdapter.notifyDataSetChanged();

                    // Send the message to ChatGPT
                    sendMessageToChatGPT();

                    userInput.setText(""); // Clear input after sending
                }
            }
        });
    }

    private void sendMessageToChatGPT() {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "gpt-3.5-turbo");

            JSONArray messages = new JSONArray();

            // Add system instruction
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", instruction);
            messages.put(systemMessage);

            // Add conversation history
            for (Message message : messageList) {
                JSONObject messageObj = new JSONObject();
                messageObj.put("role", message.isUser() ? "user" : "assistant");
                messageObj.put("content", message.getText());
                messages.put(messageObj);
            }

            jsonBody.put("messages", messages);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                // Optionally, handle the error on the UI thread
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    parseChatGPTResponse(responseData);
                } else {
                    Log.e("ChatGPTActivity", "Request failed: " + response.body().string());
                    // Optionally, handle the error on the UI thread
                }
            }
        });
    }

    private void parseChatGPTResponse(String responseData) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    JSONArray choices = jsonObject.getJSONArray("choices");
                    JSONObject firstChoice = choices.getJSONObject(0);
                    JSONObject message = firstChoice.getJSONObject("message");
                    String content = message.getString("content").trim();

                    // Add the ChatGPT response to the message list
                    messageList.add(new Message(content, false)); // false indicates ChatGPT message
                    chatAdapter.notifyDataSetChanged(); // Refresh RecyclerView

                    // Scroll to the bottom of the RecyclerView
                    chatRecyclerView.scrollToPosition(messageList.size() - 1);
                } catch (JSONException e) {
                    e.printStackTrace();
                    // Optionally, handle the error on the UI thread
                }
            }
        });
    }
}