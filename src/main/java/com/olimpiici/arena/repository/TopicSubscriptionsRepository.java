package com.olimpiici.arena.repository;

import com.olimpiici.arena.domain.TopicSubscriptions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface TopicSubscriptionsRepository extends
        JpaRepository<TopicSubscriptions, Long> {
}