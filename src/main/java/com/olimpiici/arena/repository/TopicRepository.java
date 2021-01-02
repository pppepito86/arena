package com.olimpiici.arena.repository;

import com.olimpiici.arena.domain.Topic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {
}