package ea.sof.ms_answers.entity;

import ea.sof.ms_answers.model.AnswerReqModel;
import ea.sof.shared.entities.CommentAnswerEntity;
import ea.sof.shared.models.Answer;
import ea.sof.shared.queue_models.AnswerQueueModel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Data
@Document(collection = "answers")
public class AnswerEntity {
    @Id
    private String id;
    private String userId;
    private String body;
    private LocalDateTime created;
    private LocalDateTime lastEdited;
    private Integer votes = 0;
    private List<CommentAnswerEntity> topComments = new ArrayList<>();
    private String questionId;

    public AnswerEntity(AnswerReqModel answerReqModel) {
        this.body = answerReqModel.getBody();
        this.created = LocalDateTime.now();
        this.lastEdited = this.created;

        //Todo: Add user id from logged in user token
    }

    public Answer toAnswerModel() {
        Answer answerModel = new Answer();
        answerModel.setId(this.id);
        answerModel.setBody(this.body);
        answerModel.setDate(this.created);
        answerModel.setUpvotes(this.votes);
        answerModel.setUserId(this.userId);
        answerModel.setQuestionId(this.questionId);

        return answerModel;
    }

    public AnswerQueueModel toAnswerQueueModel() {
        AnswerQueueModel answerQueueModel = new AnswerQueueModel();
        /*answerModel.setId(this.id);
        answerModel.setBody(this.body);
        answerModel.setDate(this.created);
        answerModel.setUpvotes(this.votes);
        answerModel.setUserId(this.userId);
        answerModel.setQuestionId(this.questionId);

        return answerModel;*/
        return null;
    }

    public void upvote(){
        this.votes++;
    }
    public void downvote(){
        this.votes--;
    }
}
