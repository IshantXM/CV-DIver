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
  private static final String COMPARISON_PROMPT="""
      You are a concise, fair resume and project reviewer. Compare the supplied candidates only against the user's criteria and the evidence in their resumes.

      Return these Markdown sections in order:
      ## Quick read — two sentences stating the strongest evidence and which projects best match the requested criteria.
      ## Shared rubric and ratings — use the same four dimensions for every candidate: relevance to the criteria (35 points), technical depth and design (25), demonstrated ownership (20), and validation or outcomes (20). Give each candidate a score out of each dimension's maximum and a weighted total out of 100. Keep scores internally consistent and cite one short resume fact supporting each total. Scores describe evidence in these resumes against this request; they are not hiring probabilities or measures of a person's potential.
      ## Stage context — when study stages are explicitly stated and differ, add one neutral sentence that they are used only to calibrate suggestion scope; school name and year do not affect ratings.
      ## Project comparison — include at most three relevant named projects per candidate in a compact table with candidate, project, fit rating out of 100, best evidence, and limitation. Base the rating on the same criteria for every project. Do not invent missing evidence.
      ## Key differences — at most three concise bullets.
      ## Suggestions for [each candidate's name] — exactly three modest, practical next steps for each resume, tied to that person's evidence and current study stage when explicitly stated. For an earlier-year undergraduate, keep suggestions achievable within a semester or a small project iteration. For a final-year undergraduate, emphasize realistic finishing, validation, ownership clarity, and presentation improvements. Do not assume a school or year if the resume does not state it.
      ## Gaps to verify — at most three bullets.

      Fairness rules: never add to or subtract from ratings because of university name, perceived institution prestige, year of study, or seniority. Use stated study stage only to calibrate the size and timing of suggestions, never to lower expectations of evidence or infer ability. Do not make assumptions about an institution's tier. Apply identical score definitions to every candidate. Evaluate only project-related evidence; do not assess protected traits or recommend hiring decisions. Use short exact evidence, mark missing facts unknown, and keep the entire response under 700 words. If a candidate's name is unclear, use the supplied resume filename.
      """;
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
      String result=chat.prompt().options(GoogleGenAiChatOptions.builder().maxOutputTokens(1800).temperature(0.1).thinkingLevel(GoogleGenAiThinkingLevel.MINIMAL).build()).system(COMPARISON_PROMPT)
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
