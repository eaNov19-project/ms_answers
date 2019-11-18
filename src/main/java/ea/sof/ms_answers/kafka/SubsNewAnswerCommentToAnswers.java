package ea.sof.ms_answers.kafka;

import com.google.gson.Gson;
import ea.sof.ms_answers.entity.AnswerEntity;
import ea.sof.ms_answers.repository.AnswerRepository;

import ea.sof.shared.entities.CommentAnswerEntity;
import ea.sof.shared.entities.CommentQuestionEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class SubsNewAnswerCommentToAnswers {

	@Autowired
	AnswerRepository answerRepository;

	@KafkaListener(topics = "${topicNewAnswerComment}", groupId = "${subsNewAnswerCommentToAnswers}")
	public void newCommentAnswerEntity(String message) {

		System.out.println("SubsNewAnswerCommentToAnswers: New message from topic: " + message);

		Gson gson = new Gson();
		CommentAnswerEntity commentAnswerEntity =  gson.fromJson(message, CommentAnswerEntity.class);

		String answerId = commentAnswerEntity.getAnswerId();

		AnswerEntity answerEntity = answerRepository.findById(answerId).orElse(null);

		if(answerEntity != null) {
			answerEntity.addAnswerComment(commentAnswerEntity);
			answerRepository.save(answerEntity);
			System.out.println("Answer Comment added");
		}

	}

}
