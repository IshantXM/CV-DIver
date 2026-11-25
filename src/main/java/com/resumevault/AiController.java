package com.resumevault;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/ai")
@ConditionalOnProperty(name="app.ai-enabled", havingValue="true")
class AiController {
  private final ChatClient chat;
  AiController(ChatClient chat){this.chat=chat;}
  @PostMapping("/compare") Map<String,String> compare(@RequestBody Map<String,Object> body){
    @SuppressWarnings("unchecked") List<Map<String,String>> candidates=(List<Map<String,String>>)body.getOrDefault("resumes",List.of());
    if(candidates.size()<2||candidates.size()>4) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose 2 to 4 resumes for comparison.");
    String resumes=candidates.stream().map(r->{String text=Objects.toString(r.get("text"),"");return r.getOrDefault("name","Resume")+"\n"+text.substring(0,Math.min(6000,text.length()));}).reduce((a,b)->a+"\n\n---\n\n"+b).orElse("");
    String criteria=Objects.toString(body.get("criteria"),"Compare role-relevant strengths, experience, and gaps.");
    String result=chat.prompt().system("You are a careful resume comparison assistant. Compare only role-related evidence. Do not infer or evaluate protected traits such as age, race, gender, disability, religion, or family status. Turn the user's criteria into a concise rubric with explicit weights summing to 100. Score candidates against the same rubric. For every score, quote short exact evidence from that candidate's resume. Mark missing details as unknown. Never recommend a hiring decision. Return: Normalized rubric, Comparison matrix (candidate, criterion, score, exact evidence), Gaps to verify, Follow-up questions.")
      .user("Evaluation request:\n"+criteria+"\n\nCandidate resumes:\n"+resumes).call().content();
    return Map.of("comparison",result);
  }
}
