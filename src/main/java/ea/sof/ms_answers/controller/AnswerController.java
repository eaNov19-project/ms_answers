package ea.sof.ms_answers.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import ea.sof.ms_answers.entity.AnswerEntity;
import ea.sof.ms_answers.model.AnswerReqModel;
import ea.sof.ms_answers.repository.AnswerRepository;
import ea.sof.ms_answers.service.AuthService;
import ea.sof.shared.models.Answer;
import ea.sof.shared.models.Response;
import ea.sof.shared.models.TokenUser;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/answers")
@CrossOrigin
public class AnswerController {
    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private Environment env;

    @Autowired
    AnswerRepository answerRepository;
    @Autowired
    AuthService authService;


    private Gson gson = new Gson();

    /*@GetMapping
    public ResponseEntity<?> getAllAnswers() {
        List<AnswerEntity> answerEntities = answerRepository.findAll();
        List<Answer> answers = answerEntities.stream().map(ans -> ans.toAnswerModel()).collect(Collectors.toList());

        Response response = new Response(true, "");
        response.getData().put("answers", answers);

        return ResponseEntity.ok(response);
    }*/

    @GetMapping("/question/{questionId}")
    public ResponseEntity<?> getAllAnswersByQuestionId(@PathVariable("questionId") String questionId) {
        List<AnswerEntity> answerEntities = answerRepository.findAnswerEntitiesByQuestionId(questionId);
        List<Answer> answers = answerEntities.stream().map(ans -> ans.toAnswerModel()).collect(Collectors.toList());

        Response response = new Response(true, "");
        response.getData().put("answers", answers);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{answerId}")
    public ResponseEntity<?> getAnswerById(@PathVariable("answerId") String answerId) {
        AnswerEntity answerEntity = answerRepository.findById(answerId).orElse(null);
        if(answerEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, "No match found"));
        }
        Response response = new Response(true, "");
        response.getData().put("answer", answerEntity.toAnswerModel());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{questionId}")
    public ResponseEntity<?> createAnswer(@RequestBody @Valid AnswerReqModel answerReqModel, @PathVariable("questionId") String questionId, HttpServletRequest request) {
        //Check if request is authorized
        Response authCheckResp = isAuthorized(request.getHeader("Authorization"));
        if (!authCheckResp.getSuccess()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Response(false, "Invalid Token"));
        }
       // TokenUser decodedToken = (TokenUser) authCheckResp.getData().get("decoded_token");

        ObjectMapper mapper = new ObjectMapper();
        TokenUser decodedToken = mapper.convertValue(authCheckResp.getData().get("decoded_token"), TokenUser.class);
        AnswerEntity answerEntity = new AnswerEntity(answerReqModel);
        answerEntity.setQuestionId(questionId);
        answerEntity.setUserId(decodedToken.getUserId().toString());

