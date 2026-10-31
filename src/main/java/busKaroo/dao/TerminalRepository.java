package busKaroo.dao;

import busKaroo.model.Terminal;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TerminalRepository extends JpaRepository<Terminal, Long> {}
