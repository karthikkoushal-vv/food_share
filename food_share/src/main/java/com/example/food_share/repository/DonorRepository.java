package com.example.food_share.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.food_share.model.Donor;

public interface DonorRepository extends JpaRepository<Donor, Long> {
}