package com.ecom.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecom.model.Carousel;

@Repository
public interface CarouselRepository extends JpaRepository<Carousel,Long> {
	
	 Optional<Carousel> findByPosition(int position);

}