        Response response = new Response();
        try {
            answerEntity = answerRepository.save(answerEntity);
            //response.getData().put("answer", answerEntity.toAnswerModel());
            response.setSuccess(true);
            response.setMessage("Answer has been created");
            response.addObject("answer", answerEntity.toAnswerModel());
            System.out.println("CreateAnswer :: Saved successfully. " + answerEntity.toString());
            //send topicNewAnswer
            kafkaTemplate.send(env.getProperty("topicNewAnswer"), gson.toJson(answerEntity.toAnswerQueueModel()));
        } catch (Exception ex) {
            response.setSuccess(false);
            response.setMessage(ex.getMessage());
            System.out.println("CreateAnswer :: Error. " + ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{answerId}/upvote")
    public ResponseEntity<?> upVote(@PathVariable("answerId") String answerId, HttpServletRequest request) {
        System.out.println("\nUpvote :: for answer: " + answerId);
        //Check if request is authorized
        Response authCheckResp = isAuthorized(request.getHeader("Authorization"));
        if (!authCheckResp.getSuccess()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Response(false, "Invalid Token"));
        }

        AnswerEntity answerEntity = answerRepository.findById(answerId).orElse(null);
        if(answerEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, "No match found"));
        }
        Response response = new Response();
        try {
            answerEntity.upvote();
            answerEntity = answerRepository.save(answerEntity);
            response = new Response(true, "Answer upVoted");
            //response.getData().put("answer", answerEntity.toAnswerModel());
            response.addObject("answer", answerEntity.toAnswerModel());

            System.out.println("Upvote :: Saved successfully. " + answerEntity.toString());

            kafkaTemplate.send(env.getProperty("topicAnswerVoted"), gson.toJson(answerEntity.toAnswerQueueModel()));
        }
        catch (Exception ex) {
            response.setSuccess(false);
            response.setMessage(ex.getMessage());
            System.out.println("Upvote :: Error. " + ex.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{answerId}/downvote")
    public ResponseEntity<?> downVote(@PathVariable("answerId") String answerId,  HttpServletRequest request) {
        System.out.println("\ndownvote :: for answer: " + answerId);
        //Check if request is authorized
        Response authCheckResp = isAuthorized(request.getHeader("Authorization"));

        if (!authCheckResp.getSuccess()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Response(false, "Invalid Token"));
        }

        AnswerEntity answerEntity = answerRepository.findById(answerId).orElse(null);
        if(answerEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, "No answer match found"));
        }
        Response response = new Response();
        try {
            answerEntity.downvote();
            answerEntity = answerRepository.save(answerEntity);
            response = new Response(true, "Answer downvoted");
            //response.getData().put("answer", answerEntity.toAnswerModel());
            response.addObject("answer", answerEntity.toAnswerModel());
            System.out.println("Downvote :: Saved successfully. " + answerEntity.toString());

            kafkaTemplate.send(env.getProperty("topicAnswerVoted"), gson.toJson(answerEntity.toAnswerQueueModel()));
        } catch (Exception ex) {
            response.setSuccess(false);
            response.setMessage(ex.getMessage());
            System.out.println("Upvote :: Error. " + ex.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{answerId}")
    public ResponseEntity<?> updateAnswer(@PathVariable("answerId") String answerId, @RequestBody @Valid AnswerReqModel answerReqModel, @RequestHeader("Authorization") String token) {
        System.out.println("\nupdate content :: for answer: " + answerId);
        //Check if request is authorized
        Response authCheckResp = isAuthorized(token);
        if (!authCheckResp.getSuccess()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Response(false, "Invalid Token"));
        }

        AnswerEntity answerEntity = answerRepository.findById(answerId).orElse(null);
        if(answerEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, "No match answer found"));
        }
        AnswerEntity newAnswerEntity = new AnswerEntity(answerReqModel);
        Response response = new Response();
        try {
            answerEntity.setBody(newAnswerEntity.getBody());
            answerEntity.setLastEdited(newAnswerEntity.getCreated());
            answerEntity = answerRepository.save(answerEntity);
            response = new Response(true, "Answer updated");
            //response.getData().put("answer", answerEntity.toAnswerModel());
            response.addObject("answer", answerEntity.toAnswerModel());
            System.out.println("Saved successfully. " + answerEntity.toString());

        } catch (Exception ex) {
            response.setSuccess(false);
            response.setMessage(ex.getMessage());
            System.out.println("Update Content :: Error. " + ex.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{answerId}")
    public ResponseEntity<?> deleteAnswer(@PathVariable("answerId") String answerId, @RequestHeader("Authorization") String token) {
        System.out.println("\nDelete :: Answer: " + answerId);
        //Check if request is authorized
        Response authCheckResp = isAuthorized(token);
        if (!authCheckResp.getSuccess()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Response(false, "Invalid Token"));
        }

        AnswerEntity answerEntity = answerRepository.findById(answerId).orElse(null);
        if(answerEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, "No match found"));
        }
        Response response = new Response();
        try {
            answerRepository.delete(answerEntity);

            response = new Response(true, "Answer deleted");
            //response.getData().put("answer", answerEntity.toAnswerModel());
            response.addObject("answer", answerEntity.toAnswerModel());
            System.out.println("Deleted successfully. " + answerEntity.toString());
        }catch (Exception ex) {
            response.setSuccess(false);
            response.setMessage(ex.getMessage());
            System.out.println("Delete Answer :: Error. " + ex.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    private Response isAuthorized(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return new Response(false, "Invalid token");
        }
        try {
            ResponseEntity<Response> result = authService.validateToken(authHeader);

            if (!result.getBody().getSuccess()) {
                return new Response(false, "Invalid token");
            }
            return result.getBody();

        }catch (Exception e){
            return new Response(false, "exception", e);
        }
    }
}
