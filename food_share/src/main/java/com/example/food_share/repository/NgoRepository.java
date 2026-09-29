package com.example.food_share.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.food_share.model.Ngo;

public interface NgoRepository extends JpaRepository<Ngo, Long> {
}