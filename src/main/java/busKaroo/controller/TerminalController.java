package busKaroo.controller;

import busKaroo.dto.mapper.TerminalMapper;
import busKaroo.dto.response.TerminalResponse;
import busKaroo.service.TerminalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/terminals")
public class TerminalController {
    private final TerminalService terminalService;
    private final TerminalMapper terminalMapper;

    public TerminalController(TerminalService terminalService, TerminalMapper terminalMapper) {
        this.terminalService = terminalService;
        this.terminalMapper = terminalMapper;
    }

    @GetMapping
    public ResponseEntity<List<TerminalResponse>> getAllTerminals() {
        return ResponseEntity.ok(
            terminalService.findAll().stream().map(terminalMapper::toResponse).toList()
        );
    }
}
