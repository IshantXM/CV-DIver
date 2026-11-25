package com.resumevault;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/ai")
@ConditionalOnProperty(name="app.ai-enabled", havingValue="true")
class AiController {
  private final ChatClient chat;
  AiController(ChatClient chat){this.chat=chat;}
  @PostMapping("/compare") Map<String,String> compare(@RequestBody Map<String,List<Map<String,String>>> body){
    String resumes=body.getOrDefault("resumes",List.of()).stream().map(r->r.getOrDefault("name","Resume")+"\n"+r.getOrDefault("text","").substring(0,Math.min(6000,r.getOrDefault("text","").length()))).reduce((a,b)->a+"\n\n---\n\n"+b).orElse("");
    String result=chat.prompt().system("Compare resumes against role-relevant evidence only. Do not infer or evaluate protected traits (age, race, gender, disability, religion, nationality, family status). Cite exact resume evidence, mark missing information as unknown, and avoid making a hiring decision. Return concise sections: Shared strengths, Distinct evidence, Gaps to verify, Follow-up questions.")
      .user("Compare these resumes:\n"+resumes).call().content();
    return Map.of("comparison",result);
  }
}
