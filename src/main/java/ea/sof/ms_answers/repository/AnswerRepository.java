package ea.sof.ms_answers.repository;

import ea.sof.ms_answers.entity.AnswerEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AnswerRepository extends MongoRepository<AnswerEntity, String> {
    Optional<AnswerEntity> findById(String id);

    List<AnswerEntity> findAnswerEntitiesByQuestionId(String questionId);
}
