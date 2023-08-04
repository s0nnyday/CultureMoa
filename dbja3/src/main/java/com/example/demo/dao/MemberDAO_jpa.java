package com.example.demo.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Member;
import com.example.demo.entity.Message;

import jakarta.transaction.Transactional;

@Repository
public interface MemberDAO_jpa extends JpaRepository<Member, String> {
	
	@Modifying
	@Transactional
	@Query(value = "delete from member where id=?1", nativeQuery = true)
	public void deleteById(String id);
	
	@Query(value = "select count(*) from member where id=?1", nativeQuery = true)
	public int countById(String id);
	Optional<Member> findById(String id);
}
