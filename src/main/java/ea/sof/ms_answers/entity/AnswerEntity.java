package ea.sof.ms_answers.entity;

import ea.sof.ms_answers.model.AnswerReqModel;
import ea.sof.shared.entities.CommentAnswerEntity;
import ea.sof.shared.entities.CommentQuestionEntity;
import ea.sof.shared.models.Answer;
import ea.sof.shared.models.CommentAnswer;
import ea.sof.shared.models.CommentQuestion;
import ea.sof.shared.queue_models.AnswerQueueModel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private Integer active = 1;

    public AnswerEntity(AnswerReqModel answerReqModel) {
        this.body = answerReqModel.getBody();
        this.created = LocalDateTime.now();
        this.lastEdited = this.created;
    }

    public Answer toAnswerModel() {
        Answer answerModel = new Answer();
        answerModel.setId(this.id);
        answerModel.setBody(this.body);
        answerModel.setDate(this.created);
        answerModel.setUpvotes(this.votes);
        answerModel.setUserId(this.userId);
        answerModel.setQuestionId(this.questionId);
        answerModel.setActive(this.active);
        List<CommentAnswer> topComments = this.topComments.stream().map(ca -> ca.toCommentAnswerModel()).collect(Collectors.toList());
        answerModel.setTopComments(topComments);
        return answerModel;
    }

    public AnswerQueueModel toAnswerQueueModel() {
        AnswerQueueModel answerQueueModel = new AnswerQueueModel();
        answerQueueModel.setId(this.id);
        answerQueueModel.setBody(this.body);
        answerQueueModel.setActive(this.active);
        answerQueueModel.setQuestionId(this.questionId);
        return answerQueueModel;
    }

    public void upvote(){
        this.votes++;
    }
    public void downvote(){
        this.votes--;
    }

    public void addAnswerComment(CommentAnswerEntity commentAnswerEntity) {
        topComments.add(commentAnswerEntity);

        //remove the oldest
        while (topComments.size() > 3){
            topComments.remove(0);
        }
    }
}
