package ea.sof.ms_answers.kafka;

import com.google.gson.Gson;
import ea.sof.ms_answers.controller.AnswerController;
import ea.sof.ms_answers.entity.AnswerEntity;
import ea.sof.ms_answers.repository.AnswerRepository;

import ea.sof.shared.entities.CommentAnswerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class SubsNewAnswerCommentToAnswers {
	private static final Logger LOGGER = LoggerFactory.getLogger(AnswerController.class);

	@Autowired
	AnswerRepository answerRepository;

	@KafkaListener(topics = "${topicNewAnswerComment}", groupId = "${subsNewAnswerCommentToAnswers}")
	public void newCommentAnswerEntity(String message) {
		LOGGER.info("SubsNewAnswerCommentToAnswers: New message from topic: " + message);

		Gson gson = new Gson();
		CommentAnswerEntity commentAnswerEntity =  gson.fromJson(message, CommentAnswerEntity.class);

		AnswerEntity answerEntity = answerRepository.findById(commentAnswerEntity.getAnswerId()).orElse(null);
		if(answerEntity != null) {
			answerEntity.addAnswerComment(commentAnswerEntity);
			answerRepository.save(answerEntity);
			LOGGER.info("Answer Comment added");
		} else {
			LOGGER.warn("Answer not found. Id: " + commentAnswerEntity.getAnswerId());
		}

	}

}
