package ea.sof.ms_answers.controller;

import ea.sof.ms_answers.entity.AnswerEntity;
import ea.sof.ms_answers.model.AnswerReqModel;
import ea.sof.ms_answers.repository.AnswerRepository;
import ea.sof.shared.entities.CommentEntity;
import ea.sof.shared.models.Answer;
import ea.sof.shared.models.Comment;
import ea.sof.shared.models.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/answers")
public class AnswerController {
    @Autowired
    AnswerRepository answerRepository;

    @GetMapping
    public ResponseEntity<?> getAllAnswers() {
        List<AnswerEntity> answerEntities = answerRepository.findAll();
        List<Answer> answers = answerEntities.stream().map(ans -> ans.toAnswerModel()).collect(Collectors.toList());

        Response response = new Response(true, "");
        response.getData().put("answers", answers);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{questionId}")
    public ResponseEntity<?> getAllAnswersByQuestionId(@PathVariable("questionId") String questionId) {
        List<AnswerEntity> answerEntities = answerRepository.findAnswerEntitiesByQuestionId(questionId);
        List<Answer> answers = answerEntities.stream().map(ans -> ans.toAnswerModel()).collect(Collectors.toList());

        Response response = new Response(true, "");
        response.getData().put("answers", answers);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/answerDetails/{answerId}")
    public ResponseEntity<?> getAnswerById(@PathVariable("answerId") String answerId) {
        AnswerEntity answerEntity = answerRepository.findById(answerId).orElse(null);
        if(answerEntity == null) {
            return ResponseEntity.status(404).body(new Response(false, "No match found"));
        }
        Response response = new Response(true, "");
        response.getData().put("answer", answerEntity);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{questionId}")
    public ResponseEntity<?> createAnswer(@RequestBody @Valid AnswerReqModel answerReqModel, @PathVariable("questionId") String questionId) {
        AnswerEntity answerEntity = new AnswerEntity(answerReqModel);
        answerEntity.setQuestionId(questionId);
        //todo: answerEntity.setUserId();
        Response response = new Response(true, "Answer has been created");
        answerEntity = answerRepository.save(answerEntity);
        response.getData().put("answer", answerEntity);
        return ResponseEntity.status(201).body(response);
    }

    @PatchMapping("/answerDetails/{answerId}/upvote")
    public ResponseEntity<?> upVote(@PathVariable("answerId") String answerId) {
        AnswerEntity answerEntity = answerRepository.findById(answerId).orElse(null);
        if(answerEntity == null) {
            return ResponseEntity.status(404).body(new Response(false, "No match found"));
        }
        answerEntity.upvote();
        answerEntity = answerRepository.save(answerEntity);
        Response response = new Response(true, "Answer upVoted");
        response.getData().put("answer", answerEntity);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/answerDetails/{answerId}/downvote")
    public ResponseEntity<?> downVote(@PathVariable("answerId") String answerId) {
        //
        AnswerEntity answerEntity = answerRepository.findById(answerId).orElse(null);
        if(answerEntity == null) {
            return ResponseEntity.status(404).body(new Response(false, "No match found"));
        }
        answerEntity.downvote();
        answerEntity = answerRepository.save(answerEntity);
        Response response = new Response(true, "Answer downVoted");
        response.getData().put("answer", answerEntity);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/answerDetails/{answerId}")
    public ResponseEntity<?> updateAnswer(@PathVariable("answerId") String answerId, @RequestBody @Valid AnswerReqModel answerReqModel) {
        AnswerEntity answerEntity = answerRepository.findById(answerId).orElse(null);
        if(answerEntity == null) {
            return ResponseEntity.status(404).body(new Response(false, "No match found"));
        }
        AnswerEntity newAnswerEntity = new AnswerEntity(answerReqModel);
        answerEntity.setBody(newAnswerEntity.getBody());
        answerEntity.setLastEdited(newAnswerEntity.getCreated());
        answerEntity = answerRepository.save(answerEntity);
        Response response = new Response(true, "Answer updated");
        response.getData().put("answer", answerEntity);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/answerDetails/{answerId}")
    public ResponseEntity<?> deleteAnswer(@PathVariable("answerId") String answerId) {
        AnswerEntity answerEntity = answerRepository.findById(answerId).orElse(null);
        if(answerEntity == null) {
            return ResponseEntity.status(404).body(new Response(false, "No match found"));
        }
        answerRepository.delete(answerEntity);

        Response response = new Response(true, "Answer deleted");
        response.getData().put("answer", answerEntity);
        return ResponseEntity.ok(response);
    }
}
