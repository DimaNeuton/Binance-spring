package com.gav.binancespring.repository;

import com.gav.binancespring.entities.Balance;
import com.gav.binancespring.entities.LastBotCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LastBotConditionRepository extends JpaRepository<LastBotCondition, Integer> {

}
