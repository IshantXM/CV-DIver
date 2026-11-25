package com.resumevault;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name="app.ai-enabled", havingValue="true")
class AiConfiguration {
  @Bean ChatClient resumeChatClient(
      @Value("${app.openai-api-key}") String apiKey,
      @Value("${app.openai-model}") String modelName) {
    var api=OpenAiApi.builder().apiKey(apiKey).build();
    var model=OpenAiChatModel.builder().openAiApi(api)
      .defaultOptions(OpenAiChatOptions.builder().model(modelName).temperature(0.1).build()).build();
    return ChatClient.builder(model).build();
  }
}
