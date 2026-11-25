package com.resumevault;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.common.GoogleGenAiThinkingLevel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

@RestController
@RequestMapping("/api/ai")
@ConditionalOnProperty(name="app.ai-enabled", havingValue="true")
class AiController {
  private static final int MAX_TEXT_CHARS_PER_RESUME=6000;
  private static final Logger log=LoggerFactory.getLogger(AiController.class);
  private final ChatClient chat;
  AiController(ChatClient chat){this.chat=chat;}
  @PostMapping("/compare") Map<String,String> compare(@RequestBody Map<String,Object> body){
    Object rawCandidates=body.getOrDefault("resumes",List.of());
    if(!(rawCandidates instanceof List<?> candidates)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Provide two to four resumes to compare.");
    if(candidates.size()<2||candidates.size()>4) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose 2 to 4 resumes for comparison.");
    String resumes=candidates.stream().map(candidate->{
      if(!(candidate instanceof Map<?,?> resume)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Each resume must include a name and extracted text.");
      String text=Objects.toString(resume.get("text"),"");
      String name=Objects.toString(resume.get("name"),"Resume");
      return name+"\n"+text.substring(0,Math.min(MAX_TEXT_CHARS_PER_RESUME,text.length()));
    }).reduce((a,b)->a+"\n\n---\n\n"+b).orElse("");
    if(candidates.stream().noneMatch(candidate->candidate instanceof Map<?,?> resume&&!Objects.toString(resume.get("text"),"").isBlank()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No resume text was extracted. Check the original files or configure OCR before asking Gemini to compare.");
    String criteria=Objects.toString(body.get("criteria"),"Compare projects by problem, contribution, architecture, technologies, scope, outcomes, and relevance. Explain differences and cite resume evidence.");
    try {
      String result=chat.prompt().options(GoogleGenAiChatOptions.builder().maxOutputTokens(1800).temperature(0.1).thinkingLevel(GoogleGenAiThinkingLevel.MINIMAL).build()).system("You are a concise, evidence-grounded resume project reviewer. Compare only the two to four supplied resumes against the user's criteria. From each resume, choose at most the three most relevant named projects. Return these Markdown sections in order: ## Quick read (two sentences); ## Project comparison (one compact table with candidate, project, best-fit evidence, and limitation; quote only short exact phrases); ## Key differences (at most three bullets); ## Suggestions for [each candidate's name] (exactly three practical resume or project-presentation improvements per person, each tied to their own evidence; recommend adding metrics only when the person can verify them); ## Gaps to verify (at most three bullets). Keep the whole answer under 700 words. Do not generate a separate score for every project criterion, repeat the resumes, invent evidence, evaluate protected traits, or recommend a hiring decision. If a name is unclear, use the supplied resume filename. Mark missing facts as unknown.")
        .user("Evaluation request:\n"+criteria+"\n\nCandidate resumes:\n"+resumes).call().content();
      if(result==null||result.isBlank()) throw new IllegalStateException("Gemini returned an empty response.");
      return Map.of("comparison",result);
    } catch(ResponseStatusException e) {
      throw e;
    } catch(Exception e) {
      log.error("Gemini resume comparison failed.",e);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,providerError(e));
    }
  }

  private String providerError(Throwable error) {
    for(Throwable cause=error;cause!=null;cause=cause.getCause()) {
      String message=Objects.toString(cause.getMessage(),"").toLowerCase(Locale.ROOT);
      if(message.contains("api key not valid")||message.contains("api_key_invalid"))
        return "Google Gemini rejected the API key. Replace GEMINI_API_KEY in .env with a valid Google AI Studio key, then restart the app.";
      if(message.contains("quota")||message.contains("resource_exhausted"))
        return "Google Gemini quota was exceeded. Check the API project's quota or billing, then retry.";
      if(message.contains("high demand")||message.contains("overloaded")||message.contains("unavailable"))
        return "Google Gemini is temporarily busy. Wait a moment and retry the comparison.";
      if(message.contains("model")&&(message.contains("not found")||message.contains("not supported")||message.contains("no longer available")||message.contains("not available to new users")))
        return "Google Gemini did not recognize GEMINI_MODEL. Check the model name in .env, then restart the app.";
    }
    return "Gemini could not complete the comparison. Check the API key, model name, network connection, and Google AI quota, then retry.";
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<Map<String,String>> requestError(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode()).body(Map.of("error",Objects.toString(e.getReason(),"Invalid comparison request.")));
  }
}
