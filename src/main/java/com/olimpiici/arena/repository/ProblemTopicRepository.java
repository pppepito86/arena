package com.olimpiici.arena.repository;

import com.olimpiici.arena.domain.ProblemTopic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface ProblemTopicRepository extends JpaRepository<ProblemTopic, Long> {
}