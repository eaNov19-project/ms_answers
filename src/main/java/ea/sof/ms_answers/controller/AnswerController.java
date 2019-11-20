package ea.sof.ms_answers.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import ea.sof.ms_answers.entity.AnswerEntity;
import ea.sof.ms_answers.model.AnswerReqModel;
import ea.sof.ms_answers.repository.AnswerRepository;
import ea.sof.ms_answers.service.AuthService;
import ea.sof.ms_answers.service.AuthServiceCircuitBreaker;
import ea.sof.shared.models.Answer;
import ea.sof.shared.models.Response;
import ea.sof.shared.models.TokenUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/answers")
@CrossOrigin
@Slf4j
public class AnswerController {
    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private Environment env;

    @Autowired
    AnswerRepository answerRepository;

    @Autowired
    AuthServiceCircuitBreaker authService;

    private Gson gson = new Gson();

    @GetMapping("/health")
    public ResponseEntity<?> index() {
        String host = "Unknown host";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return new ResponseEntity<>("Answer service. Host: " + host, HttpStatus.OK);
    }

    @GetMapping("/question/{questionId}")
    public ResponseEntity<?> getAllAnswersByQuestionId(@PathVariable("questionId") String questionId) {
        List<AnswerEntity> answerEntities = answerRepository.findAnswerEntitiesByQuestionIdAndActiveEquals(questionId, 1);
        List<Answer> answers = answerEntities.stream().map(ans -> ans.toAnswerModel()).collect(Collectors.toList());

        Response response = new Response(true, "");
        response.getData().put("answers", answers);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{answerId}")
    public ResponseEntity<?> getAnswerById(@PathVariable("answerId") String answerId) {
        AnswerEntity answerEntity = answerRepository.findById(answerId).orElse(null);
        if (answerEntity == null) {
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(authCheckResp);
        }

        ObjectMapper mapper = new ObjectMapper();
        TokenUser decodedToken = mapper.convertValue(authCheckResp.getData().get("decoded_token"), TokenUser.class);
        AnswerEntity answerEntity = new AnswerEntity(answerReqModel);
        answerEntity.setQuestionId(questionId);
        answerEntity.setUserId(decodedToken.getUserId().toString());
        answerEntity.setUserEmail(decodedToken.getEmail());

        Response response = new Response();
        try {
            answerEntity = answerRepository.save(answerEntity);
            response.setSuccess(true);
            response.setMessage("Answer has been created");
            response.addObject("answer", answerEntity.toAnswerModel());
            log.info("CreateAnswer :: Saved successfully. " + answerEntity.toString());

            //sending topicNewAnswer
            log.info("topicNewAnswer:: sending new answer");
            kafkaTemplate.send(env.getProperty("topicNewAnswer"), gson.toJson(answerEntity.toAnswerQueueModel()));
        } catch (Exception ex) {
            response.setSuccess(false);
            response.setMessage(ex.getMessage());
            log.warn("CreateAnswer :: Error. " + ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{answerId}/upvote")
    public ResponseEntity<?> upVote(@PathVariable("answerId") String answerId, HttpServletRequest request) {
        log.info("Upvote :: for answer: " + answerId);
        //Check if request is authorized
        Response authCheckResp = isAuthorized(request.getHeader("Authorization"));
        if (!authCheckResp.getSuccess()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(authCheckResp);
        }

        AnswerEntity answerEntity = answerRepository.findById(answerId).orElse(null);
        if (answerEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, "No match found"));
        }
        Response response = new Response();
        try {
            answerEntity.upvote();
            answerEntity = answerRepository.save(answerEntity);
            response = new Response(true, "Answer upVoted");
            response.addObject("answer", answerEntity.toAnswerModel());

            log.info("Upvote :: Saved successfully. " + answerEntity.toString());
            //sending topicAnswerVoted
            log.info("topicAnswerVoted:: sending - upvote");
            kafkaTemplate.send(env.getProperty("topicAnswerVoted"), gson.toJson(answerEntity.toAnswerQueueModel()));
        } catch (Exception ex) {
            response.setSuccess(false);
            response.setMessage(ex.getMessage());
            log.warn("Upvote :: Error. " + ex.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{answerId}/downvote")
    public ResponseEntity<?> downVote(@PathVariable("answerId") String answerId, HttpServletRequest request) {

        log.info("downvote :: for answer: " + answerId);
        //Check if request is authorized
        Response authCheckResp = isAuthorized(request.getHeader("Authorization"));

        if (!authCheckResp.getSuccess()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(authCheckResp);
        }

        AnswerEntity answerEntity = answerRepository.findById(answerId).orElse(null);
        if (answerEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, "No answer match found"));
        }
        Response response = new Response();
        try {
            answerEntity.downvote();
            answerEntity = answerRepository.save(answerEntity);
            response = new Response(true, "Answer downvoted");
            //response.getData().put("answer", answerEntity.toAnswerModel());
            response.addObject("answer", answerEntity.toAnswerModel());
            log.info("Downvote :: Saved successfully. " + answerEntity.toString());

            //sending topicAnswerVoted
            log.info("topicAnswerVoted:: sending - downvote");
            kafkaTemplate.send(env.getProperty("topicAnswerVoted"), gson.toJson(answerEntity.toAnswerQueueModel()));
        } catch (Exception ex) {
            response.setSuccess(false);
            response.setMessage(ex.getMessage());
            log.warn("Upvote :: Error. " + ex.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{answerId}")
    public ResponseEntity<?> updateAnswer(@PathVariable("answerId") String answerId, @RequestBody @Valid AnswerReqModel answerReqModel, HttpServletRequest request) {
        log.info("update content :: for answer: " + answerId);
        //Check if request is authorized
        Response authCheckResp = isAuthorized(request.getHeader("Authorization"));
        if (!authCheckResp.getSuccess()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(authCheckResp);
        }

        AnswerEntity answerEntity = answerRepository.findById(answerId).orElse(null);
        if (answerEntity == null) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, "No match answer found"));
        }
        AnswerEntity newAnswerEntity = new AnswerEntity(answerReqModel);
        Response response = new Response();
        try {
            answerEntity.setBody(newAnswerEntity.getBody());
            answerEntity.setLastEdited(newAnswerEntity.getCreated());
            answerEntity = answerRepository.save(answerEntity);
            response = new Response(true, "Answer updated");
            response.addObject("answer", answerEntity.toAnswerModel());
            log.info("Saved successfully. " + answerEntity.toString());

        } catch (Exception ex) {
            response.setSuccess(false);
            response.setMessage(ex.getMessage());
            log.warn("Update Content :: Error. " + ex.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{answerId}")
    public ResponseEntity<?> deleteAnswer(@PathVariable("answerId") String answerId, HttpServletRequest request) {
        System.out.println("\nDelete :: Answer: " + answerId);
        //Check if request is authorized
        Response authCheckResp = isAuthorized(request.getHeader("Authorization"));
        if (!authCheckResp.getSuccess()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(authCheckResp);
        }

        AnswerEntity answerEntity = answerRepository.findById(answerId).orElse(null);
        if (answerEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, "No match found"));
        }
        Response response = new Response();
        try {
            answerRepository.delete(answerEntity);

            response = new Response(true, "Answer deleted");
            //response.getData().put("answer", answerEntity.toAnswerModel());
            response.addObject("answer", answerEntity.toAnswerModel());
            System.out.println("Deleted successfully. " + answerEntity.toString());
        } catch (Exception ex) {
            response.setSuccess(false);
            response.setMessage(ex.getMessage());
            System.out.println("Delete Answer :: Error. " + ex.getMessage());
        }

        return ResponseEntity.ok(response);
    }



    //****************FOR SERVICES*****************//

    @GetMapping("/{answerId}/entity")
    public ResponseEntity<?> getAnswerEntityById(@PathVariable("answerId") String answerId) {
        //TODO: add token authentication for services
        AnswerEntity answerEntity = answerRepository.findById(answerId).orElse(null);
        if (answerEntity == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, "No match found"));
        }
//        Response response = new Response(true, "");
//        response.getData().put("answer", answerEntity);
        return ResponseEntity.ok(answerEntity);
    }

    @GetMapping("/top5/{questionId}")
    public ResponseEntity<?> getTopFiveAnswers(@PathVariable("questionId") String questionId) {
        List<AnswerEntity> answerEntities = answerRepository.findAnswerEntitiesByQuestionIdAndActiveEquals(questionId, 1);
        answerEntities = answerEntities.stream().sorted(Comparator.comparingInt(AnswerEntity::getVotes).reversed()).limit(5).collect(Collectors.toList());
//        Response response = new Response(true, "");
//        response.getData().put("answers", answerEntities);
        return ResponseEntity.ok(answerEntities);
    }

    private Response isAuthorized(String authHeader) {
        System.out.print("JWT :: Checking authorization... ");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("Invalid token. Header null or 'Bearer ' is not provided.");
            return new Response(false, "Invalid token");
        }
        try {
            System.out.print("Calling authService.validateToken... ");
            ResponseEntity<Response> result = authService.validateToken(authHeader);

            System.out.print("AuthService replied... ");
            if (!result.getBody().getSuccess()) {
                System.out.println("Filed to authorize. JWT is invalid");
                return new Response(false, "Invalid token");
            }

            System.out.println("Authorized successfully");
            return result.getBody();

        } catch (Exception e) {
            System.out.println("Failed. " + e.getMessage());
            return new Response(false, "exception", e);
        }
    }
}
