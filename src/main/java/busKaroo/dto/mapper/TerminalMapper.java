package busKaroo.dto.mapper;

import busKaroo.dto.response.TerminalResponse;
import busKaroo.model.Terminal;
import org.springframework.stereotype.Component;

@Component
public class TerminalMapper {
    public TerminalResponse toResponse(Terminal terminal) {
        return new TerminalResponse(
            terminal.getId(),
            terminal.getName(),
            terminal.getCity(),
            terminal.getCountry()
        );
    }
}
