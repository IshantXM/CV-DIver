package com.resumevault;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import com.google.genai.Client;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name="app.ai-enabled", havingValue="true")
class AiConfiguration {
  @Bean ChatClient resumeChatClient(
      @Value("${app.gemini-api-key}") String apiKey,
      @Value("${app.gemini-model}") String modelName) {
    var client=Client.builder().apiKey(apiKey).build();
    var model=GoogleGenAiChatModel.builder().genAiClient(client)
      .defaultOptions(GoogleGenAiChatOptions.builder().model(modelName).temperature(0.1).build()).build();
    return ChatClient.builder(model).build();
  }
}
