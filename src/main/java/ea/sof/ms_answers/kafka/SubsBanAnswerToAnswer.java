package ea.sof.ms_answers.kafka;

import com.google.gson.Gson;
import ea.sof.ms_answers.entity.AnswerEntity;
import ea.sof.ms_answers.repository.AnswerRepository;
import ea.sof.shared.entities.CommentAnswerEntity;
import ea.sof.shared.queue_models.AnswerQueueModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(SubsBanAnswerToAnswer.class);

    @KafkaListener(topics = "${topicBanAnswer}", groupId = "${subsBanAnswerToAnswer}")
    public void newCommentAnswerEntity(String message) {

        LOGGER.info("SubsBanAnswerToAnswer: New message from topic: " + message);

        Gson gson = new Gson();
        AnswerQueueModel answerQueueModel = null;
        try {
            answerQueueModel =  gson.fromJson(message, AnswerQueueModel.class);
        }catch (Exception ex){
            LOGGER.warn("SubsBanAnswerToAnswer: failed to decode json to AnswerQueueModel object");
            return;
        }

        String answerId = answerQueueModel.getId();

        AnswerEntity answerEntity = null;

        try {
            answerEntity = answerRepository.findById(answerId).orElse(null);
        }catch (Exception ex){
            LOGGER.warn("SubsBanAnswerToAnswer: failed to retrieve AnswerEntity from AnswerRepository");
            return;
        }

        if(answerEntity != null) {
            answerEntity.setActive(0);
            answerRepository.save(answerEntity);
            LOGGER.info("Answer is banned");

            //sending topicUpdateBannedAnswer
            LOGGER.info("topicUpdateBannedAnswer:: sending");
            kafkaTemplate.send(env.getProperty("topicUpdateBannedAnswer"), gson.toJson(answerEntity.toAnswerQueueModel()));
        }
    }
}
