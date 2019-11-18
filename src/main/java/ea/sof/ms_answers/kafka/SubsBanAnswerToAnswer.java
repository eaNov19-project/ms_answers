package ea.sof.ms_answers.kafka;

import com.google.gson.Gson;
import ea.sof.ms_answers.entity.AnswerEntity;
import ea.sof.ms_answers.repository.AnswerRepository;
import ea.sof.shared.entities.CommentAnswerEntity;
import ea.sof.shared.queue_models.AnswerQueueModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class SubsBanAnswerToAnswer {
    @Autowired
    AnswerRepository answerRepository;
    @Autowired
    private Environment env;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;


    @KafkaListener(topics = "${topicBanAnswer}", groupId = "${subsBanAnswerToAnswer}")
    public void newCommentAnswerEntity(String message) {

        System.out.println("SubsBanAnswerToAnswer: New message from topic: " + message);

        Gson gson = new Gson();
        AnswerQueueModel answerQueueModel =  gson.fromJson(message, AnswerQueueModel.class);

        String answerId = answerQueueModel.getId();

        AnswerEntity answerEntity = answerRepository.findById(answerId).orElse(null);

        if(answerEntity != null) {
            answerEntity.setActive(0);
            answerRepository.save(answerEntity);
            System.out.println("Answer is banned");

            //sending topicUpdateBannedAnswer
            System.out.println("topicUpdateBannedAnswer:: sending");
            kafkaTemplate.send(env.getProperty("topicUpdateBannedAnswer"), gson.toJson(answerEntity.toAnswerQueueModel()));
        }
    }
}
